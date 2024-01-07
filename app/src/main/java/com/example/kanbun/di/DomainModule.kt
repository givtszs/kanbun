package com.example.kanbun.di

import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

    @Provides
    fun provideRegisterUserUseCase(firebaseAuth: FirebaseAuth) = RegisterUserUseCase(firebaseAuth)
}