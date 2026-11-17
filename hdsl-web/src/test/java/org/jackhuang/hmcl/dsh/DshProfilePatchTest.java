package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The repair a pack needs when it both lists a plugin as a bundle and inserts it.
///
/// Taken from a pack that would not start: `@michengai/dsh-im-connect` in `dsh.profile.bundles` **and**
/// in the patch's insert list, which applies it twice and ends the profile with a duplicate route.
class DshProfilePatchTest {
    @TempDir
    private Path profile;

    private Path patch(String body) throws Exception {
        Path file = profile.resolve("cordis.patch.yml");
        Files.writeString(file, body);
        return file;
    }

    @Test
    void anInsertABundleAlreadyAppliesIsTakenOut() throws Exception {
        Path file = patch("""
                # Your patch layer for this dsh profile
                - insert:
                    - id: dsh-im-connect
                      name: '@michengai/dsh-im-connect'
                """);

        List<String> left = DshProfilePatch.dropRedundantInserts(file,
                List.of("@deepseek-ai/dsh-base", "@michengai/dsh-im-connect"));

        assertEquals(List.of(), left, "nothing carried configuration");
        String after = Files.readString(file);
        assertTrue(after.contains("# Your patch layer"), "the comment stays: " + after);
        assertTrue(!after.contains("dsh-im-connect"), "the duplicate insert is gone: " + after);
    }

    @Test
    void anInsertNoBundleAppliesIsKept() throws Exception {
        String body = """
                - insert:
                    - id: something-else
                      name: 'dsh-something-else'
                """;
        Path file = patch(body);

        DshProfilePatch.dropRedundantInserts(file, List.of("@deepseek-ai/dsh-base"));

        assertEquals(body, Files.readString(file), "a plugin nothing else applies is left where it is");
    }

    @Test
    void anInsertCarryingConfigurationIsKeptAndReported() throws Exception {
        String body = """
                - insert:
                    - id: better-sidebar
                      name: 'dsh-better-sidebar'
                      config:
                        css: 'body { color: red }'
                """;
        Path file = patch(body);

        List<String> left = DshProfilePatch.dropRedundantInserts(file, List.of("dsh-better-sidebar"));

        assertEquals(List.of("better-sidebar"), left, "the entry is reported by its id");
        assertEquals(body, Files.readString(file), "its settings are not thrown away");
    }

    @Test
    void theTextFormIsWhatAPackIsWrittenFrom() {
        // The exporter writes a copy of this file into the archive, and a copy has to be repaired the
        // same way — otherwise every pack made from such an instance carries the defect forward.
        String text = """
                - insert:
                    - id: dsh-im-connect
                      name: '@michengai/dsh-im-connect'
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.withoutRedundantInserts(text,
                List.of("@michengai/dsh-im-connect"));

        assertTrue(edit.changed(), "the duplicate is taken out of the copy");
        assertTrue(!edit.text().contains("dsh-im-connect"), edit.text());
        assertEquals(List.of(), edit.withConfiguration());
    }

    @Test
    void theTextFormSaysWhenThereWasNothingToDo() {
        DshProfilePatch.Edit edit = DshProfilePatch.withoutRedundantInserts("- id: x\n", List.of("y"));
        assertFalse(edit.changed(), "a file with nothing to repair is left as it is");
        assertEquals("- id: x\n", edit.text());
    }

    @Test
    void aPatchWithNoInsertsIsUntouched() throws Exception {
        String body = """
                - id: tool-web
                  config:
                    fetch: true
                """;
        Path file = patch(body);

        assertEquals(List.of(), DshProfilePatch.dropRedundantInserts(file, List.of("anything")));
        assertEquals(body, Files.readString(file));
    }

    @Test
    void aProviderGoesIntoTheEntryTheFileAlreadyHas() throws Exception {
        String body = """
                - id: llm-pi-ai
                  name: '@deepseek-ai/dsh-llm-pi-ai'
                  config:
                    providers:
                      Mine:
                        apiKeyEnv: MY_KEY
                - id: locale
                  config:
                    language: en
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: HDSL_LAUNCH_API_KEY_DEEPSEEK_00000000\napi: openai-completions\n");

        assertTrue(edit.changed());
        String after = edit.text();
        assertTrue(after.contains("""
                      Mine:
                        apiKeyEnv: MY_KEY
                      DeepSeek:
                        apiKeyEnv: HDSL_LAUNCH_API_KEY_DEEPSEEK_00000000
                        api: openai-completions
                """), after);
        assertTrue(after.contains("- id: locale\n  config:\n    language: en\n"),
                "the entry after it is untouched: " + after);
    }

