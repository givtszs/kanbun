package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    suspend fun createWorkspace(workspace: Workspace): Result<Unit>

    suspend fun getWorkspace(workspaceId: String): Result<Workspace>

    fun getWorkspaceStream(workspaceId: String): Flow<Workspace?>

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