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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for reading what a vendor says it serves.
///
/// The launcher writes a supplier of its own into the harness, and a route the harness has never
/// heard of cannot have its model list filled in from the catalogue — so the list has to come from
/// the vendor. A list this reads wrongly becomes models the harness cannot find, which is worse than
/// no list at all, so the reading is defensive and tested on the shapes that are not lists.
class ModelListTest {
    @Test
    void theIdsAreReadFromTheShapeOpenAiCompatibleServicesShare() {
        String body = """
                {"object":"list","data":[
                  {"id":"deepseek-chat","object":"model"},
                  {"id":"deepseek-reasoner","object":"model"}
                ]}""";

        assertEquals(List.of("deepseek-chat", "deepseek-reasoner"), DshAccount.readModelIds(body));
    }

    @Test
    void whatIsNotAModelListReadsAsNothingRatherThanAsAModel() {
        // "Compatible" is a claim rather than a promise, and a service that answers something else
        // has no list this can use. Inventing a model from it would put a name into the harness that
        // no request can be made against.
        assertTrue(DshAccount.readModelIds("{\"data\":\"not an array\"}").isEmpty());
        assertTrue(DshAccount.readModelIds("{\"models\":[]}").isEmpty());
        assertTrue(DshAccount.readModelIds("[]").isEmpty());
        assertTrue(DshAccount.readModelIds("not json").isEmpty());
        assertTrue(DshAccount.readModelIds("").isEmpty());
        assertTrue(DshAccount.readModelIds(null).isEmpty());

        // Entries without an id, and ids named twice, are dropped rather than written out.
        assertEquals(List.of("a"),
                DshAccount.readModelIds("{\"data\":[{\"id\":\"a\"},{\"object\":\"model\"},{\"id\":\"a\"},{\"id\":\"\"}]}"));
    }

    @Test
    void theEnrichedMapSomeGatewaysAnswerWithIsReadToo() {
        // The other published shape, and the one the harness's own reader accepts as well: a map
        // whose **keys** are the ids. Reading only the `data` array is how a gateway that answers
        // this way ends up looking like a supplier with no models.
        String body = """
                {"object":"list","models":{
                  "deepseek-chat":{"name":"DeepSeek Chat"},
                  "deepseek-reasoner":{"name":"DeepSeek Reasoner"}
                }}""";

        assertEquals(List.of("deepseek-chat", "deepseek-reasoner"), DshAccount.readModelIds(body));
    }

    @Test
    void aMapsKeyNamesTheModelAndItsOwnIdDoesNotRenameIt() {
        // The harness reads the key first for the same reason: the key is what the gateway indexes
        // the model by, and an entry's `id` is a label it may or may not carry.
        assertEquals(List.of("by-key"),
                DshAccount.readModelIds("{\"models\":{\"by-key\":{\"id\":\"something-else\"}}}"));
        assertEquals(List.of("from-the-entry"),
                DshAccount.readModelIds("{\"models\":{\"\":{\"id\":\"from-the-entry\"}}}"),
                "an entry with no key still names a model by its own id");
        assertEquals(List.of("only-objects"),
                DshAccount.readModelIds("{\"models\":{\"only-objects\":{},\"ignored\":\"not an object\"}}"),
                "a value that is not an object is not a model");
    }

    @Test
    void theListingAddressFollowsTheProtocol() {
        // The harness's own discovery, which is the authority on this: an OpenAI protocol lists
        // beside the address, Anthropic Messages lists under a `/v1` root. Asking Anthropic's
        // dialect at `/models` is a 404, and a 404 here is a route with no models at all.
        assertEquals("https://api.opencode.ai/v1/models",
                DshAccount.listingUrl("https://api.opencode.ai/v1", "openai-completions"));
        assertEquals("https://api.openai.com/v1/models",
                DshAccount.listingUrl("https://api.openai.com/v1", "openai-responses"));
        assertEquals("https://api.opencode.ai/v1/models",
                DshAccount.listingUrl("https://api.opencode.ai/v1///", "openai-completions"),
                "trailing slashes do not become a second path segment");

        assertEquals("https://api.anthropic.com/v1/models?limit=1000",
                DshAccount.listingUrl("https://api.anthropic.com", "anthropic-messages"));
        assertEquals("https://api.anthropic.com/v1/models?limit=1000",
                DshAccount.listingUrl("https://api.anthropic.com/v1", "anthropic-messages"),
                "one trailing /v1 is the root's, and is not counted twice");
        assertEquals("https://api.z.ai/api/coding/paas/v4/v1/models?limit=1000",
                DshAccount.listingUrl("https://api.z.ai/api/coding/paas/v4", "anthropic-messages"),
                "a deployment path keeps its segments");
    }

