package com.list_it.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AuthColor
import com.list_it.app.ui.components.ButtonView
import com.list_it.app.ui.components.CustomTextField
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.theme.IosColor
import kotlinx.coroutines.launch

/** iOS `LoginView` flow, in the web app's Capacitor login design. */
@Composable
fun LoginScreen(onSignUp: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var showForgotPassword by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { db.resetFields() }

    fun signUserIn() = scope.launch {
        isLoading = true
        val success = db.signIn(db.email.lowercase().trim(), db.password)
        isLoading = false
        if (success) db.isSignedIn = true else showAlert = true
    }

    AuthLayout {
        AuthHeader("Welcome back")
        AuthDivider("log in with email")
        CustomTextField(Icons.Outlined.Email, "Email", db.email, { db.email = it }, isPassword = false, keyboardType = KeyboardType.Email)
        CustomTextField(Icons.Outlined.Lock, "Password", db.password, { db.password = it }, isPassword = true)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            Text(
                "Forgot password?", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AuthColor.orange500,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { showForgotPassword = true }.padding(4.dp),
            )
        }
        ButtonView(
            if (isLoading) "Logging In..." else "Log In", showArrow = false,
            modifier = Modifier.padding(top = 24.dp), loading = isLoading,
        ) { signUserIn() }
        Spacer(Modifier.weight(1f))
        AuthFooter("Don't have an account?", "Sign up", onSignUp)
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    if (showForgotPassword) {
        IosSheet(onDismiss = { showForgotPassword = false }, dismissible = false) { dismiss -> ForgotPasswordSheet(dismiss) }
    }
}
