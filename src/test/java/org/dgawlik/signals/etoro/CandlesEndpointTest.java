package org.dgawlik.signals.etoro;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.dgawlik.signals.EventFrequency;

public class CandlesEndpointTest {

    @Test
    public void test_dot_env() {
        Dotenv e = Dotenv.load();

        Assertions.assertNotNull(e.get("ETORO_PUBLIC_KEY"));
        Assertions.assertNotNull(e.get("ETORO_API_KEY"));

        Assertions.assertNull(e.get("XXX"));
    }

    @Test
    public void test_lookup_instrument_id() {
        Dotenv e = Dotenv.load();

        var publicKey = e.get("ETORO_PUBLIC_KEY");
        var apiKey = e.get("ETORO_API_KEY");

        var endpoint = new CandlesEndpoint("https://public-api.etoro.com", publicKey, apiKey);

        Assertions.assertEquals(endpoint.lookupInstrumentId("AAPL").get(), 1001);
    }

    @Test
    public void test_lookup_instrument_id_failure() {
        Dotenv e = Dotenv.load();

        var publicKey = e.get("ETORO_PUBLIC_KEY");
        var apiKey = e.get("ETORO_API_KEY");

        var endpoint = new CandlesEndpoint("https://public-api.etoro.com", publicKey, apiKey);

        Assertions.assertFalse(endpoint.lookupInstrumentId("__").isPresent());
    }

    @Test
    public void fetch_one_day_30() {
        Dotenv e = Dotenv.load();

        var publicKey = e.get("ETORO_PUBLIC_KEY");
        var apiKey = e.get("ETORO_API_KEY");

        var endpoint = new CandlesEndpoint("https://public-api.etoro.com", publicKey, apiKey);

        Assertions.assertEquals(30, endpoint.fetch("AAPL", EventFrequency.ONE_DAY, 30).size());
    }
}
