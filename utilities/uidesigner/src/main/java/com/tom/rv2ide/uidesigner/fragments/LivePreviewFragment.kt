/*
 *  This file is part of Android Code Studio.
 *
 *  Android Code Studio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.tom.rv2ide.uidesigner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tom.rv2ide.uidesigner.databinding.FragmentLivePreviewBinding
import com.tom.rv2ide.uidesigner.models.LayoutFile
import com.tom.rv2ide.uidesigner.models.RootWorkspaceView
import com.tom.rv2ide.uidesigner.utils.UiLayoutInflater
import com.tom.rv2ide.uidesigner.viewmodel.WorkspaceViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Fragment that provides live preview of XML layouts.
 * Updates the preview in real-time as the XML source is edited.
 *
 * @author Android Code Studio Team
 */
class LivePreviewFragment : Fragment() {

  private var binding: FragmentLivePreviewBinding? = null
  private val viewModel by viewModels<WorkspaceViewModel>(ownerProducer = { requireActivity() })
  private var livePreviewView: RootWorkspaceView? = null
  private var refreshJob: kotlinx.coroutines.Job? = null
  private var lastInflationTime = 0L
  private var isInflating = false

  companion object {
    private val log = LoggerFactory.getLogger(LivePreviewFragment::class.java)
    private const val DEBOUNCE_DELAY_MS = 800L
    private const val MIN_INFLATION_INTERVAL_MS = 500L
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    binding = FragmentLivePreviewBinding.inflate(inflater, container, false)
    return binding!!.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeLivePreview()
  }

  private fun initializeLivePreview() {
    val binding = this.binding ?: return
    val file = viewModel.file

    livePreviewView =
        RootWorkspaceView(
            LayoutFile(file, ""),
            LinearLayout::class.qualifiedName!!,
            binding.previewContainer,
        )

    refreshPreview()
  }

  fun refreshPreview() {
    refreshJob?.cancel()

    refreshJob =
        viewLifecycleOwner.lifecycleScope.launch {
          delay(DEBOUNCE_DELAY_MS)
          performInflation()
        }
  }

  private suspend fun performInflation() {
    val now = System.currentTimeMillis()

    if (now - lastInflationTime < MIN_INFLATION_INTERVAL_MS) {
      delay(MIN_INFLATION_INTERVAL_MS - (now - lastInflationTime))
    }

    if (isInflating) {
      log.debug("Skipping inflation - already inflating")
      return
    }

    try {
      isInflating = true
      lastInflationTime = System.currentTimeMillis()

      val inflater = UiLayoutInflater()
      val workspaceView = livePreviewView ?: return
      val layoutFile = viewModel.file

      val inflated =
          try {
            inflater.inflate(layoutFile, workspaceView)
          } catch (e: Throwable) {
            log.error("Failed to inflate layout in live preview", e)
            updateErrorState(e.message ?: "Unknown error")
            emptyList()
          } finally {
            inflater.close()
          }

      if (inflated.isNotEmpty()) {
        updatePreviewState(null)
      } else {
        updateErrorState("No views found in layout")
      }
    } finally {
      isInflating = false
    }
  }

  private fun updatePreviewState(error: String?) {
    val binding = this.binding ?: return
    binding.previewContainer.visibility = if (error == null) View.VISIBLE else View.GONE
    binding.errorMessage.visibility = if (error != null) View.VISIBLE else View.GONE
    if (error != null) {
      binding.errorMessage.text = error
    }
  }

  private fun updateErrorState(message: String) {
    val binding = this.binding ?: return
    updatePreviewState(message)
    log.error("Live preview error: {}", message)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    refreshJob?.cancel()
    livePreviewView = null
    binding = null
  }
}
