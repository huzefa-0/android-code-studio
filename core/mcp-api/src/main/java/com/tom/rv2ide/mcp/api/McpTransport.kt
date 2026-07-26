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

/**
 * Carries JSON-RPC traffic between a client and the server.
 *
 * Implementations: stdio (Termux process), HTTP + SSE on loopback, and
 * in-process for the IDE's own AI features.
 */
interface McpTransport {

    /** Human readable id used in logs and the status notification. */
    val id: String

    val isRunning: Boolean

    /**
     * Starts accepting traffic. [handler] is invoked for each inbound request and
     * returns the response to write back, or null for notifications.
     */
    fun start(handler: suspend (JsonRpcRequest) -> JsonRpcResponse?)

    /** Stops accepting traffic and releases resources. Must be idempotent. */
    fun stop()
}
