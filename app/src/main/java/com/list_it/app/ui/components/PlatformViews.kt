package com.list_it.app.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.list_it.app.R

/** Asks the user once if the app may send notifications (needed for reminders). */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

/** The looping envelope animation on the "check your email" page. */
@Composable
fun EmailAnimation(modifier: Modifier = Modifier) {
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.email_animation)).value
    LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = modifier)
}

/** Opens Android's share menu so you can send text to another app. */
@Composable
fun rememberShareAction(): (subject: String, text: String) -> Unit {
    val context = LocalContext.current
    return { subject, text ->
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, subject.ifEmpty { "Note" }))
    }
}

/** Opens your calendar app with a new event already filled in with the task's title and notes. */
@Composable
fun rememberAddToCalendarAction(): (title: String, notes: String) -> Unit {
    val context = LocalContext.current
    return { title, notes ->
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, notes)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No calendar app installed: nothing to open.
        }
    }
}
