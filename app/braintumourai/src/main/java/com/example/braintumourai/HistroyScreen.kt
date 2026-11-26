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

// HISTORY SCREEN
@Composable
fun HistoryScreen(
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
