/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.mcp.server

import com.tom.rv2ide.mcp.api.JsonRpcRequest
import com.tom.rv2ide.mcp.api.JsonRpcResponse
import com.tom.rv2ide.mcp.api.McpTransport
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Newline-delimited JSON-RPC over stdin/stdout.
 *
 * Intended for a Termux-launched helper process, which is the simplest way to
 * connect existing MCP clients to the IDE.
 */
class StdioTransport(
    private val input: InputStream = System.`in`,
    private val output: OutputStream = System.out,
) : McpTransport {

    override val id: String = "stdio"

    private var scope: CoroutineScope? = null

    override val isRunning: Boolean
        get() = scope?.coroutineContext?.get(Job)?.isActive == true

    override fun start(handler: suspend (JsonRpcRequest) -> JsonRpcResponse?) {
        val scope = CoroutineScope(Dispatchers.IO + Job()).also { this.scope = it }
        val reader = input.bufferedReader()
        val writer = PrintWriter(output.writer(), true)

        scope.launch {
            reader.useLines { lines ->
                for (line in lines) {
                    if (line.isBlank()) continue
                    val response = runCatching { handler(parse(line)) }.getOrElse { error ->
                        JsonRpcResponse.failed(
                            id = null,
                            error = com.tom.rv2ide.mcp.api.JsonRpcError(
                                code = com.tom.rv2ide.mcp.api.JsonRpcError.PARSE_ERROR,
                                message = error.message ?: "Malformed request",
                            ),
                        )
                    }
                    response?.let { writer.println(serialize(it)) }
                }
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
    }

    private fun parse(line: String): JsonRpcRequest {
        val json = JSONObject(line)
        val params = json.optJSONObject("params")
        return JsonRpcRequest(
            id = if (json.isNull("id")) null else json.get("id").toString(),
            method = json.getString("method"),
            params = params?.toMap() ?: emptyMap(),
        )
    }

    private fun serialize(response: JsonRpcResponse): String {
        val json = JSONObject()
        json.put("jsonrpc", "2.0")
        json.put("id", response.id)
        response.result?.let { json.put("result", JSONObject(it)) }
        response.error?.let { error ->
            json.put(
                "error",
                JSONObject().apply {
                    put("code", error.code)
                    put("message", error.message)
                    error.data?.let { put("data", JSONObject(it)) }
                },
            )
        }
        return json.toString()
    }

    private fun InputStream.bufferedReader(): BufferedReader = reader().buffered()
}
