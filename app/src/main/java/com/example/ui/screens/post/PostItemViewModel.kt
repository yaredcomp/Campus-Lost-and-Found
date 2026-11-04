package com.example.ui.screens.post

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ItemRepository
import com.example.data.repository.StorageRepository
import com.example.util.Constants
import com.example.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostItemViewModel(
    private val itemRepository: ItemRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _type = MutableStateFlow(Constants.TYPE_LOST)
    val type: StateFlow<String> = _type.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()

    fun onTypeChange(newType: String) {
        _type.value = newType
    }

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
    }

    fun onDescriptionChange(newDesc: String) {
        _description.value = newDesc
    }

    fun onLocationChange(newLocation: String) {
        _location.value = newLocation
    }

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun submitPost(context: Context, onSuccess: () -> Unit) {
        val currentTitle = _title.value.trim()
        val currentDesc = _description.value.trim()
        val currentLocation = _location.value.trim()

        if (currentTitle.isBlank()) {
            _uiState.value = UiState.Error("Please enter an item title")
            return
        }
        if (currentDesc.isBlank()) {
            _uiState.value = UiState.Error("Please provide a brief description")
            return
        }
        if (currentLocation.isBlank()) {
            _uiState.value = UiState.Error("Please specify where the item was lost or found")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uploadProgress.value = 0.05f

            val uri = _selectedImageUri.value
            if (uri != null) {
                // Upload image with progress tracking
                storageRepository.uploadItemImage(context, uri).collect { uploadStatus ->
                    when (uploadStatus) {
                        is StorageRepository.UploadStatus.Progress -> {
                            _uploadProgress.value = uploadStatus.progress
                        }
                        is StorageRepository.UploadStatus.Success -> {
                            _uploadProgress.value = 1.0f
                            saveItemToFirestore(uploadStatus.imageUrl, onSuccess)
                        }
                        is StorageRepository.UploadStatus.Error -> {
                            _uiState.value = UiState.Error("Image upload failed: ${uploadStatus.message}")
                        }
                    }
                }
            } else {
                // Save without image
                _uploadProgress.value = 1.0f
                saveItemToFirestore("", onSuccess)
            }
        }
    }

    private suspend fun saveItemToFirestore(imageUrl: String, onSuccess: () -> Unit) {
        val result = itemRepository.createItem(
            type = _type.value,
            title = _title.value,
            description = _description.value,
            location = _location.value,
            imageUrl = imageUrl
        )

        result.fold(
            onSuccess = { docId ->
                _uiState.value = UiState.Success(docId)
                onSuccess()
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Failed to save item listing")
            }
        )
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Success("")
        }
    }
}
