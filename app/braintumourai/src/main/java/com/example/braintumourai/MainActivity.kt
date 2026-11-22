
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


// DASHBOARD SCREEN

@Composable
private fun DashboardScreen(
    email: String,
    onOpenHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var selectedImage by remember {
        mutableStateOf<Uri?>(null)
    }

    var previewBitmap by remember {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    var previewLoading by remember {
        mutableStateOf(false)
    }

    var imageValid by remember {
        mutableStateOf(false)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("Choose an MRI image to begin.")
    }

    // IMAGE PICKER
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        selectedImage = uri
        previewBitmap = null
        imageValid = false

        status = if (uri != null) {
            "Checking selected image..."
        } else {
            "No image selected."
        }
    }

    // IMAGE VALIDATION AND PREVIEW
    LaunchedEffect(selectedImage) {

        previewBitmap = null
        imageValid = false

        val uri = selectedImage

        if (uri == null) {
            previewLoading = false
            return@LaunchedEffect
        }

        previewLoading = true

        try {

            val result: Pair<android.graphics.Bitmap?, String?> =
                withContext(Dispatchers.IO) {

                    // Check image MIME type
                    val mimeType = context.contentResolver.getType(uri)

                    if (mimeType != null &&
                        !mimeType.startsWith("image/")
                    ) {
                        return@withContext null to
                                "Unsupported file type. Please select an image."
                    }

                    // Read image dimensions first
                    val bounds = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }

                    val boundsStream =
                        context.contentResolver.openInputStream(uri)
                            ?: return@withContext null to
                                    "Cannot open this image."

                    boundsStream.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }

                    if (bounds.outWidth <= 0 ||
                        bounds.outHeight <= 0
                    ) {
                        return@withContext null to
                                "Invalid or corrupted image. Please choose another."
                    }

                    // Protect the app from extremely large images
                    val totalPixels =
                        bounds.outWidth.toLong() * bounds.outHeight.toLong()

                    if (totalPixels > 100_000_000L) {
                        return@withContext null to
                                "Image is too large. Please select a smaller image."
                    }

                    // Reduce preview resolution to save memory
                    val options = BitmapFactory.Options()

                    var sampleSize = 1

                    while (
                        bounds.outWidth / sampleSize > 2048 ||
                        bounds.outHeight / sampleSize > 2048
                    ) {
                        sampleSize *= 2
                    }

                    options.inSampleSize = sampleSize

                    // Decode the actual image
                    val imageStream =
                        context.contentResolver.openInputStream(uri)
                            ?: return@withContext null to
                                    "Cannot read the selected image."

                    val bitmap = imageStream.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }

                    if (bitmap == null) {
                        return@withContext null to
                                "Unable to decode this image. Please try another."
                    }

                    bitmap to null
                }

            previewBitmap = result.first

            if (result.first != null) {

                imageValid = true

                status =
                    "Image validated successfully."

            } else {

                imageValid = false

                status = result.second
                    ?: "Image validation failed. Please select another image."
            }

        } catch (e: Exception) {

            previewBitmap = null
            imageValid = false

            status = "Image validation failed: ${e.localizedMessage
                ?: "Please try another image."}"

        } finally {

            previewLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            "BrainTumour AI",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Welcome, $email",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        // MRI IMAGE CARD
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "MRI Image Selection",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(12.dp))

                // IMAGE PREVIEW
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    when {

                        previewLoading -> {
                            CircularProgressIndicator()
                        }

                        previewBitmap != null -> {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Validated MRI image preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        else -> {
                            Text("Your MRI image will appear here")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    selectedImage?.lastPathSegment
                        ?: "No image selected",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                // CHOOSE OR CHANGE IMAGE
                Button(
                    onClick = {
                        imagePicker.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !previewLoading && !saving
                ) {
                    Text(
                        if (selectedImage == null) {
                            "Choose MRI Image"
                        } else {
                            "Change MRI Image"
                        }
                    )
                }

                // REMOVE IMAGE
                if (selectedImage != null) {

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            selectedImage = null
                            previewBitmap = null
                            imageValid = false
                            status = "Image removed successfully."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving
                    ) {
                        Text("Remove Image")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (imageValid) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(Modifier.height(16.dp))

        // SAVE DEMO RECORD
        Button(
            onClick = {

                val uid = auth.currentUser?.uid
                val uri = selectedImage

                if (uid == null) {

                    status = "Please sign in again."

                } else if (uri == null) {

                    status = "Please select an image first."

                } else if (!imageValid || previewBitmap == null) {

                    status = "Please select a valid image before saving."

                } else {

                    saving = true

                    val record = hashMapOf(
                        "createdAt" to Date(),
                        "status" to "NOT_ANALYSED",
                        "message" to "Image validated. AI model is not integrated.",
                        "selectedImageUri" to uri.toString()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("analyses")
                        .add(record)
                        .addOnSuccessListener {

                            saving = false

                            status =
                                "Validated image record saved. No tumour prediction has been performed."
                        }
                        .addOnFailureListener { error ->

                            saving = false

                            status =
                                "Could not save record: ${error.localizedMessage}"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving &&
                    !previewLoading &&
                    selectedImage != null &&
                    imageValid &&
                    previewBitmap != null
        ) {
            Text(
                if (saving) "Saving..."
                else "Save Demo Analysis Record"
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View History")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Educational prototype only. No medical diagnosis is produced. Do not use this application to make healthcare decisions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}


// HISTORY SCREEN
@Composable
private fun HistoryScreen(
    onBack: () -> Unit
) {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var records by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var message by remember {
        mutableStateOf("Loading history...")
    }

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

                        val status = document.getString("status")
                            ?: "Unknown status"

                        val date = document.getDate("createdAt")?.let {
                            DateFormat.getDateTimeInstance().format(it)
                        } ?: "Date unavailable"

                        "$status • $date"
                    }

                    message = if (records.isEmpty()) {
                        "No saved records yet."
                    } else {
                        ""
                    }
                }
                .addOnFailureListener { error ->

                    message = error.localizedMessage
                        ?: "Could not load history."
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            "Analysis history",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        if (message.isNotBlank()) {
            Text(message)
        }

        records.forEach { item ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    item,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back to dashboard")
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Saved entries are demo records, not medical results.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}


// SPLASH SCREEN
@Composable
private fun SplashScreen() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102D40)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "🧠",
                fontSize = 75.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "BrainTumour AI",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "AI-Powered MRI Image Analysis",
                fontSize = 14.sp,
                color = Color.LightGray
            )

            Spacer(Modifier.height(30.dp))

            CircularProgressIndicator(
                color = Color(0xFF20B8A6)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Loading...",
                color = Color.White
            )
        }
    }
}
