package systems.diath.visotaris_opmod.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AuctionCurrencyNeutralityTest {

    @Test
    void auctionModelDoesNotInferACurrencyFromItsCategory() {
        assertFalse(Arrays.stream(Auction.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch("currency"::equalsIgnoreCase));
    }
}
