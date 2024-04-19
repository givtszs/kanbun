package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toFirestoreTask
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.TaskRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : TaskRepository {

    override suspend fun createTask(
        task: Task,
        taskListId: String,
        taskListPath: String
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val taskId = UUID.randomUUID().toString()
            firestore.collection(taskListPath)
                .document(taskListId)
                .update("${FirestoreCollection.TASKS}.${taskId}", task.toFirestoreTask())
                .await()
        }
    }

    override suspend fun updateTask(
        oldTask: Task,
        newTask: Task,
        taskListId: String,
        taskListPath: String
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val taskUpdates = getTaskUpdates(oldTask, newTask)
            Log.d(TAG, "updateTask: taskUpdates: $taskUpdates")
            firestore.collection(taskListPath)
                .document(taskListId)
                .update(taskUpdates)
        }
    }

    private fun getTaskUpdates(
        oldTask: Task,
        newTask: Task
    ): Map<String, Any?> {
        val mapOfUpdates = mutableMapOf<String, Any?>()
        val taskId = newTask.id
        if (newTask.name != oldTask.name) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.name"] = newTask.name
        }
        if (newTask.description != oldTask.description) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.description"] = newTask.description
        }
        if (newTask.dateStarts != oldTask.dateStarts) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.dateStarts"] = newTask.dateStarts
        }
        if (newTask.dateEnds != oldTask.dateEnds) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.dateEnds"] = newTask.dateEnds
        }
        if (newTask.tags != oldTask.tags) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.tags"] = newTask.tags
        }
        if (newTask.members != oldTask.members) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.members"] = newTask.members
        }
        return mapOfUpdates
    }

    override suspend fun deleteTask(
        task: Task,
        taskListPath: String,
        taskListId: String
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(taskListPath)
                .document(taskListId)
                .update("${FirestoreCollection.TASKS}.${task.id}", FieldValue.delete())
        }
    }

    override suspend fun rearrangeTasks(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val updatesMap = getRearrangeUpdates(tasks, from, to)
            updateTasksPositions(taskListPath, taskListId, updatesMap)
        }
    }

    private fun getRearrangeUpdates(
        tasks: List<Task>,
        from: Int,
        to: Int
    ): Map<String, Long> {
        val updMap = mutableMapOf<String, Long>()
        if (from < to) {
            for (i in (from + 1)..to) {
                updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] =
                    tasks[i].position.dec()
            }
        } else {
            for (i in to..<from) {
                updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] =
                    tasks[i].position.inc()
            }
        }
        updMap["${FirestoreCollection.TASKS}.${tasks[from].id}.position"] = to.toLong()
        return updMap
    }

    override suspend fun deleteTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val updatesMap = deleteAndRearrange(tasks, from)
            updateTasksPositions(taskListPath, taskListId, updatesMap)
        }
    }

    private fun deleteAndRearrange(
        tasks: List<Task>,
        from: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()
        for (i in (from + 1)..<tasks.size) {
            updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] = tasks[i].position.dec()
        }
        updMap["${FirestoreCollection.TASKS}.${tasks[from].id}"] = FieldValue.delete()
        return updMap
    }

    override suspend fun insertTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        task: Task,
        to: Int
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val updatesMap = insertAndRearrange(tasks, task, to)
            Log.d("ItemTaskViewHolder", "FirestoreRepository#insert: updates: $updatesMap")
            updateTasksPositions(taskListPath, taskListId, updatesMap)
        }
    }

    private fun insertAndRearrange(
        listToInsert: List<Task>,
        task: Task,
        to: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()
        for (i in to..<listToInsert.size) {
            updMap["${FirestoreCollection.TASKS}.${listToInsert[i].id}.position"] =
                listToInsert[i].position.inc()
        }
        val newTask = task.copy(position = to.toLong())
        updMap["${FirestoreCollection.TASKS}.${task.id}"] = newTask.toFirestoreTask()
        return updMap
    }

    private suspend fun updateTasksPositions(
        listPath: String,
        listId: String,
        updatesMap: Map<String, Any>
    ) {
        firestore.collection(listPath)
            .document(listId)
            .update(updatesMap)
            .await()
    }
}