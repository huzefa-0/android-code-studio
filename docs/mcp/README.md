# MCP server + Live Preview

This directory documents the Model Context Protocol (MCP) layer and the Live Preview
feature for Android Code Studio.

## Why

Exposing IDE capabilities as MCP tools lets any MCP-capable agent (in-app AI, a
desktop client, or a Termux-hosted agent) drive real IDE work: read the project
tree, open files, ask the Java/XML language servers for completions, run Gradle
tasks, tail build logs, and trigger a live preview reload.

## Modules

| Module | Purpose |
| --- | --- |
| `:core:mcp-api` | Interfaces + JSON-RPC 2.0 envelope models. No IDE service dependencies. |
| `:core:mcp-server` | Server implementation: dispatch, permissions, transports. |
| `:core:mcp-tools` | (planned) Concrete tools bridging into existing IDE services. |

The scaffolded modules are intentionally **not yet registered** in
`settings.gradle.kts`. To activate them, add to the `include(...)` block:

```kotlin
":core:mcp-api",
":core:mcp-server",
```

## Transports

1. **stdio** — `StdioTransport`, for a Termux-launched process. Implemented in the scaffold.
2. **HTTP + SSE on 127.0.0.1** — planned; lets external clients connect over localhost.
3. **In-process** — planned; direct registry calls for the IDE's own AI features.

All transports sit behind `McpTransport`, so the tool layer never knows which one is active.

## Planned tool surface

| Tool | Backed by |
| --- | --- |
| `project.open`, `project.tree`, `project.sync` | `:core:projects`, `:tooling:api` |
| `file.read`, `file.write`, `file.patch`, `file.search` | `:core:common` |
| `editor.openFile`, `editor.applyEdit`, `editor.diagnostics` | `:editor:api`, `:editor:impl` |
| `lsp.completion`, `lsp.definition`, `lsp.rename`, `lsp.symbols` | `:core:lsp-api`, `:java:lsp`, `:xml:lsp` |
| `index.query` | `:core:indexing-api`, `:core:indexing-core` |
| `gradle.runTask`, `gradle.buildLogs`, `gradle.cancel` | `:tooling:impl`, `:tooling:events` |
| `terminal.exec`, `terminal.stream` | `:termux:emulator`, `:termux:shared` |
| `apk.install`, `apk.logcat` | `:core:app`, `:logging:logger` |
| `template.create` | `:utilities:templates-api`, `:utilities:templates-impl` |
| `preview.render`, `preview.reload` | Live Preview service |

## Safety model

- Every tool declares a permission tier: `READ`, `WRITE`, or `EXECUTE`.
- `EXECUTE` tools (`terminal.exec`, `gradle.runTask`, `apk.install`) require an
  explicit user opt-in per project.
- All file paths are sandboxed to the open project root (see the `.androidide_root` marker).
- Config persists via `:utilities:preferences`; secrets are never written to logs.
- A foreground notification is shown while a transport is listening, with a one-tap Stop.
- Every tool call is written to an audit log via `:logging:logger`.

## Adding an external ("custom") MCP server in-app

A settings screen registers outbound MCP servers the IDE's AI can call:
name, transport (stdio command or URL), auth header/token, enable toggle, and a
**Test connection** button that runs `initialize` + `tools/list` and lists the
discovered tools. Stored as JSON in app-private storage.

## Live Preview — phased

### Phase 1 — XML / Compose layout preview
- Reuse `:utilities:xml-inflater` and `:utilities:uidesigner` to inflate layouts in a split pane.
- Debounce document changes (~300 ms) and re-inflate only the changed file.
- Cache resources from `:xml:aaptcompiler` so previews work without a full build.
- Device controls: size, density, light/dark, locale, API level, RTL.
- Compose: scan `@Preview` functions and render through an on-device harness activity.
- Inflater errors render inline and link back to the offending editor line.

### Phase 2 — Web preview (HTML/CSS/JS)
- Local static server rooted at the file's folder + a `WebView` pane.
- Save-triggered reload, with a WebSocket reload hook so scroll position survives.
- Console/network messages piped into the IDE log view.

### Phase 3 — Hot reload / instant run
- Incremental Gradle invocation through `:tooling:impl`.
- Resource-only changes: swap the resource APK and restart the top activity.
- Code changes: recompile changed classes, restart the process into the same screen.
- Timing panel driven by `:tooling:events`.
- Exposed to agents as `preview.reload`.

## Delivery order

1. `:core:mcp-api` + `:core:mcp-server` with stdio and 5 read-only tools. *(this branch scaffolds the modules)*
2. Settings screen, permission tiers, audit log; then write/execute tools.
3. HTTP/SSE transport and the outbound custom-server client screen.
4. Live Preview Phase 1, exposed as `preview.render`.
5. Live Preview Phase 2, then Phase 3.
