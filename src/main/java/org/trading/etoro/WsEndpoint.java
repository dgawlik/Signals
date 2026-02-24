package org.trading.etoro;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.trading.signals.ticker.Candle;
import org.trading.signals.ticker.Ticker;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Singleton client connecting specifically to eToro's remote WebSocket APIs
 * to stream live ticking data.
 * Adheres to traditional Jakarta WebSocket message handling standards.
 */
public class WsEndpoint extends Endpoint implements MessageHandler.Whole<String> {

    private final String url;
    private final String etoroApiKey;
    private final String etoroUserKey;
    private final ObjectMapper om;
    private final CandlesEndpoint lookupEndpoint;
    private Map<Integer, String> instrumentIdToSymbol = new HashMap<>();
    private Map<String, Consumer<List<Candle>>> subscribers = new HashMap<>();
    private Session session;
    private static WsEndpoint singleton;
    private final CountDownLatch latch = new CountDownLatch(1);

    record EToroMessage(String id, String operation, Map data) {
    }

    record EToroTopicSubMessage(List<EToroSubMessage> messages) {
    }

    record EToroSubMessage(String topic, String content, String id, String type) {
    }

    /**
     * Lazily instantiates the single WsEndpoint instance configured securely via
     * environment variables.
     *
     * @return the active eToro WebSocket connection.
     */
    public static WsEndpoint ETORO() {
        Dotenv e = Dotenv.load();

        var publicKey = e.get("ETORO_PUBLIC_KEY");
        var apiKey = e.get("ETORO_API_KEY");

        if (singleton == null) {
            singleton = new WsEndpoint("wss://ws.etoro.com/ws", publicKey, apiKey);
        }

        return singleton;
    }

    private WsEndpoint(String url,
            String etoroApiKey,
            String etoroUserKey) {
        this.url = url;
        this.etoroApiKey = etoroApiKey;
        this.etoroUserKey = etoroUserKey;
        this.om = new ObjectMapper();
        this.lookupEndpoint = CandlesEndpoint.ETORO();

        ClientManager client = ClientManager.createClient();

        try {
            this.session = client.connectToServer(this, new URI(this.url));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Registers a consumer to be called whenever stream events (ticks) arrive for a
     * particular symbol.
     * Also issues backend json requests to tell the broker to begin streaming those
     * updates.
     *
     * @param id         The ticker symbol to subscribe to.
     * @param subscriber The consumer pushing transformed {@link Candle} updates
     *                   downstream.
     */
    public void subscribe(String id, Consumer<List<Candle>> subscriber) {
        this.subscribers.put(id, subscriber);

        var subData = new HashMap<String, Object>();
        subData.put("snapshot", false);

        var instrumentId = lookupEndpoint.lookupInstrumentId(id);

        if (instrumentId.isEmpty()) {
            throw new IllegalStateException("Some symbols not found");
        }

        instrumentIdToSymbol.put(instrumentId.get(), id);

        var subscriptions = List.of("instrument:" + instrumentId.get());

        subData.put("topics", subscriptions);

        var obj2 = new EToroMessage(UUID.randomUUID().toString(), "Subscribe", subData);
        var text2 = om.writeValueAsString(obj2);

        try {
            synchronized (this.latch) {
                this.latch.await();
            }
            session.getBasicRemote().sendText(text2);
        } catch (IOException e) {

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        try {
            session.addMessageHandler(this);

            var data = Map.of("userKey", etoroUserKey, "apiKey", etoroApiKey);
            var obj = new EToroMessage(UUID.randomUUID().toString(), "Authenticate", data);
            var text = om.writeValueAsString(obj);

            session.getBasicRemote().sendText(text);
            synchronized (this.latch) {
                this.latch.countDown();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onMessage(String s) {
        try {
            var obj = om.readValue(s, EToroTopicSubMessage.class);
            var update = obj.messages().stream().map((EToroSubMessage m) -> {
                var instrId = m.topic().replace("instrument:", "").trim();
                var symbol = instrumentIdToSymbol.get(Integer.parseInt(instrId));

                var content = om.readValue(m.content(), Map.class);
                var bid = content.get("Bid");
                var ask = content.get("Ask");

                var time = content.get("Date").toString().replace("Z", "");

                var close = 0.0;
                if (ask == null) {
                    close = Double.parseDouble(bid.toString());
                } else if (bid == null) {
                    close = Double.parseDouble(ask.toString());
                } else {
                    close = (Double.parseDouble(bid.toString()) + Double.parseDouble(ask.toString())) / 2.0;
                }

                var ldt = LocalDateTime.parse(time);

                var signal = Ticker.defaultOf(symbol);
                var candle = new Candle(ldt, close, close, close, close, Optional.empty());
                candle.setSignal(signal);

                return candle;
            }).collect(Collectors.groupingBy(Candle::getId));

            for (var entry : update.entrySet()) {
                var subscriber = this.subscribers.get(entry.getKey());

                if (subscriber != null) {
                    subscriber.accept(entry.getValue());
                }
            }
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            //
            // System.out.println("Passing: " + s);
        }

    }

}
