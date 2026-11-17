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

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what a DSH-PackForge pack carries, and what it refuses to.
///
/// A pack is something people publish, so the filter is the part that matters most: an installed
/// tree, a credential, a key or a nested archive that travelled would be a leak, and one that did
/// not travel would only be a smaller pack. The container's shape matters too — the marker at the
/// root and the files under `overrides/` are what an importer looks for.
class DshPackForgeTest {
    @Test
    void whatNeverTravels() {
        // Installed trees and their lock files.
        assertEquals("name", DshPackForge.exclusionOf("node_modules/dshmarket/package.json"));
        assertEquals("name", DshPackForge.exclusionOf("dist/index.js"));
        assertEquals("name", DshPackForge.exclusionOf("pnpm-lock.yaml".equals("x") ? "x" : "package-lock.json"));

        // Credentials, by name and by shape.
        assertEquals("name", DshPackForge.exclusionOf(".env"));
        assertEquals("credentials", DshPackForge.exclusionOf(".env.local"));
        assertEquals("credentials", DshPackForge.exclusionOf("plugins/credentials.yaml"));
        assertEquals("credentials", DshPackForge.exclusionOf("id_rsa"));
        assertEquals("credentials", DshPackForge.exclusionOf("secrets.json"));
        assertEquals("credentials", DshPackForge.exclusionOf("my-token.txt"));
        assertEquals("credentials", DshPackForge.exclusionOf("OPENAI_API_KEY"));

        // Keys by extension.
        assertEquals("extension", DshPackForge.exclusionOf("server.key"));
        assertEquals("extension", DshPackForge.exclusionOf("cert.pem"));

        // Archives, whatever they are.
        assertEquals("archive", DshPackForge.exclusionOf("plugin.tgz"));
        assertEquals("archive", DshPackForge.exclusionOf("nested.zip"));
        assertEquals("archive", DshPackForge.exclusionOf("other.dspack"));

        // Runtime directories that belong to the home rather than to a profile.
        assertEquals("runtime", DshPackForge.exclusionOf("attachments/aa/bb"));
        assertEquals("runtime", DshPackForge.exclusionOf("profiles/web/package.json"));

        // What does travel.
        assertNull(DshPackForge.exclusionOf("cordis.patch.yml"));
        assertNull(DshPackForge.exclusionOf("package.json"));
        assertNull(DshPackForge.exclusionOf("skills/my-skill/SKILL.md"));
    }

