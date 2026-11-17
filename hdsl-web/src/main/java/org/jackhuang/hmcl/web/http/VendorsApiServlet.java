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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

/// The vendor catalogue, mapped at `/api/vendors`:
///
/// - `GET /api/vendors` — every supplier the account dialog may offer: the
///   built-in ones, then the ones added by address. `kinds` lists the wire
///   protocols the vendor speaks (one, today; an array so a vendor speaking
///   two does not need a new field).
@NotNullByDefault
public final class VendorsApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray vendors = new JsonArray();
        for (DshVendor vendor : SettingsManager.settings().allVendors()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", vendor.id());
            entry.addProperty("name", vendor.displayName());
            entry.add("endpoint", vendor.baseUrl() == null
                    ? JsonNull.INSTANCE : new JsonPrimitive(vendor.baseUrl()));
            entry.addProperty("envVar", vendor.apiKeyEnv());
            JsonArray kinds = new JsonArray();
            kinds.add(vendor.api());
            entry.add("kinds", kinds);
            entry.addProperty("preferred", vendor.preferred());
            vendors.add(entry);
        }
        JsonObject body = new JsonObject();
        body.add("vendors", vendors);
        Json.writePreservingNulls(response, body);
    }
}
