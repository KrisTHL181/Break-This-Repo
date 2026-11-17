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

/// What the panel recorded when the owner enabled the Minecraft feature.
///
/// This is the evidence side of the enable flow (see `docs/minecraft.md` §11.4):
/// which terms version and text hash were shown, which EULA text was fetched and
/// when, who accepted, from where, and how. The panel only *records* the
/// acceptance — the user is the party that accepted, Mojang's EULA is between the
/// operator and Mojang.
///
/// The values are written once per enable and never edited by the panel; a new
/// enable (after a terms change) replaces the record as a whole, which is what
/// keeps the timestamps from mixing two different acceptances.
@NotNullByDefault
public record McConsent(
        @SerializedName("termsVersion") int termsVersion,
        @SerializedName("termsTextHash") String termsTextHash,
        @SerializedName("eulaHash") String eulaHash,
        @SerializedName("eulaRetrievedAt") String eulaRetrievedAt,
        @SerializedName("termsAcceptedAt") String termsAcceptedAt,
        @SerializedName("eulaAcceptedAt") String eulaAcceptedAt,
        @SerializedName("acceptedBy") String acceptedBy,
        @SerializedName("acceptedFrom") String acceptedFrom,
        @SerializedName("ackMethod") String ackMethod,
        @SerializedName("sawDeclineOption") boolean sawDeclineOption,
        @SerializedName("uiVersion") String uiVersion,
        @SerializedName("clientUa") @Nullable String clientUa,
        @SerializedName("buildCommit") String buildCommit,
        @SerializedName("prevAuditHash") @Nullable String prevAuditHash) {

    public McConsent {
        if (termsVersion < 1) {
            throw new IllegalArgumentException("termsVersion must be positive");
        }
        require(termsTextHash, "termsTextHash");
        require(eulaHash, "eulaHash");
        require(eulaRetrievedAt, "eulaRetrievedAt");
        require(termsAcceptedAt, "termsAcceptedAt");
        require(eulaAcceptedAt, "eulaAcceptedAt");
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