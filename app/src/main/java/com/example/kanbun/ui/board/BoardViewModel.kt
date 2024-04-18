package com.example.kanbun.ui.board

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.BoardRepository
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.ui.ViewState.BoardViewState
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropTaskItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _board = MutableStateFlow(Board())
    private var _boardLists = MutableStateFlow<List<BoardList>>(emptyList())
    private var _members = MutableStateFlow<List<User>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)
    val boardState: StateFlow<BoardViewState> =
        combine(
            _board,
            _boardLists,
            _members,
            _isLoading,
            _message
        ) { board, boardLists, members, isLoading, message ->
            Log.d(
                TAG,
                "boardState#board: $board,\nboardLists: $boardLists,\n members: $members,\nisLoading: $isLoading,\nmessage: $message"
            )
            BoardViewState(
                board = board,
                lists = boardLists,
                members = members,
                isLoading = isLoading,
                message = message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BoardViewState())

    fun getBoard(boardId: String, workspaceId: String, onSuccess: (Board) -> Unit) {
        viewModelScope.launch {
            boardRepository.getBoardStream(boardId, workspaceId).collectLatest { result ->
                Log.d(TAG, "getBoard1: result: $result")
                when (result) {
                    is Result.Success -> {
                        val board = result.data
                        // should be called only once since board ID should be empty only during the creation of the initial state
                        if (_board.value.id.isEmpty()) {
                            onSuccess(board)
                            getBoardLists(board.id, board.workspace.id)
                        }

                        if (board.members != _board.value.members) {
                            fetchBoardMembers(board.members.map { it.id })
                        }

                        _board.value = board
                    }

                    is Result.Error -> _message.value = result.message
                }
            }
        }
    }

    private fun fetchBoardMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result =
                userRepository.getUsers(memberIds)) {
                is Result.Success -> {
                    Log.d(TAG, "Fetched members: ${result.data}")
                    _members.value = result.data
                }

                is Result.Error -> _message.value = result.message
            }
        }
    }

    private fun getBoardLists(boardId: String, workspaceId: String) {
        viewModelScope.launch {
            val taskListsFlow =
                firestoreRepository.getBoardListsStream(boardId, workspaceId)
            taskListsFlow.collectLatest { result ->
                Log.d(TAG, "getBoardLists: result: $result")
                when (result) {
                    is Result.Success -> {
                        if (_boardLists.value != result.data) {
                            _board.update {
                                it.copy(lists = result.data.map { boardList -> boardList.id })
                            }
                        }
                        _boardLists.value = result.data
                        _isLoading.value = false
                    }

                    is Result.Error -> {
                        _message.value = result.message
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun stopLoading() {
        _isLoading.value = false
    }

    fun createBoardList(listName: String) = viewModelScope.launch {
        val board = _board.value
        firestoreRepository.createBoardList(
            boardList = BoardList(
                name = listName,
                position = _board.value.lists.size.toLong(),
                path = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}" +
                        "/${FirestoreCollection.BOARDS}/${board.id}" +
                        "/${FirestoreCollection.TASK_LISTS}"
            ),
            board = _board.value
        )
    }

    // TODO: Remove if not used
    fun createTask(name: String, boardList: BoardList) = viewModelScope.launch {
        val task = Task(
            position = boardList.tasks.size.toLong(),
            name = name,
            author = when (val result = userRepository.getUser(_board.value.owner)) {
                is Result.Success -> result.data.name!!
                is Result.Error -> {
                    _message.value = result.message
                    return@launch
                }
            }
        )

//        firestoreRepository.createTask(
//            task = task,
//            listId = boardList.id,
//            boardId = _board.value.id,
//            workspaceId = _board.value.settings.workspace.id
//        )
    }

    fun rearrangeTasks(listPath: String, listId: String, tasks: List<Task>, from: Int, to: Int) =
        viewModelScope.launch {
            firestoreRepository.rearrangeTasks(
                listPath = listPath,
                listId = listId,
                tasks = tasks,
                from = from,
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

    fun rearrangeLists(
        boardLists: List<BoardList>,
        from: Int,
        to: Int
    ) = viewModelScope.launch {
        if (from != to && to != -1) {
            // TODO: Move path creation to the repository
            val workspacePath =
                "${FirestoreCollection.WORKSPACES}/${_board.value.workspace.id}"
            val boardPath = "${FirestoreCollection.BOARDS}/${_board.value.id}"
            val listsPath =
                "$workspacePath/$boardPath/${FirestoreCollection.TASK_LISTS}"
            firestoreRepository.rearrangeBoardLists(
                listsPath,
                boardLists,
                from,
                to
            )
        }
    }
}