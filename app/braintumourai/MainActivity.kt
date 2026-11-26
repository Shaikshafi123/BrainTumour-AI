package com.example.braintumourai

import android.os.Bundle
import android.net.Uri
import android.graphics.BitmapFactory

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrainTumourApp()
                }
            }
        }
    }
}


// APP SCREENS
private enum class Screen {
    AUTH, DASHBOARD, HISTORY
}


// MAIN APP
@Composable
private fun BrainTumourApp() {

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    val auth = remember { FirebaseAuth.getInstance() }

    var currentUser by remember {
        mutableStateOf(auth.currentUser)
    }

    var screen by remember {
        mutableStateOf(
            if (auth.currentUser == null) Screen.AUTH
            else Screen.DASHBOARD
        )
    }

    when (screen) {

        Screen.AUTH -> AuthScreen(
            onAuthenticated = {
                currentUser = auth.currentUser
                screen = Screen.DASHBOARD
            }
        )

        Screen.DASHBOARD -> DashboardScreen(
            email = currentUser?.email ?: "User",
            onOpenHistory = {
                screen = Screen.HISTORY
            },
            onSignOut = {
                auth.signOut()
                currentUser = null
                screen = Screen.AUTH
            }
        )

        Screen.HISTORY -> HistoryScreen(
            onBack = {
                screen = Screen.DASHBOARD
            }
        )
    }
}
