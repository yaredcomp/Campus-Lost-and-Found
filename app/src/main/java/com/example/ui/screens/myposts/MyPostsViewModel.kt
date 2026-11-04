package com.example.ui.screens.myposts

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

class MyPostsViewModel(
    private val itemRepository: ItemRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId: String = authRepository.currentUser?.uid ?: ""

    val myItemsState: StateFlow<UiState<List<Item>>> = itemRepository.observeUserItems(currentUserId)
        .map<List<Item>, UiState<List<Item>>> { items ->
            UiState.Success(items)
        }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to retrieve your listings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            val result = itemRepository.deleteItem(itemId, currentUserId)
            result.fold(
                onSuccess = {
                    _deleteState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _deleteState.value = UiState.Error(error.localizedMessage ?: "Failed to delete item")
                }
            )
        }
    }
}
