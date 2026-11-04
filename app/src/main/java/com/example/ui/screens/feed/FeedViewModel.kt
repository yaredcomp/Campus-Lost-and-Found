package com.example.ui.screens.feed

import android.content.Context
import androidx.credentials.CredentialManager
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FeedViewModel(
    private val itemRepository: ItemRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "LOST", "FOUND"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentUserId: String?
        get() = authRepository.currentUser?.uid

    val currentUserEmail: String?
        get() = authRepository.currentUser?.email

    val currentUserName: String?
        get() = authRepository.currentUser?.displayName

    val itemsState: StateFlow<UiState<List<Item>>> = combine(
        itemRepository.observeAllItems(),
        _selectedFilter,
        _searchQuery
    ) { items, filter, query ->
        val filtered = items.filter { item ->
            val matchesFilter = when (filter) {
                "LOST" -> item.type.equals("LOST", ignoreCase = true)
                "FOUND" -> item.type.equals("FOUND", ignoreCase = true)
                else -> true
            }

            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.location.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
        UiState.Success(filtered) as UiState<List<Item>>
    }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to load campus items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun signOut(context: Context, credentialManager: CredentialManager, onComplete: () -> Unit) {
        authRepository.signOut(context, credentialManager, viewModelScope, onComplete)
    }
}
