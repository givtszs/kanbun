package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
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
        FirestoreTestUtil.deleteFirestoreData()
    }

    @Test
    fun addUser_withValidUserId_savesUserDataIntoFirestore() = runBlocking {
        val user = FirestoreTestUtil.userSample
        val result = repository.addUser(user)

        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun addUser_withEmptyUserId_returnsResultError() = runBlocking {
        val user = FirestoreTestUtil.createUser("")
        val result = repository.addUser(user)

        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message

        assertThat(resultMessage).isNotNull()
        assertThat(resultMessage).isNotEmpty()
    }

    @Test
    fun getUser_getsUserDataFromFirestore() = runBlocking {
        val user = FirestoreTestUtil.userSample
        repository.addUser(user)

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
        repository.addUser(user)

        val data1 = repository.getUserStream(user.id).first()

        assertThat(data1).isEqualTo(user)

        val newName = "New name"
        user = user.copy(name = newName)
        repository.addUser(user)

        val data2 = repository.getUserStream(user.id).first()

        assertThat(data2).isEqualTo(user)
    }

    @Test
    fun getUserStream_withEmptyUserId_returnsFlowOfNull() = runBlocking {
        var userFlow = repository.getUserStream("userId")

        assertThat(userFlow.first()).isNull()

        val user = FirestoreTestUtil.createUser("user")
        repository.addUser(user)
        userFlow = repository.getUserStream(user.id)

        assertThat(userFlow.first()).isEqualTo(user)
    }

    @Test
    fun createWorkspace_addsWorkspaceEntry() = runBlocking {
        val user = FirestoreTestUtil.createUser("user")
        repository.addUser(user)

        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Test")
        val workspaceRes = repository.addWorkspace(user.id, workspace)

        assertThat(workspaceRes).isInstanceOf(Result.Success::class.java)

        val resultWorkspaceId = (workspaceRes as Result.Success).data

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
        repository.addUser(user)
        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        val workspaceId = (repository.addWorkspace(user.id, workspace) as Result.Success).data

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
        repository.addUser(user)

        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        workspace = (repository.addWorkspace(user.id, workspace) as Result.Success).data.run {
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
        repository.addUser(user)
        val workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace").run {
            val id = (repository.addWorkspace(user.id, this) as Result.Success).data
            this.copy(id = id)
        }

        workspaceFlow = repository.getWorkspaceStream(workspace.id)

        assertThat(workspaceFlow.first()).isEqualTo(workspace)
    }

    @Test
    fun updateWorkspaceName_updatesWorkspaceName() = runBlocking {
        val user = FirestoreTestUtil.userSample
        repository.addUser(user)
        var workspace = FirestoreTestUtil.createWorkspace(user.id, "Workspace")
        workspace = (repository.addWorkspace(user.id, workspace) as Result.Success).data.run {
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
            repository.addUser(user1)

            val user2 = FirestoreTestUtil.createUser("user2")
            repository.addUser(user2)

            var workspace = FirestoreTestUtil.createWorkspace(user1.id, "Test")
            workspace = (repository.addWorkspace(user1.id, workspace) as Result.Success).data.run {
                workspace.copy(id = this)
            }

            val result = repository.inviteToWorkspace(workspace, user2)

            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

    @Test
    fun deleteWorkspace_deletesWorkspace(): Unit = runBlocking {
        val user1 = FirestoreTestUtil.createUser("user1")
        repository.addUser(user1)

        val user2 = FirestoreTestUtil.createUser("user2")
        repository.addUser(user2)

        var workspace = FirestoreTestUtil.createWorkspace(user1.id, "Test")
        workspace = (repository.addWorkspace(user1.id, workspace) as Result.Success).data.run {
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
}