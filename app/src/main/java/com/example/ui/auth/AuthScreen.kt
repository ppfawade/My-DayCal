package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val existingUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    if (existingUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }
    if (clientId.isBlank() || clientId.startsWith("000000000000")) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    if (clientId.isBlank() || clientId.startsWith("000000000000")) {
        onAuthError("Google Cloud Sync is not configured in this standalone APK build. Tap 'Continue Offline' below to use the app on device.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    runCatching { Firebase.auth.signOut() }
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Minimal Launch & Storage Screen matching the attached HTML specification:
 * - Surface background (#111319)
 * - 96dp x 96dp emblem box (#0c0e14) with ambient amber glow and 3-Bar Cadence SVG glyph (36w, 24w, 16w in #f59e0b)
 * - Brand title + "Lightning-fast daily time blocking."
 * - Primary CTA: Rounded pill in #f59e0b with monochromatic Google 'G' icon + "Sign in with Google"
 * - Secondary CTA: Rounded pill in #282a30 with bolt icon + "Continue Offline" on left and "Local" pill badge on right
 * - Privacy footnote: lock icon (#56e5a9) + "100% private • No account required • Stored on device"
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueWithLocalStorage: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(350L)
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111319)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Hero & Emblem Section
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0C0E14))
                    .border(1.dp, Color(0x26F59E0B), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Inner soft amber glow aura
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0x1FF59E0B))
                )

                // Tactile 3-Line Minimalist Cadence Glyph (36px, 24px, 16px stepped bars in #f59e0b)
                Canvas(modifier = Modifier.size(48.dp)) {
                    val scale = size.width / 48f
                    val barHeight = 4.5f * scale
                    val radius = CornerRadius(2.25f * scale, 2.25f * scale)
                    val startX = 6f * scale

                    // Bar 1: width 36, opacity 1.0
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(startX, 11f * scale),
                        size = Size(36f * scale, barHeight),
                        cornerRadius = radius
                    )
                    // Bar 2: width 24, opacity 0.85
                    drawRoundRect(
                        color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                        topLeft = Offset(startX, 22f * scale),
                        size = Size(24f * scale, barHeight),
                        cornerRadius = radius
                    )
                    // Bar 3: width 16, opacity 0.60
                    drawRoundRect(
                        color = Color(0xFFF59E0B).copy(alpha = 0.60f),
                        topLeft = Offset(startX, 33f * scale),
                        size = Size(16f * scale, barHeight),
                        cornerRadius = radius
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E2EB),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8C3AD),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF93000A)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFDAD6),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Interactive Action Module
            // 1. Primary CTA: Google Sign In (#f59e0b bg, #472a00 / #2a1700 text)
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    val credentialManager = CredentialManager.create(context)
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color(0xFF2A1700)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF2A1700),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    GoogleMonochromeGIcon(
                        tint = Color(0xFF2A1700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.sign_in_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2A1700)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Secondary CTA: Continue Offline + "Local" badge on right (#282a30 surface-container-high)
            Surface(
                shape = CircleShape,
                color = Color(0xFF282A30),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading) { onContinueWithLocalStorage() }
                    .testTag("continue_local_storage_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFC174),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.continue_local_storage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E2EB)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0C0E14)
                    ) {
                        Text(
                            text = "Local",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD8C3AD),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Assurance Footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF56E5A9),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.local_storage_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD8C3AD),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoogleMonochromeGIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.20f
        drawArc(
            color = tint,
            startAngle = 35f,
            sweepAngle = 295f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(w - stroke, h - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.50f, h * 0.42f),
            size = Size(w * 0.45f, stroke * 0.85f)
        )
    }
}