    @Test
    void aProviderTheEntryAlreadyHasIsReplacedWhereItSits() throws Exception {
        String body = """
                - id: llm-pi-ai
                  config:
                    providers:
                      A:
                        apiKeyEnv: A_KEY
                      DeepSeek:
                        apiKeyEnv: OLD
                        models:
                          - id: old
                      B:
                        apiKeyEnv: B_KEY
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: NEW\n");

        assertTrue(edit.changed());
        assertEquals("""
                - id: llm-pi-ai
                  config:
                    providers:
                      A:
                        apiKeyEnv: A_KEY
                      DeepSeek:
                        apiKeyEnv: NEW
                      B:
                        apiKeyEnv: B_KEY
                """, edit.text(), "only the route's own block is rewritten");
    }

    @Test
    void aProviderGoesIntoTheEntryThatHasNone() throws Exception {
        String body = """
                - id: llm-pi-ai
                  name: '@deepseek-ai/dsh-llm-pi-ai'
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: NEW\n");

        assertEquals("""
                - id: llm-pi-ai
                  name: '@deepseek-ai/dsh-llm-pi-ai'
                  config:
                    providers:
                      DeepSeek:
                        apiKeyEnv: NEW
                """, edit.text());
    }

    @Test
    void anEntryTheFileDoesNotMentionIsAddedToTheList() throws Exception {
        // The profile template ships an empty-list placeholder, which is not an entry: appending
        // after it would make two top-level elements in one document.
        DshProfilePatch.Edit edit = DshProfilePatch.putProvider("# mine\n[]\n", "llm-pi-ai",
                "DeepSeek", "apiKeyEnv: NEW\n");

        assertEquals("""
                # mine
                # []
                - id: llm-pi-ai
                  config:
                    providers:
                      DeepSeek:
                        apiKeyEnv: NEW
                """, edit.text());
    }

    @Test
    void theLastEntryForAnIdIsTheOneWritten() throws Exception {
        // The harness's own editor finds the entry to edit with `findLastIndex`, and a later entry
        // replaces an earlier one's whole config — so the last one is the entry that is in effect.
        String body = """
                - id: llm-pi-ai
                  config:
                    providers:
                      First:
                        apiKeyEnv: FIRST
                - id: llm-pi-ai
                  config:
                    providers:
                      Second:
                        apiKeyEnv: SECOND
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: NEW\n");

        String after = edit.text();
        assertTrue(after.contains("      First:\n        apiKeyEnv: FIRST\n"),
                "the entry that is not in effect is left alone: " + after);
        assertTrue(after.indexOf("Second:") < after.indexOf("DeepSeek:"),
                "and the route joins the one the editor edits: " + after);
    }

    @Test
    void aProviderAlreadyWrittenExactlySoChangesNothing() throws Exception {
        String body = """
                - id: llm-pi-ai
                  config:
                    providers:
                      DeepSeek:
                        apiKeyEnv: NEW
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: NEW\n");

        assertFalse(edit.changed(), "a launch that says the same thing must not rewrite the file");
        assertEquals(body, edit.text());
    }

    @Test
    void anInlineConfigIsRefusedRatherThanReplaced() {
        // Rewriting it would need a YAML parser this does not have, and writing a second entry for
        // the same id would take the person's settings away instead of adding one.
        DshException refused = org.junit.jupiter.api.Assertions.assertThrows(DshException.class,
                () -> DshProfilePatch.putProvider("- id: llm-pi-ai\n  config: {providers: {Mine: {}}}\n",
                        "llm-pi-ai", "DeepSeek", "apiKeyEnv: NEW\n"));

        assertTrue(refused.getMessage().contains("inline"), refused.getMessage());
    }

    @Test
    void fourSpaceIndentationIsKept() throws Exception {
        String body = """
                - id: llm-pi-ai
                    config:
                        providers:
                            Mine:
                                apiKeyEnv: MY_KEY
                """;

        DshProfilePatch.Edit edit = DshProfilePatch.putProvider(body, "llm-pi-ai", "DeepSeek",
                "apiKeyEnv: NEW\n");

        assertTrue(edit.text().contains("""
                            DeepSeek:
                                apiKeyEnv: NEW
                """), edit.text());
    }
}
