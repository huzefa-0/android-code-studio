package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tom.rv2ide.editor.ui.IDEEditor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Manages live preview updates by monitoring XML editor changes.
 * Provides debouncing and rate limiting to prevent excessive re-inflations.
 *
 * @author Android Code Studio Team
 */
class LivePreviewManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {

  private var previewRefreshListener: (() -> Unit)? = null
  private var refreshJob: Job? = null
  private var isEnabled = false
  private var lastChangeTime = 0L

  companion object {
    private val log = LoggerFactory.getLogger(LivePreviewManager::class.java)
    private const val REFRESH_DEBOUNCE_MS = 800L
    private const val MIN_REFRESH_INTERVAL_MS = 500L
  }

  fun enableLivePreview(editor: IDEEditor, onRefresh: () -> Unit) {
    if (isEnabled) {
      log.debug("Live preview already enabled")
      return
    }

    previewRefreshListener = onRefresh
    isEnabled = true
    log.debug("Live preview enabled")
  }

  fun disableLivePreview() {
    isEnabled = false
    refreshJob?.cancel()
    previewRefreshListener = null
    log.debug("Live preview disabled")
  }

  fun onEditorContentChanged() {
    lastChangeTime = System.currentTimeMillis()
    scheduleDebouncedRefresh()
  }

  private fun scheduleDebouncedRefresh() {
    refreshJob?.cancel()

    refreshJob =
        lifecycleOwner.lifecycleScope.launch {
          delay(REFRESH_DEBOUNCE_MS.toLong())
          performRefresh()
        }
  }

  private suspend fun performRefresh() {
    if (!isEnabled) {
      return
    }

    val now = System.currentTimeMillis()
    val timeSinceLastChange = now - lastChangeTime

    if (timeSinceLastChange < REFRESH_DEBOUNCE_MS) {
      delay(REFRESH_DEBOUNCE_MS - timeSinceLastChange)
    }

    try {
      previewRefreshListener?.invoke()
    } catch (e: Exception) {
      log.error("Error during live preview refresh", e)
    }
  }

  fun forceRefresh() {
    previewRefreshListener?.invoke()
  }
}
