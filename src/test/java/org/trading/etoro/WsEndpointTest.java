package org.trading.etoro;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class WsEndpointTest {

    @Test
    @SneakyThrows
    public void test_subscribe() {

        var aapl = new ArrayList<>();
        var tsla = new ArrayList<>();

        var endpoint = WsEndpoint.ETORO();
        endpoint.subscribe("AAPL", aapl::addAll);
        endpoint.subscribe("TSLA", tsla::addAll);


        Thread.sleep(30_000);

        Assertions.assertFalse(aapl.isEmpty());
        Assertions.assertFalse(tsla.isEmpty());
    }
}
