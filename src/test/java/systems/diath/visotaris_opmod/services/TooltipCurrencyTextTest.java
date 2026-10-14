package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.ShardRate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TooltipCurrencyTextTest {
    @Test void shardTooltipTextUsesTheFullOpshardsUnit() {
        String text = TooltipValueService.merchantRateText(new ShardRate("paper#626", 19.44, 0, "opshards", "Gräbergemisch"));
        assertEquals("19,44 OPShards", text);
        assertFalse(text.matches(".*\\bOPS\\b.*"));
    }

    @Test void redcoinTooltipTextUsesTheFullRedcoinsUnit() {
        String text = TooltipValueService.merchantRateText(new ShardRate("bone_block", 4.25, 0, "redcoins", "Knochenblock"));
        assertEquals("4,25 Redcoins", text);
        assertFalse(text.contains("RDC"));
    }
}
