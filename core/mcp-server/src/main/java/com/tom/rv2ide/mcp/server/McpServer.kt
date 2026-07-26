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

import com.tom.rv2ide.mcp.api.JsonRpcError
import com.tom.rv2ide.mcp.api.JsonRpcRequest
import com.tom.rv2ide.mcp.api.JsonRpcResponse
import com.tom.rv2ide.mcp.api.MCP_PROTOCOL_VERSION
import com.tom.rv2ide.mcp.api.McpMethods
import com.tom.rv2ide.mcp.api.McpPermission
import com.tom.rv2ide.mcp.api.McpToolRegistry
import com.tom.rv2ide.mcp.api.McpTransport

/**
 * Dispatches MCP requests to registered tools.
 *
 * The server owns no transport of its own; attach one (stdio, HTTP+SSE, or
 * in-process) via [serve].
 *
 * @param registry tools exposed to clients.
 * @param grantedPermissions tiers the user has opted into for the current project.
 * @param auditLog invoked once per tool call so the IDE can show what an agent did.
 */
class McpServer(
    private val registry: McpToolRegistry,
    private val grantedPermissions: Set<McpPermission> = setOf(McpPermission.READ),
    private val serverName: String = "android-code-studio",
    private val serverVersion: String = "0.1.0",
    private val auditLog: (String, Map<String, Any?>) -> Unit = { _, _ -> },
) {

    private var transport: McpTransport? = null

    /** Starts [transport] and routes its traffic through [handle]. */
    fun serve(transport: McpTransport) {
        stop()
        this.transport = transport
        transport.start { request -> handle(request) }
    }

    fun stop() {
        transport?.stop()
        transport = null
    }

    /** Handles a single request. Returns null for notifications. */
    suspend fun handle(request: JsonRpcRequest): JsonRpcResponse? {
        if (request.id == null) {
            // Notification: nothing to answer.
            return null
        }

        return when (request.method) {
            McpMethods.PING -> JsonRpcResponse.ok(request.id, emptyMap())
            McpMethods.INITIALIZE -> initialize(request)
            McpMethods.TOOLS_LIST -> listTools(request)
            McpMethods.TOOLS_CALL -> callTool(request)
            else -> JsonRpcResponse.failed(
                request.id,
                JsonRpcError(
                    code = JsonRpcError.METHOD_NOT_FOUND,
                    message = "Unknown method: ${request.method}",
                ),
            )
        }
    }

    private fun initialize(request: JsonRpcRequest) = JsonRpcResponse.ok(
        request.id,
        mapOf(
            "protocolVersion" to MCP_PROTOCOL_VERSION,
            "capabilities" to mapOf("tools" to mapOf("listChanged" to true)),
            "serverInfo" to mapOf("name" to serverName, "version" to serverVersion),
        ),
    )

    private fun listTools(request: JsonRpcRequest) = JsonRpcResponse.ok(
        request.id,
        mapOf(
            "tools" to registry.visibleTo(grantedPermissions).map { tool ->
                mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "inputSchema" to tool.inputSchema,
                )
            },
        ),
    )

    private suspend fun callTool(request: JsonRpcRequest): JsonRpcResponse {
        val name = request.params["name"] as? String
            ?: return JsonRpcResponse.failed(
                request.id,
                JsonRpcError(JsonRpcError.INVALID_PARAMS, "Missing 'name'"),
            )

        @Suppress("UNCHECKED_CAST")
        val arguments = (request.params["arguments"] as? Map<String, Any?>) ?: emptyMap()

        val tool = registry.find(name)
            ?: return JsonRpcResponse.failed(
                request.id,
                JsonRpcError(JsonRpcError.METHOD_NOT_FOUND, "Unknown tool: $name"),
            )

        if (tool.permission !in grantedPermissions) {
            return JsonRpcResponse.failed(
                request.id,
                JsonRpcError(
                    code = JsonRpcError.PERMISSION_DENIED,
                    message = "Tool '$name' requires the ${tool.permission} permission",
                    data = mapOf("required" to tool.permission.name),
                ),
            )
        }

        auditLog(name, arguments)

        return try {
            val result = tool.call(arguments)
            val content = buildList {
                result.text?.let { add(mapOf("type" to "text", "text" to it)) }
            }
            JsonRpcResponse.ok(
                request.id,
                buildMap {
                    put("content", content)
                    put("isError", result.isError)
                    result.structured?.let { put("structuredContent", it) }
                },
            )
        } catch (error: Throwable) {
            JsonRpcResponse.failed(
                request.id,
                JsonRpcError(
                    code = JsonRpcError.INTERNAL_ERROR,
                    message = error.message ?: error.javaClass.simpleName,
                ),
            )
        }
    }
}
