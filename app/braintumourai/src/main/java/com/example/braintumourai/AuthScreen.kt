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

// AUTHENTICATION SCREEN
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit
) {

    val auth = remember { FirebaseAuth.getInstance() }

    var isRegisterMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "BrainTumour AI",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isRegisterMode) "Create your account"
            else "Sign in to continue",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(24.dp))

        if (isRegisterMode) {

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (isRegisterMode) {

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {

                if (busy) return@Button

                message = ""

                if (email.isBlank() || password.isBlank()) {
                    message = "Please enter your email and password."
                    return@Button
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches()
                ) {
                    message = "Please enter a valid email address."
                    return@Button
                }

                if (isRegisterMode) {

                    if (fullName.isBlank()) {
                        message = "Please enter your full name."
                        return@Button
                    }

                    if (password.length < 6) {
                        message = "Password must contain at least 6 characters."
                        return@Button
                    }

                    if (password != confirmPassword) {
                        message = "Passwords do not match."
                        return@Button
                    }
                }

                busy = true

                if (isRegisterMode) {

                    auth.createUserWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val profile =
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName.trim())
                                    .build()

                            auth.currentUser?.updateProfile(profile)
                                ?.addOnCompleteListener { profileTask ->

                                    auth.signOut()

                                    isRegisterMode = false
                                    password = ""
                                    confirmPassword = ""
                                    busy = false

                                    message = if (profileTask.isSuccessful) {
                                        "Account created successfully! Please sign in."
                                    } else {
                                        "Account created. Please sign in. Your name could not be saved."
                                    }
                                } ?: run {

                                auth.signOut()
                                isRegisterMode = false
                                password = ""
                                confirmPassword = ""
                                busy = false
                                message = "Account created successfully! Please sign in."
                            }

                        } else {

                            busy = false
                            message = task.exception?.localizedMessage
                                ?: "Account creation failed."
                        }
                    }

                } else {

                    auth.signInWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->

                        busy = false

                        if (task.isSuccessful) {
                            message = ""
                            onAuthenticated()
                        } else {
                            message = task.exception?.localizedMessage
                                ?: "Sign in failed."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text(
                if (isRegisterMode) "Create Account"
                else "Sign In"
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                message = ""
                password = ""
                confirmPassword = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text(
                if (isRegisterMode) "Already have an account? Sign In"
                else "Don't have an account? Create Account"
            )
        }

        Spacer(Modifier.height(16.dp))

        if (busy) {
            CircularProgressIndicator()
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
