/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The two shapes a launch leaves in a home's own settings, and the note that puts them back.
///
/// What is being protected here is a file the launcher does not own. The harness writes it while it
/// runs, the person edits it, and every line that is not one of the launcher's own has to come out
/// of an edit exactly as it went in — which is why the edits are tested on their own, not only
/// through a launch.
class DshInjectedSettingsTest {
    @TempDir
    private Path home;

    /// A settings file with a person's own answers in it, and nothing of the launcher's.
    private static final String THEIRS = """
            ui-onboarding:
              welcomeNoticeVersion: 2026-08-13.1
            agent-default-model:
              provider: deepseek
              model: deepseek-v4-pro
            llm-pi-ai:
              providers:
                deepseek:
                  models:
                    - id: deepseek-v4-pro
            """;

    /// What the harness leaves behind after a launch built the route `MCXCC` and the person used it:
    /// a default model naming that route, and a route block listing models with nothing else.
    private static final String AFTER_A_LAUNCH = """
            ui-onboarding:
              welcomeNoticeVersion: 2026-08-13.1
            agent-default-model:
              provider: MCXCC
              model: deepseek-flash
            llm-pi-ai:
              providers:
                deepseek:
                  models:
                    - id: deepseek-v4-pro
                MCXCC:
                  models:
                    - id: deepseek-flash
                      name: DeepSeek-V4.1-Flash
                      contextWindow: 1048576
            """;

    private DshInstance instance() {
        return new DshInstance("test", "0.1.6-alpha.2", "web", home.toString(), "system",
                DshHomeMode.CUSTOM, home.toString(), List.of(), Map.of(), null, null, null, 0, 0L);
    }

    private Path settings() {
        return home.resolve("settings.yaml");
    }

    private String read() throws IOException {
        return Files.readString(settings());
    }

    private void write(String text) throws IOException {
        Files.writeString(settings(), text);
    }

    private Path note() {
        return home.resolve(".hdsl-injected.json");
    }

    private Path patch() {
        return DshPluginPatch.patchFile(home, "web");
    }

    private void writePatch(String text) throws IOException {
        Files.createDirectories(patch().getParent());
        Files.writeString(patch(), text);
    }

    // ---- The line surgery, on its own -----------------------------------------------------------

    @Test
    void aSectionIsFoundAndCanBeTakenAway() {
        assertEquals("agent-default-model:\n  provider: deepseek\n  model: deepseek-v4-pro\n",
                DshInjectedSettings.sectionBlock(THEIRS, "agent-default-model"));

        String without = DshInjectedSettings.withSectionBlock(THEIRS, "agent-default-model", null);
        assertFalse(without.contains("agent-default-model"));
        // Everything else is still there, and so is the order it was in.
        assertTrue(without.contains("welcomeNoticeVersion: 2026-08-13.1"));
        assertTrue(without.contains("deepseek-v4-pro"));
        assertTrue(without.indexOf("ui-onboarding") < without.indexOf("llm-pi-ai"));
    }

    @Test
    void aSectionNamedLikeAnotherIsNotIt() {
        // `agent-default-modelish` is a different key, and the writer and the reader must agree.
        assertNull(DshInjectedSettings.sectionBlock("agent-default-modelish:\n  provider: x\n",
                "agent-default-model"));
    }

    @Test
    void aRouteIsFoundAndCanBeTakenAwayWithoutItsNeighbours() {
        String route = DshInjectedSettings.routeBlock(AFTER_A_LAUNCH, "llm-pi-ai", "providers", "MCXCC");
        assertNotNull(route);
        assertTrue(route.startsWith("    MCXCC:"));
        assertTrue(route.contains("contextWindow: 1048576"));

        String without = DshInjectedSettings.withRouteBlock(AFTER_A_LAUNCH, "llm-pi-ai", "providers",
                "MCXCC", null);
        // The route is gone from the supplier list — the default model below still names it, which is
        // the other half of the cleanup and not this function's to do.
        assertNull(DshInjectedSettings.routeBlock(without, "llm-pi-ai", "providers", "MCXCC"));
        assertFalse(without.contains("DeepSeek-V4.1-Flash"), without);
        // The other route under the same key is untouched, nesting and all.
        assertTrue(without.contains("    deepseek:\n      models:\n        - id: deepseek-v4-pro\n"));
        assertTrue(without.contains("ui-onboarding:"));
    }

