package com.list_it.app.ui.screens.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.data.PasswordRules
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AuthColor
import com.list_it.app.ui.components.ButtonView
import com.list_it.app.ui.components.CustomTextField
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val PRIVACY_URL = "https://list-it-dom.netlify.app/privacy"

/** iOS `SignUpView` flow, in the web app's Capacitor sign-up design. */
@Composable
fun SignUpScreen(onSignIn: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var showEmailVerification by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { db.resetFields() }

    fun signUp() = scope.launch {
        if (db.name.isEmpty() || db.email.isEmpty() || db.password.isEmpty() || db.confirmPassword.isEmpty()) {
            db.setError("Form incomplete", "All fields must be filled in.")
            showAlert = true
            return@launch
        }
        if (!(db.isValidName() && db.isValidEmail(db.email) && db.isStrongPassword() && db.passwordsMatch())) {
            db.setError(
                "Invalid input",
                "Please ensure your name and email are valid, your passwords match, and your password has ${PasswordRules.RULE.replaceFirstChar { it.lowercase() }}",
            )
            showAlert = true
            return@launch
        }
        loading = true
        try {
            if (!db.isEmailAvailable(db.email)) {
                db.setError("Invalid Email", "Email is already taken. Please sign up with a different email or sign in.")
                showAlert = true
                return@launch
            }
            val (newUserID, success) = db.createUser(db.name.trim(), db.email.lowercase().trim(), db.password)
            if (success) {
                db.userID = newUserID
                showEmailVerification = true
            } else {
                db.setError("Error", "Account creation failed. Please try again.")
                showAlert = true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            db.setError("System Error", e.message ?: e.toString())
            showAlert = true
        } finally {
            loading = false
        }
    }

    AuthLayout {
        AuthHeader("Create your account")
        AuthDivider("sign up with email")
        val disabled = showEmailVerification || loading
        CustomTextField(Icons.Outlined.Person, "Full Name", db.name, { db.name = it }, isPassword = false, enabled = !disabled)
        CustomTextField(Icons.Outlined.Email, "Email", db.email, { db.email = it }, isPassword = false, enabled = !disabled, keyboardType = KeyboardType.Email)
        CustomTextField(Icons.Outlined.Lock, "Password", db.password, { db.password = it }, isPassword = true, enabled = !disabled)
        Text(PasswordRules.RULE, fontSize = 12.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        CustomTextField(Icons.Outlined.Lock, "Confirm Password", db.confirmPassword, { db.confirmPassword = it }, isPassword = true, enabled = !disabled)

        // Consent at the point of collection, as Google Play's User Data policy asks.
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = agreed, onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = AuthColor.orange500, uncheckedColor = if (palette.isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)),
                modifier = Modifier.padding(end = 6.dp).semantics { contentDescription = "Agree to the Privacy Policy" },
            )
            Text(
                buildAnnotatedString {
                    append("I agree to the ")
                    withLink(LinkAnnotation.Clickable("privacy", TextLinkStyles(SpanStyle(color = AuthColor.orange500, fontWeight = FontWeight.SemiBold))) {
                        uriHandler.openUri(PRIVACY_URL)
                    }) { append("Privacy Policy") }
                    append(". We only collect what the app needs, store it securely, and never sell it. You can delete your account and everything in it at any time.")
                },
                fontSize = 13.sp, lineHeight = 18.sp, color = if (palette.isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563),
            )
        }

        ButtonView(
            if (loading) "Signing Up..." else "Sign Up", showArrow = false,
            modifier = Modifier.padding(top = 24.dp), enabled = agreed && !showEmailVerification, loading = loading,
        ) { signUp() }
        Spacer(Modifier.weight(1f))
        AuthFooter("Already have an account?", "Login", onSignIn)
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    if (showEmailVerification) {
        IosSheet(onDismiss = { showEmailVerification = false }, dismissible = false) { dismiss ->
            EmailConfirmationSheet(dismiss)
        }
    }
}
