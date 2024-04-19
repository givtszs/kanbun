package com.example.kanbun.domain.repository

import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirebaseFunctionsRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.example.kanbun.isResultError
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryTest {
    // use interface implementation to test support methods (those not included in the interface)
    private lateinit var repository: FirebaseFunctionsRepositoryImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        val firestore = FirestoreTestUtil.firestore
        val dispatcher = UnconfinedTestDispatcher()
        repository = FirebaseFunctionsRepositoryImpl(firestore, dispatcher)
    }

    @After
    fun tearDown() = runBlocking {
        FirestoreTestUtil.deleteFirestoreData()
    }

    @Test
    fun createUser_validUserId_addsUserDataToFirestore() = runBlocking {
        val user = FirestoreTestUtil.createUser("user1")
        val result = repository.createUser(user)

        assertThat(result).isResultSuccess()
    }

    @Test
    fun createUser_emptyUserId_returnsResultError() = runBlocking {
        val user = FirestoreTestUtil.createUser("")
        val result = repository.createUser(user)

        assertThat(result).isResultError()

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getUser_validUserId_getsUserDataFromFirestore() = runBlocking {
        val user = createUser("user1")
        val result = repository.getUser(user.id)

        assertThat(result).isResultSuccess()

        val resultUser = (result as Result.Success).data

        assertThat(resultUser).isEqualTo(user)
    }

    @Test
    fun getUser_emptyUserId_returnsResultError() = runBlocking {
        val userId = ""
        val result = repository.getUser(userId)

        assertThat(result).isResultError()

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getUserStream_validUserId_getsUserChangesOvertime() = runBlocking {
        var user = createUser("user1")
        val data1 = repository.getUserStream(user.id).first()

        assertThat(data1).isEqualTo(user)

        // update user's name
        val newName = "New name"
        user = user.copy(name = newName)
        repository.createUser(user)

        val data2 = repository.getUserStream(user.id).first()

        assertThat(data2).isEqualTo(user)
    }

    @Test
    fun getUserStream_emptyUserId_returnsFlowOfNull() = runBlocking {
        var userFlow = repository.getUserStream("")

        assertThat(userFlow.first()).isNull()

        val user = FirestoreTestUtil.createUser("user")
        repository.createUser(user)
        userFlow = repository.getUserStream(user.id)

        assertThat(userFlow.first()).isEqualTo(user)
    }

    @Test
    fun isTagTaken_returnsTrueIfTheTagTaken() = runBlocking {
        val user = createUser("user1", "test")
        val tag = "test"
        val result = repository.isTagTaken(tag)

        assertThat(result).isResultSuccess()

        val resultData = (result as Result.Success).data

        assertThat(resultData).isTrue()
    }

    @Test
    fun isTagTaken_returnsFalseIfTheTagIsNotTaken() = runBlocking {
        val user = createUser("user1", "test")
        val tag = "test1"
        val result = repository.isTagTaken(tag)

        assertThat(result).isResultSuccess()

        val resultData = (result as Result.Success).data

        assertThat(resultData).isFalse()
    }

    @Test
    fun findUsersByTag_getsListOfUsersMatchingTag() = runBlocking {
        val user1 = createUser("user1", "test08953")
        val user2 = createUser("user2", "user_00002174")
        val user3 = createUser("user3", "johndoe")
        val user4 = createUser("user4", "john_boe")
        val user5 = createUser("user5", "user")
        val user6 = createUser("user6", "userr")
        var tag = "john"
        var result = repository.findUsersByTag(tag)

        assertThat(result).isResultSuccess()
        var resultData = (result as Result.Success).data
        assertThat(resultData.size).isEqualTo(2)
        assertThat(resultData).containsExactlyElementsIn(listOf(user3, user4))

        tag = "user"
        result = repository.findUsersByTag(tag)

        assertThat(result).isResultSuccess()
        resultData = (result as Result.Success).data
        assertThat(resultData.size).isEqualTo(3)
        assertThat(resultData).containsExactlyElementsIn(listOf(user2, user5, user6))

        tag = user1.tag
        result = repository.findUsersByTag(tag)
        assertThat(result).isResultSuccess()
        resultData = (result as Result.Success).data
        assertThat(resultData.size).isEqualTo(1)
        assertThat(resultData.first()).isEqualTo(user1)

        tag = "Alex"
        result = repository.findUsersByTag(tag)
        assertThat(result).isResultSuccess()
        resultData = (result as Result.Success).data
        assertThat(resultData).isEmpty()
    }

    @Test
    fun updateUser_updatesChangedUserFields() = runBlocking {
        val user = createUser("user1", "user_test")
        val newUser = user.copy(
            name = "New Name",
            tag = "new_tag",
            profilePicture = "https://new_prof_pic.jpeg"
        )
        var result = repository.updateUser(user, newUser)

        assertThat(result).isResultSuccess()

        val userUpdate = (repository.getUser(newUser.id) as Result.Success).data

        assertThat(userUpdate).isEqualTo(newUser)
    }

    @Test
    fun createWorkspace_addsWorkspaceToFirestore() = runBlocking {
        val user = createUser("user1")
        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Test")
        val createResult = repository.createWorkspace(workspace)

        assertThat(createResult).isResultSuccess()

        val userUpdate = (repository.getUser(user.id) as Result.Success).data

        assertThat(userUpdate.workspaces).isNotEmpty()
        assertThat(userUpdate.workspaces.first().name).isEqualTo(workspace.name)
    }

    @Test
    fun getWorkspace_validWorkspaceId_returnsWorkspace() = runBlocking {
        val user = createUser("user1")
        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").also {
            repository.createWorkspace(it)
        }

        // get the created workspace id
        val workspaceId = (repository.getUser(user.id) as Result.Success).data
            .workspaces.first { it.name == workspace.name }.id
        workspace = workspace.copy(id = workspaceId)
        val result = repository.getWorkspace(workspaceId)

        assertThat(result).isResultSuccess()

        val resultWorkspace = (result as Result.Success).data

        assertThat(resultWorkspace).isEqualTo(workspace)
    }

    @Test
    fun getWorkspace_emptyWorkspaceId_returnsResultError() = runBlocking {
        val workspaceId = ""
        val result = repository.getWorkspace(workspaceId)

        assertThat(result).isResultError()

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun updateWorkspace_newName_updatesWorkspaceName() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "test")
        val newWorkspace = workspace.copy(name = "New Name")
        val result = repository.updateWorkspace(workspace, newWorkspace)

        assertThat(result).isResultSuccess()

        val resultUpdatedWorkspace = (repository.getWorkspace(workspace.id) as Result.Success).data

        // check the name has been updated
        assertThat(resultUpdatedWorkspace.name).isEqualTo(newWorkspace.name)

        val userUpdate = (repository.getUser(user.id) as Result.Success).data

        // check the workspace name has been updated for its members
        assertThat(userUpdate.workspaces.first { it.id == workspace.id }.name).isEqualTo(
            newWorkspace.name
        )
    }

    @Test
    fun getWorkspaceStream_returnsWorkspaceChangesOvertime() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "Test")
        val workspaceFlow = repository.getWorkspaceStream(workspace.id)

        assertThat(workspaceFlow.first()).isEqualTo(workspace)

        val newWorkspace = workspace.copy(name = "New Name")
        repository.updateWorkspace(workspace, newWorkspace)

        assertThat(workspaceFlow.first()).isEqualTo(newWorkspace)
    }

    @Test
    fun getWorkspaceStream_emptyId_returnsFlowWithNullValue() = runBlocking {
        val workspaceFlow = repository.getWorkspaceStream("")

        assertThat(workspaceFlow.first()).isNull()
    }

    @Test
    fun deleteWorkspace_deletesWorkspace(): Unit = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "test")
        val result = repository.deleteWorkspace(workspace)

        assertThat(result).isResultSuccess()

        val userUpdate = (repository.getUser(user.id) as Result.Success).data

        // check that workspace was deleted in its members
        assertThat(userUpdate.workspaces.any { it.id == workspace.id }).isFalse()
    }

    @Test
    fun createBoard_addsBoardEntryInFirestore() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "workspace1")
        val board = FirestoreTestUtil.createBoard(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        )

        val result = repository.createBoard(board)

        assertThat(result).isResultSuccess()

        val workspaceUpdate = (repository.getWorkspace(workspace.id) as Result.Success).data

        // check created board was added to the hosting workspace
        assertThat(workspaceUpdate.boards).isNotEmpty()
        assertThat(workspaceUpdate.boards.any { it.name == board.name }).isTrue()
    }

    @Test
    fun getBoard_validBoardIdAndWorkspaceId_returnsBoard() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "Workspace")
        var board = FirestoreTestUtil.createBoard(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name),
            "Board"
        ).also {
            repository.createBoard(it)
        }

        // get the created board id
        val boardId = (repository.getWorkspace(workspace.id) as Result.Success).data.boards.first {
            it.name == board.name
        }.boardId
        board = board.copy(id = boardId)
        val result = repository.getBoard(board.id, board.workspace.id)

        assertThat(result).isResultSuccess()

        val resultBoard = (result as Result.Success).data

        assertThat(resultBoard).isEqualTo(board)
    }

    @Test
    fun getBoard_invalidBoardId_returnsResultError() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "Workspace")
        val result = repository.getBoard("", workspace.id)

        assertThat(result).isResultError()
    }

    @Test
    fun getBoard_invalidWorkspaceId_returnsResultError() = runBlocking {
        val result = repository.getBoard("board1", "")

        assertThat(result).isResultError()
    }

    @Test
    fun getBoardStream_returnsBoardDataContinuenly() = runBlocking {
        val board = createBoard("Board")
        val boardFlow = repository.getBoardStream(board.id, board.workspace.id)

        var result = boardFlow.first()
        assertThat(result).isResultSuccess()

        var resultData = (result as Result.Success).data
        assertThat(resultData).isEqualTo(board)

        val newBoard = board.copy(name = "New Name")
        repository.updateBoard(board, newBoard)

        result = boardFlow.first()
        assertThat(result).isResultSuccess()

        resultData = (result as Result.Success).data
        assertThat(resultData).isEqualTo(newBoard)
    }

    @Test
    fun updateBoard_newName_updatesBoardName() = runBlocking {
        val board = createBoard("Board")
        val newBoard = board.copy(name = "New Name")
        val result = repository.updateBoard(board, newBoard)

        assertThat(result).isResultSuccess()

        val boardUpdate =
            (repository.getBoard(newBoard.id, newBoard.workspace.id) as Result.Success).data

        // check the board name has been updated
        assertThat(boardUpdate.name).isEqualTo(newBoard.name)

        val workspaceUpdate = (repository.getWorkspace(board.workspace.id) as Result.Success).data

        // check the board name has been updated in the hosting workspace
        assertThat(workspaceUpdate.boards.any { it.name == newBoard.name }).isTrue()
    }

    @Test
    fun updateBoard_newNameNewDescription_updatesBoardValues() = runBlocking {
        val board = createBoard("Board")
        val newBoard = board.copy(name = "New Name", description = "New Description")
        val result = repository.updateBoard(board, newBoard)

        assertThat(result).isResultSuccess()

        val boardUpdate =
            (repository.getBoard(newBoard.id, newBoard.workspace.id) as Result.Success).data

        // check the board name and description were updated
        assertThat(boardUpdate).isEqualTo(newBoard)

        val workspaceUpdate = (repository.getWorkspace(board.workspace.id) as Result.Success).data

        assertThat(workspaceUpdate.boards.any { it.name == newBoard.name }).isTrue()
    }

    @Test
    fun deleteBoard_deletesBoard() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "Workspace")
        val board = createBoard(user.id, workspace.id, workspace.name, "Board")
        val result = repository.deleteBoard(board)

        assertThat(result).isResultSuccess()

        val workspaceUpdate = (repository.getWorkspace(board.workspace.id) as Result.Success).data

        // check the board has been deleted from the hosting workspace
        assertThat(workspaceUpdate.boards.any { it.boardId == board.id }).isFalse()
    }

    @Test
    fun createBoardList_addsBoardListToFirestore() = runBlocking {
        val board = createBoard("Board")
        val boardList = FirestoreTestUtil.createBoardList("List 1", 0)
        val resultCreate = repository.createBoardList(boardList, board)

        assertThat(resultCreate).isResultSuccess()
    }

    @Test
    fun getBoardList_validIdAndPath_getsBoardList() = runBlocking {
        val board = createBoard("Board")
        val boardList = FirestoreTestUtil.createBoardList("List 1", 0).run {
            repository.createBoardList(this, board)
            val boardListId =
                (repository.getBoard(board.id, board.workspace.id) as Result.Success).data
                    .lists.first()
            val boardListPath =
                "${FirestoreCollection.WORKSPACES}/${board.workspace.id}/${FirestoreCollection.BOARDS}/${board.id}/${FirestoreCollection.TASK_LISTS}"
            copy(id = boardListId, path = boardListPath)
        }
        val result = repository.getBoardList(boardList.path, boardList.id)

        assertThat(result).isResultSuccess()

        val resultList = (result as Result.Success).data

        assertThat(resultList).isEqualTo(boardList)
    }

    @Test
    fun getBoardList_invalidIdValidPath_returnsResultError() = runBlocking {
        val board = createBoard("Board")
        val boardList = FirestoreTestUtil.createBoardList("List 1", 0).run {
            repository.createBoardList(this, board)
            val boardListId = ""
            val boardListPath =
                "${FirestoreCollection.WORKSPACES}/${board.workspace.id}/${FirestoreCollection.BOARDS}/${board.id}/${FirestoreCollection.TASK_LISTS}"
            copy(id = boardListId, path = boardListPath)
        }
        val result = repository.getBoardList(boardList.path, boardList.id)

        assertThat(result).isResultError()
    }

    @Test
    fun getBoardList_validIdInvalidPath_returnsResultError() = runBlocking {
        val board = createBoard("Board")
        val boardList = FirestoreTestUtil.createBoardList("List 1", 0).run {
            repository.createBoardList(this, board)
            val boardListId =
                (repository.getBoard(board.id, board.workspace.id) as Result.Success).data
                    .lists.first()
            val boardListPath = ""
            copy(id = boardListId, path = boardListPath)
        }
        val result = repository.getBoardList(boardList.path, boardList.id)

        assertThat(result).isResultError()
    }

    @Test
    fun getBoardList_invalidIdInvalidPath_returnsResultError() = runBlocking {
        val board = createBoard("Board")
        val result = repository.getBoardList("", "")

        assertThat(result).isResultError()
    }


    @Test
    fun updateBoardListName_validName_updatesBoardListName() = runBlocking {
        val board = createBoard("Board")
        val boardList = createBoardList("list1", 0, board)
        val newName = "Updated board list name"
        val result = repository.updateBoardListName(newName, boardList.path, boardList.id)

        assertThat(result).isResultSuccess()

        val updBoardList =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data

        assertThat(updBoardList.name).isEqualTo(newName)
    }

    @Test
    fun deleteBoardLists_deletesBoardListAndRearrangesOther() = runBlocking {
        val board = createBoard("board1")
        val boardList1 = createBoardList("list1", 0, board)
        val boardList2 = createBoardList("list2", 1, board)
        val boardList3 = createBoardList("list3", 2, board)

        val result = repository.deleteBoardListAndRearrange(
            id = boardList1.id,
            path = boardList1.path,
            boardLists = listOf(boardList1, boardList2, boardList3),
            deleteAt = boardList1.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardList2Update =
            (repository.getBoardList(boardList2.path, boardList2.id) as Result.Success).data
        val boardList3Update =
            (repository.getBoardList(boardList3.path, boardList3.id) as Result.Success).data

        assertThat(boardList2Update.position).isEqualTo(boardList2.position.dec())
        assertThat(boardList3Update.position).isEqualTo(boardList3.position.dec())
    }

    @Test
    fun getBoardListStream_returnsBoardsListDataChangesOverTime() = runBlocking {
        val board = createBoard("Board")
        val boardList = createBoardList("List 1", 0, board)
        val boardListFlow = repository.getBoardListsStream(board.id, board.workspace.id)
        var result = boardListFlow.first()

        assertThat(result).isResultSuccess()

        val newName = "New Name"
        repository.updateBoardListName(newName, boardList.path, boardList.id)
        result = boardListFlow.first()

        assertThat(result).isResultSuccess()

        val boardListsUpdate = (result as Result.Success).data
        assertThat(boardListsUpdate.any { it.name == newName }).isTrue()
    }

    @Test
    fun rearrangeBoardLists_fromLessThanTo_updatesBoardListsPositions() = runBlocking {
        val boardList1 = createBoardList("list1", 0)
        val boardList2 = createBoardList("list2", 1)
        val boardList3 = createBoardList("list3", 2)
        val path = boardList1.path
        val result = repository.rearrangeBoardLists(
            boardListPath = path,
            boardLists = listOf(boardList1, boardList2, boardList3),
            from = boardList1.position.toInt(),
            to = boardList3.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardList1Update = (repository.getBoardList(path, boardList1.id) as Result.Success).data
        val boardList2Update = (repository.getBoardList(path, boardList2.id) as Result.Success).data
        val boardList3Update = (repository.getBoardList(path, boardList3.id) as Result.Success).data

        assertThat(boardList1Update.position).isEqualTo(boardList3.position)
        assertThat(boardList2Update.position).isEqualTo(boardList1.position)
        assertThat(boardList3Update.position).isEqualTo(boardList2.position)
    }

    @Test
    fun rearrangeBoardLists_fromGreaterThanTo_updatesBoardListsPositions() = runBlocking {
        val boardList1 = createBoardList("list1", 0)
        val boardList2 = createBoardList("list2", 1)
        val boardList3 = createBoardList("list3", 2)
        val path = boardList1.path
        val result = repository.rearrangeBoardLists(
            boardListPath = path,
            boardLists = listOf(boardList1, boardList2, boardList3),
            from = boardList3.position.toInt(),
            to = boardList1.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardList1Update = (repository.getBoardList(path, boardList1.id) as Result.Success).data
        val boardList2Update = (repository.getBoardList(path, boardList2.id) as Result.Success).data
        val boardList3Update = (repository.getBoardList(path, boardList3.id) as Result.Success).data

        assertThat(boardList1Update.position).isEqualTo(boardList2.position)
        assertThat(boardList2Update.position).isEqualTo(boardList3.position)
        assertThat(boardList3Update.position).isEqualTo(boardList1.position)
    }

    @Test
    fun createTask_taskObj_addsTaskInFirestore() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = FirestoreTestUtil.createTask("task1", 0)
        val resultCreate = repository.createTask(task, boardList.id, boardList.path)

        assertThat(resultCreate).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data

        assertThat(boardListUpdate.tasks.first().name).isEqualTo(task.name)
        assertThat(boardListUpdate.tasks.first().position).isEqualTo(task.position)
        assertThat(boardListUpdate.tasks.first().description).isEqualTo(task.description)
        assertThat(boardListUpdate.tasks.first().tags).isEqualTo(task.tags)
    }

    @Test
    fun deleteTask_validArgs_deletesTaskFromTheList() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = createTask("task1", 0, boardList)
        val result = repository.deleteTask(task, boardList.path, boardList.id)

        assertThat(result).isResultSuccess()

        val boardListUpd =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data

        assertThat(boardListUpd.tasks.any { it.position == task.position }).isFalse()
    }

    @Test
    fun deleteTask_invalidBoardListId_returnsResultError() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = createTask("task1", 0, boardList)
        val result = repository.deleteTask(task, boardList.path, "")

        assertThat(result).isResultError()
    }

    @Test
    fun deleteTask_invalidBoardListPath_returnsResultError() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = createTask("task1", 0, boardList)
        val result = repository.deleteTask(task, "", boardList.id)

        assertThat(result).isResultError()
    }

    @Test
    fun deleteTask_invalidBoardListPathInvalidBoardListId_returnsResultError() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = createTask("task1", 0, boardList)
        val result = repository.deleteTask(task, "", "")

        assertThat(result).isResultError()
    }

    @Test
    fun rearrangeTaskPositions_fromLessThanTo_updatesTasksPositions() = runBlocking {
        val boardList = createBoardList("List 1", 0)
        val task1 = createTask("Task 1", 0, boardList)
        val task2 = createTask("Task 2", 1, boardList)
        val task3 = createTask("Task 3", 2, boardList)
        val result = repository.rearrangeTasks(
            listPath = boardList.path,
            listId = boardList.id,
            tasks = listOf(task1, task2, task3),
            from = task1.position.toInt(),
            to = task3.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data
        val task1Update = boardListUpdate.tasks.first { it.id == task1.id }
        val task2Update = boardListUpdate.tasks.first { it.id == task2.id }
        val task3Update = boardListUpdate.tasks.first { it.id == task3.id }

        assertThat(task1Update.position).isEqualTo(task3.position)
        assertThat(task2Update.position).isEqualTo(task1.position)
        assertThat(task3Update.position).isEqualTo(task2.position)
    }

    @Test
    fun rearrangeTaskPositions_fromGreaterThanTo_updatesTasksPositions() = runBlocking {
        val boardList = createBoardList("List 1", 0)
        val task1 = createTask("Task 1", 0, boardList)
        val task2 = createTask("Task 2", 1, boardList)
        val task3 = createTask("Task 3", 2, boardList)
        val result = repository.rearrangeTasks(
            listPath = boardList.path,
            listId = boardList.id,
            tasks = listOf(task1, task2, task3),
            from = task3.position.toInt(),
            to = task1.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data
        val task1Update = boardListUpdate.tasks.first { it.id == task1.id }
        val task2Update = boardListUpdate.tasks.first { it.id == task2.id }
        val task3Update = boardListUpdate.tasks.first { it.id == task3.id }

        assertThat(task1Update.position).isEqualTo(task2.position)
        assertThat(task2Update.position).isEqualTo(task3.position)
        assertThat(task3Update.position).isEqualTo(task1.position)
    }

    @Test
    fun deleteTaskAndRearrange_deletesTaskAndRearrangesOther() = runBlocking {
        val boardList = createBoardList("List 1", 0)
        val task1 = createTask("Task 1", 0, boardList)
        val task2 = createTask("Task 2", 1, boardList)
        val task3 = createTask("Task 3", 2, boardList)
        val result = repository.deleteTaskAndRearrange(
            listPath = boardList.path,
            listId = boardList.id,
            tasks = listOf(task1, task2, task3),
            from = task1.position.toInt(),
        )

        assertThat(result).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data

        assertThat(boardListUpdate.tasks.any { it.id == task1.id }).isFalse()

        val task2Update = boardListUpdate.tasks.first { it.id == task2.id }
        val task3Update = boardListUpdate.tasks.first { it.id == task3.id }

        assertThat(task2Update.position).isEqualTo(task1.position)
        assertThat(task3Update.position).isEqualTo(task2.position)
    }

    @Test
    fun insertTaskAndRearrange_toInsertTaskInAList() = runBlocking {
        val boardList1 = createBoardList("List 1", 0)
        val task1 = createTask("Task 1", 0, boardList1)
        val boardList2 = createBoardList("List 2", 1)
        val task2 = createTask("Task 2", 0, boardList2)
        val task3 = createTask("Task 3", 1, boardList2)
        val result = repository.insertTaskAndRearrange(
            listPath = boardList2.path,
            listId = boardList2.id,
            tasks = listOf(task2, task3),
            task = task1,
            to = task3.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val boardList2Update =
            (repository.getBoardList(boardList2.path, boardList2.id) as Result.Success).data

        val task1Update = boardList2Update.tasks.first { it.id == task1.id }
        val task2Update = boardList2Update.tasks.first { it.id == task2.id }
        val task3Update = boardList2Update.tasks.first { it.id == task3.id }

        assertThat(task1Update.position).isEqualTo(task3.position)
        assertThat(task2Update.position).isEqualTo(task2.position)
        assertThat(task3Update.position).isEqualTo(task3.position.inc())

    }

    @Test
    fun updateTask_newName_updatesTaskName() = runBlocking {
        val boardList = createBoardList("List 1", 0)
        val task = createTask("Task", 0, boardList)
        val newTask = task.copy(name = "New name")
        val result = repository.updateTask(
            oldTask = task,
            newTask = newTask,
            boardListId = boardList.id,
            boardListPath = boardList.path
        )

        assertThat(result).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data
        val taskUpdate = boardListUpdate.tasks.first { it.id == task.id }

        assertThat(taskUpdate.name).isEqualTo(newTask.name)
    }

    @Test
    fun updateTask_newNameNewDescription_updatesTaskNameAndDescription() = runBlocking {
        val boardList = createBoardList("List 1", 0)
        val task = createTask("Task", 0, boardList)
        val newTask = task.copy(name = "New name", description = "New Description")
        val result = repository.updateTask(
            oldTask = task,
            newTask = newTask,
            boardListId = boardList.id,
            boardListPath = boardList.path
        )

        assertThat(result).isResultSuccess()

        val boardListUpdate =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data
        val taskUpdate = boardListUpdate.tasks.first { it.id == task.id }

        assertThat(taskUpdate.name).isEqualTo(newTask.name)
        assertThat(taskUpdate.description).isEqualTo(newTask.description)
    }


    @Test
    fun upsertTag_createsNewTag() = runBlocking {
        val board = createBoard("Board")
        val tag = FirestoreTestUtil.createTag("tag1", "#000000")
        val result = repository.upsertTag(
            tag,
            board.id,
            boardPath = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}" +
                    "/${FirestoreCollection.BOARDS}"
        )

        assertThat(result).isResultSuccess()

        val boardUpdate = (repository.getBoard(board.id, board.workspace.id) as Result.Success).data

        assertThat(boardUpdate.tags).isNotEmpty()
        assertThat(boardUpdate.tags.any { it.name == tag.name }).isTrue()
    }

    @Test
    fun upsertTag_newNameNewColor_updatesExistingTag() = runBlocking {
        val board = createBoard("Board")
        val tag = createTag("Tag 1", "#000000", board)
        val newTag = tag.copy(name = "New Tag", color = "#FFFFFFF")
        val result = repository.upsertTag(
            tag = newTag,
            boardId = board.id,
            boardPath = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}" +
                    "/${FirestoreCollection.BOARDS}"
        )

        assertThat(result).isResultSuccess()

        val boardUpdate = (repository.getBoard(board.id, board.workspace.id) as Result.Success).data
        val tagUpdate = boardUpdate.tags.first { it.id == tag.id }

        assertThat(tagUpdate.name).isEqualTo(newTag.name)
        assertThat(tagUpdate.color).isEqualTo(newTag.color)
    }

    @Test
    fun getTags_getsListOfAllTagsInBoard() = runBlocking {
        val board = createBoard("board1")
        val tag1 = createTag("Tag1", "#FF0000", board)
        val tag2 = createTag("Tag2", "#FFA500", board)
        val result = repository.getAllTags(board.id, board.workspace.id)

        assertThat(result).isResultSuccess()

        val resTags = (result as Result.Success).data

        assertThat(resTags).isNotEmpty()
        assertThat(resTags).isEqualTo(listOf(tag1, tag2).sortedBy { it.name })
    }

    @Test
    fun getTaskTags_getsTagsForTheTask() = runBlocking {
        val board = createBoard("board1")
        val boardList = createBoardList("list1", 0, board)
        val task = createTask("task1", 0, boardList)
        val tag1 = createTag("Tag 1", "#FFFFFF", board)
        val tag2 = createTag("Tag 2", "#000000", board)

        // add tags to the task
        val newTask = task.copy(tags = listOf(tag1.id, tag2.id))
        repository.updateTask(task, newTask, boardList.id, boardList.path)
        val result = repository.getTaskTags(newTask, boardList.id, boardList.path)

        assertThat(result).isResultSuccess()

        val resultTags = (result as Result.Success).data

        assertThat(resultTags).isNotEmpty()
        assertThat(resultTags.map { it.id }).isEqualTo(newTask.tags)
    }

    @Test
    fun getTaskTags_getsOnlyRelevantTaskTags() = runBlocking {
        val board = createBoard("board1")
        val boardList = createBoardList("list1", 0, board)
        val task = createTask("task1", 0, boardList)
        val tag1 = createTag("Tag 1", "#FFFFFF", board)
        val tag2 = createTag("Tag 2", "#000000", board)

        // add tags to the task
        val newTask = task.copy(tags = listOf(tag1.id, tag2.id))
        repository.updateTask(task, newTask, boardList.id, boardList.path)

        // delete `tag2` from the board
        val newBoard = board.copy(tags = listOf(tag1))
        repository.updateBoard(board, newBoard)

        // get task tags
        val result = repository.getTaskTags(newTask, boardList.id, boardList.path)

        assertThat(result).isResultSuccess()

        val resultTags = (result as Result.Success).data

        assertThat(resultTags).isEqualTo(newBoard.tags)
    }
//
//    @Test
//    fun updateTaskTags_updatesTaskTagIdsList() = runBlocking {
//        val board = createBoard("board1")
//        val boardList = createBoardList("boardList1", 0, board).second
//        val task = createTask("task1", 0, boardList)
//        val tag1 = createTag("Tag1", FirestoreTestUtil.black, board.id, board.workspace.id)
//        val tag2 = createTag("Tag2", FirestoreTestUtil.white, board.id, board.workspace.id)
//
//        val taskTags = listOf(tag1.id, tag2.id)
//        // add a tag to the task
//        val result = repository.updateTaskTags(task.id, taskTags, boardList.id, boardList.path)
//
//        assertThat(result).isResultSuccess()
//    }

    private suspend fun createUser(userId: String, userTag: String = ""): User {
        return FirestoreTestUtil.createUser(userId, userTag).run {
            repository.createUser(this)
            this
        }
    }

    private suspend fun createWorkspace(userId: String, name: String): Workspace {
        return FirestoreTestUtil.createWorkspace(userId, name).run {
            repository.createWorkspace(this)
            val user = (repository.getUser(userId) as Result.Success).data
            val workspace = (repository.getWorkspace(
                user.workspaces.first { workspace -> workspace.name == name }.id
            ) as Result.Success).data
            workspace
        }
    }

    private suspend fun createBoard(
        userId: String,
        workspaceId: String,
        workspaceName: String,
        name: String
    ): Board {
        return FirestoreTestUtil.createBoard(
            userId,
            WorkspaceInfo(workspaceId, workspaceName),
            name
        ).run {
            repository.createBoard(this)
            val workspace = (repository.getWorkspace(workspaceId) as Result.Success).data
            val board = (repository.getBoard(
                boardId = workspace.boards.first { board -> board.name == name }.boardId,
                workspaceId = workspace.id
            ) as Result.Success).data
            board
        }
    }

    private suspend fun createBoard(name: String): Board {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "workspace")
        return createBoard(user.id, workspace.id, workspace.name, name)
    }

    private suspend fun createBoardList(
        name: String,
        position: Int,
        board: Board
    ): TaskList {
        return FirestoreTestUtil.createBoardList(name, position).run {
            repository.createBoardList(this, board)
            val boardUpd =
                (repository.getBoard(board.id, board.workspace.id) as Result.Success).data
            (repository.getBoardList(
                boardListId = boardUpd.lists.first(),
                boardListPath = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}/${FirestoreCollection.BOARDS}/${board.id}/${FirestoreCollection.TASK_LISTS}"
            ) as Result.Success).data
        }
    }

    private suspend fun createBoardList(name: String, position: Int): TaskList {
        val board = createBoard("board1")
        return createBoardList(name, position, board)
    }

    private suspend fun createTask(name: String, position: Long, taskList: TaskList): Task {
        return FirestoreTestUtil.createTask(name, position).run {
            repository.createTask(this, taskList.id, taskList.path)
            val boardListUpd =
                (repository.getBoardList(taskList.path, taskList.id) as Result.Success).data
            boardListUpd.tasks.first { it.position == position }
        }
    }

    private suspend fun createTag(
        name: String,
        color: String,
        board: Board
    ): Tag {
        val boardPath = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}" +
                "/${FirestoreCollection.BOARDS}"
        return FirestoreTestUtil.createTag(name, color).run {
            (repository.upsertTag(this, board.id, boardPath) as Result.Success).data
        }
    }
}