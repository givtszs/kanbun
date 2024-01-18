package com.example.kanbun.di

import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ConnectivityCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
    @Binds
    @ViewModelScoped
    abstract fun bindConnectivityChecker(connectivityCheckerImpl: ConnectivityCheckerImpl): ConnectivityChecker
}