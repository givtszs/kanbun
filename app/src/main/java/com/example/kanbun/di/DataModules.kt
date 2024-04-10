package com.example.kanbun.di

import com.example.kanbun.data.AndroidEmailPatternValidator
import com.example.kanbun.data.repository.AuthenticationRepositoryImpl
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.utils.EmailPatternValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    abstract fun bindFirestoreRepository(
        firestoreRepositoryImpl: FirestoreRepositoryImpl
    ): FirestoreRepository

    @Binds
    @Singleton
    abstract fun bindAuthenticationRepository(
        authenticationRepositoryImpl: AuthenticationRepositoryImpl
    ): AuthenticationRepository

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
    fun provideAndroidEmailPatterValidator() = AndroidEmailPatternValidator()
}