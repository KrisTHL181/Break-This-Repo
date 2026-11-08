package com.mcpserver.mcp.protocol

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "Transport"

// ─── Transport Interface ──────────────────────────────────────────────────────

interface Transport {
    suspend fun connect()
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun send(message: String)
    fun receive(): Flow<String>
    fun getTransportType(): String
}

// ─── Stdio Transport ──────────────────────────────────────────────────────────

class StdioTransport(
    private val input: InputStream = System.`in`,
    private val output: OutputStream = System.out,
) : Transport {

    private var connected = false
    private val receiveChannel = Channel<String>(Channel.BUFFERED)
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun connect() {
        connected = true
        startReading()
        Log.i(TAG, "StdioTransport connected")
    }

    override suspend fun disconnect() {
        connected = false
        readJob?.cancel()
        receiveChannel.close()
        scope.cancel()
    }

    override fun isConnected(): Boolean = connected

    override suspend fun send(message: String) {
        if (!connected) throw IllegalStateException("Transport is not connected")
        withContext(Dispatchers.IO) {
            output.write((message + "\n").toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    override fun receive(): Flow<String> = receiveChannel.receiveAsFlow()

    override fun getTransportType(): String = "stdio"

    private fun startReading() {
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(input))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) receiveChannel.send(line)
                }
            } catch (e: CancellationException) {
                // normal shutdown
            } catch (e: Exception) {
                Log.e(TAG, "StdioTransport read error", e)
            } finally {
                connected = false
                receiveChannel.close()
            }
        }
    }
}

// ─── SSE / HTTP Transport ─────────────────────────────────────────────────────

/**
 * HTTP transport serving both MCP styles:
 *
 *  - legacy SSE:   GET /events (stream) + POST /messages (202 + reply on stream)
 *  - streamable:   POST /mcp (JSON-RPC reply in the HTTP response body)
 *
 * Connections get a read timeout until the request is parsed, the accept loop
 * survives per-connection failures, and SSE streams are kept alive with
 * heartbeats so clients never sit on a silently dead socket.
 */
