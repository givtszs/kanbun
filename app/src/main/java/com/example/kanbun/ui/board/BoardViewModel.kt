package com.example.kanbun.ui.board

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.BoardRepository
import com.example.kanbun.domain.repository.TaskListRepository
import com.example.kanbun.domain.repository.TaskRepository
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
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository,
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _board = MutableStateFlow(Board())
    private var _taskLists = MutableStateFlow<List<TaskList>>(emptyList())
    private var _members = MutableStateFlow<List<User>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)
    val boardState: StateFlow<BoardViewState> =
        combine(
            _board,
            _taskLists,
            _members,
            _isLoading,
            _message
        ) { board, taskLists, members, isLoading, message ->
            Log.d(
                TAG,
                "boardState#board: $board,\ntaskLists: $taskLists,\n members: $members,\nisLoading: $isLoading,\nmessage: $message"
            )
            BoardViewState(
                board = board,
                lists = taskLists,
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
                            getTaskLists(board.id, board.workspace.id)
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

    private fun getTaskLists(boardId: String, workspaceId: String) {
        viewModelScope.launch {
            val taskListsFlow =
                taskListRepository.getTaskListStream(boardId, workspaceId)
            taskListsFlow.collectLatest { result ->
                Log.d(TAG, "getTaskLists: result: $result")
                when (result) {
                    is Result.Success -> {
                        if (_taskLists.value != result.data) {
                            _board.update {
                                it.copy(lists = result.data.map { taskList -> taskList.id })
                            }
                        }
                        _taskLists.value = result.data
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

    fun createTaskList(listName: String) = viewModelScope.launch {
        val board = _board.value
        taskListRepository.createTaskList(
            taskList = TaskList(
                name = listName,
                position = _board.value.lists.size.toLong(),
                path = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}" +
                        "/${FirestoreCollection.BOARDS}/${board.id}" +
                        "/${FirestoreCollection.TASK_LISTS}"
            ),
            board = _board.value
        )
    }

    fun rearrangeTasks(listPath: String, listId: String, tasks: List<Task>, from: Int, to: Int) =
        viewModelScope.launch {
            taskRepository.rearrangeTasks(
                taskListPath = listPath,
                taskListId = listId,
                tasks = tasks,
                from = from,
                to = to
            )
        }

    fun deleteAndInsert(
        tasks: List<Task>,
        taskListInfo: TaskListInfo,
        dragItem: DragAndDropTaskItem,
        to: Int
    ) = viewModelScope.launch {
        val tasksRemoveStr = buildString { dragItem.initTasksList.forEach { append("$it, ") } }
        Log.d(
            "ItemTaskViewHolder",
            "Deleting task at position ${dragItem.initPosition} from tasks $tasksRemoveStr"
        )
        val deleteResult = taskRepository.removeTaskAndRearrange(
            dragItem.initTaskList.path,
            dragItem.initTaskList.id,
            dragItem.initTasksList,
            dragItem.initPosition
        )

        if (deleteResult is Result.Error) {
            _message.value = deleteResult.message
            return@launch
        }

        val tasksInsertStr = buildString { tasks.forEach { append("$it, ") } }
        Log.d(
            "ItemTaskViewHolder",
            "Inserting task at position $to into tasks $tasksInsertStr"
        )
        val insertResult =
            taskRepository.insertTaskAndRearrange(
                taskListInfo.path,
                taskListInfo.id,
                tasks,
                dragItem.task,
                to
            )
        if (insertResult is Result.Error) {
            _message.value = insertResult.message
        }
    }

    fun rearrangeLists(
        taskLists: List<TaskList>,
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
            taskListRepository.rearrangeTaskLists(
                listsPath,
                taskLists,
                from,
                to
            )
        }
    }
}