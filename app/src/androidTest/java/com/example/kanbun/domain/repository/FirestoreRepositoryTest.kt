package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.model.UserWorkspace
import com.google.common.truth.Truth.assertThat
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
        val savedUser = (repository.getUser(user.uid) as Result.Success).data

        // test the user is saved correctly
        assertThat(savedUser).isEqualTo(user)
    }

    @Test
    fun getUser_getsSignedInUserData() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)

        val result = repository.getUser(user.uid)
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
    fun updateUser_toUpdateName_updatesUserName() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.uid, "name", "New Name")
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun updateUser_toUpdateWorkspaces_updatesUserWorkspaces() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.uid, "workspaces", user.workspaces + UserWorkspace("workspaces/workspace1", "Workspace 1"))
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    // Current impl return success since Firestore adds a field if it was not in the document
    @Test
    fun updateUser_toUpdateNonexistingField_returnsResultError() = runBlocking {
        val user = FirestoreTestUtil.user
        repository.addUser(user)
        val result = repository.updateUser(user.uid, "book", "Book name")
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun createWorkspace_addsWorkspaceEntry() = runBlocking {
        val user = FirestoreTestUtil.userEmptyWorksAndCards
        repository.addUser(user)

        val workspace = FirestoreTestUtil.workspace
        val result = repository.addWorkspace(user, workspace)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        val getUserRes = repository.getUser(user.uid)
        assertThat(getUserRes).isInstanceOf(Result.Success::class.java)

        val getUserData = (getUserRes as Result.Success).data
        assertThat(getUserData.workspaces).isNotEmpty()
        assertThat(getUserData.workspaces.first().name).isEqualTo(workspace.name)
    }

    @Test
    fun getWorkspace_withValidWorkspaceId_returnsWorkspace() = runBlocking {
        val workspace = FirestoreTestUtil.workspace.copy(uid = "JTCtEGKBoS0EyL4phHdd")

//        repository.addWorkspace(workspace)

        val savedWorkspace = repository.getWorkspace(workspace.uid)
        assertThat(savedWorkspace).isInstanceOf(Result.Success::class.java)

        val resultData = (savedWorkspace as Result.Success).data
        assertThat(resultData).isEqualTo(workspace)
    }
}