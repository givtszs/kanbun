package com.example.kanbun.presentation

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

abstract class BaseViewModel : ViewModel() {
    val firebaseUser = Firebase.auth.currentUser
}