package com.ttcn.promotionsdk.promotionsdkui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class PRMBaseViewModel<S, A, E>(initialState: S) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<E>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val uiEffect: SharedFlow<E> = _uiEffect.asSharedFlow()

    abstract fun handleAction(action: A)

    protected fun setState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(
            context = CoroutineExceptionHandler { _, throwable ->
                onError(throwable)
            },
            block = block,
        )
    }

    open fun onError(throwable: Throwable) {}
}