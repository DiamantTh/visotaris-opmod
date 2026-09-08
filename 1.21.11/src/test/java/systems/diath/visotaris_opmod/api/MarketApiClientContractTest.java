package systems.diath.visotaris_opmod.api;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketApiClientContractTest {

    @Test
    void keepsCategoryOrderSidesAndNormalizesItemKeys() {
        var data = JsonParser.parseString("""
            {"Erze":{"DIAMOND":[
              {"orderSide":"BUY","activeOrders":12,"price":120.5},
              {"orderSide":"SELL","activeOrders":4,"price":104.0}
            ]}}
            """);

        var prices = MarketApiClient.parsePrices(data);

        assertEquals(1, prices.size());
        var diamond = prices.getFirst();
        assertEquals("diamond", diamond.getItemKey());
        assertEquals("Erze", diamond.getCategory());
        assertEquals(120.5, diamond.getBuy());
        assertEquals(104.0, diamond.getSell());
        assertEquals(12, diamond.getBuyOrders());
        assertEquals(4, diamond.getSellOrders());
    }
}
