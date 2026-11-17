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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetentionUpdateTest {

    private static JsonObject body(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void raisesWithinTheWindow() throws Exception {
        RetentionUpdate.Windows windows = RetentionUpdate.parse(body("{\"auditDays\":365}"), 180, 180);
        assertEquals(365, windows.auditDays());
        assertEquals(180, windows.panelDays());
    }

    @Test
    void acceptsTheCeilingItself() throws Exception {
        RetentionUpdate.Windows windows =
                RetentionUpdate.parse(body("{\"auditDays\":1460}"), 180, 180);
        assertEquals(LogRetention.HARD_CAP_DAYS, windows.auditDays());
    }

    @Test
    void refusesToLower() {
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":200}"), 365, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"panelDays\":179}"), 180, 180));
    }

    @Test
    void refusesBelowTheFloorOrAboveTheCeiling() {
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":90}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":1461}"), 180, 180));
    }

    @Test
    void refusesAnythingThatIsNotAJsonInteger() {
        // 字符串、浮点、布尔、null、科学计数法溢出，全部拒绝——不做隐式转换。
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":\"365\"}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":365.5}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":true}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":null}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":1e9}"), 180, 180));
    }

    @Test
    void refusesUnknownFieldsInsteadOfIgnoringThem() {
        // 这是「写了但没生效」最容易骗过人的形状：audit_days 是下划线写法，
        // 静默忽略会让调用方以为已经调大。
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"audit_days\":3650}"), 180, 180));
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{\"auditDays\":365,\"admin\":true}"), 180, 180));
    }

    @Test
    void refusesAnEmptyBody() {
        assertThrows(RetentionUpdate.Invalid.class,
                () -> RetentionUpdate.parse(body("{}"), 180, 180));
    }

    @Test
    void sameValueIsAcceptedForTheServletToTreatAsANoOp() throws Exception {
        RetentionUpdate.Windows windows = RetentionUpdate.parse(body("{\"auditDays\":180}"), 180, 180);
        assertEquals(180, windows.auditDays());
        assertEquals(180, windows.panelDays());
    }
}
