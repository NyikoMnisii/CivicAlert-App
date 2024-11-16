package com.example.civicalertoriginal.Screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.UUID

@Composable
fun ImageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("No Image Selected")
                }
            }

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectImageFromGallery(context) { uri -> selectedImageUri = uri } }) {
                    Text("Select Image")
                }
                Button(
                    onClick = {
                        isUploading = true
                        selectedImageUri?.let { uri ->
                            uploadImageToFirebase(uri) { imageName ->
                                isUploading = false
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    "imageName",
                                    imageName
                                )
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = selectedImageUri != null && !isUploading
                ) {
                    Text(if (isUploading) "Uploading..." else "Upload Image")
                }
            }
        }
    }
}

private fun uploadImageToFirebase(uri: Uri, onComplete: (String) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageName = "images/${UUID.randomUUID()}.jpg"
    val imageRef = storageRef.child(imageName)

    imageRef.putFile(uri)
        .addOnSuccessListener {
            onComplete(imageName)
        }
        .addOnFailureListener {
            println("Error uploading image: ${it.message}")
        }
}

// Function to select image from gallery
private fun selectImageFromGallery(context: Context, onImageSelected: (Uri) -> Unit) {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    (context as Activity).startActivityForResult(intent, 100)
}
