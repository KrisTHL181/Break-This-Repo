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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for what an account is: a key, the vendor it is for, and the name it is known by.
class DshAccountTest {
    @Test
    void aRefusedModelListSaysWhatTheEndpointAnswered() throws Exception {
        // 一个会说话的 401，就是“key 打错 region / 过期”在现场的样子。原因必须留到启动失败
        // 的那句话里，否则界面上只剩一句“启动失败”，人只能猜。
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            // 正文里故意带上 key：摘要必须把它抹掉。
            byte[] body = ("{\"error\":{\"message\":\"invalid_api_key\",\"hint\":\"sk-test\"}}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            DshAccount account = new DshAccount(DshAccount.AccountKind.THIRD_PARTY, "openrouter",
                    "sk-test", "http://127.0.0.1:" + port + "/v1", null, null, null);

            DshAccount.ModelsAnswer answer = account.fetchModelsAnswer();

            assertTrue(answer.empty());
            assertNotNull(answer.failure());
            assertTrue(answer.failure().contains("HTTP 401"), answer.failure());
            assertTrue(answer.failure().contains("invalid_api_key"), answer.failure());
            assertFalse(answer.failure().contains("sk-test"),
                    "摘要里不能出现 key：" + answer.failure());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void anAccountWithoutAnEndpointSaysSoInsteadOfSilentlyListingNothing() {
        DshAccount offline = new DshAccount(DshAccount.AccountKind.OFFLINE, "offline", "", null, "me", null, null);

        DshAccount.ModelsAnswer answer = offline.fetchModelsAnswer();

        assertTrue(answer.empty());
        assertNotNull(answer.failure());
        assertTrue(answer.failure().contains("no endpoint"), answer.failure());
    }

    @Test
    void anAccountIsNamedByItsVendorAndItsLabel() {
        // The key names the account, so it is written to a file and read back; a vendor used twice
        // with two keys has to stay two accounts, which is what the label is for.
        assertEquals("deepseek", new DshAccount("deepseek", "k", null, null).key());
        assertEquals("deepseek|work", new DshAccount("deepseek", "k", null, "work").key());
        assertEquals("deepseek|work", new DshAccount("deepseek", "k", null, "  work  ").key());
        assertTrue(new DshAccount("deepseek", "k", null, "work").matchesKey("deepseek|work"));
        assertFalse(new DshAccount("deepseek", "k", null, "work").matchesKey("deepseek"));
        assertFalse(new DshAccount("deepseek", "k", null, null).matchesKey(null));
    }

    @Test
    void theVendorPublishesTheEndpointUnlessTheAccountDoes() {
        DshAccount vendorDefault = new DshAccount("deepseek", "k", null, null);
        assertEquals("https://api.deepseek.com", vendorDefault.endpoint());

        // An account may point at a gateway the launcher has never heard of, which is the only way
        // to use one whose address is per account.
        DshAccount gateway = new DshAccount("totally-unknown", "k", "https://my.gateway/v1", null);
        assertEquals("https://my.gateway/v1", gateway.endpoint());
        assertNull(gateway.vendor());
    }

    @Test
    void whatTheKeyCheckSaysComesFromTheBundles() {
        // The sentences the check reports are read from the bundles like everything else the
        // interface shows. They used to be written into the code in Chinese, so a person running
        // the launcher in any other language was answered in one they may not read.
        //
        // An account on a supplier nothing knows and with no address of its own is the one path
        // through the check that answers without a request, which is what makes it testable here.
        DshAccount.Check check = new DshAccount("totally-unknown", "k", null, null).check();

        assertEquals(DshAccount.Outcome.UNREACHABLE, check.outcome());
        assertEquals(org.jackhuang.hmcl.util.i18n.I18n.i18n("dsh.account.check.no_endpoint"),
                check.message());
        assertNotEquals("dsh.account.check.no_endpoint", check.message(),
                "a key no bundle holds comes back as itself, which is not a sentence");
    }

    @Test
    void aSupplierThePersonAddedPublishesItsEndpointToo() {
        // The defect this pins: a supplier added by address is not one the launcher ships, so its
        // account found no vendor — and reached a launch with no address at all, which is the route
        // the harness refuses with "needs a baseURL".
        DshVendor added = DshVendor.discovered("opencode", "OpenCode", "https://api.opencode.ai/v1");
        java.util.List<DshVendor> addedVendors =
                org.jackhuang.hmcl.setting.SettingsManager.settings().getCustomVendors();
        addedVendors.add(added);
        try {
            DshAccount account = new DshAccount("opencode", "k", null, null);

            assertNull(account.vendor(), "a supplier somebody added is not one the launcher ships");
            assertEquals("https://api.opencode.ai/v1", account.endpoint());

            // And the account's own address still wins over the supplier's.
            assertEquals("https://proxy.example/v1",
                    new DshAccount("opencode", "k", "https://proxy.example/v1", null).endpoint());
        } finally {
            addedVendors.remove(added);
        }
    }

    @Test
    void aNameFallsBackToTheVendors() {
        assertEquals("DeepSeek", new DshAccount("deepseek", "k", null, null).displayName());
        assertEquals("work", new DshAccount("deepseek", "k", null, "work").displayName());
        assertEquals("totally-unknown", new DshAccount("totally-unknown", "k", null, null).displayName());
    }

    @Test
    void aKeyIsShownMasked() {
        // A row is a record of what is configured, not a place to read a secret back from: a full
        // key on screen is a key in every screenshot.
        String masked = new DshAccount("deepseek", "sk-abcdef0123456789", null, null).maskedKey();
        assertTrue(masked.startsWith("sk-a"), masked);
        assertFalse(masked.contains("def0123456789"), "the middle of the key must not be shown");
        assertEquals("••••", new DshAccount("deepseek", "short", null, null).maskedKey(),
                "a key too short to mask is not partly shown either");
    }

    @Test
    void aModelIsWhateverWasNamedOrNothing() {
        assertEquals("", new DshAccount("deepseek", "k", null, null).modelOrDefault());
        assertEquals("qwen3:32b", new DshAccount(DshAccount.AccountKind.OFFICIAL,
                "deepseek", "k", null, null, "qwen3:32b", null).modelOrDefault());
    }

    @Test
    void onlyAnAccountWithAKeyHandsOneOver() {
        // This is what every launch step asks before writing a route, setting a default model or
        // putting anything in the child's environment. An offline account is a name and a face, and
        // a launcher that handed its empty key over would point the harness at a supplier that
        // cannot answer.
        assertTrue(new DshAccount("deepseek", "sk-real", null, null).carriesAKey());
        assertTrue(new DshAccount(DshAccount.AccountKind.THIRD_PARTY,
                "openai", "sk-real", null, null).carriesAKey());
        assertFalse(DshAccount.offline("MCXCC").carriesAKey());
        assertFalse(new DshAccount(DshAccount.AccountKind.OFFICIAL,
                "deepseek", "", null, null).carriesAKey());
        assertFalse(new DshAccount(DshAccount.AccountKind.OFFICIAL,
                "deepseek", "   ", null, null).carriesAKey());
    }

    @Test
    void theKindFollowsWhichVendorItIs() {
        // The four-argument constructor is the one the old code and the dialog's plain path use, and
        // it must not quietly call everything third-party: the vendor the launcher is built around is
        // the official one, and that is what decides which row of the page opened the dialog.
        assertEquals(DshAccount.AccountKind.OFFICIAL,
                new DshAccount("deepseek", "k", null, null).kind());
        assertEquals(DshAccount.AccountKind.THIRD_PARTY,
                new DshAccount("openrouter", "k", null, null).kind());
        assertEquals(DshAccount.AccountKind.OFFLINE, DshAccount.offline("me").kind());
    }

    @Test
    void anOfflineAccountIsNamedAndNothingElse() {
        DshAccount offline = DshAccount.offline("MCXCC");
        assertEquals("MCXCC", offline.displayName());
        assertEquals("offline", offline.vendorId());
        assertNull(offline.vendor(), "an offline account has no supplier to look up");
        assertNull(offline.endpoint());
    }

    @Test
    void theVendorsAKeyShapeIsCheckedOnlyWhereItIsKnown() {
        // Not a validation — only the vendor can say that — but a key pasted with the wrong vendor
        // chosen is the commonest mistake and the prefixes differ enough to catch it.
        assertTrue(DshVendor.byId("anthropic").looksLikeItsKey("sk-ant-abc"));
        // `sk-or-` is OpenRouter's and also starts with `sk-`, so it passes for Anthropic too. That
        // is the shape check being honest about what it is: a guess narrow enough to catch a key
        // pasted under the wrong vendor that still claims a prefix, and never a refusal on its own.
        assertTrue(DshVendor.byId("anthropic").looksLikeItsKey("sk-or-v1-abc"));
        assertFalse(DshVendor.byId("anthropic").looksLikeItsKey("plain-text-key"));
        // A vendor with no known shape accepts anything, because refusing on a guess is worse.
        assertTrue(DshVendor.byId("deepseek").looksLikeItsKey("anything-at-all"));
        assertFalse(DshVendor.byId("deepseek").looksLikeItsKey("   "));
    }

    @Test
    void theVendorListIsConsistent() {
        for (DshVendor vendor : DshVendor.offered()) {
            assertNotNull(DshVendor.byId(vendor.id()), vendor.id() + " must be findable by its id");
            assertTrue(DshVendor.APIS.contains(vendor.api()),
                    vendor.id() + " declares a protocol the harness accepts: " + vendor.api());
            assertFalse(vendor.apiKeyEnv().isBlank(), vendor.id() + " needs a variable to read");
            assertFalse(vendor.displayName().isBlank());
            assertEquals(vendor, DshVendor.byId(vendor.id().toUpperCase(java.util.Locale.ROOT)),
                    "the id is matched without regard to case");
        }
        assertEquals(DshVendor.offered().size(),
                DshVendor.offered().stream().map(DshVendor::id).distinct().count(),
                "two vendors with one id would make byId ambiguous");
    }

    @Test
    void aSuppliersOwnNameIsSpokenFor() {
        // An account under a supplier's name would make a route the launcher would later take away
        // believing it had made it, so the name is refused where accounts are made.
        assertTrue(DshVendor.answersTo("deepseek", vendor -> true));
        assertTrue(DshVendor.answersTo("  OpenCode ", vendor -> true), "trimmed, and without case");
        assertFalse(DshVendor.answersTo("MCXCC", vendor -> true));
        assertFalse(DshVendor.answersTo("", vendor -> true));
        assertFalse(DshVendor.answersTo(null, vendor -> true));
    }

    @Test
    void theVendorsOwnVendorComesFirst() {
        assertEquals("deepseek", DshVendor.offered().get(0).id(),
                "the harness's own vendor is the one the list leads with");
    }

    @Test
    void catalogueSuppliersAreAddressableWithoutAnEndpoint() {
        // `/api/models/providers` 把 catalogue 里能路由的供应商也标成 known，前端因此不会再
        // 让用户填地址。byId 必须也认这些 id，否则选小米就会得到
        // “unknown vendor `xiaomi`; pass `endpoint` to add a supplier by address”。
        DshVendor xiaomi = DshVendor.byId("xiaomi");
        assertNotNull(xiaomi, "catalogue vendors are offered as known; byId has to find them");
        assertEquals("openai-completions", xiaomi.api());
        assertTrue(xiaomi.hasBaseUrl());
        assertEquals(xiaomi, DshVendor.byId("  Xiaomi  "), "trimmed and without case");

        DshAccount account = new DshAccount(DshAccount.AccountKind.THIRD_PARTY, "xiaomi",
                "sk-test", null, null, null, null);
        assertEquals("https://api.xiaomimimo.com/v1", account.endpoint(),
                "an account made without an endpoint falls back to the vendor's own address");
    }
}