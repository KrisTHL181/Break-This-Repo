package org.jackhuang.hmcl.web.developer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HumanExpiryTest {
    @Test
    void clearingTheKeyClearsTheHumanExpiry() {
        DeveloperElevation e = new DeveloperElevation();
        e.elevate("token:a");
        assertNotNull(e.latestHumanExpiry());
        e.clear("token:a");
        assertNull(e.latestHumanExpiry(), "still saw " + e.latestHumanExpiry());
    }
}