    @Test
    void aRouteThatIsNotThereChangesNothing() {
        assertEquals(THEIRS, DshInjectedSettings.withRouteBlock(THEIRS, "llm-pi-ai", "providers",
                "MCXCC", null));
    }

    @Test
    void aScalarIsReadThroughQuotesAndComments() {
        String block = "  provider: \"MCXCC\"  # the one I picked\n  model: deepseek-flash\n";
        assertEquals("MCXCC", DshInjectedSettings.scalarOf(block, "provider"));
        assertEquals("deepseek-flash", DshInjectedSettings.scalarOf(block, "model"));
        assertNull(DshInjectedSettings.scalarOf(block, "absent"));
    }

    // ---- A launch, and the note it leaves --------------------------------------------------------

    @Test
    void aLaunchPutsBackTheRouteTheHarnessAdoptedAndTheModelItNamed() throws Exception {
        write(THEIRS);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        assertTrue(Files.isRegularFile(note()));

        // The launch runs; the harness adopts the route and starts on it.
        write(AFTER_A_LAUNCH);

        assertTrue(DshInjectedSettings.settle(instance()));
        // The route the launcher caused is gone, the person's own is not, and their default model is
        // back — a route the launcher no longer builds must not be the one a later launch starts on.
        assertEquals(THEIRS, read());
        assertFalse(Files.exists(note()));
    }

    @Test
    void aDefaultModelThePersonMovedIsLeftAlone() throws Exception {
        write(THEIRS);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");

        // While the harness was up the person pointed it at something of their own.
        write(AFTER_A_LAUNCH.replace("provider: MCXCC", "provider: deepseek"));
        DshInjectedSettings.settle(instance());

        String after = read();
        assertTrue(after.contains("provider: deepseek"), after);
        // The adopted route still goes: it appeared under this launcher's watch and it is not usable
        // without the patch that made it.
        assertFalse(after.contains("MCXCC"), after);
    }

    @Test
    void aNoteLeftByAKilledLaunchIsHonouredByTheNextOneWhateverItIs() throws Exception {
        write(THEIRS);
        // A launch with a key, killed before it could tidy up: the note stays, the settings keep
        // what the harness wrote.
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        write(AFTER_A_LAUNCH);

        // The next launch has no account at all — which is the whole point: it still cleans up, so a
        // launch that wanted no supplier does not start on the last one's.
        assertTrue(DshInjectedSettings.settle(instance()));
        assertEquals(THEIRS, read());

        // And settling twice is settling once.
        assertFalse(DshInjectedSettings.settle(instance()));
        assertEquals(THEIRS, read());
    }

    @Test
    void aLaunchThatLeftNoNoteChangesNothing() throws Exception {
        write(THEIRS);
        assertFalse(DshInjectedSettings.settle(instance()));
        assertEquals(THEIRS, read());
    }

    @Test
    void aHomeWithNoSettingsIsNoTrouble() throws Exception {
        // Nothing to capture and nothing to put back, but a note either way — a launch notes what it
        // disturbs, and a home with no file is one where it disturbs nothing.
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        assertTrue(Files.isRegularFile(note()));
        assertFalse(DshInjectedSettings.settle(instance()));
        assertFalse(Files.exists(note()));
    }

    @Test
    void aRouteThatWasAlreadyThereIsLeftAsTheHarnessHasIt() throws Exception {
        // A route of the same name already existed before this launch, so whatever is in it now is
        // not this launcher's to remove: an edit made since would be lost.
        write(AFTER_A_LAUNCH);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        write(AFTER_A_LAUNCH.replace("DeepSeek-V4.1-Flash", "renamed by the person"));

        DshInjectedSettings.settle(instance());
        assertTrue(read().contains("renamed by the person"), read());
    }

    // ---- The ledger, and the routes it lets the launcher recognise as its own -------------------

    @Test
    void aLaunchRemembersTheRouteItBuiltLongAfterTheLaunchIsOver() throws Exception {
        write(THEIRS);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        assertTrue(Files.readString(home.resolve(".hdsl-injected-routes.json")).contains("MCXCC"));

        // The launch ends and its note is honoured; the ledger is what is left.
        write(AFTER_A_LAUNCH);
        DshInjectedSettings.settle(instance());
        assertFalse(Files.exists(note()));
        assertTrue(Files.readString(home.resolve(".hdsl-injected-routes.json")).contains("MCXCC"));
    }

