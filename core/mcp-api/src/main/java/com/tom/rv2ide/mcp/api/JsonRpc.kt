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

package com.tom.rv2ide.mcp.api

/** Protocol version this implementation speaks. */
const val MCP_PROTOCOL_VERSION: String = "2024-11-05"

/** JSON-RPC 2.0 request or notification. A notification has a null [id]. */
data class JsonRpcRequest(
    val id: String?,
    val method: String,
    val params: Map<String, Any?> = emptyMap(),
)

/** JSON-RPC 2.0 response. Exactly one of [result] or [error] is non-null. */
data class JsonRpcResponse(
    val id: String?,
    val result: Map<String, Any?>? = null,
    val error: JsonRpcError? = null,
) {
    companion object {
        fun ok(id: String?, result: Map<String, Any?>) = JsonRpcResponse(id = id, result = result)

        fun failed(id: String?, error: JsonRpcError) = JsonRpcResponse(id = id, error = error)
    }
}

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Map<String, Any?>? = null,
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603

        /** Tool exists but the user has not granted its permission tier. */
        const val PERMISSION_DENIED = -32001

        /** A path argument escaped the open project root. */
        const val PATH_OUT_OF_SANDBOX = -32002
    }
}

/** MCP method names handled by the server. */
object McpMethods {
    const val INITIALIZE = "initialize"
    const val TOOLS_LIST = "tools/list"
    const val TOOLS_CALL = "tools/call"
    const val PING = "ping"
}
