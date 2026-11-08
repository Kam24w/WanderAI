package com.vibra.wanderai.data.auth

import android.util.Log
import com.vibra.wanderai.core.network.SupabaseConfig
import com.vibra.wanderai.data.supabase.SupabaseTables
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.AuthUser
import com.vibra.wanderai.domain.model.SignUpResult
import com.vibra.wanderai.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState> =
        client.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.Authenticated ->
                        status.session.user?.toDomain()?.let(AuthState::Authenticated)
                            ?: AuthState.Unauthenticated
                    is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                    // A failed token refresh (e.g. offline) keeps the cached session usable.
                    is SessionStatus.RefreshFailure ->
                        client.auth.currentUserOrNull()?.toDomain()?.let(AuthState::Authenticated)
                            ?: AuthState.Unauthenticated
                }
            }
            .distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<Unit> = runAuth("signIn") {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): Result<SignUpResult> = runAuth("signUp") {
        client.auth.signUpWith(Email, redirectUrl = SupabaseConfig.REDIRECT_URL) {
            this.email = email
            this.password = password
            data = buildJsonObject { put(METADATA_FULL_NAME, displayName) }
        }
        if (client.auth.currentSessionOrNull() != null) {
            SignUpResult.SESSION_STARTED
        } else {
            SignUpResult.CONFIRMATION_REQUIRED
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> = runAuth("updateUser") {
        client.auth.updateUser {
            data = buildJsonObject { put(METADATA_FULL_NAME, displayName) }
        }
        Unit
    }

    override suspend fun signOut() {
        runAuth("signOut") { client.auth.signOut() }
    }

    override suspend fun deleteAccount(): Result<Unit> = runAuth("deleteAccount") {
        // Borra auth.users del llamante; profiles, user_achievements y saved_itineraries
        // caen por ON DELETE CASCADE (ver supabase/migrations).
        client.postgrest.rpc(SupabaseTables.RPC_DELETE_ACCOUNT)
        // El usuario ya no existe en el servidor: solo queda descartar la sesión local.
        client.auth.clearSession()
    }

    private suspend inline fun <T> runAuth(operation: String, block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Supabase $operation failed", e)
            Result.failure(e)
        }

    private fun UserInfo.toDomain(): AuthUser {
        val mail = email.orEmpty()
        val name = (userMetadata?.get(METADATA_FULL_NAME) as? JsonPrimitive)
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: mail.substringBefore('@').replaceFirstChar { it.uppercase() }
        return AuthUser(id = id, email = mail, displayName = name)
    }

    private companion object {
        const val TAG = "WanderAIAuth"
        const val METADATA_FULL_NAME = "full_name"
    }
}
