package dev.slopium.visibility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlockSelectionModeTest {
    @Test void emptyWhitelistAllowsNothingAndEmptyBlacklistAllowsEverything(){
        assertFalse(BlockSelectionMode.WHITELIST.allows(false));assertTrue(BlockSelectionMode.BLACKLIST.allows(false));
        assertTrue(BlockSelectionMode.ALL.allows(false));assertTrue(BlockSelectionMode.ALL.allows(true));
        assertTrue(BlockSelectionMode.WHITELIST.allows(true));assertFalse(BlockSelectionMode.BLACKLIST.allows(true));
    }
}