class SseTransport(
    private val port: Int = 8392,
    private val host: String = "0.0.0.0",
    private val authValidator: (String?) -> Boolean = { true },
    /** When set, POST bodies are handled inline and answered in the HTTP response. */
    private val onRequest: (suspend (String) -> String)? = null,
) : Transport {

    private var connected = false
    private val receiveChannel = Channel<String>(Channel.BUFFERED)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sseClients = CopyOnWriteArrayList<PrintWriter>()

    @Volatile private var startedAt: Long = 0

    override suspend fun connect() {
        disconnect()
        serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName(host))
        connected = true
        startedAt = System.currentTimeMillis()
        startAccepting()
        Log.i(TAG, "SseTransport listening on $host:$port")
    }

    override suspend fun disconnect() {
        connected = false
        sseClients.clear()
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        Log.i(TAG, "SseTransport disconnected")
    }

    override fun isConnected(): Boolean = connected

    override suspend fun send(message: String) {
        broadcast(message)
    }

    override fun receive(): Flow<String> = receiveChannel.receiveAsFlow()

    override fun getTransportType(): String = "sse"

    private fun startAccepting() {
        serverJob = scope.launch {
            while (isActive) {
                val socket = try {
                    serverSocket?.accept() ?: break
                } catch (cancelled: CancellationException) {
                    break
                } catch (error: Exception) {
                    if (!isActive) break
                    Log.w(TAG, "accept() failed, retrying", error)
                    delay(200)
                    continue
                }
                launch {
                    runCatching { handleClient(socket) }
                        .onFailure { Log.e(TAG, "client handler failed", it) }
                }
            }
        }
    }

    private fun broadcast(message: String) {
        val payload = "data: $message\n\n"
        val dead = mutableListOf<PrintWriter>()
        sseClients.forEach { writer ->
            try {
                writer.print(payload)
                writer.flush()
                if (writer.checkError()) dead.add(writer)
            } catch (e: Exception) {
                dead.add(writer)
            }
        }
        if (dead.isNotEmpty()) sseClients.removeAll(dead.toSet())
    }

    private suspend fun handleClient(socket: Socket) {
        var writer: PrintWriter? = null
        try {
            socket.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val rawPath = parts[1]

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                }
            }

            val path = rawPath.substringBefore('?')
            val query = rawPath.substringAfter('?', "")
            val queryToken = query.split('&')
                .map { it.split('=', limit = 2) }
                .firstOrNull { it.size == 2 && (it[0] == "key" || it[0] == "token") }
                ?.get(1)
            val token = headers["authorization"]?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotBlank() }
                ?: queryToken

            when {
                path == "/health" || path == "/" -> {
                    respondJson(writer, """{"status":"ok","server":"mcp-android-server","transport":"sse","port":$port}""")
                }
                path == "/diag" -> {
                    respondJson(
                        writer,
                        """{"connected":$connected,"sse_clients":${sseClients.size},""" +
                            """"uptime_ms":${if (startedAt == 0L) 0 else System.currentTimeMillis() - startedAt}}"""
                    )
                }
                method == "GET" && (path == "/events" || path == "/sse" || path == "/mcp") -> {
                    if (!authValidator(token)) {
                        respondUnauthorized(writer)
                    } else {
                        handleSseConnection(socket, writer)
                    }
                }
                method == "POST" && (path == "/messages" || path == "/message" || path == "/mcp") -> {
                    if (!authValidator(token)) {
                        respondUnauthorized(writer)
                        return
                    }
                    val body = readBody(reader, headers)
                    if (body.isBlank()) {
                        respondJson(writer, """{"error":"empty body"}""", status = "400 Bad Request")
                        return
                    }
                    val handler = onRequest
                    if (handler != null) {
                        val response = runCatching { handler.invoke(body) }
                            .getOrElse { """{"jsonrpc":"2.0","error":{"code":-32603,"message":"${escape(it.message)}"}}""" }
                        broadcast(response)
                        respondJson(writer, response.ifBlank { """{"jsonrpc":"2.0","result":{}}""" })
                    } else {
                        writer.println("HTTP/1.1 202 Accepted")
                        writer.println("Content-Length: 0")
                        writer.println("Connection: close")
                        writer.println()
                        writer.flush()
                        receiveChannel.send(body)
                    }
                }
                else -> {
                    writer.println("HTTP/1.1 404 Not Found")
                    writer.println("Connection: close")
                    writer.println()
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "client handling error: ${e.message}")
        } finally {
            runCatching { writer?.flush() }
            runCatching { socket.close() }
        }
    }

    private fun readBody(reader: BufferedReader, headers: Map<String, String>): String {
        val chunked = headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true
        return if (chunked) readChunked(reader) else readFixed(reader, headers["content-length"]?.toIntOrNull() ?: 0)
    }

    private fun readFixed(reader: BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val chars = CharArray(length)
        var read = 0
        while (read < length) {
            val count = reader.read(chars, read, length - read)
            if (count < 0) break
            read += count
        }
        return String(chars, 0, read)
    }

    private fun readChunked(reader: BufferedReader): String {
        val builder = StringBuilder()
        while (true) {
            val sizeLine = reader.readLine() ?: break
            val size = sizeLine.trim().substringBefore(';').toIntOrNull(16) ?: break
            if (size == 0) {
                reader.readLine()
                break
            }
            builder.append(readFixed(reader, size))
            reader.readLine()
        }
        return builder.toString()
    }

    private suspend fun handleSseConnection(socket: Socket, writer: PrintWriter) {
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/event-stream")
        writer.println("Cache-Control: no-cache")
        writer.println("Connection: keep-alive")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println()
        writer.flush()

        sseClients.add(writer)
        Log.i(TAG, "SSE client connected (total=${sseClients.size})")

        socket.soTimeout = 0
        val heartbeat = scope.launch {
            while (isActive) {
                delay(15_000)
                runCatching {
                    writer.print(": ping\n\n")
                    writer.flush()
                }
            }
        }
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (!socket.isClosed) {
                if (reader.readLine() == null) break
            }
        } catch (_: Exception) {
            // client went away
        } finally {
            heartbeat.cancel()
            sseClients.remove(writer)
            runCatching { socket.close() }
            Log.i(TAG, "SSE client disconnected (total=${sseClients.size})")
        }
    }

    private fun respondJson(writer: PrintWriter, body: String, status: String = "200 OK") {
        val bytes = body.toByteArray(Charsets.UTF_8)
        writer.println("HTTP/1.1 $status")
        writer.println("Content-Type: application/json; charset=utf-8")
        writer.println("Content-Length: ${bytes.size}")
        writer.println("Connection: close")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println()
        writer.print(body)
        writer.flush()
    }

    private fun respondUnauthorized(writer: PrintWriter) {
        respondJson(
            writer,
            """{"error":"unauthorized","hint":"Authorization: Bearer <api key> or ?key=<api key>"}""",
            status = "401 Unauthorized",
        )
    }

    private fun escape(message: String?): String = (message ?: "unknown").replace("\\", "\\\\").replace("\"", "\\\"")
}
