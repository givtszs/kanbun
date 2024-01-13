package com.example.kanbun.di

import com.example.kanbun.EmulatorDetector
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Firebase-related dependencies specifically for the debug build variant.
 */
@Module
@InstallIn(SingletonComponent::class)
class FirebaseModule {
    private val host = "10.0.2.2"

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth {
      return if (EmulatorDetector.isEmulator()) {
          Firebase.auth.apply { useEmulator(host, 9099) }
      } else {
          Firebase.auth
      }
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return if (EmulatorDetector.isEmulator()) {
            Firebase.firestore.apply { useEmulator(host, 8080) }
        } else {
            Firebase.firestore
        }
    }
}