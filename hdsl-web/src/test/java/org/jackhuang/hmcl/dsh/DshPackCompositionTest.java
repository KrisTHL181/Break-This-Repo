package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a person is shown when an installed pack does not compose.
///
/// The harness prints its plugin tree before it fails, so the output is a page of rows with the
/// reason buried in it. Showing all of it would bury the reason in the dialog too.
class DshPackCompositionTest {

    @Test
    void theReasonAndWhatItNamesAreKeptAndTheTreeIsNot() {
        String output = """
                    - id: session-title
                      name: '@deepseek-ai/dsh-session-title'
                    - id: plugins
                      name: '@deepseek-ai/dsh-plugin-manager'
                Error: dsh: plugin tree failed to load: failed to apply loader entry dsh-im-connect (@michengai/dsh-im-connect): webserver: duplicate exact route "/api/michengai/dsh-im-connect/update"
                Error: webserver: duplicate exact route "/api/michengai/dsh-im-connect/update"
                    at Proxy.register (file:///…/dsh-host-webserver/lib/index.js:178:36)
                """;

        String failure = DshPackCompositionTest.failure(output);

        assertTrue(failure.startsWith("Error: dsh: plugin tree failed to load"), failure);
        assertTrue(failure.contains("duplicate exact route"), "the route is named: " + failure);
        assertTrue(!failure.contains("- id: session-title"), "the tree above it is not: " + failure);
    }

    @Test
    void aLineTheHarnessTaggedIsStillTheReason() {
        String failure = failure("""
                    [INFO] dsh web: starting
                    [ERROR] Error: dsh: plugin tree failed to load: failed to apply loader entry dsh-im-connect (@michengai/dsh-im-connect): webserver: duplicate exact route "/api/michengai/dsh-im-connect/update"
                    [ERROR]     at boot (file:///…/dsh-app-boot/lib/index.js:1545:9)
                """);

        assertTrue(failure.contains("duplicate exact route"), failure);
        assertTrue(!failure.contains("[INFO] dsh web: starting"), "and not the lines before it: " + failure);
    }

    @Test
    void aMissingPackageIsKept() {
        String failure = DshPackCompositionTest.failure("""
                Error: dsh: plugin esm load failed
                Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'definitely-not-installed-pkg'
                """);

        assertTrue(failure.contains("Cannot find package 'definitely-not-installed-pkg'"), failure);
    }

    @Test
    void outputWithNoErrorLineIsShownWholeRatherThanSwallowed() {
        assertEquals("something else entirely", DshPackCompositionTest.failure("something else entirely"));
    }

    private static String failure(String output) {
        return DshPackInstaller.failureOf(output);
    }
}
