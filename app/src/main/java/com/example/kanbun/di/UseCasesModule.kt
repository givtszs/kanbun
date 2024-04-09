package com.example.kanbun.di

import com.example.kanbun.domain.usecase.CredentialsValidationUseCases
import com.example.kanbun.domain.usecase.ValidateNameUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCasesModule {

    @Provides
    @Singleton
    fun provideCredentialsValidationUseCases(): CredentialsValidationUseCases {
        return CredentialsValidationUseCases(
            validateNameUseCase = ValidateNameUseCase()
        )
    }
}