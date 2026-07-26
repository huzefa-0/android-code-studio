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

import java.util.concurrent.ConcurrentHashMap

/** Holds the tools available to MCP clients. Thread-safe. */
class McpToolRegistry {

    private val tools = ConcurrentHashMap<String, McpTool>()

    fun register(tool: McpTool) {
        require(tool.name.isNotBlank()) { "Tool name must not be blank" }
        tools[tool.name] = tool
    }

    fun registerAll(vararg tools: McpTool) = tools.forEach(::register)

    fun unregister(name: String) {
        tools.remove(name)
    }

    fun find(name: String): McpTool? = tools[name]

    fun all(): List<McpTool> = tools.values.sortedBy { it.name }

    /** Tools the caller is allowed to see, given the granted permission tiers. */
    fun visibleTo(granted: Set<McpPermission>): List<McpTool> =
        all().filter { it.permission in granted }
}
