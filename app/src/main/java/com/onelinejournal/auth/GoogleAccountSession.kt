package com.onelinejournal.auth

import android.accounts.Account
import android.app.Activity
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.onelinejournal.R
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Drive access can't be obtained without showing UI, so the user must sign in again. */
class DriveAuthRequiredException :
    IllegalStateException("Drive access needs to be granted again. Sign in from Settings.")

class GoogleAccountSession(
    private val activity: ComponentActivity,
    private val preferences: SharedPreferences
) {
    private val authorizationClient = Identity.getAuthorizationClient(activity)
    private val credentialManager = CredentialManager.create(activity)

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedAccessTokenAt: Long = 0L

    private var authorizationContinuation: CancellableContinuation<AuthorizationResult>? = null

    private val authorizationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val continuation = authorizationContinuation
        authorizationContinuation = null
        if (continuation == null || !continuation.isActive) {
            return@registerForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            continuation.resumeWithException(CancellationException("Drive access was cancelled"))
            return@registerForActivityResult
        }
        try {
            continuation.resume(
                authorizationClient.getAuthorizationResultFromIntent(result.data)
            )
        } catch (error: Exception) {
            continuation.resumeWithException(error)
        }
    }

    val signedInEmail: String?
        get() = preferences.getString(EMAIL_KEY, null)

    fun isSignedIn(): Boolean = !signedInEmail.isNullOrBlank()

    suspend fun signIn(): String {
        val webClientId = activity.getString(R.string.default_web_client_id)
        if (webClientId.isBlank() || webClientId.startsWith("YOUR_")) {
            throw IllegalStateException(
                "Google OAuth is not configured. Add GOOGLE_WEB_CLIENT_ID to local.properties."
            )
        }

        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = try {
            credentialManager.getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            throw CancellationException("Sign-in was cancelled")
        } catch (_: NoCredentialException) {
            throw IllegalStateException("No Google account is available on this device.")
        } catch (error: GetCredentialException) {
            throw IllegalStateException(error.message ?: "Google Sign-In failed.")
        }

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Unexpected Google credential type.")
        }
        val googleId = GoogleIdTokenCredential.createFrom(credential.data)
        val email = googleId.id
        if (email.isBlank()) {
            throw IllegalStateException("Google Sign-In did not return an account.")
        }

        val token = authorizeDrive(allowUi = true, email = email).accessToken
            ?: throw IllegalStateException("Google did not return a Drive access token.")
        cacheToken(token)
        preferences.edit().putString(EMAIL_KEY, email).apply()
        return email
    }

    suspend fun getAccessToken(allowUi: Boolean): String {
        val cached = cachedAccessToken
        if (
            cached != null &&
            System.currentTimeMillis() - cachedAccessTokenAt < TOKEN_CACHE_MS
        ) {
            return cached
        }

        val token = authorizeDrive(allowUi, signedInEmail).accessToken
            ?: throw IllegalStateException("Google did not return a Drive access token.")
        cacheToken(token)
        return token
    }

    suspend fun signOut() {
        cachedAccessToken = null
        cachedAccessTokenAt = 0L
        preferences.edit().remove(EMAIL_KEY).apply()
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun cacheToken(token: String) {
        cachedAccessToken = token
        cachedAccessTokenAt = System.currentTimeMillis()
    }

    // Pin the request to the account the user picked. Without it, a device with several
    // Google accounts can authorize Drive for a different account than the one shown.
    private suspend fun authorizeDrive(allowUi: Boolean, email: String?): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .apply {
                if (!email.isNullOrBlank()) setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
            }
            .build()
        val result = authorizationClient.authorize(request).await()
        if (!result.hasResolution()) {
            return result
        }
        if (!allowUi) {
            throw DriveAuthRequiredException()
        }
        val pendingIntent = result.pendingIntent
            ?: throw IllegalStateException("Drive authorization is missing a resolution.")
        return suspendCancellableCoroutine { continuation ->
            authorizationContinuation = continuation
            continuation.invokeOnCancellation {
                if (authorizationContinuation === continuation) {
                    authorizationContinuation = null
                }
            }
            try {
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (error: Exception) {
                authorizationContinuation = null
                continuation.resumeWithException(error)
            }
        }
    }

    companion object {
        /** Gets a Drive token without any UI, for background work. */
        suspend fun silentAccessToken(context: android.content.Context, email: String): String {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
                .build()
            val result = Identity.getAuthorizationClient(context).authorize(request).await()
            if (result.hasResolution()) throw DriveAuthRequiredException()
            return result.accessToken
                ?: throw IllegalStateException("Google did not return a Drive access token.")
        }

        const val EMAIL_KEY = "google_backup_email"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val TOKEN_CACHE_MS = 45L * 60L * 1000L
    }
}
