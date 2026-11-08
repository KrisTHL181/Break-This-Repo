package com.mcpserver.mcp.router

import android.content.Context
import com.mcpserver.platform.browser.BrowserEngine
import com.mcpserver.util.PublicFiles
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Geometry probing, human-like gestures and cropped screenshots — the pieces
 * needed to work with slider / puzzle style human-verification widgets.
 */
fun CommandRegistry.registerBrowserAdvancedTools(browser: BrowserEngine, context: Context) {

    // ── geometry ────────────────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_bbox",
            description = "Bounding box of an element: size, viewport position, page position and centre.",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector"),
                "index" to gProp("integer", "Which match (default 0)"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val index = args.gInt("index") ?: 0
                val raw = browser.evaluate(rectScript(selector, index))
                buildJsonObject {
                    put("success", raw.startsWith("{"))
                    put("selector", selector)
                    put("data", raw)
                    rectToJson(raw)?.let { rect ->
                        put("x", rect.optDouble("x", 0.0))
                        put("y", rect.optDouble("y", 0.0))
                        put("width", rect.optDouble("width", 0.0))
                        put("height", rect.optDouble("height", 0.0))
                        put("center_x", rect.optDouble("centerX", 0.0))
                        put("center_y", rect.optDouble("centerY", 0.0))
                        put("page_x", rect.optDouble("pageX", 0.0))
                        put("page_y", rect.optDouble("pageY", 0.0))
                    }
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_bbox_all",
            description = "Bounding boxes of every element matching a selector, optionally filtered by minimum size.",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector"),
                "limit" to gProp("integer", "Maximum entries (default 50)"),
                "min_width" to gProp("number", "Only keep boxes at least this wide"),
                "min_height" to gProp("number", "Only keep boxes at least this tall"),
                "min_area" to gProp("number", "Only keep boxes with at least this area"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val limit = (args.gInt("limit") ?: 50).coerceIn(1, 500)
                val minWidth = args.gDouble("min_width") ?: 0.0
                val minHeight = args.gDouble("min_height") ?: 0.0
                val minArea = args.gDouble("min_area") ?: 0.0
                val raw = browser.evaluate(rectAllScript(selector, limit))
                buildJsonObject {
                    put("success", raw.startsWith("["))
                    put("selector", selector)
                    put(
                        "boxes",
                        buildJsonArray {
                            runCatching {
                                val array = org.json.JSONArray(raw)
                                for (i in 0 until array.length()) {
                                    val rect = array.optJSONObject(i) ?: continue
                                    val width = rect.optDouble("width", 0.0)
                                    val height = rect.optDouble("height", 0.0)
                                    if (width < minWidth || height < minHeight || width * height < minArea) continue
                                    add(
                                        buildJsonObject {
                                            put("index", i)
                                            put("tag", rect.optString("tag"))
                                            put("text", rect.optString("text"))
                                            put("x", rect.optDouble("x", 0.0))
                                            put("y", rect.optDouble("y", 0.0))
                                            put("width", width)
                                            put("height", height)
                                            put("center_x", rect.optDouble("centerX", 0.0))
                                            put("center_y", rect.optDouble("centerY", 0.0))
                                            put("page_x", rect.optDouble("pageX", 0.0))
                                            put("page_y", rect.optDouble("pageY", 0.0))
                                        }
                                    )
                                }
                            }
                        },
                    )
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_element_at",
            description = "Which element sits at viewport coordinates (tag, id, class, text, box).",
            inputSchema = gSchema(
                "x" to gProp("integer", "Viewport X"),
                "y" to gProp("integer", "Viewport Y"),
                required = listOf("x", "y"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val x = args.gInt("x") ?: throw IllegalArgumentException("Missing 'x'")
                val y = args.gInt("y") ?: throw IllegalArgumentException("Missing 'y'")
                val raw = browser.evaluate(
                    """
                    (function(){
                      var el=document.elementFromPoint($x,$y);
                      if(!el) return '{}';
                      var r=el.getBoundingClientRect();
                      return JSON.stringify({tag:el.tagName, id:el.id||'', cls:(el.className||'')+'',
                        text:((el.innerText||el.textContent||'')+'').trim().slice(0,120),
                        x:r.left, y:r.top, width:r.width, height:r.height,
                        centerX:r.left+r.width/2, centerY:r.top+r.height/2,
                        pageX:r.left+window.scrollX, pageY:r.top+window.scrollY});
                    })()
                    """.trimIndent()
                )
                buildJsonObject {
                    put("success", raw.startsWith("{"))
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_visible",
            description = "Visibility and layout facts for an element (display, opacity, size, in-viewport).",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val raw = browser.evaluate(
                    """
                    (function(){
                      var el=document.querySelector(${jsQuote(selector)});
                      if(!el) return '{}';
                      var cs=getComputedStyle(el); var r=el.getBoundingClientRect();
                      return JSON.stringify({visible:!!(el.offsetWidth||el.offsetHeight||el.getClientRects().length),
                        display:cs.display, visibility:cs.visibility, opacity:cs.opacity,
                        width:r.width, height:r.height,
                        in_viewport:(r.top>=0 && r.left>=0 && r.bottom<=window.innerHeight && r.right<=window.innerWidth)});
                    })()
                    """.trimIndent()
                )
                buildJsonObject {
                    put("success", raw.startsWith("{"))
                    put("selector", selector)
                    put("data", raw)
                }
            },
        )
    )

    // ── gestures ────────────────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_drag",
            description = "Human-like drag inside the browser: eased path, jitter, hold and optional overshoot correction.",
            inputSchema = gSchema(
                "x1" to gProp("integer", "Start X"),
                "y1" to gProp("integer", "Start Y"),
                "x2" to gProp("integer", "End X"),
                "y2" to gProp("integer", "End Y"),
                "duration_ms" to gProp("integer", "Movement duration (default 600)"),
                "hold_ms" to gProp("integer", "Hold before moving (default 120)"),
                "jitter" to gProp("number", "Perpendicular wobble in px (default 2.0)"),
                "overshoot" to gProp("number", "Overshoot past the target in px before correcting (default 0)"),
                "steps" to gProp("integer", "Path resolution (default 32)"),
                required = listOf("x1", "y1", "x2", "y2"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x1 = (args.gInt("x1") ?: throw IllegalArgumentException("Missing 'x1'")).toFloat()
                val y1 = (args.gInt("y1") ?: throw IllegalArgumentException("Missing 'y1'")).toFloat()
                val x2 = (args.gInt("x2") ?: throw IllegalArgumentException("Missing 'x2'")).toFloat()
                val y2 = (args.gInt("y2") ?: throw IllegalArgumentException("Missing 'y2'")).toFloat()
                val duration = (args.gInt("duration_ms") ?: 600).coerceIn(50, 10_000)
                val hold = (args.gInt("hold_ms") ?: 120).coerceIn(0, 5_000)
                val jitter = (args.gDouble("jitter") ?: 2.0).coerceIn(0.0, 20.0).toFloat()
                val overshoot = (args.gDouble("overshoot") ?: 0.0).coerceIn(0.0, 60.0).toFloat()
                val steps = (args.gInt("steps") ?: 32).coerceIn(4, 200)
                browser.dragHumanized(x1, y1, x2, y2, duration, hold, jitter, overshoot, steps)
                buildJsonObject {
                    put("success", true)
                    put("distance", kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()))
                    put("duration_ms", duration)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_drag_selector",
            description = "Drag an element (e.g. a slider handle) from its centre by dx/dy with a human-like path.",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector of the handle"),
                "dx" to gProp("number", "Horizontal distance to move"),
                "dy" to gProp("number", "Vertical distance to move (default 0)"),
                "duration_ms" to gProp("integer", "Movement duration (default 700)"),
                "hold_ms" to gProp("integer", "Hold before moving (default 150)"),
                "jitter" to gProp("number", "Perpendicular wobble in px (default 2.0)"),
                "overshoot" to gProp("number", "Overshoot past the target in px (default 0)"),
                "index" to gProp("integer", "Which match (default 0)"),
                required = listOf("selector", "dx"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val dx = args.gDouble("dx") ?: throw IllegalArgumentException("Missing 'dx'")
                val dy = args.gDouble("dy") ?: 0.0
                val index = args.gInt("index") ?: 0
                val duration = (args.gInt("duration_ms") ?: 700).coerceIn(50, 10_000)
                val hold = (args.gInt("hold_ms") ?: 150).coerceIn(0, 5_000)
                val jitter = (args.gDouble("jitter") ?: 2.0).coerceIn(0.0, 20.0).toFloat()
                val overshoot = (args.gDouble("overshoot") ?: 0.0).coerceIn(0.0, 60.0).toFloat()

                val rectJson = browser.evaluate(rectScript(selector, index))
                val rect = rectToJson(rectJson)
                    ?: throw IllegalStateException("Element not found: $selector")
                val startX = rect.optDouble("centerX", 0.0).toFloat()
                val startY = rect.optDouble("centerY", 0.0).toFloat()
                val endX = (startX + dx).toFloat()
                val endY = (startY + dy).toFloat()

                browser.dragHumanized(startX, startY, endX, endY, duration, hold, jitter, overshoot)
                buildJsonObject {
                    put("success", true)
                    put("selector", selector)
                    put("from_x", startX)
                    put("from_y", startY)
                    put("to_x", endX)
                    put("to_y", endY)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_mouse_move",
            description = "Move the pointer along a human-like path (hover motion without pressing).",
            inputSchema = gSchema(
                "x1" to gProp("integer", "Start X"),
                "y1" to gProp("integer", "Start Y"),
                "x2" to gProp("integer", "End X"),
                "y2" to gProp("integer", "End Y"),
                "duration_ms" to gProp("integer", "Duration (default 300)"),
                "steps" to gProp("integer", "Steps (default 20)"),
                required = listOf("x1", "y1", "x2", "y2"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x1 = (args.gInt("x1") ?: 0).toFloat()
                val y1 = (args.gInt("y1") ?: 0).toFloat()
                val x2 = (args.gInt("x2") ?: 0).toFloat()
                val y2 = (args.gInt("y2") ?: 0).toFloat()
                val duration = (args.gInt("duration_ms") ?: 300).coerceIn(20, 10_000)
                val steps = (args.gInt("steps") ?: 20).coerceIn(2, 200)
                browser.mouseMove(x1, y1, x2, y2, duration, steps)
                buildJsonObject { put("success", true) }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_long_press",
            description = "Press and hold at viewport coordinates.",
            inputSchema = gSchema(
                "x" to gProp("integer", "X"),
                "y" to gProp("integer", "Y"),
                "hold_ms" to gProp("integer", "Hold duration (default 800)"),
                required = listOf("x", "y"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = (args.gInt("x") ?: throw IllegalArgumentException("Missing 'x'")).toFloat()
                val y = (args.gInt("y") ?: throw IllegalArgumentException("Missing 'y'")).toFloat()
                val hold = (args.gInt("hold_ms") ?: 800).coerceIn(100, 10_000)
                browser.longPress(x, y, hold)
                buildJsonObject {
                    put("success", true)
                    put("hold_ms", hold)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_double_click",
            description = "Double tap at viewport coordinates.",
            inputSchema = gSchema(
                "x" to gProp("integer", "X"),
                "y" to gProp("integer", "Y"),
                required = listOf("x", "y"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = (args.gInt("x") ?: throw IllegalArgumentException("Missing 'x'")).toFloat()
                val y = (args.gInt("y") ?: throw IllegalArgumentException("Missing 'y'")).toFloat()
                browser.doubleClick(x, y)
                buildJsonObject { put("success", true) }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_scroll_to",
            description = "Scroll an element into view and return its fresh bounding box.",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                browser.evaluate(
                    "(function(){var el=document.querySelector(${jsQuote(selector)});if(!el)return 'NOT_FOUND';el.scrollIntoView({block:'center',inline:'center'});return 'OK';})()"
                )
                val rect = browser.evaluate(rectScript(selector, 0))
                buildJsonObject {
                    put("success", rect.startsWith("{"))
                    put("selector", selector)
                    put("data", rect)
                }
            },
        )
    )

    // ── screenshots ─────────────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_screenshot_full",
            description = "Capture the whole rendered page (not just the viewport) to a PNG in Download.",
            inputSchema = gSchema(
                "file_name" to gProp("string", "Optional file name"),
                "max_height" to gProp("integer", "Height cap in px (default 20000)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val name = args.gStr("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_fullpage_${gTimestamp()}.png"
                val maxHeight = (args.gInt("max_height") ?: 20_000).coerceIn(500, 30_000)
                val bytes = browser.screenshotFull(maxHeight)
                val path = PublicFiles.saveToDownloads(context, name, "image/png", bytes)
                buildJsonObject {
                    put("success", path != null)
                    put("path", path ?: "")
                    put("bytes", bytes.size)
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_screenshot_element",
            description = "Crop a screenshot to one element (plus padding) — handy for reading a puzzle piece or gap.",
            inputSchema = gSchema(
                "selector" to gProp("string", "CSS selector"),
                "padding" to gProp("integer", "Extra pixels around the element (default 4)"),
                "file_name" to gProp("string", "Optional file name"),
                "index" to gProp("integer", "Which match (default 0)"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val selector = args.gStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val padding = (args.gInt("padding") ?: 4).coerceIn(0, 200)
                val index = args.gInt("index") ?: 0
                val rect = rectToJson(browser.evaluate(rectScript(selector, index)))
                    ?: throw IllegalStateException("Element not found: $selector")
                val name = args.gStr("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_element_${gTimestamp()}.png"
                val x = (rect.optDouble("pageX", 0.0) - padding).toInt()
                val y = (rect.optDouble("pageY", 0.0) - padding).toInt()
                val width = (rect.optDouble("width", 1.0) + padding * 2).toInt().coerceAtLeast(1)
                val height = (rect.optDouble("height", 1.0) + padding * 2).toInt().coerceAtLeast(1)
                val bytes = browser.screenshotPageRegion(x, y, width, height)
                val path = PublicFiles.saveToDownloads(context, name, "image/png", bytes)
                buildJsonObject {
                    put("success", path != null)
                    put("path", path ?: "")
                    put("bytes", bytes.size)
                    put("region_x", x)
                    put("region_y", y)
                    put("region_width", width)
                    put("region_height", height)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_screenshot_region",
            description = "Crop a screenshot to an explicit page-coordinate rectangle.",
            inputSchema = gSchema(
                "x" to gProp("integer", "Page X"),
                "y" to gProp("integer", "Page Y"),
                "width" to gProp("integer", "Width"),
                "height" to gProp("integer", "Height"),
                "file_name" to gProp("string", "Optional file name"),
                required = listOf("x", "y", "width", "height"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = args.gInt("x") ?: throw IllegalArgumentException("Missing 'x'")
                val y = args.gInt("y") ?: throw IllegalArgumentException("Missing 'y'")
                val width = (args.gInt("width") ?: throw IllegalArgumentException("Missing 'width'")).coerceAtLeast(1)
                val height = (args.gInt("height") ?: throw IllegalArgumentException("Missing 'height'")).coerceAtLeast(1)
                val name = args.gStr("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_region_${gTimestamp()}.png"
                val bytes = browser.screenshotPageRegion(x, y, width, height)
                val path = PublicFiles.saveToDownloads(context, name, "image/png", bytes)
                buildJsonObject {
                    put("success", path != null)
                    put("path", path ?: "")
                    put("bytes", bytes.size)
                }
            },
        )
    )
}

// ── helpers ───────────────────────────────────────────────────────────────────

private fun rectScript(selector: String, index: Int): String = """
    (function(){
      var list=document.querySelectorAll(${jsQuote(selector)});
      if(!list || list.length<=$index) return '{}';
      var el=list[$index]; var r=el.getBoundingClientRect();
      return JSON.stringify({tag:el.tagName, text:((el.innerText||'')+'').trim().slice(0,120),
        x:r.left, y:r.top, width:r.width, height:r.height,
        centerX:r.left+r.width/2, centerY:r.top+r.height/2,
        pageX:r.left+window.scrollX, pageY:r.top+window.scrollY});
    })()
""".trimIndent()

private fun rectAllScript(selector: String, limit: Int): String = """
    (function(){
      var list=document.querySelectorAll(${jsQuote(selector)}); var out=[];
      for(var i=0;i<list.length && i<$limit;i++){
        var el=list[i]; var r=el.getBoundingClientRect();
        out.push({tag:el.tagName, text:((el.innerText||'')+'').trim().slice(0,120),
          x:r.left, y:r.top, width:r.width, height:r.height,
          centerX:r.left+r.width/2, centerY:r.top+r.height/2,
          pageX:r.left+window.scrollX, pageY:r.top+window.scrollY});
      }
      return JSON.stringify(out);
    })()
""".trimIndent()

private fun rectToJson(raw: String): JSONObject? =
    runCatching { if (raw.startsWith("{")) JSONObject(raw) else null }.getOrNull()

private fun JsonObject?.gStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.gInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject?.gDouble(name: String): Double? =
    (this?.get(name) as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()

private fun gProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun gSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}

private fun gTimestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
