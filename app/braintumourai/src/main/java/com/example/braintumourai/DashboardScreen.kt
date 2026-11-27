
package com.example.braintumourai

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun DashboardScreen(
    email: String,
    onOpenHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dashboardViewModel: DashboardViewModel = viewModel()

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

    var analyzing by remember {
        mutableStateOf(false)
    }

    var prediction by remember {
        mutableStateOf<TumourPrediction?>(null)
    }

    // IMAGE PICKER
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImage = uri
        previewBitmap = null
        imageValid = false
        prediction = null

        dashboardViewModel.status = if (uri != null) {
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

                    val mimeType = context.contentResolver.getType(uri)

                    if (mimeType != null &&
                        !mimeType.startsWith("image/")
                    ) {
                        return@withContext null to
                                "Unsupported file type. Please select an image."
                    }

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

                    val totalPixels =
                        bounds.outWidth.toLong() * bounds.outHeight.toLong()

                    if (totalPixels > 100_000_000L) {
                        return@withContext null to
                                "Image is too large. Please select a smaller image."
                    }

                    val options = BitmapFactory.Options()
                    var sampleSize = 1

                    while (
                        bounds.outWidth / sampleSize > 2048 ||
                        bounds.outHeight / sampleSize > 2048
                    ) {
                        sampleSize *= 2
                    }

                    options.inSampleSize = sampleSize

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
                dashboardViewModel.status =
                    "Image validated successfully."
            } else {
                imageValid = false
                dashboardViewModel.status = result.second
                    ?: "Image validation failed."
            }

        } catch (e: Exception) {
            previewBitmap = null
            imageValid = false

            dashboardViewModel.status =
                "Image validation failed: ${e.localizedMessage ?: "Please try another image."}"

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
                                contentDescription = "MRI image preview",
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
                    enabled = !previewLoading && !analyzing
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
                            prediction = null

                            dashboardViewModel.status =
                                "Image removed successfully."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !analyzing
                    ) {
                        Text("Remove Image")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            dashboardViewModel.status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        // ANALYSE MRI BUTTON
        Button(
            onClick = {
                val uid = auth.currentUser?.uid
                val bitmapToAnalyse = previewBitmap
                val uri = selectedImage

                if (uid == null) {
                    dashboardViewModel.status =
                        "Please sign in again."
                } else if (uri == null || bitmapToAnalyse == null || !imageValid) {
                    dashboardViewModel.status =
                        "Please select a valid MRI image first."
                } else {

                    analyzing = true
                    prediction = null

                    dashboardViewModel.status =
                        "Loading CNN model and analysing image..."

                    scope.launch {
                        try {
                            // Run TensorFlow Lite away from the UI thread.
                            val result = withContext(Dispatchers.IO) {

                                val classifier =
                                    BrainTumourClassifier(context)

                                try {
                                    classifier.classify(bitmapToAnalyse)
                                } finally {
                                    classifier.close()
                                }
                            }

                            prediction = result

                            dashboardViewModel.status =
                                "Analysis completed. Saving result..."

                            // Save experimental prediction to Firestore.
                            val record = hashMapOf(
                                "createdAt" to Date(),
                                "status" to "EXPERIMENTAL_RESULT",
                                "message" to "Experimental CNN model output. Not a medical diagnosis.",
                                "selectedImageUri" to uri.toString(),
                                "predictedClass" to result.label,
                                "confidence" to result.confidence.toDouble()
                            )

                            db.collection("users")
                                .document(uid)
                                .collection("analyses")
                                .add(record)
                                .addOnSuccessListener {
                                    analyzing = false
                                    dashboardViewModel.status =
                                        "Experimental result saved to Firebase History."
                                }
                                .addOnFailureListener { error ->
                                    analyzing = false
                                    dashboardViewModel.status =
                                        "Prediction completed, but saving failed: ${error.localizedMessage}"
                                }

                        } catch (e: Exception) {
                            analyzing = false
                            dashboardViewModel.status =
                                "Analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !analyzing &&
                    !previewLoading &&
                    selectedImage != null &&
                    imageValid &&
                    previewBitmap != null
        ) {
            Text(
                if (analyzing) {
                    "Analysing..."
                } else {
                    "Analyse MRI"
                }
            )
        }

        // ANALYSIS RESULT
        if (prediction != null) {

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        "CNN Model Result",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Predicted class: ${prediction!!.label}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Model score: ${"%.2f".format(prediction!!.confidence * 100)}%"
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "This is an experimental model output, not a medical diagnosis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth(),
            enabled = !analyzing
        ) {
            Text("View History")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            enabled = !analyzing
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
