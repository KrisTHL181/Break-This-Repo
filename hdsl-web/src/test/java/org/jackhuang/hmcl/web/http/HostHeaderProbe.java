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

/// Runs in a **fresh JVM** (see [HostHeaderSupportTest]) to reproduce the
/// ordering hazard: the JDK freezes the restricted-header allow-list when the
/// first `HttpClient` is constructed.
///
/// - `bad`: touch a class that owns a static `HttpClient` first, then ask the
///   guard to verify → it must refuse;
/// - `good`: set the property first, then the same class, then verify → pass.
public final class HostHeaderProbe {

    private HostHeaderProbe() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "good" : args[0];
        // The brand installer owns `static final HttpClient CLIENT`; loading it
        // is what the panel did too early on 2026-10-28.
        if ("bad".equals(mode)) {
            Class.forName("org.jackhuang.hmcl.web.brand.BrandInstaller");
            try {
                HostHeaderSupport.verify();
                System.out.println("LEAKED");
            } catch (IllegalStateException expected) {
                System.out.println("REFUSED");
            }
            return;
        }
        HostHeaderSupport.allow();
        Class.forName("org.jackhuang.hmcl.web.brand.BrandInstaller");
        HostHeaderSupport.verify();
        System.out.println("OK");
    }
}
