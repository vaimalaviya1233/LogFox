package com.f0x1d.logfox.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.navArgs
import com.f0x1d.logfox.databinding.SheetRecordingBinding
import com.f0x1d.logfox.extensions.exportFormatted
import com.f0x1d.logfox.extensions.logToZip
import com.f0x1d.logfox.extensions.shareFileIntent
import com.f0x1d.logfox.extensions.toLocaleString
import com.f0x1d.logfox.ui.dialog.base.BaseViewModelBottomSheet
import com.f0x1d.logfox.viewmodel.recordings.RecordingViewModel
import com.f0x1d.logfox.viewmodel.recordings.RecordingViewModelAssistedFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingBottomSheet: BaseViewModelBottomSheet<RecordingViewModel, SheetRecordingBinding>() {

    @Inject
    lateinit var assistedFactory: RecordingViewModelAssistedFactory

    override val viewModel by viewModels<RecordingViewModel> {
        viewModelFactory {
            initializer {
                assistedFactory.create(navArgs.recordingId)
            }
        }
    }

    private val zipLogLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
        viewModel.logToZip(
            it ?: return@registerForActivityResult,
            viewModel.recording.value ?: return@registerForActivityResult
        )
    }
    private val logExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
        viewModel.exportFile(it ?: return@registerForActivityResult)
    }

    private val navArgs by navArgs<RecordingBottomSheetArgs>()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) = SheetRecordingBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.recording.observe(viewLifecycleOwner) { logRecording ->
            if (logRecording == null) return@observe

            binding.timeText.text = logRecording.dateAndTime.toLocaleString()

            binding.exportLayout.setOnClickListener {
                logExportLauncher.launch("${logRecording.dateAndTime.exportFormatted}.txt")
            }
            binding.shareLayout.setOnClickListener {
                requireContext().shareFileIntent(File(logRecording.file))
            }
            binding.zipLayout.setOnClickListener {
                zipLogLauncher.launch("${logRecording.dateAndTime.exportFormatted}.zip")
            }
        }

        viewModel.currentTitle.filterNotNull().take(1).asLiveData().observe(viewLifecycleOwner) {
            binding.title.apply {
                setText(it)
                doAfterTextChanged { viewModel.updateTitle(it?.toString() ?: "") }
            }
        }
    }
}