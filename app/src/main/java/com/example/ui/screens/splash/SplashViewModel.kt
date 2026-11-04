package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object Feed : SplashDestination()
    data object Login : SplashDestination()
}

class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            // Brief branding delay for smooth transition
            delay(800)
            if (authRepository.currentUser != null) {
                _destination.value = SplashDestination.Feed
            } else {
                _destination.value = SplashDestination.Login
            }
        }
    }
}
