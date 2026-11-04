package com.example.ui.screens.register

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onRegisterSubmit(context: Context, credentialManager: CredentialManager, onSuccess: () -> Unit) {
        if (_name.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter your name")
            return
        }
        if (_email.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter your campus email")
            return
        }
        // Direct to Google Sign-In with university credentials
        signInWithGoogle(context, credentialManager, onSuccess)
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.signInWithGoogle(context, credentialManager)
            result.fold(
                onSuccess = {
                    _uiState.value = UiState.Success(Unit)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        error.localizedMessage ?: "Registration cancelled or failed."
                    )
                }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Success(Unit)
        }
    }
}