    @Test
    void aScanSkipsSymlinksAndSaysWhy(@TempDir Path profile) throws Exception {
        Files.writeString(profile.resolve("package.json"), "{}");
        Files.writeString(profile.resolve("cordis.patch.yml"), "[]\n");
        Files.createDirectories(profile.resolve("node_modules/dshmarket"));
        Files.writeString(profile.resolve("node_modules/dshmarket/package.json"), "{}");
        Files.writeString(profile.resolve("secret.pem"), "not really a key");
        try {
            Files.createSymbolicLink(profile.resolve("link"), profile.resolve("package.json"));
        } catch (UnsupportedOperationException | java.io.IOException e) {
            // A filesystem without links cannot test that part.
        }

        DshPackForge.Scan scan = DshPackForge.scan(profile);

        assertEquals(java.util.List.of("cordis.patch.yml", "package.json"),
                scan.files().stream().map(DshPackForge.Entry::relative).toList());
        assertEquals("name", scan.excluded().get("node_modules/"));
        assertEquals("extension", scan.excluded().get("secret.pem"));
        if (Files.exists(profile.resolve("link"), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            assertEquals("symlink", scan.excluded().get("link"));
        }
    }

    @Test
    void theManifestSaysWhatTheSpecificationRequires() throws Exception {
        DshInstance instance = DshInstanceManager.find("packforge-test");
        Path home = Files.createTempDirectory("packforge-home");
        if (instance == null) {
            instance = DshInstanceManager.create("packforge-test", "0.1.6-alpha.2",
                    DshInstance.DEFAULT_PROFILE, home, DshHomeMode.ISOLATED, null,
                    java.util.List.of(), java.util.Map.of());
        }
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"), """
                {"name":"dsh-profile-web","private":true,
                 "dependencies":{"dshmarket":"1.52.0"},
                 "dsh":{"profile":{"bundles":["@deepseek-ai/dsh-base","dshmarket"]}}}
                """);

        try {
        JsonObject manifest = DshPackForge.buildManifest(instance,
                new DshPackForge.Options("My Pack", "2.1.0", "我的整合包", "a description", "someone",
                        java.util.List.of()),
                null);

        assertEquals(5, manifest.get("manifestVersion").getAsInt());
        assertEquals("profile", manifest.get("type").getAsString());
        assertEquals("my-pack", manifest.get("name").getAsString(), "names have to be kebab-case");
        assertEquals("2.1.0", manifest.get("version").getAsString());
        assertEquals("我的整合包", manifest.get("displayName").getAsString());
        assertEquals(instance.version(), manifest.get("dshVersion").getAsString(),
                "the harness version is exact");
        assertEquals(instance.profile(), manifest.get("profileName").getAsString());
        assertEquals(java.util.List.of("@deepseek-ai/dsh-base", "dshmarket"),
                manifest.getAsJsonArray("bundles").asList().stream()
                        .map(com.google.gson.JsonElement::getAsString).toList(),
                "the bundle order is what the profile loads in");
        assertEquals("1.52.0", manifest.getAsJsonObject("dependencies").get("dshmarket").getAsString());
        assertEquals(java.util.List.of(instance.version()),
                manifest.getAsJsonArray("dshVersions").asList().stream()
                        .map(com.google.gson.JsonElement::getAsString).toList(),
                "the set the author measured against always holds the pinned version");

        } finally {
            DshInstanceManager.delete("packforge-test");
        }
    }

    /// The set an author measured against travels, the pinned version first.
    ///
    /// The manifest spec asks for the pinned version to be one of the set, and for the set to be
    /// versions: anything else is left out and said so, rather than written into a manifest an
    /// importer would compare against what it has installed.
    ///
    /// @throws Exception when the instance or the manifest cannot be made
    @Test
    void theVersionsThePackWasMeasuredAgainstTravelWithThePinFirst() throws Exception {
        String id = "packforge-versions";
        Path home = Files.createTempDirectory("packforge-versions-home");
        DshInstance instance = DshInstanceManager.create(id, "0.1.6-alpha.2",
                DshInstance.DEFAULT_PROFILE, home, DshHomeMode.ISOLATED, null,
                java.util.List.of(), java.util.Map.of());
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"),
                "{\"name\":\"dsh-profile-web\",\"private\":true}\n");

        try {
            java.util.List<String> said = new java.util.ArrayList<>();
            JsonObject manifest = DshPackForge.buildManifest(instance,
                    new DshPackForge.Options("measured", "1.0.0", "", "", "",
                            java.util.List.of("0.1.7-rc.1", instance.version(), "not a version", "0.1.5")),
                    said::add);

            assertEquals(java.util.List.of(instance.version(), "0.1.7-rc.1", "0.1.5"),
                    manifest.getAsJsonArray("dshVersions").asList().stream()
                            .map(com.google.gson.JsonElement::getAsString).toList(),
                    "the pin comes first, the rest follow in the order given, and a duplicate is one entry");
            assertEquals(manifest.get("dshVersion").getAsString(),
                    manifest.getAsJsonArray("dshVersions").get(0).getAsString(),
                    "the specification requires the pinned version to be in the set");
            assertTrue(said.stream().anyMatch(line -> line.contains("not a version")),
                    "what is not a version is left out and said so: " + said);
        } finally {
            DshInstanceManager.delete(id);
        }
    }

    /// The home travels only when it is asked for, and the filter still decides what may.
    ///
    /// A profile pack may carry the global `skills/`, the `.agent-presets/` and `AGENTS.md`; none of
    /// it travels unasked, because the home is the machine's. What the caller does ask for is still
    /// put through the security filter — naming a credential does not carry it — and the home's own
    /// `profiles/` is never a candidate, since that is every other profile on the machine.
    ///
    /// @throws Exception when the instance, the home or the packs cannot be made
    @Test
    void theHomeTravelsOnlyWhenItIsAskedForAndNeverWithCredentials() throws Exception {
        String id = "packforge-home-travel";
        Path workspace = Files.createTempDirectory("packforge-home-travel-workspace");
        DshInstance instance = DshInstanceManager.create(id, "0.1.6-alpha.2",
                DshInstance.DEFAULT_PROFILE, workspace, DshHomeMode.ISOLATED, null,
                java.util.List.of(), java.util.Map.of());
        // An isolated instance keeps its own home inside the instance, which is what the export
        // scans; the workspace is only where its sessions are scoped.
        Path home = instance.homeDirectory();
        Files.createDirectories(home);
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"), "{\"name\":\"dsh-profile-web\"}\n");

        Files.createDirectories(home.resolve("skills/my-skill"));
        Files.writeString(home.resolve("skills/my-skill/SKILL.md"), "# a skill\n");
        Files.createDirectories(home.resolve(".agent-presets/coding"));
        Files.writeString(home.resolve(".agent-presets/coding/agent.cordis.yml"), "[]\n");
        Files.writeString(home.resolve("AGENTS.md"), "global instructions\n");
        Files.writeString(home.resolve(".credentials.yaml"), "token: secret\n");
        Files.writeString(home.resolve("settings.yaml"), "model: demo\n");
        Files.writeString(home.resolve(".anonymous-user-id"), "id\n");
        Files.createDirectories(home.resolve("attachments"));
        Files.writeString(home.resolve("attachments/a.txt"), "runtime data\n");
        Files.createDirectories(home.resolve("profiles/other"));
        Files.writeString(home.resolve("profiles/other/package.json"), "{\"name\":\"other\"}\n");
        Files.createDirectories(home.resolve("skills/.system"));
        Files.writeString(home.resolve("skills/.system/note.md"), "an installed system skill\n");

        Path withoutAsking = Files.createTempFile("packforge-home-none", ".dspack");
        Path asked = Files.createTempFile("packforge-home-asked", ".dspack");
        try {
            DshPackForge.export(instance, withoutAsking, DshPackForge.Options.of(instance), null);
            assertTrue(members(withoutAsking).stream().noneMatch(member -> member.startsWith("home/")),
                    "nothing from the home travels unasked: " + members(withoutAsking));

            java.util.List<String> said = new java.util.ArrayList<>();
            DshPackForge.export(instance, asked, DshPackForge.Options.of(instance),
                    java.util.List.of("skills/my-skill/SKILL.md", "AGENTS.md",
                            ".agent-presets/coding/agent.cordis.yml", ".credentials.yaml",
                            "settings.yaml", "profiles/other/package.json", "nowhere.txt"),
                    said::add);

            java.util.Set<String> travelled = members(asked);
            assertTrue(travelled.contains("home/skills/my-skill/SKILL.md"), travelled.toString());
            assertTrue(travelled.contains("home/AGENTS.md"), travelled.toString());
            assertTrue(travelled.contains("home/.agent-presets/coding/agent.cordis.yml"),
                    travelled.toString());
            assertTrue(travelled.stream().noneMatch(member -> member.startsWith("home/.credentials")
                            || member.startsWith("home/settings.yaml")
                            || member.startsWith("home/profiles/")),
                    "what the filter refuses does not travel even when it is named: " + travelled);
            assertTrue(said.stream().anyMatch(line -> line.contains("Not carried from the home")),
                    "and what was named but did not travel is said so: " + said);
            // The profile's own files are where they were: `home/` is an addition, not a move.
            assertTrue(travelled.contains("package.json"), travelled.toString());

            // What the caller can choose from is the filtered home, and it is asked of the same scan.
            DshPackForge.Scan candidates = DshPackForge.scanHome(home);
            java.util.List<String> offered = candidates.files().stream()
                    .map(DshPackForge.Entry::relative).toList();
            assertTrue(offered.contains("skills/my-skill/SKILL.md"), offered.toString());
            assertTrue(offered.contains("AGENTS.md"), offered.toString());
            assertFalse(offered.contains("skills/.system/note.md"), offered.toString());
            assertEquals("runtime", candidates.excluded().get("skills/.system/"),
                    "an installed system skill is not offered");
            assertEquals("profile", candidates.excluded().get("profiles/"),
                    "and the home's own profiles are not, at any depth");
            assertTrue(offered.stream().noneMatch(candidate -> candidate.startsWith("profiles/")),
                    "every other profile on the machine is not this pack's to carry: " + offered);
            assertTrue(offered.stream().noneMatch(candidate -> candidate.contains("credentials")
                            || candidate.equals("settings.yaml") || candidate.equals(".anonymous-user-id")
                            || candidate.startsWith("attachments/")),
                    "the home-level filter still applies: " + offered);

            // And the importer lands them at the home root rather than in the profile.
            Path destination = Files.createTempDirectory("packforge-home-landed");
            Path targetHome = Files.createTempDirectory("packforge-home-target");
            DshPackInstaller.land(asked, destination, targetHome);
            assertTrue(Files.isRegularFile(targetHome.resolve("skills/my-skill/SKILL.md")));
            assertTrue(Files.isRegularFile(targetHome.resolve("AGENTS.md")));
            assertFalse(Files.exists(destination.resolve("AGENTS.md")),
                    "a home-level file does not land in the profile");
        } finally {
            Files.deleteIfExists(withoutAsking);
            Files.deleteIfExists(asked);
            DshInstanceManager.delete(id);
        }
    }

    /// Lists an archive's members.
    ///
    /// @param archive the archive
    /// @return its member names
    /// @throws Exception when it cannot be read
    private static java.util.Set<String> members(Path archive) throws Exception {
        java.util.Set<String> names = new java.util.TreeSet<>();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive.toFile())) {
            zip.stream().map(java.util.zip.ZipEntry::getName).forEach(names::add);
        }
        return names;
    }

    @Test
    void aPluginUnderDevelopmentIsNamedBeforeTheExportRefusesIt() throws Exception {
        // What `dsh plugin add <a directory>` leaves behind: a `link:` entry pointing at somebody's
        // working tree, outside the instance. This format names published sources, so it cannot carry
        // such a plugin at all — and the two things that matter are that the refusal says which kind of
        // plugin it is (not "the registry does not publish that version", which is the other case), and
        // that the name comes out **before** it, while the person can still install a published version
        // instead.
        String id = "packforge-dev-" + Long.toHexString(System.nanoTime());
        Path home = Files.createTempDirectory("packforge-dev-home");
        DshInstance instance = DshInstanceManager.create(id, "0.1.6-alpha.2",
                DshInstance.DEFAULT_PROFILE, home, DshHomeMode.ISOLATED, null,
                java.util.List.of(), java.util.Map.of());
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"), """
                {"name":"dsh-profile-web","private":true,
                 "dependencies":{"dsh-fork":"link:/somebody/working/tree"}}
                """);

        try {
            java.util.List<String> said = new java.util.ArrayList<>();
            DshException refused = org.junit.jupiter.api.Assertions.assertThrows(DshException.class,
                    () -> DshPackForge.buildManifest(instance, DshPackForge.Options.of(instance),
                            said::add));

            assertTrue(refused.getMessage().contains("under development"),
                    "the refusal says which kind of plugin this is: " + refused.getMessage());
            assertTrue(refused.getMessage().contains("dsh-fork"),
                    "and which one it is: " + refused.getMessage());
            assertTrue(said.stream().anyMatch(line -> line.contains("dsh-fork")),
                    "the name is said before the refusal, while the person can act on it: " + said);
        } finally {
            DshInstanceManager.delete(id);
        }
    }

    /// The profile's own files sit at the root, and everything else under `overrides/`.
    ///
    /// The two places are not a style: the installer copies the root files **before** it resolves
    /// the dependencies and the overrides afterwards, so a `package.json` written under
    /// `overrides/` arrives after the install that needed it — the pack then installs cleanly and
    /// boots with none of its plugins.
    ///
    /// @throws Exception when the pack cannot be written, read, or landed
    @Test
    void theProfilesOwnFilesSitAtTheRootAndTheRestUnderOverrides() throws Exception {
        String id = "packforge-layout";
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            instance = DshInstanceManager.create(id, "0.1.6-alpha.2",
                    DshInstance.DEFAULT_PROFILE, Files.createTempDirectory("packforge-layout-home"),
                    DshHomeMode.ISOLATED, null, java.util.List.of(), java.util.Map.of());
        }
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"), """
                {"name":"dsh-profile-web","private":true,
                 "dependencies":{"dshmarket":"1.52.0"},
                 "dsh":{"profile":{"bundles":["@deepseek-ai/dsh-base","dshmarket"]}}}
                """);
        Files.writeString(profile.resolve("pnpm-lock.yaml"), "lockfileVersion: '9.0'\n");
        Files.writeString(profile.resolve("cordis.patch.yml"), "- id: title\n");

        Path pack = Files.createTempFile("packforge-layout", ".dspack");
        try {
            DshPackForge.export(instance, pack, DshPackForge.Options.of(instance), null);

            java.util.Set<String> members = new java.util.TreeSet<>();
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(pack.toFile())) {
                zip.stream().map(java.util.zip.ZipEntry::getName).forEach(members::add);
            }
            assertTrue(members.contains("package.json"),
                    "the manifest belongs at the root, where the resolve reads it: " + members);
            assertTrue(members.contains("pnpm-lock.yaml"), "and so does the lock file");
            assertTrue(members.contains("overrides/cordis.patch.yml"),
                    "the patch is composition, so it is an override");
            assertFalse(members.contains("overrides/package.json"),
                    "a manifest under overrides lands after the install that needed it");

            // And what the installer does with each place, in the order it does it.
            Path destination = Files.createTempDirectory("packforge-landed");
            DshPackInstaller.Landed machine = DshPackInstaller.land(pack, destination,
                    Files.createTempDirectory("packforge-target-home"),
                    java.util.EnumSet.of(DshPackInstaller.Part.MACHINE));
            assertEquals(2, machine.machine(), "the pack carries two machine files");
            assertTrue(Files.isRegularFile(destination.resolve("package.json")),
                    "they land first, because the dependency install reads them");
            assertFalse(Files.exists(destination.resolve("cordis.patch.yml")),
                    "and the overrides do not land with them");

            DshPackInstaller.land(pack, destination, null,
                    java.util.EnumSet.of(DshPackInstaller.Part.OVERRIDES));
            assertTrue(Files.isRegularFile(destination.resolve("cordis.patch.yml")),
                    "the composition lands afterwards, on top of what the install wrote");
        } finally {
            Files.deleteIfExists(pack);
            DshInstanceManager.delete(id);
        }
    }

    /// Every credential the importer refuses, the exporter refuses too.
    ///
    /// The two sides used to hold two hand-written lists, and the importer's comment claimed they
    /// were the same one. They were not, and the direction the difference ran was the wrong one:
    /// `.netrc`, `.pypirc`, `.yarnrc` and `.yarnrc.yml` could be **published** inside a `.dspack`
    /// that the importer would have refused to write, and a key named `id_ed25519` with no extension
    /// at all escaped both. There is one list now, and this asks it of both sides — the export side
    /// through `exclusionOf`, which is what its scan decides with, and the import side through
    /// `land`, the entry point that writes a pack's members — so re-splitting the list fails here
    /// rather than in somebody's published pack.
    ///
    /// @param temp where the packs are built
    /// @throws Exception when a pack cannot be written
    @Test
    void everyCredentialTheImporterRefusesTheExporterRefusesToo(@TempDir Path temp) throws Exception {
        java.util.List<String> spellings = new java.util.ArrayList<>(DshPackForge.CREDENTIAL_NAMES);
        for (String prefix : DshPackForge.CREDENTIAL_KEY_PREFIXES) {
            spellings.add(prefix);
            spellings.add(prefix + ".pub");
        }
        for (String extension : DshPackForge.CREDENTIAL_EXTENSIONS) {
            spellings.add("carried" + extension);
        }
        // The shapes, which are rules rather than spellings and so cannot be read off a list.
        spellings.addAll(java.util.List.of("credentials.yml", "kept.credentials"));

        for (String spelling : spellings) {
            assertNotNull(DshPackForge.exclusionOf(spelling),
                    "the export side refuses what the import side does: " + spelling);
            assertThrows(DshException.class,
                    () -> DshPackInstaller.land(credentialPack(temp, spelling),
                            temp.resolve("landed"), null,
                            java.util.EnumSet.of(DshPackInstaller.Part.OVERRIDES)),
                    "the import side refuses " + spelling);
        }

        // The spellings this defect was about, named individually, because each of them used to be
        // refused on one side and not the other: the first four the export side published, the next
        // three escaped both, and the last three the import side refused and the export side did not.
        for (String spelling : java.util.List.of(".netrc", ".pypirc", ".yarnrc", ".yarnrc.yml",
                "id_ed25519", "id_ecdsa", "id_ed448", "carried.jks", "carried.keystore",
                "credentials.yml")) {
            assertNotNull(DshPackForge.exclusionOf(spelling),
                    "a credential one side used to refuse and the other did not: " + spelling);
        }
    }

    /// Builds a pack carrying one file under `overrides/`.
    ///
    /// @param directory where the pack is written
    /// @param member    the member's name
    /// @return the pack
    /// @throws Exception when the archive cannot be written
    private static Path credentialPack(Path directory, String member) throws Exception {
        Path pack = directory.resolve("pack-" + member.replace('.', '-') + ".dspack");
        try (java.util.zip.ZipOutputStream zip =
                     new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(pack))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("overrides/" + member));
            zip.write("a credential".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return pack;
    }

    @Test
    void aNpmVersionNeedsNoResolving() throws Exception {
        assertEquals("1.2.3", DshPackForge.pin("anything", "1.2.3", null));
        assertEquals("^1.2.3", DshPackForge.pin("anything", "^1.2.3", null));
    }

    @Test
    void hashingMatchesWhatAPublisherWouldWrite(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("pack.dspack");
        Files.writeString(file, "hello");
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DshPackForge.sha256(file));
        assertEquals("pack", DshPackForge.Options.kebab(""),
                "an instance whose name has nothing usable still gets an identifier");
    }
}
