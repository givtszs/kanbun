package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
import com.example.kanbun.common.WorkspaceType
import com.example.kanbun.common.getResult
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toFirestoreMembers
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.example.kanbun.domain.repository.FirebaseFunctionsRepository
import com.example.kanbun.domain.repository.WorkspaceRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkspaceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val firebaseFunctionsRepository: FirebaseFunctionsRepository
) : WorkspaceRepository {

    override suspend fun createWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES)
                .add(workspace.toFirestoreWorkspace())
                .getResult {
                    addWorkspaceInfoToUser(
                        userId = workspace.owner,
                        workspaceInfo = WorkspaceInfo(result.id, workspace.name)
                    )
                }
        }
    }

    /**
     * Adds the [workspace information][WorkspaceInfo] to the user's list of workspaces.
     *
     * @param userId the user id
     * @param workspaceInfo the object containing the workspace information
     */
    private fun addWorkspaceInfoToUser(
        userId: String,
        workspaceInfo: WorkspaceInfo,
        isSharedWorkspace: Boolean = false
    ) {
        val fieldName =
            if (!isSharedWorkspace) WorkspaceType.USER_WORKSPACE else WorkspaceType.SHARED_WORKSPACE
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("$fieldName.${workspaceInfo.id}", workspaceInfo.name)
    }

    override suspend fun getWorkspace(workspaceId: String): Result<Workspace> = runCatching {
        withContext(dispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES)
                .document(workspaceId)
                .get()
                .getResult {
                    val firestoreWorkspace = result.toObject(FirestoreWorkspace::class.java)
                        ?: throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")

                    Log.d(TAG, "$firestoreWorkspace")

                    firestoreWorkspace.toWorkspace(workspaceId)
                }
        }
    }

    override fun getWorkspaceStream(workspaceId: String): Flow<Workspace?> = callbackFlow {
        var listener: ListenerRegistration? = null
        if (workspaceId.isEmpty()) {
            trySend(null)
        } else {
            listener = firestore.collection(FirestoreCollection.WORKSPACES)
                .document(workspaceId)
                .addSnapshotListener { docSnapshot, error ->
                    if (error != null) {
                        Log.d(TAG, "getWorkspaceStream: error occurred: ${error.message}")
//                        trySend(null)
                        close(error)
                        return@addSnapshotListener
                    }

                    val workspace = docSnapshot?.toObject(FirestoreWorkspace::class.java)
                        ?.toWorkspace(workspaceId)
                        ?: throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")
                    trySend(workspace)
                }
        }

        awaitClose {
            listener?.remove()
        }
    }

    override suspend fun updateWorkspace(
        oldWorkspace: Workspace,
        newWorkspace: Workspace
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val workspaceUpdates = getWorkspaceUpdates(oldWorkspace, newWorkspace)
            firestore.collection(FirestoreCollection.WORKSPACES)
                .document(newWorkspace.id)
                .update(workspaceUpdates)
                .getResult {
                    if ("name" in workspaceUpdates) {
                        updateWorkspaceNameInUserWorkspaces(
                            scope = this@withContext,
                            workspaceId = newWorkspace.id,
                            members = newWorkspace.members,
                            name = workspaceUpdates["name"] as String
                        )
                    }

                    if ("members" in workspaceUpdates) {
                        val oldMembersIds = oldWorkspace.members.keys
                        val newMembersIds = newWorkspace.members.keys
                        if (oldMembersIds != newMembersIds) {
                            val membersToAdd = newMembersIds.filterNot { it in oldMembersIds }
                            membersToAdd.forEach { memberId ->
                                addWorkspaceInfoToUser(
                                    userId = memberId,
                                    workspaceInfo = WorkspaceInfo(
                                        id = newWorkspace.id,
                                        name = newWorkspace.name
                                    ),
                                    isSharedWorkspace = true
                                )
                            }

                            val membersToDelete = oldMembersIds.filterNot { it in newMembersIds }
                            membersToDelete.forEach {
                                deleteWorkspaceInfoFromUser(it, newWorkspace.id)
                            }
                        }
                    }
                }
        }
    }

    private fun getWorkspaceUpdates(
        oldWorkspace: Workspace,
        newWorkspace: Workspace
    ): Map<String, Any> {
        val mapOfUpdates = mutableMapOf<String, Any>()
        if (newWorkspace.name != oldWorkspace.name) {
            mapOfUpdates["name"] = newWorkspace.name
        }
        if (newWorkspace.members != oldWorkspace.members) {
            mapOfUpdates["members"] = newWorkspace.members.toFirestoreMembers()
        }
        return mapOfUpdates
    }

    private fun updateWorkspaceNameInUserWorkspaces(
        workspaceId: String,
        members: Map<String, Role.Workspace>,
        name: String,
        scope: CoroutineScope
    ) {
        members.forEach { member ->
            scope.launch {
                firestore.collection(FirestoreCollection.USERS)
                    .document(member.key)
                    .update("workspaces.$workspaceId", name)
            }
        }
    }

    private fun deleteWorkspaceInfoFromUser(
        userId: String,
        workspaceId: String
    ) {
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("${WorkspaceType.SHARED_WORKSPACE}.$workspaceId", FieldValue.delete())
    }

    override suspend fun deleteWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val workspacePath = "${FirestoreCollection.WORKSPACES}/${workspace.id}"
            val boardsPath =
                "${FirestoreCollection.WORKSPACES}/${workspace.id}/${FirestoreCollection.BOARDS}"

            // delete the workspace
            firebaseFunctionsRepository.recursiveDelete(workspacePath)

            // delete the workspace boards
            firebaseFunctionsRepository.recursiveDelete(boardsPath)

            deleteWorkspaceFromMembers(workspace.id, workspace.owner, workspace.members, scope = this@withContext)
        }
    }

    /**
     * Deletes the workspace information from its members.
     *
     * @param workspaceId the workspace id
     * @param members the list of workspace members
     */
    private fun deleteWorkspaceFromMembers(
        workspaceId: String,
        ownerId: String,
        members: Map<String, Role.Workspace>,
        scope: CoroutineScope
    ) {
        val collectionRef = firestore.collection(FirestoreCollection.USERS)
        members.forEach { member ->
            val field = if (member.key != ownerId) "sharedWorkspaces" else "workspaces"
            scope.launch {
                collectionRef
                    .document(member.key)
                    .update("$field.$workspaceId", FieldValue.delete())
            }
        }
    }
}