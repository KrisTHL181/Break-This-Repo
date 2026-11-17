package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a pack carries of a home's settings, and what it must leave behind.
///
/// The harness keeps one section per plugin in `settings.yaml`, and that file is most of what "the
/// same environment" means for a plugin: a sidebar's custom CSS, a workstation's switches. It is also
/// where a plugin that wants an API key puts it, and where a tunnel token in fact is — so the two
/// halves of this are equally load-bearing: the settings have to arrive, and the credentials have to
/// stay.
class DshPluginSettingsTest {

    /// A settings file shaped like a real one: a block scalar full of CSS, a section the launcher's own
    /// account plumbing owns, a literal secret, and a credential that is only a *name*.
    private static final String SETTINGS = """
            ui-onboarding:
              welcomeNoticeVersion: 2026-08-13.1
            dsh-better-sidebar:
              agentOpenTools: true
              customCss: |-
                /* a stylesheet, indented under its key */
                [class*="footerActions"] {
                  gap: 4px !important;
                  padding: 2px 4px 0 8px !important;
                }
              workspaceFence: false
            agent-default-model:
              provider: deepseek-official
              model: deepseek-flash
            # 化学/药学工作站
            chem-workbench:
              apiKeyCredentialRef: PUBCHEM_API_KEY
              defaultLimit: 25
            remote-web-ui:
              lanBind: true
              publicBaseUrl: https://example.invalid
              tunnelToken: eyJhIjoiODEzZTY2NTQxMTgyNTNmOTA1YTU4ZjVmYmEzMjA0OGYifQ
              autoTunnel: true
            """;

    private static Path homeWith(@TempDir Path home, String text) throws Exception {
        Files.writeString(home.resolve("settings.yaml"), text);
        return home;
    }

    @Test
    void theSectionsAPackCanCarryAreThePluginOnes(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);

