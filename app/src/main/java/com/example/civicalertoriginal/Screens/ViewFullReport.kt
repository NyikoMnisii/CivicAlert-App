package com.example.civicalertoriginal.Screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.civicalertoriginal.Components.ReportDescriptionText
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage


data class getReport(
    var incidentType: String = "",
    var description: String = "",
    var dateTime: String = "",
    var location: String = "",
    var refNumber: String = "",
    var status: String = ""

)


@Composable
fun LoadUploadedImage(refNumber: String) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val storage = FirebaseStorage.getInstance()

    // Storage path using refNumber
    val photoRef = storage.reference.child("incident_photos/$refNumber.jpg")

    // Fetch download URL asynchronously
    LaunchedEffect(refNumber) {
        photoRef.downloadUrl.addOnSuccessListener { uri ->
            // Log the URL for debugging
            Log.d("LoadUploadedImage", "Image URL: $uri")
            imageUrl = uri.toString()
        }.addOnFailureListener { exception ->
            // Log failure for debugging
            imageUrl = null // Ensure that the imageUrl is set to null on failure
        }
    }

    // Display image or loading text
    if (imageUrl != null) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = "Incident picture",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(250.dp) // Add height to ensure space is allocated for the image
        )
    } else {
        Text(
            text = "Loading image...",
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 16.sp
        )
    }
}


@Composable
fun ViewFullReport(navController: NavController, reportId: String) {
    val report = remember { mutableStateOf(getReport()) }
    LaunchedEffect(reportId) {
        val database = Firebase.database
        val myRef = database.getReference("Make Report Instance").child(reportId)
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchReport = snapshot.getValue(getReport::class.java)
                fetchReport?.let { report.value = it }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation error (optional)
            }
        })
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize().padding(10.dp)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "View Incident",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 35.sp,
                    fontFamily = FontFamily.Default,
                    fontStyle = FontStyle.Normal
                )
            }

            item {
                Row {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "",
                        modifier = Modifier.clickable { navController.navigate("Viewreports") }
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    Text(text = "Back")
                }
            }

            item { Spacer(modifier = Modifier.size(25.dp)) }

            item {
                LoadUploadedImage(refNumber = report.value.refNumber)
            }

            item { Spacer(modifier = Modifier.size(25.dp)) }

            item { ReportDescriptionText(value1 = "Incident Type:", value = report.value.incidentType) }
            item { Spacer(modifier = Modifier.size(8.dp)) }
            item { ReportDescriptionText(value1 = "Incident Description:", value = report.value.description) }
            item { Spacer(modifier = Modifier.size(8.dp)) }
            item { ReportDescriptionText(value1 = "Status:", value = report.value.status) }
            item { Spacer(modifier = Modifier.size(8.dp)) }
            item { ReportDescriptionText(value1 = "Location:", value = report.value.location) }
            item { Spacer(modifier = Modifier.size(8.dp)) }
            item { ReportDescriptionText(value1 = "Reference Number:", value = report.value.refNumber) }
        }
    }
}
