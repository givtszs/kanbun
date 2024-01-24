package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.model.User
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        val user = FirestoreTestUtil.createUser("user")
        repository.createUser(user)
        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").run {
            this.copy(
                id = (repository.createWorkspace(this) as Result.Success).data
            )
        }

        var board = FirestoreTestUtil.createBoard(
            user.id,
            User.WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        )

        val result = repository.createBoard(board)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultId = (result as Result.Success).data
        board = board.copy(id = resultId)

        assertThat(resultId).isNotEmpty()

        workspace = (repository.getWorkspace(workspace.id) as Result.Success).data

        assertThat(workspace.boards).isNotEmpty()
        assertThat(workspace.boards.any { it.id == board.id }).isTrue()
    }

    @Test
    fun getBoard_withValidArgs_returnsBoard() = runBlocking {
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
            User.WorkspaceInfo(workspace.id, workspace.name),
            "Board 1"
        ).run {
            val boardId = (repository.createBoard(this) as Result.Success).data
            this.copy(id = boardId)
        }

        val resultGet = repository.getBoard(workspace.id, board.id)

        assertThat(resultGet).isInstanceOf(Result.Success::class.java)
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
            User.WorkspaceInfo(workspace.id, workspace.name),
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
            User.WorkspaceInfo(workspace.id, workspace.name),
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

        val boardListFlow = repository.getBoardListsFlow(board)

        assertThat(boardListFlow.first().first()).isEqualTo(boardList)
    }
}