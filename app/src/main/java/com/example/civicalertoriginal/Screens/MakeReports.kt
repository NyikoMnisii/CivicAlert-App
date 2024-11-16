package civicalertoriginal.Screen

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.civicalertoriginal.Components.DescriptionTextFields
import com.example.civicalertoriginal.Components.ExposedDropdownMenuBox
import com.example.civicalertoriginal.Components.LocationTextFields
import com.example.civicalertoriginal.Components.PictureTextFields
import com.example.civicalertoriginal.Components.ReportDescriptionText
import com.example.civicalertoriginal.Components.SubmitButton
import com.example.civicalertoriginal.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
data class Reports(
    val incidentType: String = "",
    val location: String = "",
    val description: String = "",
    val dateTime: String = "",
    val refNumber: String = "",
    val status: String = "",
    val userID: String = ""
)

class SharedPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reference_number_prefs", Context.MODE_PRIVATE)

    var increment: Int
        get() = prefs.getInt("increment", 0)
        set(value) {
            prefs.edit().putInt("increment", value).apply()
        }
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateReferenceNumber(context: Context): String {
    val sharedPrefs = SharedPrefs(context)
    val currentDateTime = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyMMdd")
    val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
    val datePart = currentDateTime.format(dateFormatter)
    val timePart = currentDateTime.format(timeFormatter)

    val incrementedPart = String.format("%04d", sharedPrefs.increment++)
    sharedPrefs.increment = sharedPrefs.increment // Save the updated increment value

    return "$datePart-$timePart-$incrementedPart"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MakeReports(navController: NavController) {
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Surface(color = Color.White) {
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: ""

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
            AnimatedMakeReports(navController, email) {
                isVisible = false
                navController.navigate("Dashboard")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnimatedMakeReports(navController: NavController, userEmail: String, onClose: () -> Unit) {
    val database = Firebase.database
    val myRef = database.getReference("Make Report Instance")
    var pictureUri: Uri? by remember { mutableStateOf(null) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    var showDialog by remember { mutableStateOf(false) }
    val referenceNumber = generateReferenceNumber(context)

    Column(
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 50.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "",
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

        var selectedIncident by remember { mutableStateOf("Water") }
        ExposedDropdownMenuBox(
            selectedIncident = selectedIncident,
            onIncidentSelected = { newIncident -> selectedIncident = newIncident })

        LocationTextFields(value = location, onChange = { location = it }, fieldLabel = "Enter location")

        ReportDescriptionText("Photos (Optional)", "Take photos of the incident you are reporting")
        PictureTextFields(value = pictureUri?.toString() ?: "", onChange = { pictureUri = Uri.parse(it) })

        ReportDescriptionText("Report Description *", "Short Description of the incident")
        DescriptionTextFields(value = description, onChange = { description = it }, fieldLabel = "Brief description")

        val userReport = Reports(
            incidentType = selectedIncident,
            location = location,
            description = description,
            dateTime = formattedDateTime,
            refNumber = referenceNumber,
            status = "Submitted",
            userID = userEmail
        )

        fun saveReport(report: Reports) {
            myRef.child(report.refNumber).setValue(report).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showDialog = true
                } else {
                    task.exception?.let {
                        println("Error saving report: ${it.message}")
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SubmitButton("Submit") {
                if (description.isBlank()) {
                    Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                } else {
                    saveReport(userReport)
                }
            }
        }

        if (showDialog) {
            SuccessDialog(referenceNumber = referenceNumber) {
                showDialog = false
                onClose()
            }
        }
    }
}

@Composable
fun SuccessDialog(referenceNumber: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB2F4B8))
            ) {
                Text("DONE", color = Color.Black)
            }
        },
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE0F7EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(60.dp)
                    )
                }
                Text(
                    text = "Report Successfully Submitted",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "Reference No: $referenceNumber",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    )
}