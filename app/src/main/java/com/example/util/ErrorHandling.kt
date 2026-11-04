package com.example.util

import android.util.Log

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    Log.e(
        "FirestoreError",
        "Firestore operation failed [operation=$operation, path=$path]: ${error.localizedMessage}",
        error
    )
}

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
