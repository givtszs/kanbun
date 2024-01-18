package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.model.UserWorkspace
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryTest {
    private lateinit var repository: FirestoreRepository

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
    fun addUser_addsSignedInUserData() = runBlocking {
        val user = FirestoreTestUtil.user
        val result = repository.addUser(user)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        // suppose the result is always successful
        val savedUser = (repository.getUser(user.id) as Result.Success).data

        // test the user is saved correctly
        assertThat(savedUser).isEqualTo(user)
    }

    @Test
    fun getUser_getsSignedInUserData() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)

        val result = repository.getUser(user.id)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultUser = (result as Result.Success).data
        assertThat(resultUser).isEqualTo(user)
    }

    @Test
    fun getUser_toGetNonexistentUser_returnsResultError() = runBlocking {
        val userId = "nonexistent_id"

        val result = repository.getUser(userId)
        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message
        assertThat(resultMessage).isNotNull()
    }

    @Test
    fun getUser_withNullUserId_returnsResultError() = runBlocking {
        val userId = null
        val result = repository.getUser(userId)
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun getUserStream_withValidUserId_getsUserChangesOvertime() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)

        val data1 = repository.getUserStream(user.id).first()
        assertThat(data1).isEqualTo(user)

        val nameUpd = "New name"
        repository.updateUser(user.id, "name", nameUpd)

        val data2 = repository.getUserStream(user.id).first()
        assertThat(data2).isEqualTo(user.copy(name = nameUpd))
    }

    @Test
    fun updateUser_toUpdateName_updatesUserName() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.id, "name", "New Name")
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun updateUser_toUpdateWorkspaces_updatesUserWorkspaces() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.id, "workspaces", user.workspaces + UserWorkspace("workspaces/workspace1", "Workspace 1"))
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    // Current impl return success since Firestore adds a field if it was not in the document
    @Test
    fun updateUser_toUpdateNonexistingField_returnsResultError() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.id, "book", "Book name")
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun createWorkspace_addsWorkspaceEntry() = runBlocking {
        val user = FirestoreTestUtil.userEmptyWorksAndCards
        repository.addUser(user)

        val workspace = FirestoreTestUtil.workspace
        val workspaceRes = repository.addWorkspace(user, workspace)

        assertThat(workspaceRes).isInstanceOf(Result.Success::class.java)

        val workspaceResData = (workspaceRes as Result.Success).data
        assertThat(workspaceResData).isNotEmpty()

        val getUserRes = repository.getUser(user.id)
        assertThat(getUserRes).isInstanceOf(Result.Success::class.java)

        val getUserData = (getUserRes as Result.Success).data
        assertThat(getUserData.workspaces).isNotEmpty()
        assertThat(getUserData.workspaces.first().name).isEqualTo(workspace.name)
        assertThat(getUserData.workspaces.first().id).isEqualTo(workspaceResData)
    }

    @Test
    fun getWorkspace_withValidWorkspaceId_returnsWorkspace() = runBlocking {
        val workspace = FirestoreTestUtil.workspace.copy(id = "JTCtEGKBoS0EyL4phHdd")

//        repository.addWorkspace(workspace)

        val savedWorkspace = repository.getWorkspace(workspace.id)
        assertThat(savedWorkspace).isInstanceOf(Result.Success::class.java)

        val resultData = (savedWorkspace as Result.Success).data
        assertThat(resultData).isEqualTo(workspace)
    }

    @Test
    fun updateWorkspace_toUpdateName_updatesWorkspaceName() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val workspace = FirestoreTestUtil.workspace
        val workspaceId = (repository.addWorkspace(user, workspace) as Result.Success).data
        val result = repository.updateWorkspace(workspaceId, "name", "New Name")
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }
}