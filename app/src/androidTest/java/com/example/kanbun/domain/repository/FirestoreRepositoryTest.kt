package com.example.kanbun.domain.repository

import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.defaultTagColors
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryTest {
    // use interface implementation to test support methods (those not included in the interface)
    private lateinit var repository: FirestoreRepositoryImpl

    @Before
    fun setUp() {
        val firestore = FirestoreTestUtil.firestore
        repository = FirestoreRepositoryImpl(firestore)
    }

    @After
    fun tearDown() = runBlocking {
//        FirestoreTestUtil.deleteFirestoreData()
    }

    private fun Subject.isResultSuccess() = isInstanceOf(Result.Success::class.java)

    @Test
    fun createUser_withValidUserId_savesUserDataIntoFirestore() = runBlocking {
        val user = FirestoreTestUtil.userSample
        val result = repository.createUser(user)

        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun createUser_withEmptyUserId_returnsResultError() = runBlocking {
        val user = FirestoreTestUtil.createUser("")
        val result = repository.createUser(user)

        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getUser_getsUserDataFromFirestore() = runBlocking {
        val user = FirestoreTestUtil.userSample
        repository.createUser(user)

        val result = repository.getUser(user.id)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultUser = (result as Result.Success).data

        assertThat(resultUser).isEqualTo(user)
    }

    @Test
    fun getUser_withEmptyUserId_returnsResultError() = runBlocking {
        val userId = ""
        val result = repository.getUser(userId)

        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getUserStream_withValidUserId_getsUserChangesOvertime() = runBlocking {
        var user = FirestoreTestUtil.createUser("user1")
        repository.createUser(user)

        val data1 = repository.getUserStream(user.id).first()

        assertThat(data1).isEqualTo(user)

        val newName = "New name"
        user = user.copy(name = newName)
        repository.createUser(user)

        val data2 = repository.getUserStream(user.id).first()

        assertThat(data2).isEqualTo(user)
    }

    @Test
    fun getUserStream_withEmptyUserId_returnsFlowOfNull() = runBlocking {
        var userFlow = repository.getUserStream("userId")

        assertThat(userFlow.first()).isNull()

        val user = FirestoreTestUtil.createUser("user")
        repository.createUser(user)
        userFlow = repository.getUserStream(user.id)

        assertThat(userFlow.first()).isEqualTo(user)
    }

    @Test
    fun createWorkspace_addsWorkspaceEntryInFirestore() = runBlocking {
        val user = FirestoreTestUtil.createUser("user")
        repository.createUser(user)

        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Test")
        val createResult = repository.createWorkspace(workspace)

        assertThat(createResult).isInstanceOf(Result.Success::class.java)

        val resultWorkspaceId = (createResult as Result.Success).data

        assertThat(resultWorkspaceId).isNotEmpty()

        val getUserResult = repository.getUser(user.id)

        assertThat(getUserResult).isInstanceOf(Result.Success::class.java)

        val resultUser = (getUserResult as Result.Success).data

        assertThat(resultUser.workspaces).isNotEmpty()
        assertThat(resultUser.workspaces.first().name).isEqualTo(workspace.name)
        assertThat(resultUser.workspaces.first().id).isEqualTo(resultWorkspaceId)
    }

    @Test
    fun getWorkspace_withValidWorkspaceId_returnsWorkspace() = runBlocking {
        val user = FirestoreTestUtil.createUser("user1")
        repository.createUser(user)
        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        val workspaceId = (repository.createWorkspace(workspace) as Result.Success).data

        val result = repository.getWorkspace(workspaceId)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultWorkspace = (result as Result.Success).data

        assertThat(resultWorkspace).isEqualTo(workspace.copy(id = workspaceId))
    }

    @Test
    fun getWorkspace_withEmptyWorkspaceId_returnsResultError() = runBlocking {
        val workspaceId = ""
        val result = repository.getWorkspace(workspaceId)

        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getWorkspaceStream_returnsWorkspaceChangesOvertime() = runBlocking {
        val user = FirestoreTestUtil.createUser("user1")
        repository.createUser(user)

        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        workspace = (repository.createWorkspace(workspace) as Result.Success).data.run {
            workspace.copy(id = this)
        }

        val workspaceFlow = repository.getWorkspaceStream(workspace.id)

        assertThat(workspaceFlow.first()).isEqualTo(workspace)

        val newName = "New Workspace Name"
        repository.updateWorkspaceName(workspace, newName)
        workspace = workspace.copy(name = newName)

        assertThat(workspaceFlow.first()).isEqualTo(workspace)

        repository.deleteWorkspace(workspace)

        assertThat(workspaceFlow.first()).isNull()
    }

    @Test
    fun getWorkspaceStream_withEmptyId_returnsFlowOfNull() = runBlocking {
        var workspaceFlow = repository.getWorkspaceStream("")

        assertThat(workspaceFlow.first()).isNull()

        val user = FirestoreTestUtil.createUser("user")
        repository.createUser(user)
        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").run {
            val id = (repository.createWorkspace(this) as Result.Success).data
            this.copy(id = id)
        }

        workspaceFlow = repository.getWorkspaceStream(workspace.id)

        assertThat(workspaceFlow.first()).isEqualTo(workspace)
    }

    @Test
    fun updateWorkspaceName_updatesWorkspaceName() = runBlocking {
        val user = FirestoreTestUtil.userSample
        repository.createUser(user)
        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        workspace = (repository.createWorkspace(workspace) as Result.Success).data.run {
            workspace.copy(id = this)
        }

        val newName = "New Name"
        val result = repository.updateWorkspaceName(workspace, newName)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultUpdatedWorkspace = (repository.getWorkspace(workspace.id) as Result.Success).data

        assertThat(resultUpdatedWorkspace.name).isEqualTo(newName)

        val users = resultUpdatedWorkspace.members.map { member ->
            (repository.getUser(member.id) as Result.Success).data
        }

        users.forEach { _user ->
            assertThat(_user.workspaces.first { it.id == workspace.id }.name).isEqualTo(newName)
        }
    }

    @Test
    fun inviteToWorkspace_addsUserIntoWorkspaceMembers_addsWorkspaceIntoUserWorkspaces() =
        runBlocking {
            val user1 = FirestoreTestUtil.createUser("user1")
            repository.createUser(user1)

            val user2 = FirestoreTestUtil.createUser("user2")
            repository.createUser(user2)

            var workspace = FirestoreTestUtil.createWorkspace(user1.id, "Test")
            workspace =
                (repository.createWorkspace(workspace) as Result.Success).data.run {
                    workspace.copy(id = this)
                }

            val result = repository.inviteToWorkspace(workspace, user2)

            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

    @Test
    fun deleteWorkspace_deletesWorkspace(): Unit = runBlocking {
        val user1 = FirestoreTestUtil.createUser("user1")
        repository.createUser(user1)

        val user2 = FirestoreTestUtil.createUser("user2")
        repository.createUser(user2)

        var workspace = FirestoreTestUtil.createWorkspace(user1.id, "Test")
        workspace = (repository.createWorkspace(workspace) as Result.Success).data.run {
            workspace.copy(id = this)
        }
        repository.inviteToWorkspace(workspace, user2)
        workspace = (repository.getWorkspace(workspace.id) as Result.Success).data

        val result = repository.deleteWorkspace(workspace)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultUpdatedUser1 = (repository.getUser(user1.id) as Result.Success).data

        assertThat(resultUpdatedUser1.workspaces.any { it.id == workspace.id }).isFalse()

        val resultUpdatedUser2 = (repository.getUser(user2.id) as Result.Success).data

        assertThat(resultUpdatedUser2.workspaces.any { it.id == workspace.id }).isFalse()
    }

    @Test
    fun createBoard_addsBoardEntryInFirestore() = runBlocking {
        val user = createUser("user1")
        var workspace = createWorkspace(user.id, "workspace1")

        var board = FirestoreTestUtil.createBoard(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        )

        val result = repository.createBoard(board)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultId = (result as Result.Success).data
        board = board.copy(id = resultId)
        workspace = (repository.getWorkspace(workspace.id) as Result.Success).data

        assertThat(workspace.boards).isNotEmpty()
        assertThat(workspace.boards.any { it.boardId == board.id }).isTrue()
    }

    @Test
    fun getBoard_withValidArgs_returnsBoard() = runBlocking {
        val board = createBoard("board1")
        val result = repository.getBoard(board.id, board.workspace.id)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultBoard = (result as Result.Success).data

        assertThat(resultBoard).isEqualTo(board)
    }

    @Test
    fun updateBoard_updatesBoardValues() = runBlocking {
        val board = createBoard("board1")
        val newBoard =
            FirestoreTestUtil.createBoard(board.owner, board.workspace, "Updated name").run {
                copy(id = board.id)
            }
        val result = repository.updateBoard(newBoard)

        assertThat(result).isResultSuccess()

        val updBoard =
            (repository.getBoard(newBoard.id, newBoard.workspace.id) as Result.Success).data

        assertThat(updBoard).isEqualTo(newBoard)
    }

    @Test
    fun deleteBoard_deletesBoard() = runBlocking {
        val board = createBoard("board1")
        val result = repository.deleteBoard(board)

        assertThat(result).isResultSuccess()

        val updWorkspace = (repository.getWorkspace(board.workspace.id) as Result.Success).data

        assertThat(updWorkspace.boards.any { it.boardId == board.id }).isFalse()
    }

    @Test
    fun createBoardList_createsDocumentInTheListsSubcollection() = runBlocking {
        val user = FirestoreTestUtil.createUser("user").also {
            repository.createUser(it)
        }

        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").run {
            this.copy(
                id = (repository.createWorkspace(this) as Result.Success).data
            )
        }

        var board = FirestoreTestUtil.createBoard(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        ).run {
            this.copy(
                id = (repository.createBoard(this) as Result.Success).data
            )
        }

        var boardList = FirestoreTestUtil.createBoardList("List 1", 0)

        val resultCreate = repository.createBoardList(boardList, board)

        assertThat(resultCreate).isInstanceOf(Result.Success::class.java)

        val resultId = (resultCreate as Result.Success).data
        boardList = boardList.copy(id = resultId)

        assertThat(boardList.id).isEqualTo(resultId)
    }

    @Test
    fun getBoardList_getsBoardList() = runBlocking {
        val boardList = createBoardList("list1", 0)

        val result = repository.getBoardList(boardList.path, boardList.id)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultList = (result as Result.Success).data

        assertThat(resultList).isEqualTo(boardList)
    }

    @Test
    fun updateBoardListName_changesBoardListName() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val newName = "Updated board list name"

        val result = repository.updateBoardListName(newName, boardList.path, boardList.id)

        assertThat(result).isResultSuccess()

        val updBoardList =
            (repository.getBoardList(boardList.path, boardList.id) as Result.Success).data

        assertThat(updBoardList.name).isEqualTo(newName)
    }

    @Test
    fun deleteBoardLists_deletesBoardListAndRearrangesOthers() = runBlocking {
        var board = createBoard("board1")
        val boardList1 = createBoardList("list1", 0, board).run {
            board = first
            second
        }
        val boardList2 = createBoardList("list2", 1, board).run {
            board = first
            second
        }
        val boardList3 = createBoardList("list3", 2, board).run {
            board = first
            second
        }

        val toDelete = boardList2

        val result: Result<Unit> = repository.deleteBoardListAndRearrange(
            toDelete.id,
            toDelete.path,
            listOf(boardList1, boardList2, boardList3),
            toDelete.position.toInt()
        )

        assertThat(result).isResultSuccess()

        val updBoardList =
            (repository.getBoardList(boardList3.path, boardList3.id) as Result.Success).data

        assertThat(updBoardList.position).isEqualTo(boardList3.position.dec())
    }

    @Test
    fun getBoardListStream_returnsBoardsListDataChangesOverTime() = runBlocking {
        val user = FirestoreTestUtil.createUser("user").also {
            repository.createUser(it)
        }

        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").run {
            this.copy(
                id = (repository.createWorkspace(this) as Result.Success).data
            )
        }

        val board = FirestoreTestUtil.createBoard(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        ).run {
            this.copy(
                id = (repository.createBoard(this) as Result.Success).data
            )
        }

        val boardList = FirestoreTestUtil.createBoardList("List 1", 0).run {
            val listId = (repository.createBoardList(this, board) as Result.Success).data
            this.copy(id = listId)
        }

        val boardListFlow = repository.getBoardListsStream(board.id, workspace.id).take(2)
        val results = mutableListOf<Result<List<BoardList>>>()

        boardListFlow.collect {
            results.add(it)
        }

        assertThat(results.first()).isInstanceOf(Result.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Result.Success::class.java)

        val resultData = (results[1] as Result.Success).data.first()

        assertThat(resultData).isEqualTo(boardList)
    }

    @Test
    fun createTask_withValidArguments_addsTaskInFirestore() = runBlocking {
        var board = createBoard("board1")
        val boardList = createBoardList("list1", 0, board).run {
            board = first
            second
        }
        var task = FirestoreTestUtil.createTask("task1", 0)

        val resultCreate = repository.createTask(task, boardList.id, boardList.path)

        assertThat(resultCreate).isInstanceOf(Result.Success::class.java)

        task = task.copy(
            id = (resultCreate as Result.Success).data
        )

        val boardListFlow = repository.getBoardListsStream(board.id, board.workspace.id)
            .take(2) // take 2 because first result is Result.Loading
        val listResults = mutableListOf<Result<List<BoardList>>>().apply {
            boardListFlow.collectLatest { this.add(it) }
        }
        val boardListData = (listResults[1] as Result.Success).data.first()

        assertThat(boardListData.tasks.any { it.id == task.id }).isTrue()

        val taskData = boardListData.tasks.first { it.id == task.id }

        assertThat(taskData).isEqualTo(task)
    }

    @Test
    fun deleteTask_deletesTaskFromTheList() = runBlocking {
        val boardList = createBoardList("list1", 0)
        val task = createTask("task1", 0, boardList)

        val result = repository.deleteTask(task, boardList.path, boardList.id)

        assertThat(result).isResultSuccess()
    }

//    @Test
//    fun rearrangeTaskPositions_toMoveTaskInAList() = runBlocking {
//        val taskArgs = createTaskArgs()
//
//        val tasks = mutableListOf<Task>()
//        repeat(4) { i ->
//            val task = FirestoreTestUtil.createTask("Task $i", i.toLong()).run {
//                this.copy(
//                    id = (repository.createTask(this, taskArgs["boardListId"]!!, taskArgs["boardId"]!!, taskArgs["workspaceId"]!!) as Result.Success).data
//                )
//            }
//
//            tasks.add(task)
//        }
//
//        val from = 0
//        val to = 2
//
//        val boardListPath = "${FirestoreCollection.WORKSPACES.collectionName}/${taskArgs["workspaceId"]!!}/" +
//                "${FirestoreCollection.BOARDS.collectionName}/${taskArgs["boardId"]!!}/" +
//                FirestoreCollection.BOARD_LIST.collectionName
//        val result = repository.rearrangeTasksPositions(boardListPath, taskArgs["boardListId"]!!, tasks, from, to)
//
//        assertThat(result).isInstanceOf(Result.Success::class.java)
//    }

//    @Test
//    fun deleteTaskAndRearrange_toRemoveTaskInAList() = runBlocking {
//        val taskArgs = createTaskArgs()
//
//        val tasks = mutableListOf<Task>()
//        repeat(4) { i ->
//            val task = FirestoreTestUtil.createTask("Task $i", i.toLong()).run {
//                this.copy(
//                    id = (repository.createTask(this, taskArgs["boardListId"]!!, taskArgs["boardId"]!!, taskArgs["workspaceId"]!!) as Result.Success).data
//                )
//            }
//
//            tasks.add(task)
//        }
//
//        val from = 1
//
//        val boardListPath = "${FirestoreCollection.WORKSPACES.collectionName}/${taskArgs["workspaceId"]!!}/" +
//                "${FirestoreCollection.BOARDS.collectionName}/${taskArgs["boardId"]!!}/" +
//                FirestoreCollection.BOARD_LIST.collectionName
//        val result = repository.deleteTaskAndRearrange(boardListPath, taskArgs["boardListId"]!!, tasks, from)
//
//        assertThat(result).isInstanceOf(Result.Success::class.java)
//    }
//    @Test
//    fun insertTaskAndRearrange_toInsertTaskInAList() = runBlocking {
//        val taskArgs = createTaskArgs()
//
//        val tasks = mutableListOf<Task>()
//        repeat(4) { i ->
//            val task = FirestoreTestUtil.createTask("Task $i", i.toLong()).run {
//                this.copy(
//                    id = (repository.createTask(this, taskArgs["boardListId"]!!, taskArgs["boardId"]!!, taskArgs["workspaceId"]!!) as Result.Success).data
//                )
//            }
//
//            tasks.add(task)
//        }
//
//        val to = 2
//        val taskToInsert = FirestoreTestUtil.createTask("Task Unknown", to.toLong()).run {
//            this.copy(id = "test_id")
//        }
//
//        val boardListPath = "${FirestoreCollection.WORKSPACES.collectionName}/${taskArgs["workspaceId"]!!}/" +
//                "${FirestoreCollection.BOARDS.collectionName}/${taskArgs["boardId"]!!}/" +
//                FirestoreCollection.BOARD_LIST.collectionName
//        val result = repository.insertTaskAndRearrange(boardListPath, taskArgs["boardListId"]!!, tasks, taskToInsert, to)
//
//        assertThat(result).isInstanceOf(Result.Success::class.java)
//    }

    @Test
    fun createTag_createsNewTag() = runBlocking {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "workspace")
        val board = createBoard(user.id, WorkspaceInfo(workspace.id, workspace.name), "board")
        val tag = FirestoreTestUtil.createTag("tag1", "#000000")

        val result = repository.upsertTag(
            tag,
            board.id,
            boardPath = "${FirestoreCollection.WORKSPACES.collectionName}/${workspace.id}" +
                    "/${FirestoreCollection.BOARDS.collectionName}"
        )

        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun upsertTag_updatesExistingTag() = runBlocking {
        val board = createBoard("board1")
        val tag = createTag("Test", "#000000", board.id, board.workspace.id)

        val newTag = tag.copy(name = "Test 1")
        val result = repository.upsertTag(
            newTag,
            board.id,
            "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}" +
                    "/${FirestoreCollection.BOARDS.collectionName}"
        )

        assertThat(result).isResultSuccess()

        val updTag = (repository.getTags(board.id, board.workspace.id) as Result.Success).data.first { it.id == tag.id }

        assertThat(updTag).isEqualTo(newTag)
    }

    @Test
    fun getTags_getsListOfTags() = runBlocking {
        val board = createBoard("board1")
        val tag1 = createTag("Tag1", "FF0000", board.id, board.workspace.id)
        val tag2 = createTag("Tag2", "FFA500", board.id, board.workspace.id)

        val result = repository.getTags(board.id, board.workspace.id)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resTags = (result as Result.Success).data

        assertThat(resTags).isEqualTo(listOf(tag1, tag2).sortedBy { it.name })
    }

    @Test
    fun getTaskTags_getsTagsForTheTask() = runBlocking {
        var board = createBoard("board1")
        val workspaceId = board.workspace.id
        val boardList = createBoardList("list1", 0, board).run {
            board = first
            second
        }
        var task = createTask("task1", 0, boardList)
        val tag1 = createTag("Tag 1", "#FFFFFF", board.id, workspaceId)
        val tag2 = createTag("Tag 2", "#000000", board.id, workspaceId)
        task = task.copy(
            tags = listOf(tag1.id, tag2.id)
        )

        val result = repository.getTaskTags(task.id, task.tags, boardList.id, boardList.path)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultTags = (result as Result.Success).data

        assertThat(resultTags.map { it.id }).isEqualTo(task.tags)
    }

    @Test
    fun getTaskTags_getsUpdatedTaskTags() = runBlocking {
        var board = createBoard("board1")
        val boardList = createBoardList("list1", 0, board).run {
            board = first
            second
        }
        var task = createTask("task1", 0, boardList)
        val tag1 = createTag("Tag 1", "#FFFFFF", board.id, board.workspace.id)
        val tag2 = createTag("Tag 2", "#000000", board.id, board.workspace.id)
        board = board.copy(tags = listOf(tag1, tag2))

        // add tags to the task
        task = task.copy(tags = listOf(tag1.id, tag2.id))
        repository.updateTaskTags(task.id, task.tags, boardList.id, boardList.path)

        // delete `tag2` from the board
        val updatedTags = board.tags.filterNot { it.id == tag2.id }
        repository.updateBoard(
            board.copy(tags = updatedTags)
        )

        // get task tags
        val result = repository.getTaskTags(task.id, task.tags, boardList.id, boardList.path)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultTags = (result as Result.Success).data

        assertThat(resultTags).isEqualTo(updatedTags)
    }

    @Test
    fun updateTaskTags_updatesTaskTagIdsList() = runBlocking {
        val board = createBoard("board1")
        val boardList = createBoardList("boardList1", 0, board).second
        val task = createTask("task1", 0, boardList)
        val tag1 = createTag("Tag1", FirestoreTestUtil.black, board.id, board.workspace.id)
        val tag2 = createTag("Tag2", FirestoreTestUtil.white, board.id, board.workspace.id)

        val taskTags = listOf(tag1.id, tag2.id)
        // add a tag to the task
        val result = repository.updateTaskTags(task.id, taskTags, boardList.id, boardList.path)

        assertThat(result).isResultSuccess()
    }

    @Test
    fun test(): Unit = runBlocking {
        val firestore = FirestoreTestUtil.firestore

        val data = mapOf(
            "tasks" to mapOf(
                "task_1" to mapOf(
                    "name" to "Task 1",
                    "position" to 1
                ),
                "task_2" to mapOf(
                    "name" to "Task 2",
                    "position" to 2
                ),
                "task_3" to mapOf(
                    "name" to "Task 3",
                    "position" to 3
                ),
            )
        )

        firestore.collection("test")
            .document("doc1")
            .set(data)
            .await()

        firestore.collection("test")
            .document("doc1")
            .update(
                mapOf(
                    "tasks.task_2.position" to 5,
                    "tasks.task_1.position" to 2,
                    "tasks.task_3.position" to 0,
                )
            )
            .await()

        firestore.collection("test")
            .document("doc1")
            .update(
                mapOf(
                    "tasks.task_1.position" to 10,
                    "tasks.task_3" to FieldValue.delete(),
                )
            )
            .await()
    }

    private suspend fun createUser(userId: String): User {
        return FirestoreTestUtil.createUser(userId).also {
            repository.createUser(it)
        }
    }

    private suspend fun createWorkspace(userId: String, name: String): Workspace {
        return FirestoreTestUtil.createWorkspace(userId, name).run {
            copy(
                id = (repository.createWorkspace(this) as Result.Success).data
            )
        }
    }

    private suspend fun createBoard(
        userId: String,
        workspaceInfo: WorkspaceInfo,
        name: String
    ): Board {
        return FirestoreTestUtil.createBoard(userId, workspaceInfo, name).run {
            copy(
                id = (repository.createBoard(this) as Result.Success).data
            )
        }
    }

    private suspend fun createBoard(name: String): Board {
        val user = createUser("user1")
        val workspace = createWorkspace(user.id, "workspace")
        return createBoard(user.id, WorkspaceInfo(workspace.id, workspace.name), name)
    }

    private suspend fun createBoardList(
        name: String,
        position: Int,
        board: Board
    ): Pair<Board, BoardList> {
        val boardList = FirestoreTestUtil.createBoardList(name, position).run {
            copy(
                id = (repository.createBoardList(this, board) as Result.Success).data,
                path = "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}" +
                        "/${FirestoreCollection.BOARDS.collectionName}/${board.id}" +
                        "/${FirestoreCollection.BOARD_LIST.collectionName}"
            )
        }
        val updBoard = board.copy(lists = board.lists + boardList.id)
        return updBoard to boardList
    }

    private suspend fun createBoardList(name: String, position: Int): BoardList {
        val board = createBoard("board1")
        return FirestoreTestUtil.createBoardList(name, position).run {
            copy(
                id = (repository.createBoardList(this, board) as Result.Success).data,
                path = "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}" +
                        "/${FirestoreCollection.BOARDS.collectionName}/${board.id}" +
                        "/${FirestoreCollection.BOARD_LIST.collectionName}"
            )
        }
    }

    private suspend fun createTask(name: String, position: Long, boardList: BoardList): Task {
        return FirestoreTestUtil.createTask(name, position).run {
            copy(
                id = (repository.createTask(
                    this,
                    boardList.id,
                    boardList.path
                ) as Result.Success).data
            )
        }
    }

    private suspend fun createTag(
        name: String,
        color: String,
        boardId: String,
        workspaceId: String
    ): Tag {
        val boardPath = "${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId" +
                "/${FirestoreCollection.BOARDS.collectionName}"
        return FirestoreTestUtil.createTag(name, color).run {
            (repository.upsertTag(this, boardId, boardPath) as Result.Success).data
        }
    }
}