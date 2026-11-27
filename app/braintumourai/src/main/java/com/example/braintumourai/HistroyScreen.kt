
package com.example.braintumourai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat

private data class AnalysisHistoryItem(
    val id: String,
    val status: String,
    val date: String,
    val prediction: String?,
    val confidence: Double?,
    val message: String
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var records by remember {
        mutableStateOf<List<AnalysisHistoryItem>>(emptyList())
    }

    var message by remember {
        mutableStateOf("Loading history...")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            message = "Please sign in."
            loading = false
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

                        val prediction =
                            document.getString("predictedClass")

                        val confidence =
                            document.getDouble("confidence")

                        val recordMessage =
                            document.getString("message") ?: ""

                        AnalysisHistoryItem(
                            id = document.id,
                            status = status,
                            date = date,
                            prediction = prediction,
                            confidence = confidence,
                            message = recordMessage
                        )
                    }.reversed()

                    message = if (records.isEmpty()) {
                        "No saved records yet."
                    } else {
                        ""
                    }

                    loading = false
                }
                .addOnFailureListener { error ->
                    message = error.localizedMessage
                        ?: "Could not load history."
                    loading = false
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
            "Analysis History",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
        }

        if (message.isNotBlank()) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        records.forEach { item ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        "Analysis date",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        item.date,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Status: ${item.status}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (item.prediction != null) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Predicted class: ${item.prediction}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (item.confidence != null) {
                            Spacer(Modifier.height(4.dp))

                            Text(
                                "Model score: ${"%.2f".format(item.confidence * 100)}%"
                            )
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            "No prediction stored for this record.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (item.message.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            item.message,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Dashboard")
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Educational prototype only. Model outputs are experimental and are not medical diagnoses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