    @Test
    void aRouteTheLauncherBuiltBeforeIsTakenAwayWithoutBeingAsked() throws Exception {
        // The case this exists for: a home carrying a route from a launch that left no note — one
        // killed before it wrote one, or one from before there were notes. The settings say the
        // route is there; only the ledger says who put it there.
        write(AFTER_A_LAUNCH);
        Files.writeString(home.resolve(".hdsl-injected-routes.json"), "{\"routes\":[\"MCXCC\"]}");

        assertTrue(DshInjectedSettings.clean(instance(), "web", List.of(), null));
        String after = read();
        assertFalse(after.contains("MCXCC"), after);
        assertTrue(after.contains("deepseek-v4-pro"), after);
        assertFalse(after.contains("agent-default-model"), after);
    }

    @Test
    void aDefaultModelThatNamesARouteThisLaunchWillNotBuildIsTakenAway() throws Exception {
        write(AFTER_A_LAUNCH);
        assertTrue(DshInjectedSettings.clean(instance(), "web", List.of("MCXCC"), null));
        String after = read();
        // The pointer named a supplier this launch makes none of, so it goes with it.
        assertFalse(after.contains("agent-default-model"), after);
        assertFalse(after.contains("MCXCC"), after);
    }

    @Test
    void aDefaultModelNamingTheRouteBeingBuiltIsLeftForTheLaunchToUse() throws Exception {
        write(AFTER_A_LAUNCH);
        // The route this launch is about to build is the one the pointer names: it will be good again
        // in a moment, so it stays.
        assertTrue(DshInjectedSettings.clean(instance(), "web", List.of("MCXCC"), "MCXCC"));
        String after = read();
        assertTrue(after.contains("agent-default-model"), after);
        assertTrue(after.contains("provider: MCXCC"), after);
        // The stale model list still goes: this launch's route comes from its own patch.
        assertFalse(after.contains("DeepSeek-V4.1-Flash"), after);
    }

    @Test
    void aSupplierOfThePersonsOwnIsNeverTouched() throws Exception {
        write(THEIRS);
        assertFalse(DshInjectedSettings.clean(instance(), "web", List.of("MCXCC"), null));
        assertEquals(THEIRS, read());
    }

    // ---- The same contract for the route in the profile's own patch layer -----------------------

    /// A patch with the launcher's route for an account, and a supplier of the person's own.
    private static final String PATCH_WITH_A_ROUTE = """
            # mine
            - id: llm-pi-ai
              name: "@deepseek-ai/dsh-llm-pi-ai"
              config:
                providers:
                  Mine:
                    apiKeyEnv: MY_KEY
                    models:
                      - id: mine
                  MCXCC:
                    apiKeyEnv: HDSL_LAUNCH_API_KEY_MCXCC_00000000
                    models:
                      - id: deepseek-flash
            - id: locale
              config:
                language: en
            """;

    @Test
    void theRouteInThePatchGoesWhenTheLaunchEnds() throws Exception {
        write(THEIRS);
        writePatch(PATCH_WITH_A_ROUTE);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");

        assertTrue(DshInjectedSettings.settle(instance()));

        String after = Files.readString(patch());
        assertFalse(after.contains("MCXCC"), "the account's route is the launch's: " + after);
        assertTrue(after.contains("Mine:"), "the person's own supplier stays: " + after);
        assertTrue(after.contains("- id: locale"), "and so does everything after it: " + after);
        assertFalse(Files.exists(note()), "the note is spent");
    }

    @Test
    void aRouteTheLedgerKnowsIsTakenOutOfThePatchAtTheNextLaunch() throws Exception {
        // The case the person sees: an instance launched with an account, then launched offline. The
        // route cannot outlive its launch — nothing sets its key variable afterwards — so a launch
        // that wants no supplier must not be offered one.
        writePatch(PATCH_WITH_A_ROUTE);
        Files.writeString(home.resolve(".hdsl-injected-routes.json"), "{\"routes\":[\"MCXCC\"]}");

        assertTrue(DshInjectedSettings.clean(instance(), "web", List.of(), null));

        String after = Files.readString(patch());
        assertFalse(after.contains("MCXCC"), after);
        assertTrue(after.contains("Mine:"), after);
    }

