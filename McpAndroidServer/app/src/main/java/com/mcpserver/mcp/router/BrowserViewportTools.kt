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
 * Measurement, viewport emulation (desktop / mobile), page size reporting and
 * iframe (picture-in-picture) handling. Every length is expressed in CSS px.
 */
fun CommandRegistry.registerBrowserViewportTools(browser: BrowserEngine, context: Context) {

    // ── measuring ───────────────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_measure",
            description = "Measure the distance between two points in px; also between two elements' centres.",
            inputSchema = vSchema(
                "x1" to vProp("number", "Start X (px)"),
                "y1" to vProp("number", "Start Y (px)"),
                "x2" to vProp("number", "End X (px)"),
                "y2" to vProp("number", "End Y (px)"),
                "from_selector" to vProp("string", "Use this element's centre as the start point"),
                "to_selector" to vProp("string", "Use this element's centre as the end point"),
                "from_index" to vProp("integer", "Index for from_selector (default 0)"),
                "to_index" to vProp("integer", "Index for to_selector (default 0)"),
                "space" to vProp("string", "'viewport' (default) or 'page'"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val space = (args.vStr("space") ?: "viewport").lowercase()
                val axisX = if (space == "page") "pageX" else "x"
                val axisY = if (space == "page") "pageY" else "y"

                val fromSelector = args.vStr("from_selector")
                val toSelector = args.vStr("to_selector")

                val startX = if (!fromSelector.isNullOrBlank()) {
                    elementCentre(browser, fromSelector, args.vInt("from_index") ?: 0, axisX, axisY)
                } else {
                    args.vDouble("x1") ?: throw IllegalArgumentException("Provide x1/y1 or from_selector")
                }
                val startY = if (!fromSelector.isNullOrBlank()) {
                    elementCentre(browser, fromSelector, args.vInt("from_index") ?: 0, axisX, axisY, y = true)
                } else {
                    args.vDouble("y1") ?: 0.0
                }
                val endX = if (!toSelector.isNullOrBlank()) {
                    elementCentre(browser, toSelector, args.vInt("to_index") ?: 0, axisX, axisY)
                } else {
                    args.vDouble("x2") ?: throw IllegalArgumentException("Provide x2/y2 or to_selector")
                }
                val endY = if (!toSelector.isNullOrBlank()) {
                    elementCentre(browser, toSelector, args.vInt("to_index") ?: 0, axisX, axisY, y = true)
                } else {
                    args.vDouble("y2") ?: 0.0
                }

                val dx = endX - startX
                val dy = endY - startY
                val distance = kotlin.math.hypot(dx, dy)
                val angle = Math.toDegrees(kotlin.math.atan2(dy, dx))

                buildJsonObject {
                    put("success", true)
                    put("unit", "px")
                    put("space", space)
                    put("from_x", round(startX))
                    put("from_y", round(startY))
                    put("to_x", round(endX))
                    put("to_y", round(endY))
                    put("dx", round(dx))
                    put("dy", round(dy))
                    put("distance", round(distance))
                    put("manhattan", round(kotlin.math.abs(dx) + kotlin.math.abs(dy)))
                    put("angle_deg", round(angle))
                }
            },
        )
    )

    // ── page / viewport size ────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_page_size",
            description = "Current page metrics in px: viewport, document, scroll size, screen and device pixel ratio.",
            inputSchema = vSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val raw = browser.evaluate(PAGE_SIZE_SCRIPT)
                buildJsonObject {
                    put("success", raw.startsWith("{"))
                    put("data", raw)
                    put("unit", "px")
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_viewport",
            description = "Read or switch the emulated viewport: desktop mode, mobile mode or an explicit px size.",
            inputSchema = vSchema(
                "preset" to vProp("string", "desktop | laptop | mobile | android | iphone | tablet"),
                "width" to vProp("integer", "Explicit CSS width in px"),
                "height" to vProp("integer", "Explicit CSS height in px"),
                "user_agent" to vProp("string", "Optional User-Agent override"),
                "reload" to vProp("boolean", "Reload the page after switching (default true)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val preset = args.vStr("preset")?.lowercase()
                val explicitWidth = args.vInt("width")
                val explicitHeight = args.vInt("height")
                val explicitUa = args.vStr("user_agent")

                if (preset == null && explicitWidth == null && explicitHeight == null && explicitUa == null) {
                    val raw = browser.evaluate(PAGE_SIZE_SCRIPT)
                    val viewport = browser.currentViewport()
                    buildJsonObject {
                        put("success", raw.startsWith("{"))
                        put("emulated_width", viewport.first)
                        put("emulated_height", viewport.second)
                        put("user_agent", browser.userAgent())
                        put("data", raw)
                        put("unit", "px")
                    }
                } else {
                val target = when (preset) {
                    "desktop", "pc", "电脑" -> Triple(1280, 800, DESKTOP_UA)
                    "laptop" -> Triple(1440, 900, DESKTOP_UA)
                    "mobile", "手机" -> Triple(393, 852, ANDROID_UA)
                    "android" -> Triple(412, 915, ANDROID_UA)
                    "iphone" -> Triple(390, 844, IPHONE_UA)
                    "tablet", "平板" -> Triple(768, 1024, IPHONE_UA)
                    null -> Triple(explicitWidth ?: browser.currentViewport().first, explicitHeight ?: browser.currentViewport().second, null)
                    else -> throw IllegalArgumentException("Unknown preset: $preset")
                }

                val width = explicitWidth ?: target.first
                val height = explicitHeight ?: target.second
                val ua = explicitUa ?: target.third
                val mobileMode = preset == "mobile" || preset == "android" || preset == "iphone" ||
                    (preset == null && width <= 600)

                browser.setViewport(width, height)
                browser.setMobileMode(mobileMode)
                if (!ua.isNullOrBlank()) browser.setUserAgent(ua)

                val shouldReload = args.vBool("reload") != false
                if (shouldReload) {
                    browser.reload()
                    browser.waitForSelector("", 1_200L)
                }

                val raw = browser.evaluate(PAGE_SIZE_SCRIPT)
                buildJsonObject {
                    put("success", true)
                    put("mode", preset ?: if (mobileMode) "mobile" else "desktop")
                    put("emulated_width", width)
                    put("emulated_height", height)
                    put("mobile_mode", mobileMode)
                    put("user_agent", browser.userAgent())
                    put("reloaded", shouldReload)
                    put("data", raw)
                    put("unit", "px")
                }
                }
            },
        )
    )

    // ── iframes (picture in picture) ────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_iframes",
            description = "List iframes embedded in the page with their index, source, box and same-origin flag.",
            inputSchema = vSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val raw = browser.evaluate(IFRAME_SCRIPT)
                buildJsonObject {
                    put("success", raw.startsWith("{"))
                    put("active_frame", browser.frameIndex())
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_frame",
            description = "Enter an iframe so subsequent browser_* scripts run inside it, or reset to the top document.",
            inputSchema = vSchema(
                "index" to vProp("integer", "Iframe index from browser_iframes; -1 resets to the top document"),
                "reset" to vProp("boolean", "true to return to the top document"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val reset = args.vBool("reset") == true
                val index = args.vInt("index")
                when {
                    reset || index == -1 -> browser.setFrame(-1)
                    index != null -> browser.setFrame(index)
                }
                val active = browser.frameIndex()
                val probe = if (active >= 0) browser.evaluate("location.href") else ""
                buildJsonObject {
                    put("success", true)
                    put("active_frame", active)
                    put("in_frame", active >= 0)
                    put("frame_url", probe)
                    put(
                        "hint",
                        when {
                            active < 0 -> "当前在顶层文档"
                            probe.startsWith("CROSS_ORIGIN") -> "该 iframe 跨域，脚本无法进入；请用 browser_bbox/坐标手势在框内操作"
                            probe == "NO_FRAME" -> "iframe 索引不存在"
                            else -> "脚本已切换到该 iframe"
                        },
                    )
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_screenshot_frame",
            description = "Crop a screenshot to one iframe's box — useful for verification popups drawn in an iframe.",
            inputSchema = vSchema(
                "index" to vProp("integer", "Iframe index (default 0)"),
                "padding" to vProp("integer", "Extra px around the frame (default 0)"),
                "file_name" to vProp("string", "Optional file name"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val index = (args.vInt("index") ?: 0).coerceAtLeast(0)
                val padding = (args.vInt("padding") ?: 0).coerceIn(0, 200)
                val rectRaw = browser.evaluate(IFRAME_RECT_SCRIPT.replace("__INDEX__", index.toString()))
                val rect = runCatching { if (rectRaw.startsWith("{")) JSONObject(rectRaw) else null }.getOrNull()
                    ?: throw IllegalStateException("Iframe #$index not found")
                val name = args.vStr("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_frame_${vTimestamp()}.png"
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
}

// ── scripts ───────────────────────────────────────────────────────────────────

private const val PAGE_SIZE_SCRIPT = """
(function(){
  var de=document.documentElement, b=document.body;
  return JSON.stringify({
    viewport_width: window.innerWidth||0,
    viewport_height: window.innerHeight||0,
    client_width: de?de.clientWidth:0,
    client_height: de?de.clientHeight:0,
    page_width: Math.max(de?de.scrollWidth:0, b?b.scrollWidth:0),
    page_height: Math.max(de?de.scrollHeight:0, b?b.scrollHeight:0),
    scroll_x: window.scrollX||0,
    scroll_y: window.scrollY||0,
    device_pixel_ratio: window.devicePixelRatio||1,
    screen_width: screen.width||0,
    screen_height: screen.height||0,
    unit: 'px'
  });
})()
"""

private const val IFRAME_SCRIPT = """
(function(){
  var fs=document.querySelectorAll('iframe,frame'); var out=[];
  for(var i=0;i<fs.length;i++){
    var f=fs[i]; var r=f.getBoundingClientRect(); var same=false;
    try { same = !!f.contentDocument; } catch(e) { same=false; }
    out.push({index:i, src:f.getAttribute('src')||'', name:f.getAttribute('name')||'', id:f.id||'',
      x:r.left, y:r.top, width:r.width, height:r.height,
      pageX:r.left+window.scrollX, pageY:r.top+window.scrollY,
      same_origin:same, visible:(r.width>2 && r.height>2)});
  }
  return JSON.stringify({count:fs.length, frames:out});
})()
"""

private const val IFRAME_RECT_SCRIPT = """
(function(){
  var fs=document.querySelectorAll('iframe,frame'); var i=__INDEX__;
  if(i>=fs.length) return '{}';
  var r=fs[i].getBoundingClientRect();
  return JSON.stringify({x:r.left, y:r.top, width:r.width, height:r.height,
    pageX:r.left+window.scrollX, pageY:r.top+window.scrollY});
})()
"""

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
private const val ANDROID_UA =
    "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val IPHONE_UA =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

// ── helpers ───────────────────────────────────────────────────────────────────

private suspend fun elementCentre(
    browser: BrowserEngine,
    selector: String,
    index: Int,
    axisX: String,
    axisY: String,
    y: Boolean = false,
): Double {
    val raw = browser.evaluate(
        """
        (function(){
          var list=document.querySelectorAll(${jsQuote(selector)});
          if(!list || list.length<=$index) return '{}';
          var r=list[$index].getBoundingClientRect();
          return JSON.stringify({x:r.left, y:r.top, width:r.width, height:r.height,
            centerX:r.left+r.width/2, centerY:r.top+r.height/2,
            pageX:r.left+window.scrollX, pageY:r.top+window.scrollY});
        })()
        """.trimIndent()
    )
    val rect = runCatching { if (raw.startsWith("{")) JSONObject(raw) else null }.getOrNull()
        ?: throw IllegalStateException("Element not found: $selector")
    return if (axisX == "page") {
        if (y) rect.optDouble("pageY", 0.0) + rect.optDouble("height", 0.0) / 2 else rect.optDouble("pageX", 0.0) + rect.optDouble("width", 0.0) / 2
    } else {
        if (y) rect.optDouble("centerY", 0.0) else rect.optDouble("centerX", 0.0)
    }
}

private fun round(value: Double): Double = Math.round(value * 100.0) / 100.0

private fun JsonObject?.vStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.vInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject?.vDouble(name: String): Double? =
    (this?.get(name) as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()

private fun JsonObject?.vBool(name: String): Boolean? =
    (this?.get(name) as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

private fun vProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun vSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}

private fun vTimestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
