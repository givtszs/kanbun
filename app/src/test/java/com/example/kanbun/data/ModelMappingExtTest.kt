package com.example.kanbun.data

import com.example.kanbun.common.AuthProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ModelMappingExtTest {

    @Test
    fun `User#toFirestoreUser maps User to FirestoreUser`() {
        val user = TestData.user
        val firestoreUser = TestData.firestoreUser
        val result = user.toFirestoreUser()
        assertThat(result).isEqualTo(firestoreUser)
    }

    @Test
    fun `FirestoreUser#toUser maps mapped FirestoreUser to User`() {
        val firestoreUser = TestData.firestoreUser
        val user = TestData.user
        val result = firestoreUser.toUser(user.uid)
        assertThat(result).isEqualTo(user)
    }

    @Test
    fun `FirebaseUser#toUser maps FirebaseUser to User`() {
        val firebaseUser = mock(FirebaseUser::class.java)
        `when`(firebaseUser.uid).thenReturn("test1")

        // Mock UserInfo
        val mockUserInfo = mock(UserInfo::class.java)
        `when`(mockUserInfo.providerId).thenReturn("password")
        `when`(mockUserInfo.email).thenReturn("mock@example.com")
        `when`(mockUserInfo.displayName).thenReturn("Test")
        `when`(mockUserInfo.photoUrl).thenReturn(null)

        // Mock providerData list
        `when`(firebaseUser.providerData).thenReturn(listOf(mockUserInfo))

        // Call the function
        val result = firebaseUser.toUser(AuthProvider.EMAIL)

        val firebaseUserProviderData = firebaseUser.providerData.first { it.providerId == AuthProvider.EMAIL.providerId }
        // Verify the result
        assertThat(result.uid).isEqualTo(firebaseUser.uid)
        assertThat(result.email).isEqualTo(firebaseUserProviderData.email)
        assertThat(result.name).isEqualTo(firebaseUserProviderData.displayName)
        assertThat(result.profilePicture).isEqualTo("null")
        assertThat(result.authProvider.providerId).isEqualTo(firebaseUserProviderData.providerId)
    }
}