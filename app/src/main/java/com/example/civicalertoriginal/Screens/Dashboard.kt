
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.civicalertoriginal.Components.CardButton
import com.example.civicalertoriginal.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(navController: NavController) {
    var recentReport by remember { mutableStateOf<IncidentReport?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // Fetch recent report and image URL when the composable is launched
    LaunchedEffect(Unit) {
        recentReport = fetchRecentReport()
        recentReport?.let {
            imageUrl = fetchImageUrl(it.refNumber) // Use `refNumber` for image fetching
        }
    }

    Surface(color = Color.White) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                ) {
                }
            }

            recentReport?.let { report ->
                item {
                    Column(modifier = Modifier.height(300.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Recently Reported Incident",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )

                                // Display the image or placeholder
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = imageUrl ?: R.drawable.photo // Placeholder image
                                    ),
                                    contentDescription = "Picture of reported incident",
                                    modifier = Modifier.height(150.dp)
                                )

                                Text(text = report.description, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(text = report.location, fontSize = 16.sp)
                                Text(text = report.dateTime, fontSize = 16.sp)
                                Text(text = report.incidentType, color = Color.Blue, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } ?: item {
                Text("No recent report available", fontWeight = FontWeight.Bold, color = Color.Gray  , fontSize = 20.sp)
            }

            // Other UI components
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CardButton(
                        iconRes = R.drawable.info,
                        label = "Report Incident",
                        onClick = { navController.navigate("makeReports") }
                    )
                    CardButton(
                        iconRes = R.drawable.clipboard,
                        label = "View Reports",
                        onClick = { navController.navigate("Viewreports") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CardButton(
                        iconRes = R.drawable.headphones,
                        label = "Help & Support",
                        onClick = { navController.navigate("helpSupport") }
                    )
                    CardButton(
                        iconRes = R.drawable.emergency_contacts,
                        label = "Emergency\n Contacts",
                        onClick = { navController.navigate("emergencyContacts") }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Fetch image URL from Firebase Storage
suspend fun fetchImageUrl(refNumber: String): String? {
    val storage = FirebaseStorage.getInstance()
    val photoRef = storage.reference.child("incident_photos/$refNumber.jpg")
    return try {
        photoRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.e("fetchImageUrl", "Error fetching image URL: $refNumber", e)
        null
    }
}

// Fetch the most recent report from Firebase Database
suspend fun fetchRecentReport(): IncidentReport? {
    val database = FirebaseDatabase.getInstance()
    val reportsRef = database.getReference("Make Report Instance")

    return try {
        val dataSnapshot = reportsRef.get().await()
        val reports = dataSnapshot.children.mapNotNull { snapshot ->
            val description = snapshot.child("description").getValue(String::class.java)
            val location = snapshot.child("location").getValue(String::class.java)
            val dateTime = snapshot.child("dateTime").getValue(String::class.java)
            val incidentType = snapshot.child("incidentType").getValue(String::class.java)
            val refNumber = snapshot.child("refNumber").getValue(String::class.java)

            if (description != null && location != null && dateTime != null && incidentType != null && refNumber != null) {
                IncidentReport(description, location, dateTime, incidentType, refNumber)
            } else {
                null
            }
        }

        reports.maxByOrNull { it.dateTime }
    } catch (e: Exception) {
        Log.e("fetchRecentReport", "Error fetching report", e)
        null
    }
}

// Incident report data class
data class IncidentReport(
    val description: String,
    val location: String,
    val dateTime: String,
    val incidentType: String,
    val refNumber: String
)


