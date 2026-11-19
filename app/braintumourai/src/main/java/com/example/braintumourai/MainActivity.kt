package com.example.braintumourai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

private enum class Screen {
    AUTH, DASHBOARD, HISTORY
}

@Composable
private fun BrainTumourApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var screen by remember {
        mutableStateOf(if (auth.currentUser == null) Screen.AUTH else Screen.DASHBOARD)
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
            onOpenHistory = { screen = Screen.HISTORY },
            onSignOut = {
                auth.signOut()
                currentUser = null
                screen = Screen.AUTH
            }
        )

        Screen.HISTORY -> HistoryScreen(
            onBack = { screen = Screen.DASHBOARD }
        )
    }
}

@Composable
private fun AuthScreen(onAuthenticated: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Brain Tumour AI", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("MRI image classification – educational prototype")
        Spacer(Modifier.height(24.dp))

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
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.length < 6) {
                    message = "Enter a valid email and a password of at least 6 characters."
                } else {
                    busy = true
                    message = ""
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            busy = false
                            if (task.isSuccessful) onAuthenticated()
                            else message = task.exception?.localizedMessage ?: "Sign-in failed."
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("Sign in")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                if (email.isBlank() || password.length < 6) {
                    message = "Enter a valid email and a password of at least 6 characters."
                } else {
                    busy = true
                    message = ""
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            busy = false
                            if (task.isSuccessful) onAuthenticated()
                            else message = task.exception?.localizedMessage ?: "Registration failed."
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("Create account")
        }

        if (busy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DashboardScreen(
    email: String,
    onOpenHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Choose an MRI image to begin.") }
    var saving by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImage = uri
        status = if (uri != null) "Image selected. No AI analysis has been performed yet."
        else "No image selected."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Signed in as $email")
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("MRI image", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    selectedImage?.lastPathSegment
                        ?: "No image selected"
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose MRI image")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(status)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val uid = auth.currentUser?.uid
                val uri = selectedImage

                if (uid == null) {
                    status = "Please sign in again."
                } else if (uri == null) {
                    status = "Please select an image first."
                } else {
                    saving = true
                    val record = hashMapOf(
                        "createdAt" to Date(),
                        "status" to "NOT_ANALYSED",
                        "message" to "Demo record only. AI model is not integrated.",
                        "selectedImageUri" to uri.toString()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("analyses")
                        .add(record)
                        .addOnSuccessListener {
                            saving = false
                            status = "Demo record saved. This is NOT a tumour prediction."
                        }
                        .addOnFailureListener { error ->
                            saving = false
                            status = "Could not save record: ${error.localizedMessage}"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            Text(if (saving) "Saving..." else "Save demo analysis record")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View history")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign out")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Educational prototype only. No medical diagnosis is produced. " +
                    "Do not use this application to make healthcare decisions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun HistoryScreen(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var records by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf("Loading history...") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            message = "Please sign in."
        } else {
            db.collection("users")
                .document(uid)
                .collection("analyses")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snapshot ->
                    records = snapshot.documents.map { document ->
                        val status = document.getString("status") ?: "Unknown status"
                        val date = document.getDate("createdAt")?.let {
                            DateFormat.getDateTimeInstance().format(it)
                        } ?: "Date unavailable"
                        "$status • $date"
                    }
                    message = if (records.isEmpty()) "No saved records yet." else ""
                }
                .addOnFailureListener { error ->
                    message = error.localizedMessage ?: "Could not load history."
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Analysis history", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        if (message.isNotBlank()) Text(message)
        records.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedButton(onClick = onBack) {
                Text("Back to dashboard")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Saved entries are demo records, not medical results.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
