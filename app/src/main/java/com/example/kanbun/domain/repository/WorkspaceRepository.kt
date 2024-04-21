package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {

    /**
     * Adds a new workspace entry to the Firestore database.
     *
     * @param workspace the workspace to add.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun createWorkspace(workspace: Workspace): Result<Unit>

    /**
     * Retrieves the [Workspace] from the Firestore database based on the given [workspaceId].
     *
     * @param workspaceId the id of a workspace to retrieve.
     * @return [Result] with the [Workspace] on success, or an error on failure.
     */
    suspend fun getWorkspace(workspaceId: String): Result<Workspace>

    /**
     * Retrieves the stream of [Workspace] data from the Firestore database for the given [workspaceId].
     *
     * @param workspaceId the id of a workspace to get data from.
     * @return [Flow] of the [Workspace] data.
     */
    fun getWorkspaceStream(workspaceId: String): Flow<Result<Workspace?>>

    suspend fun updateWorkspace(oldWorkspace: Workspace, newWorkspace: Workspace): Result<Unit>

    /**
     * Deletes the [workspace] using the deployed Cloud Function which recursively deletes all
     * data in the document, including sub collections.
     *
     * @param workspace the workspace to delete
     * @return the result of the function execution
     */
    suspend fun deleteWorkspace(workspace: Workspace): Result<Unit>
}