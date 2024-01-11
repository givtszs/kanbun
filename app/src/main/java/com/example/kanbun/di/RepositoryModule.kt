package com.example.kanbun.di

import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFirestoreRepository(firestoreRepositoryImpl: FirestoreRepositoryImpl): FirestoreRepository
}