    @Test
    void aStatusAloneDoesNotMakeAnAddressASupplier() {
        // The defect this pins, taken from a real host: `api.opencode.ai` answers **200** to every
        // path, with the body `Not Found`. Reading the status for the answer accepted it as a
        // supplier, and the account made on it then had no models to route to — for months of
        // debugging, a 200 was the whole of the evidence.
        DshAccount.Answer plainPage = new DshAccount.Answer(200, false);
        assertFalse(plainPage.listsModels(), "a page that answers 200 to everything is not a supplier");

        // A 2xx whose body is a listing is the answer, and an empty listing is still a listing: a
        // supplier that hides its models until it is given a key answers with one.
        assertTrue(new DshAccount.Answer(200, true).listsModels());
        assertTrue(DshAccount.looksLikeAListing("{\"object\":\"list\",\"data\":[]}"));
        assertTrue(DshAccount.looksLikeAListing("{\"models\":{}}"));

        // A refusal counts as an answer — it is an API saying it wants a key, which is the ordinary
        // thing to hear while adding a supplier.
        assertTrue(new DshAccount.Answer(401, false).listsModels());
        assertTrue(new DshAccount.Answer(403, false).listsModels());

        // Anything else answered as something other than a model service, or did not answer.
        assertFalse(new DshAccount.Answer(404, false).listsModels());
        assertFalse(new DshAccount.Answer(500, false).listsModels());
        assertFalse(new DshAccount.Answer(-1, false).listsModels());
    }

    @Test
    void whatAVendorSaidAboutARefusalIsReadBack() {
        // Taken from a real refusal: SiliconFlow answers this to a request with no key, with a wrong
        // one, and with a key issued for its other region — so the status says only that the token was
        // not accepted, and what the vendor was willing to say is the rest of the answer.
        assertEquals("Token is invalid.", DshAccount.saidIn(
                "{\"code\":30014,\"data\":null,\"message\":\"Token is invalid.\"}"));

        // The other spellings services use.
        assertEquals("invalid api key", DshAccount.saidIn("{\"error\":{\"message\":\"invalid api key\"}}"));
        assertEquals("quota exceeded", DshAccount.saidIn("{\"error\":\"quota exceeded\"}"));

        // A reason is one line under a row, so a long one is cut and a multi-line one flattened.
        assertEquals("b".repeat(120) + "...",
                DshAccount.saidIn("{\"message\":\"" + "b".repeat(200) + "\"}"));
        assertEquals("first second", DshAccount.saidIn("{\"message\":\"first\\n  second\"}"));

        // Pages and errors with nothing to quote say nothing.
        assertNull(DshAccount.saidIn("Not Found"));
        assertNull(DshAccount.saidIn("<html><body>nope</body></html>"));
        assertNull(DshAccount.saidIn("{\"error\":\"\"}"));
        assertNull(DshAccount.saidIn("{\"error\":{\"code\":12}}"));
        assertNull(DshAccount.saidIn("{\"message\":42}"));
        assertNull(DshAccount.saidIn(""));
        assertNull(DshAccount.saidIn(null));
    }

    @Test
    void whatIsNotAListingIsNotOne() {
        assertFalse(DshAccount.looksLikeAListing("Not Found"), "a plain-text page");
        assertFalse(DshAccount.looksLikeAListing("<html><body>hi</body></html>"));
        assertFalse(DshAccount.looksLikeAListing("{\"error\":\"invalid api key\"}"));
        assertFalse(DshAccount.looksLikeAListing("{\"data\":\"not an array\"}"));
        assertFalse(DshAccount.looksLikeAListing("{\"models\":[]}"), "an array is not the enriched map");
        assertFalse(DshAccount.looksLikeAListing("[]"));
        assertFalse(DshAccount.looksLikeAListing(""));
        assertFalse(DshAccount.looksLikeAListing(null));
    }
}
