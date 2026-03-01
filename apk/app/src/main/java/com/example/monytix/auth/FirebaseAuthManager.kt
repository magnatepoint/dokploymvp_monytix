package com.example.monytix.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Central Firebase Auth manager. Provides current user, ID token for API calls, and auth state.
 */
object FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Flow of whether user is signed in. Emits true when authenticated, false otherwise. */
    val isSignedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser != null)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Get the current Firebase ID token for API Authorization header. */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
    }

    fun signOut() {
        auth.signOut()
    }
}
