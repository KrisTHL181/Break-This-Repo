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
package org.jackhuang.hmcl.web.logs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNullByDefault;

import java.math.BigDecimal;

/// One validated `PUT /api/settings/logs` body.
///
/// Validation is deliberately paranoid because this is the one endpoint that
/// writes a deployment setting from the browser:
///
/// - **numbers only** — `"200"` (a string), `true`, `null` and `1.5` are all
///   refused instead of being coerced, so a payload cannot smuggle a value
///   through a lenient parser;
/// - **exact integers** — `1e9` and `Integer.MAX_VALUE` overflow out rather
///   than wrapping;
/// - **allow-list** — an unknown field is an error, not something to ignore:
///   silently dropping `{"audit_days": 3650}` would leave the caller believing
///   it was applied;
/// - **up only** — a new window may never be shorter than the current one, and
///   never below [LogRetention#FLOOR_DAYS]; dragging retention down to make an
///   audit trail disappear is exactly what this rule is for.
@NotNullByDefault
public final class RetentionUpdate {

    /// The parsed, validated pair.
    public record Windows(int auditDays, int panelDays) {
    }

    /// 输入不合法；`getMessage()` 直接回给调用方。
    public static final class Invalid extends Exception {
        public Invalid(String message) {
            super(message);
        }
    }

    private RetentionUpdate() {
    }

    /// @param body         the request body
    /// @param currentAudit the audit window in force
    /// @param currentPanel the panel window in force
    /// @throws Invalid when the body is not an allow-listed, numeric, in-range, non-decreasing update
    public static Windows parse(JsonObject body, int currentAudit, int currentPanel) throws Invalid {
        int auditDays = currentAudit;
        int panelDays = currentPanel;
        boolean touched = false;

        for (var entry : body.entrySet()) {
            switch (entry.getKey()) {
                case "auditDays" -> {
                    auditDays = window(entry.getValue(), "auditDays");
                    touched = true;
                }
                case "panelDays" -> {
                    panelDays = window(entry.getValue(), "panelDays");
                    touched = true;
                }
                default -> throw new Invalid("unknown field `" + entry.getKey()
                        + "` (only auditDays and panelDays are accepted)");
            }
        }
        if (!touched) {
            throw new Invalid("a body with auditDays and/or panelDays is required");
        }
        if (auditDays < currentAudit || panelDays < currentPanel) {
            throw new Invalid("retention can only be raised, never lowered");
        }
        if (auditDays < LogRetention.FLOOR_DAYS || auditDays > LogRetention.HARD_CAP_DAYS
                || panelDays < LogRetention.FLOOR_DAYS || panelDays > LogRetention.HARD_CAP_DAYS) {
            throw new Invalid("retention must stay between " + LogRetention.FLOOR_DAYS
                    + " and " + LogRetention.HARD_CAP_DAYS + " days");
        }
        return new Windows(auditDays, panelDays);
    }

    /// Reads one day count. Anything that is not a JSON integer inside `int` is refused.
    private static int window(JsonElement element, String name) throws Invalid {
        if (!element.isJsonPrimitive()) {
            throw new Invalid(name + " must be an integer number of days");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            throw new Invalid(name + " must be an integer number of days");
        }
        BigDecimal value = primitive.getAsBigDecimal();
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw new Invalid(name + " must be a whole number of days");
        }
    }
}
