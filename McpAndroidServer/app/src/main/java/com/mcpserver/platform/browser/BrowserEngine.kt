package com.mcpserver.platform.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * A single headless WebView driven from the MCP server.
 *
 * Every WebView call is marshalled onto the main looper, so callers can use it
 * from the server's IO coroutines. The view is created lazily and reused.
 */
class BrowserEngine(private val context: Context) {

    companion object {
        private const val TAG = "BrowserEngine"
        private const val VIEWPORT_WIDTH = 1080
        private const val VIEWPORT_HEIGHT = 1920
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null

    /** CSS viewport override; 0 means "use the natural viewport". */
    @Volatile private var viewportWidth: Int = 0
    @Volatile private var viewportHeight: Int = 0

    /** Active iframe index for script evaluation; -1 means the top document. */
    @Volatile private var activeFrame: Int = -1

    suspend fun isOpen(): Boolean = withContext(Dispatchers.Main) { webView != null }

    /** Load [url] and wait for the main frame to settle. */
    suspend fun open(url: String, timeoutMs: Long = 30_000L): String = withContext(Dispatchers.Main) {
        val target = normalize(url)
        val view = obtain()
        suspendCancellableCoroutine { continuation ->
            val settled = AtomicBoolean(false)
            val timeout = Runnable {
                if (settled.compareAndSet(false, true)) continuation.resume("timeout")
            }
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    if (settled.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeout)
                        continuation.resume("loaded")
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true && settled.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeout)
                        continuation.resume("error:${error?.description ?: "load failed"}")
                    }
                }
            }
            continuation.invokeOnCancellation { mainHandler.removeCallbacks(timeout) }
            mainHandler.postDelayed(timeout, timeoutMs)
            runCatching { view.loadUrl(target) }.onFailure {
                if (settled.compareAndSet(false, true)) {
                    mainHandler.removeCallbacks(timeout)
                    continuation.resume("error:${it.message}")
                }
            }
        }
    }

    /** Runtime viewport override in CSS px (0 = natural). */
    suspend fun setViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        withContext(Dispatchers.Main) {
            val view = obtain()
            view.measure(
                View.MeasureSpec.makeMeasureSpec(viewportWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(viewportHeight, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, viewportWidth, viewportHeight)
        }
    }

    fun currentViewport(): Pair<Int, Int> = viewportWidth to viewportHeight

    /** Desktop/mobile emulation flags that change how a responsive page lays out. */
    suspend fun setMobileMode(mobile: Boolean) {
        withContext(Dispatchers.Main) {
            obtain().settings.apply {
                useWideViewPort = mobile
                loadWithOverviewMode = mobile
            }
        }
    }

    fun frameIndex(): Int = activeFrame

    fun setFrame(index: Int) {
        activeFrame = index
    }

    /** Runs [script] inside the active iframe when one is selected. */
    suspend fun evaluate(script: String, timeoutMs: Long = 15_000L): String {
        val frame = activeFrame
        val effective = if (frame >= 0) wrapInFrame(script, frame) else script
        return evaluateRaw(effective, timeoutMs)
    }

    private suspend fun evaluateRaw(script: String, timeoutMs: Long = 15_000L): String = withContext(Dispatchers.Main) {
        val view = obtain()
        suspendCancellableCoroutine { continuation ->
            val settled = AtomicBoolean(false)
            val timeout = Runnable { if (settled.compareAndSet(false, true)) continuation.resume("") }
            continuation.invokeOnCancellation { mainHandler.removeCallbacks(timeout) }
            mainHandler.postDelayed(timeout, timeoutMs)
            runCatching {
                view.evaluateJavascript(script) { value ->
                    if (settled.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeout)
                        continuation.resume(decodeJsValue(value))
                    }
                }
            }.onFailure {
                if (settled.compareAndSet(false, true)) {
                    mainHandler.removeCallbacks(timeout)
                    continuation.resume("error:${it.message}")
                }
            }
        }
    }

    suspend fun currentUrl(): String = evaluate("location.href")

    suspend fun pageTitle(): String = evaluate("document.title")

    suspend fun pageText(maxChars: Int = 20_000): String {
        val raw = evaluate("(function(){try{return document.body?document.body.innerText:'';}catch(e){return 'ERROR: '+e;}})()")
        return if (raw.length > maxChars) raw.take(maxChars) else raw
    }

    /** Render the current page to PNG bytes. */
    suspend fun screenshot(): ByteArray = withContext(Dispatchers.Main) {
        val view = obtain()
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(VIEWPORT_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEWPORT_HEIGHT, View.MeasureSpec.AT_MOST),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    }

    // ── interaction ─────────────────────────────────────────────────────────

    /** Synthesize a real touch at viewport coordinates. */
    suspend fun tapAt(x: Float, y: Float) {
        withContext(Dispatchers.Main) {
            val view = obtain()
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0)
            runCatching { view.dispatchTouchEvent(down) }
            runCatching { view.dispatchTouchEvent(up) }
            down.recycle()
            up.recycle()
        }
    }

    /** Synthesize a swipe gesture as a sequence of move events. */
    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int) {
        val view = withContext(Dispatchers.Main) { obtain() }
        val start = SystemClock.uptimeMillis()
        val steps = 24
        val stepDelay = (durationMs / steps).coerceAtLeast(1)
        withContext(Dispatchers.Main) {
            val down = MotionEvent.obtain(start, start, MotionEvent.ACTION_DOWN, x1, y1, 0)
            runCatching { view.dispatchTouchEvent(down) }
            down.recycle()
        }
        for (step in 1..steps) {
            val fraction = step.toFloat() / steps
            val move = MotionEvent.obtain(
                start,
                start + (durationMs * fraction).toLong(),
                MotionEvent.ACTION_MOVE,
                x1 + (x2 - x1) * fraction,
                y1 + (y2 - y1) * fraction,
                0,
            )
            withContext(Dispatchers.Main) { runCatching { view.dispatchTouchEvent(move) } }
            move.recycle()
            delay(stepDelay.toLong())
        }
        withContext(Dispatchers.Main) {
            val up = MotionEvent.obtain(start, start + durationMs, MotionEvent.ACTION_UP, x2, y2, 0)
            runCatching { view.dispatchTouchEvent(up) }
            up.recycle()
        }
    }

    suspend fun html(maxChars: Int = 50_000): String {
        val raw = evaluate("(function(){try{return document.documentElement.outerHTML||'';}catch(e){return 'ERROR: '+e;}})()")
        return if (raw.length > maxChars) raw.take(maxChars) else raw
    }

    suspend fun readyState(): String = evaluate("document.readyState")

    /** Waits for a selector; a blank selector just waits [timeoutMs]. */
    suspend fun waitForSelector(selector: String, timeoutMs: Long): Boolean {
        if (selector.isBlank()) {
            delay(timeoutMs.coerceAtLeast(0))
            return true
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (evaluate("!!document.querySelector(${jsString(selector)})") == "true") return true
            delay(250)
        }
        return false
    }

    // ── human-like gestures ─────────────────────────────────────────────────

    /** Press, optional hold, then move along a jittered eased path, then release. */
    suspend fun dragHumanized(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Int = 600,
        holdMs: Int = 0,
        jitterPx: Float = 0f,
        overshootPx: Float = 0f,
        steps: Int = 32,
    ) {
        val points = humanizedPath(x1, y1, x2, y2, steps.coerceIn(4, 200), jitterPx, overshootPx)
        dispatchPath(points, durationMs.coerceAtLeast(50), holdMs.coerceAtLeast(0))
    }

    suspend fun mouseMove(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int = 300, steps: Int = 20) {
        val points = humanizedPath(x1, y1, x2, y2, steps.coerceIn(2, 200), 1.5f, 0f)
        withContext(Dispatchers.Main) {
            val view = obtain()
            val start = SystemClock.uptimeMillis()
            points.forEachIndexed { index, point ->
                val event = MotionEvent.obtain(
                    start,
                    start + (durationMs * (index + 1) / points.size).toLong(),
                    MotionEvent.ACTION_HOVER_MOVE,
                    point.first,
                    point.second,
                    0,
                )
                runCatching { view.dispatchGenericMotionEvent(event) }
                event.recycle()
            }
        }
    }

    suspend fun longPress(x: Float, y: Float, holdMs: Int = 800) {
        dispatchPath(listOf(x to y, x to y), holdMs.coerceAtLeast(100), holdMs.coerceAtLeast(100))
    }

    suspend fun doubleClick(x: Float, y: Float) {
        tapAt(x, y)
        delay(90)
        tapAt(x, y)
    }

    /**
     * Eased path with perpendicular jitter and optional overshoot + correction,
     * which is what makes a gesture look human to behaviour checks.
     */
    private fun humanizedPath(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        steps: Int,
        jitterPx: Float,
        overshootPx: Float,
    ): List<Pair<Float, Float>> {
        val random = java.util.Random()
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        val length = kotlin.math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat().coerceAtLeast(1f)
        val normalX = -deltaY / length
        val normalY = deltaX / length
        val points = mutableListOf<Pair<Float, Float>>()
        for (index in 0..steps) {
            val t = index.toFloat() / steps
            val eased = if (t < 0.5f) {
                4f * t * t * t
            } else {
                1f - Math.pow((-2f * t + 2f).toDouble(), 3.0).toFloat() / 2f
            }
            val wobble = jitterPx * kotlin.math.sin(Math.PI * t).toFloat()
            val offset = if (wobble > 0f) (random.nextFloat() * 2f - 1f) * wobble else 0f
            points.add((x1 + deltaX * eased + normalX * offset) to (y1 + deltaY * eased + normalY * offset))
        }
        if (overshootPx > 0f && kotlin.math.abs(deltaX) > 1f) {
            val direction = if (deltaX >= 0f) 1f else -1f
            points.add((x2 + direction * overshootPx) to y2)
            points.add((x2 + direction * overshootPx * 0.4f) to (y2 + random.nextFloat() * 2f - 1f))
            points.add(x2 to y2)
        }
        return points
    }

    private suspend fun dispatchPath(points: List<Pair<Float, Float>>, durationMs: Int, holdMs: Int) {
        if (points.isEmpty()) return
        withContext(Dispatchers.Main) {
            val view = obtain()
            val start = SystemClock.uptimeMillis()
            val down = touchEvent(MotionEvent.ACTION_DOWN, points.first().first, points.first().second, start, start)
            runCatching { view.dispatchTouchEvent(down) }
            down.recycle()
            if (holdMs > 0) delay(holdMs.toLong())
            val movePoints = if (points.size > 1) points.drop(1) else points
            movePoints.forEachIndexed { index, point ->
                val fraction = (index + 1).toFloat() / movePoints.size
                val event = touchEvent(
                    MotionEvent.ACTION_MOVE,
                    point.first,
                    point.second,
                    start + (durationMs * fraction).toLong(),
                    start,
                )
                runCatching { view.dispatchTouchEvent(event) }
                event.recycle()
                delay((durationMs / movePoints.size).toLong().coerceAtLeast(1))
            }
            val last = points.last()
            val up = touchEvent(
                MotionEvent.ACTION_UP,
                last.first,
                last.second,
                start + durationMs + holdMs,
                start,
            )
            runCatching { view.dispatchTouchEvent(up) }
            up.recycle()
        }
    }

    // ── screenshots in page coordinates ─────────────────────────────────────

    /** Full-page screenshot by laying the WebView out at its content height. */
    suspend fun screenshotFull(maxHeightPx: Int = 20_000): ByteArray {
        val contentHeight = runCatching {
            evaluate("Math.max((document.body?document.body.scrollHeight:0),(document.documentElement?document.documentElement.scrollHeight:0))")
                .toFloatOrNull()?.toInt() ?: 0
        }.getOrDefault(0)
        val contentWidth = runCatching {
            evaluate("Math.max(window.innerWidth||0, document.documentElement?document.documentElement.clientWidth:0)")
                .toFloatOrNull()?.toInt() ?: 0
        }.getOrDefault(0)
        return withContext(Dispatchers.Main) {
            val view = obtain()
            val width = (if (contentWidth > 0) contentWidth else VIEWPORT_WIDTH).coerceIn(200, 4096)
            val height = (if (contentHeight > 0) contentHeight else VIEWPORT_HEIGHT).coerceIn(200, maxHeightPx)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            // restore the interactive viewport
            view.measure(
                View.MeasureSpec.makeMeasureSpec(VIEWPORT_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEWPORT_HEIGHT, View.MeasureSpec.AT_MOST),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    /** Crop a page-coordinate rectangle out of the full-page render. */
    suspend fun screenshotPageRegion(x: Int, y: Int, width: Int, height: Int): ByteArray {
        val full = screenshotFull(maxHeightPx = (y + height + 200).coerceAtMost(30_000))
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(full, 0, full.size)
            ?: return full
        val safeX = x.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
        val safeY = y.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        val safeWidth = width.coerceIn(1, bitmap.width - safeX)
        val safeHeight = height.coerceIn(1, bitmap.height - safeY)
        val cropped = Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
        val output = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (cropped != bitmap) cropped.recycle()
        bitmap.recycle()
        return output.toByteArray()
    }

    // ── async evaluation & condition waiting ────────────────────────────────

    /**
     * Evaluates [script] as an expression, awaiting promises, and returns a JSON
     * envelope: {"status":"OK"|"ERROR"|"TIMEOUT","type":...,"json":...,"text":...}
     */
    suspend fun evaluateAwait(script: String, timeoutMs: Long = 20_000L): String {
        val token = "mcp" + System.nanoTime().toString(36)
        val done = "window['__${token}_done']"
        val value = "window['__${token}_val']"
        val error = "window['__${token}_err']"

        evaluate(
            """
            (function(){
              $done=false; $value=undefined; $error=null;
              try {
                Promise.resolve((function(){ return ($script); })()).then(function(v){
                  $value=v; $done=true;
                }).catch(function(e){
                  $error=(e&&e.message)?e.message:(''+e); $done=true;
                });
              } catch(e) {
                $error=(e&&e.message)?e.message:(''+e); $done=true;
              }
              return 'PENDING';
            })()
            """.trimIndent(),
            8_000L,
        )

        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val state = runCatching { evaluate("(function(){ return $done===true?'DONE':'PENDING'; })()", 5_000L) }
                .getOrDefault("PENDING")
            if (state.contains("DONE")) break
            delay(120L)
        }

        return runCatching {
            evaluate(
                """
                (function(){
                  if ($done !== true) return JSON.stringify({status:'TIMEOUT'});
                  if ($error !== null) return JSON.stringify({status:'ERROR', error:''+$error});
                  var v = $value;
                  var t = (v === null) ? 'null' : (typeof v);
                  var j = null;
                  try { j = JSON.stringify(v); } catch(e) { j = null; }
                  var txt = null;
                  if (v === undefined) txt = 'undefined';
                  else if (t !== 'object') txt = '' + v;
                  return JSON.stringify({status:'OK', type:t, json:j, text:txt});
                })()
                """.trimIndent(),
                6_000L,
            )
        }.getOrDefault("""{"status":"ERROR","error":"evaluation failed"}""")
    }

    /** Polls a JS condition until it is truthy or the timeout expires. */
    suspend fun waitForCondition(condition: String, timeoutMs: Long, pollMs: Long = 250L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = runCatching {
                evaluate("(function(){ try { return (!!($condition)) ? 'TRUE' : 'FALSE'; } catch(e) { return 'ERROR'; } })()", 5_000L)
            }.getOrDefault("ERROR")
            if (result.contains("TRUE")) return true
            delay(pollMs.coerceAtLeast(50L))
        }
        return false
    }

    // ── cookies / identity / history ────────────────────────────────────────

    suspend fun cookies(url: String): String = withContext(Dispatchers.Main) {
        obtain()
        val target = url.ifBlank { "about:blank" }
        runCatching {
            CookieManager.getInstance().apply { setAcceptCookie(true) }.getCookie(target)
        }.getOrNull().orEmpty()
    }

    suspend fun setCookie(url: String, cookie: String) {
        withContext(Dispatchers.Main) {
            obtain()
            runCatching {
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setCookie(url.ifBlank { "https://localhost" }, cookie)
                }
            }
        }
    }

    suspend fun clearCookies() {
        withContext(Dispatchers.Main) {
            obtain()
            runCatching { CookieManager.getInstance().removeAllCookies(null) }
        }
    }

    suspend fun setUserAgent(userAgent: String) {
        withContext(Dispatchers.Main) { obtain().settings.userAgentString = userAgent }
    }

    suspend fun userAgent(): String = withContext(Dispatchers.Main) { obtain().settings.userAgentString.orEmpty() }

    suspend fun goBack() {
        withContext(Dispatchers.Main) {
            val view = obtain()
            if (view.canGoBack()) view.goBack()
        }
    }

    suspend fun goForward() {
        withContext(Dispatchers.Main) {
            val view = obtain()
            if (view.canGoForward()) view.goForward()
        }
    }

    suspend fun reload() {
        withContext(Dispatchers.Main) { obtain().reload() }
    }

    suspend fun close() {
        withContext(Dispatchers.Main) {
            webView?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }.onFailure { Log.w(TAG, "failed to destroy WebView", it) }
            }
            webView = null
        }
    }

    // -- internals -----------------------------------------------------------

    private suspend fun obtain(): WebView = withContext(Dispatchers.Main) {
        webView ?: createView().also { webView = it }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createView(): WebView = WebView(context.applicationContext).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = settings.userAgentString + " McpAndroidServer/1.1"
        webViewClient = WebViewClient()
    }

    /**
     * Re-scopes a script into iframe [index]. Same-origin frames are reached
     * through window.frames[i].document; cross-origin frames report CROSS_ORIGIN
     * so the caller can fall back to coordinate gestures.
     */
    private fun wrapInFrame(script: String, index: Int): String = """
        (function(){
          try {
            var w = window.frames[$index];
            if (!w) return 'NO_FRAME';
            var d = w.document;
            if (!d) return 'NO_FRAME';
            return (function(document, window){ return ($script); })(d, w);
          } catch (e) { return 'CROSS_ORIGIN: ' + e; }
        })()
    """.trimIndent()

    /** Injected gestures carry the touchscreen source so pages treat them as real touch. */
    private fun touchEvent(action: Int, x: Float, y: Float, eventTime: Long, downTime: Long): MotionEvent {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        runCatching { event.source = InputDevice.SOURCE_TOUCHSCREEN }
        return event
    }

    private fun jsString(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
        append('"')
    }

    private fun normalize(url: String): String {
        val trimmed = url.trim()
        require(trimmed.isNotEmpty()) { "url must not be blank" }
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private fun decodeJsValue(value: String?): String {
        if (value == null) return ""
        val trimmed = value.trim()
        if (!trimmed.startsWith("\"")) return trimmed
        return runCatching { org.json.JSONTokener(trimmed).nextValue() as String }.getOrDefault(trimmed)
    }
}
