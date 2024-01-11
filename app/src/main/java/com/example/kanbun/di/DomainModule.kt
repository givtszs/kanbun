package com.example.kanbun.di

import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class DomainModule {

    @Provides
    @ViewModelScoped
    fun provideRegisterUserUseCase(firebaseAuth: FirebaseAuth) = RegisterUserUseCase(firebaseAuth)
}