package systems.diath.visotaris_opmod.api;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.ShardRate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verhindert, dass neue Merchant-Zielwährungen beim API-Mapping verloren gehen. */
class MerchantApiClientContractTest {

    @Test
    void preservesAllCurrenciesAndNormalizesCustomItemSources() throws Exception {
        var stream = getClass().getResourceAsStream("/merchant-rates-contract.json");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<ShardRate> rates = MerchantApiClient.parseRates(JsonParser.parseReader(reader));

            assertEquals(3, rates.size());
            assertEquals("opshards", rates.get(0).getTarget());
            assertEquals("redcoins", rates.get(1).getTarget());
            assertEquals("futurecoins", rates.get(2).getTarget());
            assertEquals("paper#626", rates.get(2).getSource());
        }
    }
}
