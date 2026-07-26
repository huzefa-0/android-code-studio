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
 * Permission tier of a tool. The user opts in per tier, per project.
 *
 * - [READ] never mutates workspace state.
 * - [WRITE] mutates files or editor buffers.
 * - [EXECUTE] runs processes: Gradle tasks, shell commands, installs.
 */
enum class McpPermission {
    READ,
    WRITE,
    EXECUTE,
}

/** Result of a tool call, mapped to MCP `content` blocks by the server. */
data class McpToolResult(
    val text: String? = null,
    val structured: Map<String, Any?>? = null,
    val isError: Boolean = false,
) {
    companion object {
        fun text(value: String) = McpToolResult(text = value)

        fun structured(value: Map<String, Any?>) = McpToolResult(structured = value)

        fun error(message: String) = McpToolResult(text = message, isError = true)
    }
}

/**
 * A single capability exposed to MCP clients.
 *
 * Implementations live in `:core:mcp-tools` and bridge into existing IDE
 * services. They must not assume a specific transport.
 */
interface McpTool {

    /** Stable, dotted identifier, e.g. `project.tree`. */
    val name: String

    /** One-line description surfaced to the client in `tools/list`. */
    val description: String

    /** Permission tier required to invoke this tool. */
    val permission: McpPermission

    /** JSON Schema object describing accepted arguments. */
    val inputSchema: Map<String, Any?>

    /** Executes the tool. Throwing is allowed; the server maps it to a JSON-RPC error. */
    suspend fun call(arguments: Map<String, Any?>): McpToolResult
}
