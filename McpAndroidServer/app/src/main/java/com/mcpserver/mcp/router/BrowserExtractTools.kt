package com.mcpserver.mcp.router

import com.mcpserver.platform.browser.BrowserEngine
import com.mcpserver.platform.web.WebToolkit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Structured extraction from the headless browser: text, links, forms, tables,
 * images, metadata and a readable markdown rendering.
 */
fun CommandRegistry.registerBrowserExtractTools(browser: BrowserEngine) {

    register(
        ToolDefinition(
            name = "browser_text_of",
            description = "Return the rendered text of one element, found by CSS selector.",
            inputSchema = eSchema(
                "selector" to eProp("string", "CSS selector, e.g. article or #content"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.eStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val text = browser.evaluate(
                    "(function(){var el=document.querySelector(${jsQuote(selector)});return el?((el.innerText||el.textContent||'')+''):'NOT_FOUND';})()"
                )
                buildJsonObject {
                    put("success", text != "NOT_FOUND")
                    put("selector", selector)
                    put("text", WebToolkit.tidySpacing(text))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_links",
            description = "Extract every link (text + absolute href) from the rendered page.",
            inputSchema = eSchema(
                "limit" to eProp("integer", "Maximum links (default 200)"),
                "filter" to eProp("string", "Only keep links whose text or href contains this"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val limit = (args.eInt("limit") ?: 200).coerceIn(1, 1_000)
                val filter = args.eStr("filter")
                val script = """
                    (function(){
                      var out=[]; var as=document.querySelectorAll('a[href]');
                      for(var i=0;i<as.length;i++){
                        var a=as[i];
                        var t=((a.innerText||a.textContent||'')+'').trim().slice(0,150);
                        out.push({text:t, href:a.href||'', title:a.getAttribute('title')||''});
                      }
                      return JSON.stringify({total:as.length, links:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank())
                    put("limit", limit)
                    put("filter", filter ?: "")
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_forms",
            description = "List forms and their fields (name/id/type/placeholder) for scripted login or check-in.",
            inputSchema = eSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val script = """
                    (function(){
                      var out=[]; var forms=document.querySelectorAll('form');
                      for(var i=0;i<forms.length && i<20;i++){
                        var f=forms[i]; var fields=[];
                        var els=f.querySelectorAll('input,textarea,select,button');
                        for(var j=0;j<els.length && j<60;j++){
                          var e=els[j];
                          fields.push({tag:e.tagName, type:e.getAttribute('type')||'', name:e.getAttribute('name')||'',
                                       id:e.id||'', placeholder:e.getAttribute('placeholder')||''});
                        }
                        out.push({action:f.getAttribute('action')||'', method:(f.getAttribute('method')||'get')+'', fields:fields});
                      }
                      return JSON.stringify({count:forms.length, forms:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank())
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_tables",
            description = "Extract HTML tables as arrays of rows and cells.",
            inputSchema = eSchema(
                "max_tables" to eProp("integer", "Maximum tables (default 5)"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val maxTables = (args.eInt("max_tables") ?: 5).coerceIn(1, 20)
                val script = """
                    (function(){
                      var out=[]; var tables=document.querySelectorAll('table');
                      for(var i=0;i<tables.length && i<$maxTables;i++){
                        var rows=[]; var trs=tables[i].querySelectorAll('tr');
                        for(var r=0;r<trs.length && r<200;r++){
                          var cells=[]; var tds=trs[r].querySelectorAll('th,td');
                          for(var c=0;c<tds.length;c++) cells.push(((tds[c].innerText||'')+'').trim());
                          rows.push(cells);
                        }
                        out.push(rows);
                      }
                      return JSON.stringify({count:tables.length, tables:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank())
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_images",
            description = "Extract image sources, alt text and natural sizes.",
            inputSchema = eSchema(
                "limit" to eProp("integer", "Maximum images (default 100)"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val limit = (args.eInt("limit") ?: 100).coerceIn(1, 500)
                val script = """
                    (function(){
                      var out=[]; var imgs=document.querySelectorAll('img');
                      for(var i=0;i<imgs.length && i<$limit;i++){
                        out.push({src:imgs[i].currentSrc||imgs[i].src||'', alt:imgs[i].alt||'',
                                  width:imgs[i].naturalWidth||0, height:imgs[i].naturalHeight||0});
                      }
                      return JSON.stringify({count:imgs.length, images:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank())
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_meta",
            description = "Page metadata: title, URL, description, keywords, OpenGraph tags and canonical link.",
            inputSchema = eSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val script = """
                    (function(){
                      var meta={}; var ms=document.querySelectorAll('meta');
                      for(var i=0;i<ms.length;i++){
                        var n=ms[i].getAttribute('name')||ms[i].getAttribute('property')||'';
                        var c=ms[i].getAttribute('content')||'';
                        if(n) meta[n]=c;
                      }
                      var canon=document.querySelector('link[rel=canonical]');
                      return JSON.stringify({title:document.title||'', url:location.href,
                                             canonical:canon?canon.href:'', meta:meta});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank())
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_extract",
            description = "Bulk extraction: for each CSS selector return its text plus href/src/value attributes.",
            inputSchema = eSchema(
                "selectors" to buildJsonObject {
                    put("type", "array")
                    put("description", "List of CSS selectors")
                    put("items", buildJsonObject { put("type", "string") })
                },
                required = listOf("selectors"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selectors = (args?.get("selectors") as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?.filter { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Missing 'selectors' array")
                buildJsonObject {
                    put("success", true)
                    put(
                        "results",
                        buildJsonArray {
                            selectors.forEach { selector ->
                                val script = """
                                    (function(){
                                      var list=document.querySelectorAll(${jsQuote(selector)});
                                      var items=[];
                                      for(var i=0;i<list.length && i<50;i++){
                                        var el=list[i];
                                        items.push({text:((el.innerText||el.textContent||'')+'').trim().slice(0,500),
                                                    href:el.href||el.getAttribute('href')||'',
                                                    src:el.getAttribute('src')||'',
                                                    value:(el.value===undefined?'':''+el.value)});
                                      }
                                      return JSON.stringify({count:list.length, items:items});
                                    })()
                                """.trimIndent()
                                add(
                                    buildJsonObject {
                                        put("selector", selector)
                                        put("data", browser.evaluate(script))
                                    }
                                )
                            }
                        },
                    )
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_find_text",
            description = "Search the rendered page text and return matches with surrounding context.",
            inputSchema = eSchema(
                "query" to eProp("string", "Text to find (case-insensitive)"),
                "max_matches" to eProp("integer", "Maximum matches (default 10)"),
                required = listOf("query"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val query = args.eStr("query") ?: throw IllegalArgumentException("Missing 'query'")
                val maxMatches = (args.eInt("max_matches") ?: 10).coerceIn(1, 50)
                val script = """
                    (function(){
                      var q=${jsQuote(query)}; var body=(document.body?document.body.innerText:'')||'';
                      var lower=body.toLowerCase(); var needle=q.toLowerCase();
                      var out=[]; var idx=lower.indexOf(needle); var guard=0;
                      while(idx>=0 && out.length<$maxMatches && guard<200){
                        out.push(body.slice(Math.max(0,idx-90), idx+needle.length+90));
                        idx=lower.indexOf(needle, idx+needle.length); guard++;
                      }
                      return JSON.stringify({found:out.length, matches:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank() && raw != "null")
                    put("query", query)
                    put("data", WebToolkit.tidySpacing(raw))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_markdown",
            description = "Readable rendering of the page: title, headings, paragraphs, list items and code blocks.",
            inputSchema = eSchema(
                "max_blocks" to eProp("integer", "Maximum blocks (default 400)"),
                "max_chars" to eProp("integer", "Maximum characters (default 40000)"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val maxBlocks = (args.eInt("max_blocks") ?: 400).coerceIn(10, 2_000)
                val maxChars = (args.eInt("max_chars") ?: 40_000).coerceIn(500, 500_000)
                val script = """
                    (function(){
                      var parts=['# '+(document.title||'')];
                      var nodes=document.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,pre,td');
                      for(var i=0;i<nodes.length && parts.length<$maxBlocks;i++){
                        var n=nodes[i]; var t=((n.innerText||'')+'').trim();
                        if(!t) continue;
                        var tag=n.tagName.toLowerCase();
                        if(tag==='h1') parts.push('## '+t);
                        else if(tag==='h2') parts.push('### '+t);
                        else if(tag==='h3'||tag==='h4') parts.push('#### '+t);
                        else if(tag==='li') parts.push('- '+t);
                        else if(tag==='blockquote') parts.push('> '+t);
                        else if(tag==='pre') parts.push('```' + String.fromCharCode(10) + t + String.fromCharCode(10) + '```');
                        else if(tag==='td') parts.push('| '+t+' |');
                        else parts.push(t);
                      }
                      return parts.join(String.fromCharCode(10)+String.fromCharCode(10));
                    })()
                """.trimIndent()
                val raw = WebToolkit.tidySpacing(browser.evaluate(script))
                val text = if (raw.length > maxChars) raw.take(maxChars) else raw
                buildJsonObject {
                    put("success", text.isNotBlank())
                    put("length", text.length)
                    put("truncated", raw.length > maxChars)
                    put("markdown", text)
                }
            },
        )
    )
}

private fun JsonObject?.eStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.eInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun eProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun eSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}
