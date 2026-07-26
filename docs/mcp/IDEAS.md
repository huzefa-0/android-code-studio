# Feature ideas backlog

Ideas beyond the MCP layer and Live Preview, grouped by area. Not committed work —
use this as a pick-list.

## Editor & code intelligence
- Multi-caret editing and column selection in `:editor:impl`.
- Tree-sitter driven structural selection and folding (`:editor:treesitter` already present).
- Inline diagnostics with quick-fix bulbs for the Java and XML language servers.
- Project-wide rename with a reviewable diff.
- Kotlin language server alongside `:java:lsp`.
- Regex find-and-replace across the project with diff preview.
- Bookmarks and "recent locations" navigation.

## Build & run
- Build output analyzer: rank tasks by duration, flag configuration-cache misses.
- Gradle task favorites and per-project run configurations.
- Dependency inspector: version catalog editing, update checks, conflict tree.
- APK/AAB analyzer: method counts, resource sizes, merged manifest.
- Offline dependency cache warmer for slow or metered connections.

## AI (built on the MCP layer)
- Inline completion and "explain this build error".
- Agent mode: multi-file edit proposals reviewed as a diff before apply.
- Commit message generation from the staged diff.
- Bring-your-own-key providers configured next to MCP server settings.

## Git & collaboration
- Built-in Git panel: stage, commit, push, branch switcher, conflict resolver.
- Inline blame and diff gutter.
- GitHub integration: clone, PR list, review comments.

## Platform & polish
- Material 3 theme engine with importable editor themes.
- Keyboard-first mode for external keyboards plus a shortcut cheat sheet.
- Tablet and foldable multi-pane layout.
- Templates for Compose, KMP, and library modules via `:utilities:templates-impl`.
- Opt-in crash/ANR reporting through `:logging:idestats`.
- Plugin API so third parties can register MCP tools and preview renderers.
