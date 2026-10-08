package com.korkoor.pardos.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.korkoor.pardos.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Sesión con cuenta de Google (Credential Manager + Firebase Auth).
 *
 * Sin sesión el juego funciona igual de forma local. Con sesión, el progreso se guarda en la nube
 * de forma segura (colección `players/{uid}`) y se activan amigos y ranking.
 */
class AuthManager(context: Context) {
    private val appContext = context.applicationContext

    sealed interface Result {
        data object Success : Result
        data object Cancelled : Result
        /** No hay ninguna cuenta de Google en el dispositivo. */
        data object NoAccount : Result
        data class Error(val message: String) : Result
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) FirebaseApp.initializeApp(appContext)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth no disponible: ${e.message}")
            null
        }
    }

    val currentUser: FirebaseUser? get() = auth?.currentUser
    val isSignedIn: Boolean get() = currentUser != null

    /** Emite el usuario actual y cada cambio de sesión. */
    fun userFlow(): Flow<FirebaseUser?> = callbackFlow {
        val a = auth
        if (a == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        a.addAuthStateListener(listener)
        awaitClose { a.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(activity: Activity): Result {
        val a = auth ?: return Result.Error("Firebase no está configurado")
        return try {
            val option = GetSignInWithGoogleOption.Builder(appContext.getString(R.string.default_web_client_id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                signInTask(a, firebaseCredential)
            } else {
                Result.Error("Tipo de credencial no soportado")
            }
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            Result.NoAccount
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager: ${e.type} ${e.message}")
            Result.Error(e.message ?: "No se pudo iniciar sesión")
        } catch (e: Exception) {
            Log.w(TAG, "Inicio de sesión falló: ${e.message}")
            Result.Error(e.message ?: "No se pudo iniciar sesión")
        }
    }

    private suspend fun signInTask(a: FirebaseAuth, credential: com.google.firebase.auth.AuthCredential): Result =
        suspendCancellableCoroutine { cont ->
            a.signInWithCredential(credential)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Result.Success) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Firebase signInWithCredential: ${e.message}")
                    if (cont.isActive) cont.resume(Result.Error(e.message ?: "Error de autenticación"))
                }
        }

    suspend fun signOut() {
        auth?.signOut()
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "AuthManager"
    }
}