    @Test
    void theRouteThisLaunchWillBuildIsLeftForItToRefresh() throws Exception {
        writePatch(PATCH_WITH_A_ROUTE);

        assertFalse(DshInjectedSettings.clean(instance(), "web", List.of("MCXCC"), "MCXCC"),
                "nothing to change: the route this launch builds is the one that is there");
        assertTrue(Files.readString(patch()).contains("MCXCC"));
    }

    @Test
    void aNoteFromBeforeThereWerePatchRoutesStillSettles() throws Exception {
        // What a home carries over from the version whose routes travelled as `--patch` overlays:
        // the note has no profile in it, so there is no patch route to take back — and reading it
        // must not fail.
        write(THEIRS);
        Files.writeString(note(), """
                {"instance":"test","route":"MCXCC","account":"deepseek|MCXCC","writtenAt":"2026-01-01T00:00:00Z"}
                """);

        DshInjectedSettings.settle(instance());

        assertFalse(Files.exists(note()), "the note is spent");
    }

    // ---- What makes a route show up on the harness's own models page ----------------------------

    @Test
    void aLaunchedRouteIsMadeVisibleToTheSettingsPage() throws Exception {
        write(THEIRS);
        assertTrue(DshInjectedSettings.publish(instance(), "MCXCC", "HDSL_LAUNCH_API_KEY"));

        String after = read();
        // The profile object is what the page's row is chosen by, and the credential reference is
        // what stops it asking for a key that is already there.
        assertTrue(after.contains("    MCXCC:\n      apiKeyEnv: HDSL_LAUNCH_API_KEY\n"), after);
        // And nothing else is said: the route itself is the overlay's to describe.
        assertFalse(after.contains("baseURL"), after);
        assertFalse(after.contains("MCXCC:\n      models:"), after);
        // The person's own answers are still there, where they were.
        assertTrue(after.contains("agent-default-model:"), after);
        assertTrue(after.contains("    deepseek:"), after);
    }

    @Test
    void aPublishedRouteGoesAwayWithTheLaunchThatPublishedIt() throws Exception {
        // The whole arrangement: note first, then the profile, then the launch. The note is what
        // remembers the file without it, so the cleanup needs no second mechanism.
        write(THEIRS);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        DshInjectedSettings.publish(instance(), "MCXCC", "HDSL_LAUNCH_API_KEY");
        assertTrue(read().contains("MCXCC"));

        assertTrue(DshInjectedSettings.settle(instance()));
        assertEquals(THEIRS, read());
    }

    @Test
    void aHomeWithNoSupplierSectionGrowsOne() throws Exception {
        write("ui-onboarding:\n  welcomeNoticeVersion: 2026-08-13.1\n");
        DshInjectedSettings.publish(instance(), "MCXCC", "HDSL_LAUNCH_API_KEY");

        String after = read();
        // Whatever the file was missing is made, and nested the way the harness writes it.
        assertTrue(after.contains("llm-pi-ai:\n  providers:\n    MCXCC:\n      apiKeyEnv: HDSL_LAUNCH_API_KEY\n"),
                after);
        assertTrue(after.endsWith("\n"), "a settings file ends in a newline");
    }

    @Test
    void aPublishedRouteIsAlsoTakenAwayByTheNextLaunch() throws Exception {
        write(THEIRS);
        DshInjectedSettings.publish(instance(), "MCXCC", "HDSL_LAUNCH_API_KEY");
        // No note: the launcher was killed before it wrote one, or this is a home from before notes.
        assertTrue(DshInjectedSettings.clean(instance(), "web", List.of(), "MCXCC"));
        assertEquals(THEIRS, read());
    }

    @Test
    void theNoteNamesTheLaunchItBelongsTo() throws Exception {
        write(THEIRS);
        DshInjectedSettings.capture(instance(), "MCXCC", "deepseek|MCXCC", "web");
        String json = Files.readString(note());
        assertTrue(json.contains("\"route\": \"MCXCC\""), json);
        assertTrue(json.contains("\"account\": \"deepseek|MCXCC\""), json);
        // It carries no key of its own: the note is about configuration, and a key on disk is what
        // the environment variable exists to avoid.
        assertFalse(json.contains("sk-"));
    }
}
