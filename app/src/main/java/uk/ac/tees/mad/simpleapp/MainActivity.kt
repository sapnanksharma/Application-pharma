package uk.ac.tees.mad.simpleapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

data class MedicineInfo(
    val name: String,
    val type: String,
    val description: String,
    val whenToUse: String,
    val howToUse: String,
    val caution: String,
    val color: Color
)

object MedicineRepository {
    private val medicines = listOf(
        MedicineInfo(
            name = "Paracetamol",
            type = "Pain relief",
            description = "Paracetamol is used to reduce fever and relieve mild to moderate pain.",
            whenToUse = "For headache, fever, toothache, sore throat, cold symptoms, and general body pain.",
            howToUse = "Take as directed on the packet or by your clinician. Do not exceed the maximum daily dose.",
            caution = "Avoid taking more than the recommended dose. Do not combine with other medicines containing paracetamol.",
            color = Color(0xFF7ED957)
        ),
        MedicineInfo(
            name = "Ibuprofen",
            type = "Anti-inflammatory",
            description = "Ibuprofen helps reduce pain, swelling, and inflammation caused by injury or joint discomfort.",
            whenToUse = "For back pain, joint pain, muscle pain, swelling, and mild inflammation.",
            howToUse = "Take with food or milk if advised. Follow the dose on the package or as recommended by a pharmacist.",
            caution = "Avoid if you have stomach ulcers or kidney problems unless directed by a healthcare professional.",
            color = Color(0xFF4AA3FF)
        ),
        MedicineInfo(
            name = "Amoxicillin",
            type = "Antibiotic",
            description = "Amoxicillin is an antibiotic used to treat certain bacterial infections.",
            whenToUse = "For bacterial ear, throat, chest, or other prescribed infections.",
            howToUse = "Take at regular intervals and complete the prescribed course even if symptoms improve early.",
            caution = "This medicine requires a prescription. Do not use it without medical guidance.",
            color = Color(0xFFFF9F43)
        ),
        MedicineInfo(
            name = "Cetirizine",
            type = "Antihistamine",
            description = "Cetirizine helps reduce allergy symptoms like sneezing, itching, watery eyes, and runny nose.",
            whenToUse = "For hay fever, seasonal allergies, itchy eyes, runny nose, and mild allergy symptoms.",
            howToUse = "Take as directed, usually once a day. It can be taken with or without food.",
            caution = "Avoid if you are allergic to cetirizine or similar medicines. Seek advice if you are pregnant or breastfeeding.",
            color = Color(0xFFB984FF)
        )
    )

    fun searchByName(query: String): MedicineInfo? {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return null
        return medicines.firstOrNull { it.name.equals(cleaned, ignoreCase = true) }
            ?: medicines.firstOrNull { it.name.contains(cleaned, ignoreCase = true) }
    }

    fun searchBySymptoms(symptomText: String): MedicineInfo? {
        val text = symptomText.lowercase()
        return when {
            text.contains("allergy") || text.contains("itch") || text.contains("runny nose") || text.contains("hay fever") -> medicines.first { it.name.equals("Cetirizine", ignoreCase = true) }
            text.contains("infection") || text.contains("sore throat") || text.contains("ear") || text.contains("bacterial") -> medicines.first { it.name.equals("Amoxicillin", ignoreCase = true) }
            text.contains("headache") || text.contains("fever") || text.contains("cold") || text.contains("tooth") -> medicines.first { it.name.equals("Paracetamol", ignoreCase = true) }
            text.contains("pain") || text.contains("swelling") || text.contains("inflammation") || text.contains("joint") -> medicines.first { it.name.equals("Ibuprofen", ignoreCase = true) }
            else -> medicines.firstOrNull()
        }
    }

    fun suggestions(): List<String> = medicines.map { it.name }
}

class AppAuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("pharma_auth", Context.MODE_PRIVATE)
    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth?.currentUser != null || prefs.getBoolean("local_logged_in", false)
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            onResult(false, "Please enter email and password.")
            return
        }

        if (trimmedPassword.length < 6) {
            onResult(false, "Password must be at least 6 characters.")
            return
        }

        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("local_logged_in", true).apply()
                        onResult(true, "")
                    } else {
                        val error = task.exception?.message ?: "Authentication failed."
                        onResult(false, error)
                    }
                }
        } else {
            prefs.edit().putBoolean("local_logged_in", true).apply()
            onResult(true, "")
        }
    }

    fun logout() {
        firebaseAuth?.signOut()
        prefs.edit().putBoolean("local_logged_in", false).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleAppTheme {
                val context = LocalContext.current
                val authManager = remember { AppAuthManager(context) }
                AppNavigation(authManager)
            }
        }
    }
}

