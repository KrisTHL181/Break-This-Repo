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
package org.jackhuang.hmcl.web.transcode;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// What was recorded when the owner enabled the transcode feature.
///
/// There is no upstream EULA and no second sub-feature: this record only answers
/// "which notice did this person accept, when, and from where". It is written once
/// per enable and replaced as a whole on the next enable, so timestamps from two
/// different acceptances never mix.
@NotNullByDefault
public record TranscodeConsent(
        @SerializedName("termsVersion") int termsVersion,
        @SerializedName("termsTextHash") String termsTextHash,
        @SerializedName("acceptedAt") String acceptedAt,
        @SerializedName("acceptedBy") String acceptedBy,
        @SerializedName("acceptedFrom") String acceptedFrom,
        @SerializedName("ackMethod") String ackMethod,
        @SerializedName("uiVersion") String uiVersion,
        @SerializedName("clientUa") @Nullable String clientUa,
        @SerializedName("buildCommit") String buildCommit,
        @SerializedName("sawDeclineOption") boolean sawDeclineOption) {

    public TranscodeConsent {
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