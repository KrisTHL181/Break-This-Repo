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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what the launcher reads out of a packed plugin, and what it refuses.
///
/// A plugin file is an npm package, which is a gzipped tar; the launcher reads the
/// one entry it needs out of it rather than taking a dependency the size of the
/// launcher to do so, so that reading is what is pinned here. Whether a package
/// declares a bundle patch is the other half: a package that does not is installed
/// and then does nothing at all, which is worth saying before it is installed
/// rather than after.
class DshLocalPluginsTest {
    @Test
    void aPackedPluginIsReadWithoutUnpackingIt(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("dsh-example-1.2.3.tgz");
        Files.write(file, pack("dsh-example", "1.2.3", "./cordis.patch.yml", "the readme"));

        DshLocalPlugins.Package pkg = DshLocalPlugins.inspect(file);

        assertEquals("dsh-example", pkg.name());
        assertEquals("1.2.3", pkg.version());
        assertEquals("./cordis.patch.yml", pkg.bundlePatch());
        assertTrue(pkg.isBundle(), "a package that declares a patch is what the profile can activate");
    }

    @Test
    void aPackageThatDeclaresNoBundleIsSaidSo(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("plain-1.0.0.tgz");
        Files.write(file, pack("plain", "1.0.0", null, "no patch here"));

        DshLocalPlugins.Package pkg = DshLocalPlugins.inspect(file);

        assertEquals("plain", pkg.name());
        assertFalse(pkg.isBundle(),
                "the harness would install it and leave it out of the bundle list, which does nothing");
    }

    @Test
    void somethingThatIsNotAPackedPluginIsRefused(@TempDir Path directory) throws Exception {
        Path notATar = directory.resolve("notes.tgz");
        Files.writeString(notATar, "this is not an archive");
        assertThrows(DshException.class, () -> DshLocalPlugins.inspect(notATar));

        Path wrongExtension = directory.resolve("plugin.zip");
        Files.write(wrongExtension, pack("x", "1.0.0", null, ""));
        assertThrows(DshException.class, () -> DshLocalPlugins.inspect(wrongExtension),
                "the launcher installs packed plugins, and says which kind it takes");

        Path noManifest = directory.resolve("empty-1.0.0.tgz");
        Files.write(noManifest, packWithoutManifest());
        assertThrows(DshException.class, () -> DshLocalPlugins.inspect(noManifest));
    }

    @Test
    void theSpecificationTheProfileRecordssIstheInstancesOwnCopy(@TempDir Path directory) throws Exception {
        // The point of the whole exercise: a profile records the path it installed
        // from, and resolves it again on every later operation, so the path has to
        // be one that stays where it is. This checks the name the launcher gives
        // that copy, which is what the caller then installs from.
        Path file = directory.resolve("whatever-the-user-called-it.tgz");
        Files.write(file, pack("@someone/dsh-thing", "2.0.0", "./patch.yml", "body"));

        DshLocalPlugins.Package pkg = DshLocalPlugins.inspect(file);
        assertEquals("@someone/dsh-thing", pkg.name());

        String expected = "someone_dsh-thing-2.0.0.tgz";
        Path copy = directory.resolve(expected);
        Files.copy(file, copy);
        assertTrue(Files.isRegularFile(copy), "the copy is named after the package, not after the file it came from");
    }

    /// Builds a gzipped tar holding a `package/package.json`.
    ///
    /// @param name    the package name
    /// @param version the package version
    /// @param patch   the bundle patch, or `null` for a package that declares none
    /// @param readme  the contents of another entry, so the reader has to walk past one
    /// @return the archive's bytes
    private static byte[] pack(String name, String version, String patch, String readme) throws Exception {
        String manifest = """
                {"name":"%s","version":"%s"%s}
                """.formatted(name, version,
                patch == null ? "" : ",\"dsh\":{\"bundle\":{\"patch\":\"" + patch + "\"}}");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(entry("package/README.md", readme.getBytes(StandardCharsets.UTF_8)));
            gzip.write(entry("package/package.json", manifest.getBytes(StandardCharsets.UTF_8)));
            gzip.write(new byte[1024]);
        }
        return out.toByteArray();
    }

    /// Builds a gzipped tar that holds no manifest at all.
    ///
    /// @return the archive's bytes
    private static byte[] packWithoutManifest() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(entry("package/index.js", "module.exports = 1".getBytes(StandardCharsets.UTF_8)));
            gzip.write(new byte[1024]);
        }
        return out.toByteArray();
    }

    /// Builds one tar entry.
    ///
    /// @param name the entry's name
    /// @param body the entry's bytes
    /// @return the header and body, padded to a block
    private static byte[] entry(String name, byte[] body) {
        byte[] block = new byte[512];
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, block, 0, Math.min(nameBytes.length, 100));
        byte[] size = Long.toOctalString(body.length).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(size, 0, block, 124, size.length);
        block[156] = '0';
        for (int i = 148; i < 156; i++) {
            block[i] = ' ';
        }

        int padding = (512 - (body.length % 512)) % 512;
        byte[] result = new byte[512 + body.length + padding];
        System.arraycopy(block, 0, result, 0, 512);
        System.arraycopy(body, 0, result, 512, body.length);
        return result;
    }
}
