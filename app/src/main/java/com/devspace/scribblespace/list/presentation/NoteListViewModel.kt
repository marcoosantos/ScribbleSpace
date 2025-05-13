package com.devspace.scribblespace.list.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devspace.scribblespace.list.data.NoteRepository
import com.devspace.scribblespace.list.presentation.ui.NoteUiData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteListViewModel internal constructor(
    private val repository: NoteRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiNotes = MutableStateFlow<NoteListUiState>(NoteListUiState.Loading)
    val uiNotes: StateFlow<NoteListUiState> = _uiNotes.asStateFlow()

    init {
        fetchNotes()
    }

    private fun fetchNotes() {
        viewModelScope.launch(dispatcher) {
            repository
                .getNoteList()
                .collectLatest { noteDataList ->
                    val notesUiData = noteDataList.map { noteData ->
                        NoteUiData(
                            id = noteData.key,
                            title = noteData.title,
                            description = noteData.description
                        )
                    }

                    if (notesUiData.isEmpty()) {
                        _uiNotes.value = NoteListUiState.Empty
                    } else {
                        _uiNotes.value = NoteListUiState.Data(list = notesUiData)

                    }
                }
        }
    }

    fun deleteNote(noteUiData: NoteUiData) {
        viewModelScope.launch(dispatcher) {
            repository.deleteNote(noteUiData.id)
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                return NoteListViewModel(
                    repository = NoteRepository.create(application),
                    dispatcher = Dispatchers.IO
                ) as T
            }
        }
    }
}

sealed class NoteListUiState {

    data object Loading : NoteListUiState()

    data object Empty : NoteListUiState()

    data class Data(val list: List<NoteUiData>) : NoteListUiState()
}