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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a launch hands the harness for an account, and where it puts it.
///
/// The capacities matter as much as the address. A route the launcher writes is not in the
/// harness's catalogue, so a model on it is the size the launcher says and nothing else — and
/// `pi-ai`'s fallback for a model it cannot size is 256 Ki, a quarter of what the harness's own
/// DeepSeek route gives the same model.
///
/// Where it goes matters more than either. An account's route used to travel as a `--patch` overlay,
/// and an overlay's entry `config` **replaces** the profile's rather than merging with it — so the
/// harness's configuration editor, which refuses a save that does not survive recomposition, could
/// not save any supplier at all while an instance was launched with an account. The route is written
/// into the profile's own patch layer now, which is the document that editor writes, and these tests
/// are about that file's shape.
class DshAccountRouteTest {
    @TempDir
    private Path home;

    /// An account whose supplier served a model, which is what a route needs to be written.
    ///
    /// The list is named on the account here rather than fetched, so that a test with no network
    /// still has the thing a launch would have had. It is the account's own model, which is the one
    /// fallback a route may use when the supplier cannot be reached.
    private static DshAccount accountOn(String vendor) {
        return new DshAccount(DshAccount.AccountKind.THIRD_PARTY, vendor, "sk-test", null, null,
                "a-model", null);
    }

    private String routeFor(String vendor) {
        return routeFor(accountOn(vendor));
    }

    private String routeFor(DshAccount account) {
        DshAccountRoute.Prepared prepared = DshAccountRoute.prepare(account).orElseThrow();
        try {
            return prepared.render();
        } catch (DshException e) {
            throw new AssertionError("the route could not be written: " + e.getMessage(), e);
        }
    }

