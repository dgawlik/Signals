package org.trading.etoro;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.SneakyThrows;
import org.trading.EventFrequency;
import org.trading.signals.ticker.Candle;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Restful HTTP client specifically implemented to parse and retrieve continuous
 * historical
 * {@link Candle} objects from eToro's public analytics APIs.
 */
public class CandlesEndpoint {

    private final String baseUrl;
    private final String etoroApiKey;
    private final String etoroUserKey;
    private final ObjectMapper om;

    /**
     * Statically factory constructs a default endpoint instance reading API/Host
     * keys from dotEnv variables.
     *
     * @return the configured historical endpoint connection.
     */
    public static CandlesEndpoint ETORO() {
        Dotenv e = Dotenv.load();

        var publicKey = e.get("ETORO_PUBLIC_KEY");
        var apiKey = e.get("ETORO_API_KEY");

        return new CandlesEndpoint("https://public-api.etoro.com", publicKey, apiKey);
    }

    public CandlesEndpoint(String baseUrl, String etoroApiKey, String etoroUserKey) {
        this.baseUrl = baseUrl;

        this.etoroApiKey = etoroApiKey;
        this.etoroUserKey = etoroUserKey;

        if (this.etoroApiKey == null || this.etoroUserKey == null) {
            throw new IllegalArgumentException("Etoro envirnoment keys not set");
        }

        this.om = new ObjectMapper();
    }

    /**
     * Executes a REST API hit against the downstream server to request
     * chronological candlesticks.
     * Performs JSON un-marshalling heavily nested API returns.
     *
     * @param symbol    The string symbol to fetch.
     * @param frequency The discrete resolution size (e.g. 5m, 1h).
     * @param count     Maximum amount of bars to capture sequentially.
     * @return The aggregated historical records mapped to internal Candle
     *         representations.
     */
    @SneakyThrows
    public List<Candle> fetch(String symbol, EventFrequency frequency, int count) {

        var instrumentId = lookupInstrumentId(symbol);

        if (instrumentId.isEmpty()) {
            throw new IllegalStateException("Instrument not found");
        }

        var url = "{baseUrl}/api/v1/market-data/instruments/{instrumentId}/history/candles/asc/{interval}/{count}"
                .replace("{baseUrl}", this.baseUrl)
                .replace("{instrumentId}", instrumentId.get().toString())
                .replace("{interval}", frequency.getName())
                .replace("{count}", count + "");

        var client = HttpClient.newHttpClient();

        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("x-api-key", this.etoroApiKey)
                .header("x-user-key", this.etoroUserKey)
                .header("x-request-id", UUID.randomUUID().toString())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var map = om.readValue(response.body(), Map.class);
        var unwrap1 = (List<Map<String, Object>>) map.get("candles");
        var unwrap2 = (List<Map<String, Object>>) unwrap1.get(0).get("candles");

        return unwrap2.stream()
                .map(m -> new Candle(
                        LocalDateTime.parse(((String) m.get("fromDate")).replace("Z", "")),
                        (Double) m.get("open"),
                        (Double) m.get("high"),
                        (Double) m.get("low"),
                        (Double) m.get("close"),
                        m.get("volume") != null
                                ? Optional.of((Double) m.get("volume"))
                                : Optional.empty()))
                .toList();

    }

    @SneakyThrows
    Optional<Integer> lookupInstrumentId(String symbol) {
        var client = HttpClient.newHttpClient();

        var request = HttpRequest.newBuilder()
                .uri(new URI("{baseUrl}/api/v1/market-data/search?internalSymbolFull={internalSymbolFull}"
                        .replace("{baseUrl}", this.baseUrl)
                        .replace("{internalSymbolFull}", symbol)))
                .header("x-api-key", this.etoroApiKey)
                .header("x-user-key", this.etoroUserKey)
                .header("x-request-id", UUID.randomUUID().toString())
                .build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var map = om.readValue(response.body(), Map.class);
            var unwrap1 = (List<Map<String, Object>>) map.get("items");
            return Optional.of((int) unwrap1.get(0).get("internalInstrumentId"));
        } catch (Exception e) {
            return Optional.empty();
        }

    }
}
