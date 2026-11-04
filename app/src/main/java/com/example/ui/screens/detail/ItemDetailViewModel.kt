package com.example.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Item
import com.example.data.repository.AuthRepository
import com.example.data.repository.ItemRepository
import com.example.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val itemId: String,
    private val itemRepository: ItemRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String?
        get() = authRepository.currentUser?.uid

    val itemState: StateFlow<UiState<Item?>> = itemRepository.observeItemById(itemId)
        .map<Item?, UiState<Item?>> { item ->
            if (item != null) UiState.Success(item) else UiState.Error("Item not found")
        }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to retrieve item"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = (itemState.value as? UiState.Success)?.data
            if (item == null) return@launch
            val uid = currentUserId
            if (uid == null || uid != item.ownerId) {
                _deleteState.value = UiState.Error("Only the poster can delete this item")
                return@launch
            }

            _deleteState.value = UiState.Loading
            val result = itemRepository.deleteItem(itemId, item.ownerId)
            result.fold(
                onSuccess = {
                    _deleteState.value = UiState.Success(Unit)
                    onDeleted()
                },
                onFailure = { error ->
                    _deleteState.value = UiState.Error(error.localizedMessage ?: "Failed to delete item")
                }
            )
        }
    }
}
