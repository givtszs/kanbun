package com.example.kanbun.ui.board

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.ViewState.BoardViewState
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropTaskItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BoardViewModel"

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _board = MutableStateFlow(Board())

    @OptIn(ExperimentalCoroutinesApi::class)
    private var _boardLists: Flow<Result<List<BoardList>>> = _board.flatMapLatest { board ->
        firestoreRepository.getBoardListsStream(
            boardId = board.id,
            workspaceId = board.settings.workspace.id
        )
    }

    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)
    val boardState: StateFlow<BoardViewState> =
        combine(
            _board,
            _boardLists,
            _isLoading,
            _message
        ) { board, boardLists, isLoading, message ->
            Log.d(
                TAG,
                "boardState#_board: $board,\n_boardLists: $boardLists,\nisLoading: $isLoading,\nmessage: $message"
            )
            var isLoading1 = isLoading
            BoardViewState(
                board = board,
                lists = when (boardLists) {
                    is Result.Success -> {
                        Log.d(TAG, "boardState#_boardLists: ${boardLists.data.reversed()}")
                        boardLists.data
                    }

                    is Result.Error -> emptyList()
                    is Result.Loading -> {
                        isLoading1 = true
                        emptyList()
                    }
                },
                isLoading = isLoading1,
                message = message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BoardViewState())

    suspend fun getBoard(boardId: String, workspaceId: String) {
        // get board
        when (val result = firestoreRepository.getBoard(boardId, workspaceId)) {
            is Result.Success -> _board.value = result.data
            is Result.Error -> _message.value = result.message
            is Result.Loading -> _isLoading.value = true
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun stopLoading() {
        _isLoading.value = false
    }

    fun createBoardList(listName: String) = viewModelScope.launch {
        firestoreRepository.createBoardList(
            boardList = BoardList(
                name = listName,
                position = _board.value.lists.size.toLong(),
            ),
            board = _board.value
        )

        // update board
        getBoard(
            boardId = _board.value.id,
            workspaceId = _board.value.settings.workspace.id
        )
    }

    fun createTask(name: String, boardList: BoardList) = viewModelScope.launch {
        val task = Task(
            position = boardList.tasks.size.toLong(),
            name = name,
        )

        firestoreRepository.createTask(
            task = task,
            listId = boardList.id,
            boardId = _board.value.id,
            workspaceId = _board.value.settings.workspace.id
        )
    }

    fun rearrangeTasks(listPath: String, listId: String, tasks: List<Task>, from: Int, to: Int) =
        viewModelScope.launch {
            firestoreRepository.rearrangeTasksPositions(
                listPath = listPath,
                listId = listId,
                tasks = tasks,
                from = from,
                to = to
            )
        }

    fun deleteTaskAndRearrange(listPath: String, listId: String, tasks: List<Task>, from: Int) =
        viewModelScope.launch {
            firestoreRepository.deleteTaskAndRearrange(
                listPath = listPath,
                listId = listId,
                tasks = tasks,
                from = from,
            )
        }

    fun insertTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<Task>,
        task: Task,
        to: Int
    ) = viewModelScope.launch {
        firestoreRepository.insertTaskAndRearrange(
            listPath = listPath,
            listId = listId,
            tasks = tasks,
            task = task,
            to = to
        )
    }

    fun deleteAndInsert(
        adapter: TasksAdapter,
        dragItem: DragAndDropTaskItem,
        to: Int
    ) = viewModelScope.launch {
        val tasksRemoveStr = buildString { dragItem.initTasksList.forEach { append("$it, ") } }
        Log.d(
            "ItemTaskViewHolder",
            "Deleting task in adapter $adapter at position ${dragItem.initPosition} from tasks $tasksRemoveStr"
        )
        val deleteResult = firestoreRepository.deleteTaskAndRearrange(
            dragItem.initBoardList.path,
            dragItem.initBoardList.id,
            dragItem.initTasksList,
            dragItem.initPosition
        )

        if (deleteResult is Result.Error) {
            _message.value = deleteResult.message
            return@launch
        }

        val tasksInsertStr = buildString { adapter.tasks.forEach { append("$it, ") } }
        Log.d(
            "ItemTaskViewHolder",
            "Inserting task in adapter $adapter at position $to into tasks $tasksInsertStr"
        )
        val insertResult =
            firestoreRepository.insertTaskAndRearrange(
                adapter.listInfo.path,
                adapter.listInfo.id,
                adapter.tasks,
                dragItem.task,
                to
            )
        if (insertResult is Result.Error) {
            _message.value = insertResult.message
        }
    }
}