@Composable
fun AppNavigation(authManager: AppAuthManager) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (authManager.isLoggedIn()) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                authManager = authManager,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                authManager = authManager,
                onContactSupport = { navController.navigate("contact") },
                onSearchByName = { navController.navigate("search_name") },
                onSearchBySymptoms = { navController.navigate("search_symptoms") },
                onLogout = {
                    authManager.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("search_name") {
            SearchByNameScreen(
                onSearch = { medicineName ->
                    val medicine = MedicineRepository.searchByName(medicineName)
                    if (medicine != null) {
                        navController.navigate("details/${medicine.name}")
                    } else {
                        navController.navigate("details/Medicine")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("search_symptoms") {
            SearchBySymptomsScreen(
                onSearch = { symptom ->
                    val medicine = MedicineRepository.searchBySymptoms(symptom)
                    if (medicine != null) {
                        navController.navigate("details/${medicine.name}")
                    } else {
                        navController.navigate("details/Medicine")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("details/{medicineName}") { backStackEntry ->
            val medicineName = backStackEntry.arguments?.getString("medicineName") ?: "Medicine"
            val medicine = MedicineRepository.searchByName(medicineName)
                ?: MedicineRepository.searchBySymptoms(medicineName)
                ?: MedicineInfo(
                    name = medicineName,
                    type = "General medicine",
                    description = "Please confirm with a pharmacist or clinician before use.",
                    whenToUse = "Use according to your symptoms and healthcare advice.",
                    howToUse = "Follow the package instructions carefully and do not exceed the dose limit.",
                    caution = "Only use when appropriate and if it matches your condition.",
                    color = Color(0xFF7EC8FF)
                )

            DetailsScreen(
                medicine = medicine,
                onContactSupport = { navController.navigate("contact") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("contact") {
            ContactSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LoginScreen(
    authManager: AppAuthManager,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0xFF7A1FA2), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💊",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email or username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFB00020),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    authManager.login(email, password) { success, message ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            errorMessage = message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1FA2))
            ) {
                Text(if (isLoading) "Logging in..." else "Login")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Forgot password?",
                color = Color(0xFF6B6B6B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HomeScreen(
    authManager: AppAuthManager,
    onContactSupport: () -> Unit,
    onSearchByName: () -> Unit,
    onSearchBySymptoms: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F7F7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Logout")
                }
            }

            Text(
                text = "Welcome to Pharma Care",
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                color = Color(0xFF5A5A5A)
            )

            HomeOptionCard(
                title = "Contact Support Team",
                subtitle = "Talk to our help desk for any issue.",
                color = Color(0xFF7A1FA2),
                onClick = onContactSupport
            )

            HomeOptionCard(
                title = "Search Medicine by Name",
                subtitle = "Find medicine details quickly.",
                color = Color(0xFF2E8B57),
                onClick = onSearchByName
            )

            HomeOptionCard(
                title = "Search Medicine by Symptoms",
                subtitle = "Check which medicine matches your condition.",
                color = Color(0xFF1E88E5),
                onClick = onSearchBySymptoms
            )
        }
    }
}

@Composable
fun HomeOptionCard(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, RoundedCornerShape(50f))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F1F1F)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF626262),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SearchByNameScreen(onSearch: (String) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Search by Medicine Name",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D2A39)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Medicine name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1FA2))
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Suggestions: ${MedicineRepository.suggestions().joinToString(", ")}",
                color = Color(0xFF4B5563)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E8F0), contentColor = Color(0xFF1D2A39))
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
fun SearchBySymptomsScreen(onSearch: (String) -> Unit, onBack: () -> Unit) {
    var symptom by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Search by Symptoms",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D2A39)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = symptom,
                onValueChange = { symptom = it },
                label = { Text("Describe your symptoms") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { if (symptom.isNotBlank()) onSearch(symptom) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("Find medicine")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Try: fever, headache, allergy, pain, sore throat, infection",
                color = Color(0xFF4B5563)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E8F0), contentColor = Color(0xFF1D2A39))
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
fun DetailsScreen(medicine: MedicineInfo, onContactSupport: () -> Unit, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F9FC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Medicine Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF17314F)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 16.dp)
                    .background(
                        Brush.linearGradient(listOf(medicine.color, medicine.color.copy(alpha = 0.75f))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = medicine.type,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Text(
                text = medicine.description,
                color = Color(0xFF2F3B4D),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            DetailSection(title = "Condition used for", value = medicine.whenToUse)
            DetailSection(title = "How to use", value = medicine.howToUse)
            DetailSection(title = "Caution", value = medicine.caution)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContactSupport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1FA2))
            ) {
                Text("Contact support")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF2FF), contentColor = Color(0xFF17314F))
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
fun DetailSection(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF17314F)
        )
        Text(
            text = value,
            color = Color(0xFF465566),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ContactSupportScreen(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F7F7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Contact Support",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D2A39)
            )

            Spacer(modifier = Modifier.height(18.dp))

            ContactCard(title = "Phone", value = "+44 7700 900123")
            ContactCard(title = "Email", value = "support@pharmacare.com")
            ContactCard(title = "WhatsApp", value = "+44 7700 900123")
            ContactCard(title = "Office Hours", value = "Mon - Sat, 9:00 AM - 6:00 PM")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1FA2))
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
fun ContactCard(title: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6B7280)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SimpleAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        content = content
    )
}
