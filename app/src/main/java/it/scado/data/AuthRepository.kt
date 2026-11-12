package it.scado.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun register(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Registrazione non riuscita")
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        ).await()
        db.collection("users").document(user.uid)
            .set(mapOf("displayName" to name.trim()), SetOptions.merge())
            .await()
    }

    fun updateDisplayName(name: String) {
        auth.currentUser?.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        )
    }

    fun signOut() = auth.signOut()

    companion object {
        fun describe(e: Throwable): String = when (e) {
            is FirebaseAuthWeakPasswordException -> "Password troppo debole: usa almeno 6 caratteri."
            is FirebaseAuthUserCollisionException -> "Esiste già un account con questa email."
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Email o password non corretti."
            is FirebaseNetworkException -> "Nessuna connessione a internet."
            else -> e.localizedMessage ?: "Qualcosa è andato storto."
        }
    }
}
