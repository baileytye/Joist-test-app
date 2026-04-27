package com.joist.echoapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.joist.echoapp.domain.TextRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

internal class MainViewModel(private val repository: TextRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<EchoUiState>(EchoUiState.Idle)
    val uiState: StateFlow<EchoUiState> = _uiState.asStateFlow()

    private val _remainingChars = MutableStateFlow(100)
    val remainingChars: StateFlow<Int> = _remainingChars.asStateFlow()

    private val _currentLength = MutableStateFlow(0)

    val isInputValid: StateFlow<Boolean> = combine(_currentLength, _uiState) { length, state ->
        length in 5..100 && state !is EchoUiState.Loading
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onTextChanged(text: String) {
        _currentLength.value = text.length
        _remainingChars.value = 100 - text.length
    }

    fun submit(text: String) {
        if (_uiState.value is EchoUiState.Loading) return
        if (text.length >= 100) {
            _uiState.value = EchoUiState.Error("Text must be under 100 characters")
            return
        }
        if (text.length < 5) {
            _uiState.value = EchoUiState.Error("Text must be at least 5 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = EchoUiState.Loading
            repository.validate(text)
                .onSuccess { _uiState.value = EchoUiState.Success(it) }
                .onFailure { _uiState.value = EchoUiState.Error(it.message ?: "Unknown error") }
        }
    }

    companion object {
        internal fun factory(repository: TextRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(repository) as T
            }
    }
}
