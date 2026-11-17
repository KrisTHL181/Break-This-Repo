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
package org.jackhuang.hmcl.dsh.skin;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Files;
import java.nio.file.Path;

/// Where chosen account skins are kept on disk.
///
/// Split out of the desktop `DshSkin`, which also decoded and normalised the
/// pictures for its 3D preview — rendering HDSL-web does not do (the browser
/// draws what it is pointed at). What survives here is the file half: one
/// file per account inside the launcher's own directory, named after a hash
/// of the account's key.
@NotNullByDefault
public final class DshSkinFiles {
    /// Where chosen skins are kept, inside the launcher's data directory.
    private static final String DIRECTORY = "skins";

    /// The name a skin's file has inside [#DIRECTORY].
    private static final String SUFFIX = ".png";

    /// The name a cape's file has inside [#DIRECTORY].
    private static final String CAPE_SUFFIX = "-cape.png";

    private DshSkinFiles() {
    }

    /// Returns the file an account's skin is kept in.
    ///
    /// The account's key is hashed rather than used literally: a key is `vendor|name`, so a name
    /// containing a separator or a slash would put the file somewhere else entirely.
    ///
    /// @param accountKey the account's key
    /// @return the path, which may not exist
    public static Path file(String accountKey) {
        return org.jackhuang.hmcl.Metadata.HMCL_USER_HOME.resolve(DIRECTORY)
                .resolve(hashedName(accountKey) + SUFFIX);
    }

    /// Returns the file an account's cape is kept in.
    ///
    /// Beside the skin and named from the same hash, so the two travel together and neither can be
    /// found without the account they belong to.
    ///
    /// @param accountKey the account's key
    /// @return the path, which may not exist
    public static Path capeFile(String accountKey) {
        return org.jackhuang.hmcl.Metadata.HMCL_USER_HOME.resolve(DIRECTORY)
                .resolve(hashedName(accountKey) + CAPE_SUFFIX);
    }

    /// Reports whether a skin has been chosen.
    ///
    /// The desktop build answered by decoding the picture, which also caught
    /// corrupt files; this copy only checks that the file is there, which is
    /// the question the callers ask.
    ///
    /// @param accountKey the account's key
    /// @return whether the file is there
    public static boolean isSet(String accountKey) {
        return Files.isRegularFile(file(accountKey));
    }

    /// Returns the identity the launcher picks an account's default skin from.
    ///
    /// There is no login here, so there are no account UUIDs; the closest honest
    /// equivalent is a name-based UUID over the account's key, which is what the
    /// original feeds its default-skin arithmetic for offline accounts.
    ///
    /// @param accountKey the account's key
    /// @return its identity
    public static java.util.UUID uuidOf(String accountKey) {
        return java.util.UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + accountKey).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /// Returns the file name for an account's key.
    ///
    /// @param accountKey the account's key
    /// @return the name
    private static String hashedName(String accountKey) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(accountKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // Every JVM has SHA-256; a fallback that cannot collide with a hash is still needed so the
            // method never throws.
            return "key-" + Integer.toHexString(accountKey.hashCode());
        }
    }
}