    /// The profile patch a launch of `test`'s profile would write into.
    private Path patch(String text) throws Exception {
        Path file = DshPluginPatch.patchFile(home, "web");
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void aDeepSeekRouteSaysTheCapacityTheAdaptersOwnRouteHas() {
        String yaml = routeFor("deepseek");
        assertTrue(yaml.startsWith("apiKeyEnv: "), yaml);
        assertTrue(yaml.contains("defaultContextWindow: 1000000\n"), yaml);
        assertTrue(yaml.contains("defaultMaxTokens: 256000\n"), yaml);
    }

    @Test
    void anotherVendorsRouteIsNotSizedByDeepSeeksNumbers() {
        String yaml = routeFor("openrouter");
        assertFalse(yaml.contains("defaultContextWindow"), yaml);
        assertFalse(yaml.contains("defaultMaxTokens"), yaml);
    }

    @Test
    void aSupplierThePersonAddedArrivesWithItsAddress() {
        // What the harness reads, and the half of the defect that was visible from outside: a route
        // written without a `baseURL` is refused with "needs a baseURL; the installed catalog does
        // not describe this route", because a route the launcher invents is not one the harness can
        // look an address up for.
        DshVendor added = DshVendor.discovered("opencode", "OpenCode", "https://api.opencode.ai/v1");
        java.util.List<DshVendor> addedVendors =
                org.jackhuang.hmcl.setting.SettingsManager.settings().getCustomVendors();
        addedVendors.add(added);
        try {
            String yaml = routeFor("opencode");

            assertTrue(yaml.contains("baseURL: \"https://api.opencode.ai/v1\"\n"), yaml);
            assertTrue(yaml.contains("api: openai-completions\n"), yaml);
        } finally {
            addedVendors.remove(added);
        }
    }

    @Test
    void theKeyTravelsInAVariableNamedAfterTheRoute() {
        // One variable per route is what keeps a route left behind by one account from being handed
        // the key of another: it names a variable nothing sets, so it fails instead.
        String one = DshAccountRoute.environmentVariable("DeepSeek");
        String two = DshAccountRoute.environmentVariable("SiliconFlow");

        assertTrue(one.startsWith(DshAccountRoute.KEY_ENVIRONMENT_VARIABLE), one);
        assertTrue(one.contains("DEEPSEEK"), one);
        assertNotEquals(one, two);
        // Stable across launches, or the route in the file would stop matching the variable.
        assertEquals(one, DshAccountRoute.environmentVariable("DeepSeek"));
        // Two names a variable cannot tell apart still get two variables.
        assertNotEquals(DshAccountRoute.environmentVariable("a-b"),
                DshAccountRoute.environmentVariable("a_b"));
        assertTrue(DshAccountRoute.environmentVariable("a-b")
                .startsWith(DshAccountRoute.KEY_ENVIRONMENT_VARIABLE + "_A_B_"),
                DshAccountRoute.environmentVariable("a-b"));
    }

    @Test
    void theModelsTheSupplierServedAreWhatTheRouteCarries() throws Exception {
        List<String> served = List.of("ling-3.0-flash-fin-free", "mimo-v2.5-free");
        DshAccountRoute.Prepared prepared = DshAccountRoute.prepare(accountOn("openrouter")).orElseThrow();
        prepared.resolveModels(served);

        String yaml = prepared.render();

        assertTrue(yaml.contains("  - id: ling-3.0-flash-fin-free\n"), yaml);
        assertTrue(yaml.contains("  - id: mimo-v2.5-free\n"), yaml);
        assertFalse(yaml.contains("- id: default\n"), yaml);
        assertFalse(yaml.contains("- id: a-model\n"),
                "an answer from the supplier is the list, not the name the account kept: " + yaml);
    }

    @Test
    void aRouteWithNoModelsTheSupplierCouldBeAskedForIsRefusedRatherThanInvented() {
        // What this replaced wrote a model called `default` whenever the list could not be read. The
        // harness fills a route's models in only for a supplier it ships, and a route is named after
        // the account, so there was nothing for it to fill in either: the launch failed anyway, with
        // a sentence about a model nobody chose. Saying what happened is what the person can act on.
        DshAccount noModel = new DshAccount(DshAccount.AccountKind.THIRD_PARTY, "openrouter", "sk-test",
                null, null, null, null);

        DshException refused = assertThrows(DshException.class,
                () -> DshAccountRoute.prepare(noModel).orElseThrow().render());

        assertTrue(refused.getMessage().contains("Could not read the models of"), refused.getMessage());
        assertTrue(refused.getMessage().contains("https://openrouter.ai/api/v1"),
                "the address is what the person has to check: " + refused.getMessage());
        assertFalse(refused.getMessage().contains("default"),
                "and nothing is invented to get past it: " + refused.getMessage());
    }

    @Test
    void aModelNamedOnTheAccountIsWhatGetsWrittenWhenTheSupplierSaysNothing() throws Exception {
        // 降级路径：供应商这一会儿读不到，但账户上存过一个模型名——写它，别中止。
        DshAccountRoute.Prepared prepared = DshAccountRoute.prepare(accountOn("openrouter")).orElseThrow();
        prepared.resolveModels(List.of(), "the endpoint answered HTTP 502");

        String yaml = prepared.render();

        assertTrue(yaml.contains("  - id: a-model\n"), yaml);
        assertEquals(1, yaml.split("- id: ", -1).length - 1, "只写这一个，别把失败编成一串：" + yaml);
    }

    @Test
    void theReasonTheSupplierNamedNoModelsReachesTheRefusal() {
        // 拒绝本身没错，但“为什么”要跟着一起出去：界面只会把它当一句 toast 显示。
        DshAccount noModel = new DshAccount(DshAccount.AccountKind.THIRD_PARTY, "openrouter", "sk-test",
                null, null, null, null);
        DshAccountRoute.Prepared prepared = DshAccountRoute.prepare(noModel).orElseThrow();
        prepared.resolveModels(List.of(), "the endpoint answered HTTP 401: invalid_api_key");

        DshException refused = assertThrows(DshException.class, prepared::render);

        assertTrue(refused.getMessage().contains("HTTP 401"), refused.getMessage());
        assertTrue(refused.getMessage().contains("invalid_api_key"), refused.getMessage());
        assertTrue(refused.getMessage().contains("模型"), "要指路到账户里的那一栏：" + refused.getMessage());
    }

    @Test
    void theRouteJoinsTheEntryTheHarnessItselfEdits() throws Exception {
        // The whole reason this is a file edit rather than an overlay. The person's own supplier, the
        // models they chose for it and the comments they wrote all have to survive, and the account's
        // route has to end up **inside the same entry** — a second entry for `llm-pi-ai` would replace
        // that entry's config instead of adding to it.
        Path file = patch("""
                # my own suppliers
                - id: llm-pi-ai
                  name: "@deepseek-ai/dsh-llm-pi-ai"
                  config:
                    providers:
                      MyOwn:            # the one I pay for
                        apiKeyEnv: MY_KEY
                        api: openai-completions
                        baseURL: https://example.invalid/v1
                        models:
                          - id: mine
                            name: mine
                - id: locale
                  config:
                    language: zh-Hans
                """);

        DshAccountRoute.apply(home, "web", DshAccountRoute.prepare(accountOn("deepseek")).orElseThrow());

        String text = Files.readString(file);
        assertEquals(1, text.split("- id: llm-pi-ai", -1).length - 1,
                "one entry for the supplier routes, not two: " + text);
        assertTrue(text.contains("MyOwn:            # the one I pay for\n"),
                "the person's own route and its comment are theirs: " + text);
        assertTrue(text.contains("    - id: mine\n"), text);
        assertTrue(text.contains("- id: locale\n") && text.contains("    language: zh-Hans\n"),
                "and nothing else in the file moved: " + text);
        // Where it went: a key of the same `providers` mapping, at the same indentation as the
        // person's own route — not a second entry, and not another file.
        int mine = text.indexOf("      MyOwn:");
        int theirs = text.indexOf("      " + DshAccountRoute.prepare(accountOn("deepseek"))
                .orElseThrow().route() + ":");
        assertTrue(mine > 0 && theirs > mine, "both routes sit under one providers mapping: " + text);
        assertTrue(text.contains("        apiKeyEnv: "
                        + DshAccountRoute.environmentVariable(accountOn("deepseek").displayName()) + "\n"),
                "and the route reads the variable this launch sets: " + text);
    }

    @Test
    void theRendererdBlockDoesNotCarryTheRoutesOwnKey() {
        // What shipped first put the key in the block *and* the patch writer added one, so the
        // harness was handed `providers.MCXCC-sf1.MCXCC-sf1` — a route whose settings are a route,
        // with no models where models are looked for, refused at boot with "resolves no models".
        for (String vendor : List.of("deepseek", "openrouter")) {
            String yaml = routeFor(vendor);

            assertFalse(yaml.contains(accountOn(vendor).displayName() + ":"),
                    "the block is the route's settings, not the route: " + yaml);
            assertTrue(yaml.startsWith("apiKeyEnv: "), yaml);
        }
    }

    @Test
    void theRouteIsWrittenAsOneKeyWithItsSettingsUnderIt() throws Exception {
        // The shape the harness reads, at the indentation the file uses — the assertion the first
        // version was missing, because it only looked for substrings that a doubled key also has.
        Path file = patch("""
                - id: llm-pi-ai
                  name: "@deepseek-ai/dsh-llm-pi-ai"
                  config:
                    providers:
                """);
        DshAccountRoute.Prepared route = DshAccountRoute.prepare(accountOn("openrouter")).orElseThrow();
        route.resolveModels(List.of("a-model"));

        DshAccountRoute.apply(home, "web", route);

        String expected = "      " + route.route() + ":\n"
                + "        apiKeyEnv: " + route.environmentVariable() + "\n"
                + "        api: openai-completions\n";
        String text = Files.readString(file);
        assertTrue(text.contains(expected), "expected under the entry's own providers:\n" + expected
                + "\nbut the file reads:\n" + text);
        assertEquals(1, text.lines().filter(line -> line.trim().equals(route.route() + ":")).count(),
                "the key appears once: " + text);
    }

    @Test
    void aRouteReplacesOnlyItsOwnBlock() throws Exception {
        DshAccountRoute.Prepared route = DshAccountRoute.prepare(accountOn("deepseek")).orElseThrow();
        Path file = patch("""
                - id: llm-pi-ai
                  config:
                    providers:
                      %s:
                        apiKeyEnv: HDSL_LAUNCH_API_KEY_STALE_00000000
                        api: openai-completions
                        baseURL: https://api.deepseek.com
                        models:
                          - id: old-model
                            name: old-model
                      Other:
                        apiKeyEnv: OTHER_KEY
                """.formatted(route.route()));
        route.resolveModels(List.of("deepseek-flash"));
        DshAccountRoute.apply(home, "web", route);

        String text = Files.readString(file);
        assertFalse(text.contains("old-model"), "the route's own models are refreshed: " + text);
        assertTrue(text.contains("    - id: deepseek-flash\n"), text);
        assertTrue(text.contains("      Other:\n        apiKeyEnv: OTHER_KEY\n"),
                "the route next to it is untouched: " + text);
    }

    @Test
    void aLaunchThatWouldWriteWhatIsAlreadyThereLeavesTheFileAlone() throws Exception {
        Path file = patch("""
                - id: llm-pi-ai
                  name: "@deepseek-ai/dsh-llm-pi-ai"
                """);
        DshAccountRoute.Prepared route = DshAccountRoute.prepare(accountOn("openrouter")).orElseThrow();
        route.resolveModels(List.of("a-model"));

        assertTrue(DshAccountRoute.apply(home, "web", route), "the first launch writes it");
        String written = Files.readString(file);
        assertTrue(written.contains("        providers:\n") || written.contains("    providers:\n"),
                written);
        assertFalse(DshAccountRoute.apply(home, "web", route),
                "the next launch says the same thing and must not touch the person's file");
        assertEquals(written, Files.readString(file));
    }

    @Test
    void theRouteGoesBackOutWhenTheLaunchEnds() throws Exception {
        String original = """
                - id: llm-pi-ai
                  name: "@deepseek-ai/dsh-llm-pi-ai"
                  config:
                    providers:
                      Mine:
                        apiKeyEnv: MY_KEY
                """;
        Path file = patch(original);
        DshAccountRoute.Prepared route = DshAccountRoute.prepare(accountOn("openrouter")).orElseThrow();
        route.resolveModels(List.of("a-model"));
        DshAccountRoute.apply(home, "web", route);
        assertTrue(Files.readString(file).contains(route.route()), "written by the launch");

        assertTrue(DshAccountRoute.remove(home, "web", route.route()));

        assertEquals(original, Files.readString(file),
                "the file is what it was before the launch, byte for byte");
        assertFalse(DshAccountRoute.remove(home, "web", route.route()),
                "and a route that is not there changes nothing");
    }

    @Test
    void anEntryWrittenInlineIsSaidSoRatherThanRewritten() throws Exception {
        // The harness's editor writes block style, so this is a hand edit — and one this will not
        // parse and re-emit. Refusing with a sentence about the entry is what the person can act on;
        // writing a second entry would take their suppliers away instead of adding one.
        patch("""
                - id: llm-pi-ai
                  config: {providers: {Mine: {apiKeyEnv: MY_KEY}}}
                """);

        DshException refused = assertThrows(DshException.class, () -> DshAccountRoute
                .apply(home, "web", DshAccountRoute.prepare(accountOn("deepseek")).orElseThrow()));

        assertTrue(refused.getMessage().contains("inline"), refused.getMessage());
        assertTrue(refused.getMessage().contains("llm-pi-ai"), refused.getMessage());
    }
}
