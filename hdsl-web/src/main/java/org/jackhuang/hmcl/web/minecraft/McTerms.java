/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
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
package org.jackhuang.hmcl.web.minecraft;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/// The panel's own terms for the Minecraft feature.
///
/// The text ships with the panel (`src/main/resources/minecraft/terms-v3.txt`)
/// and its hash is what the enable flow records — the client never supplies the
/// hash on its own, it echoes the one this class published. Bumping the text
/// means bumping [VERSION]; the doc's rule is "文本改了但版本号没改，等于没有记录".
@NotNullByDefault
public final class McTerms {

    /// The version the panel offers. The text and this number move together.
    public static final int VERSION = 3;

    private static final String RESOURCE = "/minecraft/terms-v3.txt";

    private static volatile String text;
    private static volatile String hash;

    private McTerms() {
    }

    /// The terms text as shipped.
    ///
    /// @return the text
    public static String text() {
        ensureLoaded();
        return text;
    }

    /// `sha256:` plus the hex digest of the text's UTF-8 bytes.
    ///
    /// @return the hash
    public static String hash() {
        ensureLoaded();
        return hash;
    }

    private static synchronized void ensureLoaded() {
        if (text != null) {
            return;
        }
        try (InputStream in = McTerms.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                text = "";
                hash = "sha256:missing";
                return;
            }
            byte[] bytes = in.readAllBytes();
            text = new String(bytes, StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
        } catch (IOException | NoSuchAlgorithmException e) {
            text = "";
            hash = "sha256:unavailable";
        }
    }
}