package com.joist.echoapp.presentation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.joist.echoapp.data.FakeTextRepository
import com.joist.echoapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(FakeTextRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitButton.setOnClickListener { submitText() }

        binding.textInput.doOnTextChanged { text, _, _, _ ->
            binding.textInputLayout.error = null
            viewModel.onTextChanged(text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is EchoUiState.Success) binding.outputText.text = state.text

                    binding.apply {
                        textInputLayout.error = if (state is EchoUiState.Error) state.message else null
                        progressBar.isVisible = state is EchoUiState.Loading
                        submitButton.isEnabled = state !is EchoUiState.Loading
                        outputText.isVisible = state is EchoUiState.Success
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.remainingChars.collect { remaining ->
                binding.charCounter.text = "$remaining characters remaining"
            }
        }
    }

    private fun submitText() {
        viewModel.submit(binding.textInput.text!!.toString())
    }
}