        assertEquals(List.of("dsh-better-sidebar", "chem-workbench", "remote-web-ui"),
                DshPluginSettings.sectionsOf(home),
                "the launcher's own account plumbing and the onboarding record do not travel");
    }

    @Test
    void aCredentialStaysBehindAndIsNamed(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);

        DshPluginSettings.Carried carried = DshPluginSettings.extract(home,
                Set.of("dsh-better-sidebar", "chem-workbench", "remote-web-ui"));

        assertFalse(carried.text().contains("eyJhIjoi"), "the tunnel token is a secret, not a setting");
        assertEquals(List.of("remote-web-ui.tunnelToken"), carried.omitted(),
                "and the pack says which value it left out, so nobody wonders why it is missing");
        assertTrue(carried.text().contains("apiKeyCredentialRef: PUBCHEM_API_KEY"),
                "a name of a key is the portable half: the key it names stays where it belongs");
        assertTrue(carried.text().contains("customCss: |-"),
                "a setting like this is the reason the settings travel at all");
    }

    @Test
    void aStylesheetIsNotMistakenForSettings(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);

        DshPluginSettings.Carried carried = DshPluginSettings.extract(home,
                Set.of("dsh-better-sidebar"));

        assertTrue(carried.text().contains("padding: 2px 4px 0 8px !important;"),
                "a block scalar's body is carried verbatim, whatever its lines look like");
        assertTrue(carried.omitted().isEmpty());
    }

    @Test
    void onlyTheSectionsSomebodyChoseTravel(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);

        DshPluginSettings.Carried carried = DshPluginSettings.extract(home, Set.of("chem-workbench"));

        assertEquals(List.of("chem-workbench"), carried.sections());
        assertFalse(carried.text().contains("dsh-better-sidebar"));
    }

    @Test
    void theLaunchersOwnSectionsAreNeverCarriedEvenWhenAsked(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);

        DshPluginSettings.Carried carried = DshPluginSettings.extract(home,
                Set.of("agent-default-model", "ui-onboarding", "chem-workbench"));

        assertEquals(List.of("chem-workbench"), carried.sections(),
                "a provider route built for this machine's account names nothing on the next one");
    }

    @Test
    void mergingReplacesTheSectionsThePackHasAndLeavesEveryOtherLineAlone(@TempDir Path home)
            throws Exception {
        homeWith(home, SETTINGS);

        // What a pack written from another instance carries.
        String carried = """
                # dsh-better-sidebar
                dsh-better-sidebar:
                  agentOpenTools: false
                  customCss: |-
                    body { color: red; }
                # chem-workbench
                chem-workbench:
                  apiKeyCredentialRef: PUBCHEM_API_KEY
                  defaultLimit: 50
                """;

        List<String> merged = DshPluginSettings.merge(home, carried);

        assertEquals(List.of("dsh-better-sidebar", "chem-workbench"), merged);
        String written = Files.readString(home.resolve("settings.yaml"));
        assertTrue(written.contains("agentOpenTools: false"), "the pack's value replaced the home's");
        assertTrue(written.contains("workspaceFence: false"),
                "a key the pack does not declare keeps the value this machine had — which is what keeps"
                        + " a value the pack deliberately left out from being deleted here");
        assertTrue(written.contains("defaultLimit: 50"));
        assertTrue(written.contains("welcomeNoticeVersion: 2026-08-13.1"),
                "a section the pack says nothing about is left exactly as it was");
        assertTrue(written.contains("tunnelToken: eyJhIjoi"),
                "including a credential the home has: a pack neither adds nor removes one");
        assertTrue(written.contains("model: deepseek-flash"),
                "and the launcher's own sections are untouched");
    }

    @Test
    void aCredentialTheRecipientHasSurvivesAPackThatDoesNotCarryOne(@TempDir Path home) throws Exception {
        // The pack has no `tunnelToken` because it was left out on the way here. Replacing the section
        // wholesale would delete the *recipient's* token — the opposite of what leaving one out for.
        homeWith(home, """
                remote-web-ui:
                  lanBind: false
                  tunnelToken: THEIR-OWN-TOKEN
                  autoTunnel: false
                """);

        DshPluginSettings.merge(home, """
                # remote-web-ui
                remote-web-ui:
                  lanBind: true
                  autoTunnel: true
                """);

        String written = Files.readString(home.resolve("settings.yaml"));
        assertTrue(written.contains("tunnelToken: THEIR-OWN-TOKEN"),
                "a key the pack does not have keeps the value this machine had");
        assertTrue(written.contains("lanBind: true"), "and a key it does have takes the pack's value");
        assertTrue(written.contains("autoTunnel: true"));
        assertFalse(written.contains("lanBind: false"), "the pack's value is the one that stays");
    }

    @Test
    void mergingAddsASectionTheHomeDoesNotHave(@TempDir Path home) throws Exception {
        homeWith(home, """
                dsh-better-sidebar:
                  agentOpenTools: true
                """);

        DshPluginSettings.merge(home, """
                # skin-custom-theme
                skin-custom-theme:
                  applied: true
                """);

        String written = Files.readString(home.resolve("settings.yaml"));
        assertTrue(written.contains("agentOpenTools: true"), "what was there is still there");
        assertTrue(written.contains("skin-custom-theme:"), "and what arrived was added");
    }

    @Test
    void mergingRefusesASectionThisLauncherDoesNotCarry(@TempDir Path home) throws Exception {
        homeWith(home, """
                agent-default-model:
                  provider: deepseek-official
                  model: deepseek-flash
                """);

        List<String> merged = DshPluginSettings.merge(home, """
                agent-default-model:
                  provider: somebody-elses-route
                  model: their-model
                """);

        assertTrue(merged.isEmpty(), "a pack a person edited by hand is refused just the same");
        assertTrue(Files.readString(home.resolve("settings.yaml")).contains("deepseek-official"),
                "and the home's own answer is left alone");
    }

    @Test
    void aHomeWithNoSettingsFileGetsOne(@TempDir Path home) throws Exception {
        List<String> merged = DshPluginSettings.merge(home, """
                # dsh-cost-meter
                dsh-cost-meter:
                  currency: CNY
                """);

        assertEquals(List.of("dsh-cost-meter"), merged);
        assertTrue(Files.readString(home.resolve("settings.yaml")).contains("currency: CNY"));
    }

    @Test
    void mergingTheSameSettingsTwiceChangesNothingTheSecondTime(@TempDir Path home) throws Exception {
        homeWith(home, SETTINGS);
        String carried = DshPluginSettings.extract(home, Set.of("chem-workbench")).text();

        DshPluginSettings.merge(home, carried);
        String once = Files.readString(home.resolve("settings.yaml"));
        DshPluginSettings.merge(home, carried);

        assertEquals(once, Files.readString(home.resolve("settings.yaml")),
                "a pack installed twice leaves the file as it was after the first time");
    }
}
