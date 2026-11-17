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

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// What the panel recorded when the owner enabled third-party server cores.
///
/// The second gate has its own acceptance because it is a different decision:
/// Mojang's EULA is between the operator and Mojang, while Paper/Fabric/Purpur
/// are other people's software with their own licences, and the panel does not
/// warrant that they run. The record is deliberately lighter than [McConsent]
/// — there is no EULA to hash — but it keeps the same evidence shape so an
/// audit can answer "who turned this on, when, from where, having read which
/// text".
@NotNullByDefault
public record McThirdPartyConsent(
        @SerializedName("termsVersion") int termsVersion,
        @SerializedName("termsTextHash") String termsTextHash,
        @SerializedName("acceptedAt") String acceptedAt,
        @SerializedName("acceptedBy") String acceptedBy,
        @SerializedName("acceptedFrom") String acceptedFrom,
        @SerializedName("ackMethod") String ackMethod,
        @SerializedName("uiVersion") String uiVersion,
        @SerializedName("clientUa") @Nullable String clientUa,
        @SerializedName("buildCommit") String buildCommit) {

    public McThirdPartyConsent {
        if (termsVersion < 1) {
            throw new IllegalArgumentException("termsVersion must be positive");
        }
        require(termsTextHash, "termsTextHash");
        require(acceptedAt, "acceptedAt");
        require(acceptedBy, "acceptedBy");
        require(acceptedFrom, "acceptedFrom");
        require(ackMethod, "ackMethod");
        require(uiVersion, "uiVersion");
        require(buildCommit, "buildCommit");
    }

    private static void require(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("the acceptance record needs " + name);
        }
    }
}
