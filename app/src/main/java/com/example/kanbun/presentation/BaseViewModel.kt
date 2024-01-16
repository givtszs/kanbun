package com.example.kanbun.presentation

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

open class BaseViewModel : ViewModel() {
    val firebaseUser = Firebase.auth.currentUser
}