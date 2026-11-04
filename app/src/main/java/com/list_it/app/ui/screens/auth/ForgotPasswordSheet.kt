package com.list_it.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.ButtonView
import com.list_it.app.ui.components.CustomTextField
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** iOS `ForgotPasswordView`. */
@Composable
fun ColumnScope.ForgotPasswordSheet(dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }

    fun forgotPassword() = scope.launch {
        if (db.resetPassword()) dismiss() else showAlert = true
    }

    IosTopBar("", trailing = { BarButton("Cancel") { db.resetFields(); dismiss() } })
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text("Reset password", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.primary)
        Text(
            "Enter your email below to receive a link to reset your password",
            fontSize = 15.sp, color = LocalPalette.current.secondary, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        CustomTextField(Icons.Outlined.Email, "Enter email", db.forgotPasswordEmail, { db.forgotPasswordEmail = it }, isPassword = false, keyboardType = KeyboardType.Email)
        ButtonView("Send Reset Link", showArrow = false, modifier = Modifier.padding(top = 16.dp)) { forgotPassword() }
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}
