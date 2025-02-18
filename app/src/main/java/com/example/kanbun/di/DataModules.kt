package com.example.kanbun.di

import com.example.kanbun.data.AndroidEmailPatternValidator
import com.example.kanbun.data.repository.AuthenticationRepositoryImpl
import com.example.kanbun.data.repository.BoardRepositoryImpl
import com.example.kanbun.data.repository.FirebaseFunctionsRepositoryImpl
import com.example.kanbun.data.repository.StorageRepositoryImpl
import com.example.kanbun.data.repository.TaskListRepositoryImpl
import com.example.kanbun.data.repository.TaskRepositoryImpl
import com.example.kanbun.data.repository.UserRepositoryImpl
import com.example.kanbun.data.repository.WorkspaceRepositoryImpl
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.repository.BoardRepository
import com.example.kanbun.domain.repository.FirebaseFunctionsRepository
import com.example.kanbun.domain.repository.StorageRepository
import com.example.kanbun.domain.repository.TaskListRepository
import com.example.kanbun.domain.repository.TaskRepository
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.domain.repository.WorkspaceRepository
import com.example.kanbun.domain.utils.EmailPatternValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindFirebaseFunctionsRepository(
        firebaseFunctionsRepositoryImpl: FirebaseFunctionsRepositoryImpl
    ): FirebaseFunctionsRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindBoardRepository(
        boardRepositoryImpl: BoardRepositoryImpl
    ): BoardRepository

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(
        workspaceRepositoryImpl: WorkspaceRepositoryImpl
    ): WorkspaceRepository

    @Binds
    @Singleton
    abstract fun bindTaskListRepository(
        taskListRepositoryImpl: TaskListRepositoryImpl
    ): TaskListRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAuthenticationRepository(
        authenticationRepositoryImpl: AuthenticationRepositoryImpl
    ): AuthenticationRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository

    @Binds
    @Singleton
    abstract fun bindEmailPatternValidator(
        androidEmailPatternValidator: AndroidEmailPatternValidator
    ): EmailPatternValidator
}

/**
 * Dagger Hilt module for providing Firebase-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = Firebase.functions

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    @Provides
    @Singleton
    fun provideAndroidEmailPatterValidator() = AndroidEmailPatternValidator()
}