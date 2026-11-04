package com.list_it.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.VerificationStatus
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.EmailAnimation
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** iOS `EmailConfirmationView`: waits for the user to confirm their email. */
@Composable
fun ColumnScope.EmailConfirmationSheet(dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isChecking = true
        db.verificationCheck(db.userID, db.name, db.email, db.password)
        isChecking = false
    }
    LaunchedEffect(db.verificationStatus) {
        if (db.verificationStatus == VerificationStatus.Verified) {
            // redirectUser()
            isChecking = false
            delay(1_500)
            db.signIn(db.email, db.password)
            db.isSignedIn = true
        }
    }

    fun deleteUser() = scope.launch {
        if (db.userID.isEmpty()) {
            db.setError("Error", "User ID is empty")
            showAlert = true
            return@launch
        }
        if (!db.deleteAccountById(db.userID)) showAlert = true else dismiss()
    }

    IosTopBar("", trailing = { BarButton("Cancel") { deleteUser() } })
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 20.dp)) {
        if (db.verificationStatus == VerificationStatus.Verified) SuccessView()
        else PendingVerificationView(db.email, isChecking) {
            scope.launch {
                isChecking = true
                db.verificationCheck(db.userID, db.name, db.email, db.password)
                isChecking = false
            }
        }
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}

@Composable
private fun SuccessView() {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Icon(Icons.Filled.CheckCircle, null, tint = IosColor.green, modifier = Modifier.size(80.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Email Verified!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = palette.primary)
            Text(
                "🎉 Congratulations! Your email has been successfully verified. We're setting up your account...",
                fontSize = 18.sp, fontWeight = FontWeight.Medium, color = palette.secondary, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PendingVerificationView(email: String, isChecking: Boolean, onCheck: () -> Unit) {
    val palette = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Box(Modifier.fillMaxWidth().height(240.dp).background(IosColor.blue.copy(alpha = 0.05f), RoundedCornerShape(24.dp))) {
            EmailAnimation(Modifier.fillMaxWidth().height(240.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Check Your Email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.primary)
            Text("We've sent a verification link to", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.secondary)
            Text(email, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = IosColor.blue)
        }
        Text(
            "How to verify: Check your inbox, tap the link, return here, and tap the button below.",
            fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.secondary, textAlign = TextAlign.Center,
        )
        Row(
            Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .alpha(if (isChecking) 0.7f else 1f)
                .background(IosColor.blue, RoundedCornerShape(16.dp))
                .clickable(enabled = !isChecking, onClick = onCheck)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isChecking) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(if (isChecking) "Checking..." else "I've verified my email", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
