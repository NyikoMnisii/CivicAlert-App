package ci

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.civicalertoriginal.Components.DescriptionTextFields
import com.example.civicalertoriginal.Components.ExposedDropdownMenuBox
import com.example.civicalertoriginal.Components.LocationTextFields
import com.example.civicalertoriginal.Components.PictureTextFields
import com.example.civicalertoriginal.Components.ReportDescriptionText
import com.example.civicalertoriginal.Components.SubmitButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Reports(
    val incidentType: String = "",
    var location: String = "",
    val description: String = "",
    val dateTime: String = "",
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MakeReports(navController: NavController) {
    var isVisible by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val sharedP = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    locationText = sharedP.getString("incidentAddress", "").toString()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Surface(color = Color.White) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(1000, easing = LinearEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(1000, easing = LinearEasing)
            )
        ) {
            // Separate the Close Action
            AnimatedMakeReports(
                navController = navController,
                locationText = locationText,
                onLocationChange = { locationText = it },
                onClose = { navController.popBackStack() }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnimatedMakeReports(
    navController: NavController,
    locationText: String,
    onLocationChange: (String) -> Unit,
    onClose: () -> Unit
) {
    // State Management
    var description by remember { mutableStateOf("") }
    var pictureUri: Uri? by remember { mutableStateOf(null) }
    var selectedIncident by remember { mutableStateOf("Water") }

    // Firebase References
    val database = Firebase.database.reference.child("Make Report Instance")
    val storage = Firebase.storage.reference
    val context = LocalContext.current
    val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    Column(
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onClose() },
                tint = Color.Red
            )
            Spacer(modifier = Modifier.size(25.dp))
            Text(
                text = "Make A Report",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // Incident Type Dropdown
        ReportDescriptionText("Incident", "Choose Incident type")
        ExposedDropdownMenuBox(
            selectedIncident = selectedIncident,
            onIncidentSelected = { selectedIncident = it }
        )

        // Location Section
        ReportDescriptionText("Location", "Share the location of the incident")
        LocationTextFields(
            value = locationText,
            onChange = onLocationChange,
            fieldLabel = "Enter location",
            navController = navController
        )

        // Photo and Description
        ReportDescriptionText("Photos (Optional)", "Take photos of the incident you are reporting")
        PictureTextFields(value = pictureUri?.toString() ?: "", onChange = { pictureUri = Uri.parse(it) })

        ReportDescriptionText("Report Description *", "Short Description of the incident")
        DescriptionTextFields(value = description, onChange = { description = it }, fieldLabel = "Describe the incident")

        // Create Report Object
        val userReport = Reports(
            incidentType = selectedIncident,
            location = locationText,
            description = description,
            dateTime = currentDateTime
        )

        // Submit Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SubmitButton("Submit") {
                if (description.isBlank()) {
                    Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                } else {
                    handleReportSubmission(
                        report = userReport,
                        photoUri = pictureUri,
                        database = database,
                        storage = storage,
                        context = context,
                        onSuccess = {
                            Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                            navController.navigate("Dashboard")
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}

/**
 * Handles the submission of a report, including uploading photos and saving the report.
 */
fun handleReportSubmission(
    report: Reports,
    photoUri: Uri?,
    database: DatabaseReference,
    storage: StorageReference,
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    if (photoUri != null) {
        val photoRef = storage.child("incident_photos/${System.currentTimeMillis()}.jpg")
        photoRef.putFile(photoUri)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    val updatedReport = report.copy(description = "${report.description}\nPhoto: $uri")
                    saveReportToDatabase(updatedReport, database, onSuccess, onFailure)
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Photo upload failed") }
    } else {
        saveReportToDatabase(report, database, onSuccess, onFailure)
    }
}

/**
 * Saves the report to Firebase Realtime Database.
 */
fun saveReportToDatabase(
    report: Reports,
    database: DatabaseReference,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val reportId = database.push().key ?: return onFailure("Could not generate report ID")
    database.child(reportId).setValue(report)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it.message ?: "Failed to save report") }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun MakeReportsPreview() {
    val navController = rememberNavController()
    MakeReports(navController)
}
