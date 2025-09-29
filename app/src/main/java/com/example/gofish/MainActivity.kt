package com.example.gofish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gofish.ui.theme.VirusTotalViewModel
import com.example.gofish.util.PatternAnalysisResult
import com.example.gofish.util.PatternLinkDetector

// Navigation Sealed Class
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "GoFish", Icons.Filled.Security)
    object History : Screen("history", "SCAN_LOG", Icons.Filled.History)
    object Profile : Screen("profile", "USER_ID", Icons.Filled.Person)
}

// Define detection method constants
private const val METHOD_PATTERN = "Pattern Analysis"
private const val METHOD_API = "VirusTotal API"

// Cyber Theme Colors
val CyberDarkBackground = Color(0xFF0D1117)
val CyberCardBackground = Color(0xFF161B22)
val CyberTextPrimary = Color(0xFFC9D1D9)
val CyberTextSecondary = Color(0xFF8B949E)
val CyberAccentCyan = Color(0xFF58A6FF)
val CyberAccentGreen = Color(0xFF3FB950)
val CyberAccentRed = Color(0xFFF85149)
val CyberAccentYellow = Color(0xFFDBAB0A)
val CyberBorderColor = CyberAccentCyan.copy(alpha = 0.3f)
val CyberMutedBorderColor = Color.DarkGray.copy(alpha = 0.5f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = CyberDarkBackground) {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    Scaffold(
        containerColor = CyberDarkBackground,
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.History,
        Screen.Profile
    )
    NavigationBar(
        containerColor = CyberCardBackground.copy(alpha = 0.95f),
        contentColor = CyberTextSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier.height(65.dp).border(BorderStroke(1.dp, CyberBorderColor.copy(alpha = 0.5f)))
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title, tint = if (currentRoute == screen.route) CyberAccentCyan else CyberTextSecondary) },
                label = { Text(screen.title, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (currentRoute == screen.route) CyberAccentCyan else CyberTextSecondary) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberAccentCyan,
                    unselectedIconColor = CyberTextSecondary,
                    selectedTextColor = CyberAccentCyan,
                    unselectedTextColor = CyberTextSecondary,
                    indicatorColor = CyberAccentCyan.copy(alpha = 0.1f)
                ),
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.History.route) { HistoryScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}

@Composable
fun HomeScreen(virusTotalViewModel: VirusTotalViewModel = viewModel()) {
    // This is the content previously in GoFishApp
    var inputText by remember { mutableStateOf("") }

    val apiScanResultFromVM by virusTotalViewModel.scanResult.collectAsState()
    val isLoadingApi by virusTotalViewModel.isLoading.collectAsState()
    val apiError by virusTotalViewModel.error.collectAsState()

    var selectedMethod by remember { mutableStateOf(METHOD_API) }
    var patternAnalysisResult by remember { mutableStateOf<PatternAnalysisResult?>(null) }
    var isLoadingPattern by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val scannedUrl = inputText.trim()
    val isLoading = isLoadingApi || isLoadingPattern

    val placeholderRiskLevel = "MEDIUM RISK"
    val placeholderPotentialThreat = "Potential Threat Detected"
    val placeholderThreatDetectionRate = 0.3f
    val placeholderThreatSummary = listOf("Example: High number of redirects", "Example: Shortened URL")
    val placeholderSecurityRecommendation = "Exercise caution. Verify the source."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Screen.Home.icon, "Security Shield", modifier = Modifier.size(50.dp), tint = CyberAccentCyan)
        Spacer(Modifier.height(8.dp))
        Text(Screen.Home.title, style = MaterialTheme.typography.headlineSmall.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
        Text("Threat Analysis Terminal", style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))

        Spacer(Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CyberCardBackground),
            border = BorderStroke(1.dp, CyberBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Terminal, "Selector Icon", tint = CyberAccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SELECT_DETECTION_MODE", style = MaterialTheme.typography.labelLarge.copy(color = CyberAccentCyan, fontFamily = FontFamily.Monospace))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetectionMethodButton(
                        modifier = Modifier.weight(1f),
                        methodName = "[LOCAL] PATTERN_SCAN",
                        description = "Offline • Instant Analysis",
                        isSelected = selectedMethod == METHOD_PATTERN,
                        onClick = {
                            selectedMethod = METHOD_PATTERN
                            patternAnalysisResult = null
                        }
                    )
                    DetectionMethodButton(
                        modifier = Modifier.weight(1f),
                        methodName = "[API] VIRUSTOTAL_V3",
                        description = "70+ Vendors • Deep Scan",
                        icon = Icons.Filled.Storage,
                        isSelected = selectedMethod == METHOD_API,
                        onClick = {
                            selectedMethod = METHOD_API
                            patternAnalysisResult = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
             modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = CyberCardBackground),
            border = BorderStroke(1.dp, CyberBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Link, "URL Icon", tint = CyberAccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TARGET_URL", style = MaterialTheme.typography.labelLarge.copy(color = CyberAccentCyan, fontFamily = FontFamily.Monospace))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it; generalError = null },
                        label = { Text("https://suspicious-site.com", fontFamily = FontFamily.Monospace, color = CyberTextSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = generalError != null,
                        textStyle = LocalTextStyle.current.copy(color = CyberAccentGreen, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberAccentCyan,
                            unfocusedBorderColor = CyberMutedBorderColor,
                            focusedContainerColor = CyberDarkBackground.copy(alpha=0.5f),
                            unfocusedContainerColor = CyberDarkBackground.copy(alpha=0.5f),
                            cursorColor = CyberAccentCyan,
                            focusedLabelColor = CyberAccentCyan,
                            unfocusedLabelColor = CyberTextSecondary,
                            errorBorderColor = CyberAccentRed,
                            errorLabelColor = CyberAccentRed,
                            errorCursorColor = CyberAccentRed
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputText.isBlank()) {
                                generalError = "TARGET_URL cannot be empty."
                                return@Button
                            }
                            generalError = null
                            patternAnalysisResult = null
                            if (selectedMethod == METHOD_PATTERN) {
                                isLoadingPattern = true
                                patternAnalysisResult = PatternLinkDetector.analyzeUrl(scannedUrl)
                                isLoadingPattern = false
                            } else {
                                virusTotalViewModel.scanUrlWithVirusTotal(scannedUrl)
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberAccentCyan,
                            contentColor = CyberDarkBackground,
                            disabledContainerColor = CyberTextSecondary.copy(alpha = 0.3f),
                            disabledContentColor = CyberTextSecondary.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyberDarkBackground)
                        } else {
                            Icon(Icons.Filled.Search, "Scan URL", tint = CyberDarkBackground)
                            Spacer(Modifier.width(4.dp))
                            Text("EXECUTE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (generalError != null) {
                    Text(generalError!!, color = CyberAccentRed, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(start = 0.dp, top = 4.dp))
                }
            }
        }
        
        Spacer(Modifier.height(20.dp))

        val showResultsCard = patternAnalysisResult != null || apiScanResultFromVM != null || apiError != null || (isLoadingApi && scannedUrl.isNotBlank())

        if (showResultsCard) {
            val currentRiskLevel: String
            val currentPotentialThreat: String
            val currentScannedUrlToDisplay = scannedUrl.ifBlank { "N/A" }
            val currentApiStatus: String
            val currentThreatDetectionRate: Float
            val currentThreatSummary: List<String>
            val currentSecurityRecommendation: String
            val resultCardColor: Color
            val resultSideBarColor: Color

            if (selectedMethod == METHOD_PATTERN && patternAnalysisResult != null) {
                val result = patternAnalysisResult!!
                currentRiskLevel = result.riskLevelText
                currentPotentialThreat = if (result.isSuspicious) "Pattern Threat Detected" else "No Significant Pattern Threats"
                currentApiStatus = "[LOCAL] Pattern Analysis Result"
                currentThreatDetectionRate = result.riskScore
                currentThreatSummary = result.reasons
                currentSecurityRecommendation = if (result.isSuspicious) "Review URL carefully. Patterns suggest potential risk." else "URL appears safe based on patterns."
                resultCardColor = when (result.riskLevelText) {
                    "HIGH RISK" -> CyberAccentRed
                    "MEDIUM RISK" -> CyberAccentYellow
                    "LOW RISK" -> CyberAccentGreen
                    else -> CyberTextSecondary
                }
                resultSideBarColor = resultCardColor
            } else if (selectedMethod == METHOD_API && (apiScanResultFromVM != null || apiError != null)) {
                if (apiError != null) {
                    currentRiskLevel = "ERROR"
                    currentPotentialThreat = "Could not scan URL via API."
                    currentApiStatus = "[API] Error"
                    currentThreatDetectionRate = 0f
                    currentThreatSummary = listOf(apiError ?: "Unknown API error")
                    currentSecurityRecommendation = "Check connection or URL and try again."
                    resultCardColor = CyberAccentRed
                } else {
                    currentRiskLevel = placeholderRiskLevel 
                    currentPotentialThreat = placeholderPotentialThreat 
                    currentApiStatus = "[API] VirusTotal (Raw: ${apiScanResultFromVM?.take(30)}${if((apiScanResultFromVM?.length ?: 0)>30) "..." else ""})"
                    currentThreatDetectionRate = placeholderThreatDetectionRate 
                    currentThreatSummary = placeholderThreatSummary 
                    currentSecurityRecommendation = placeholderSecurityRecommendation 
                    resultCardColor = CyberAccentYellow 
                }
                resultSideBarColor = resultCardColor
            } else if (isLoadingApi && selectedMethod == METHOD_API) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Connecting to VirusTotal Matrix...", style = MaterialTheme.typography.titleMedium.copy(color = CyberAccentCyan, fontFamily = FontFamily.Monospace))
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(color = CyberAccentCyan)
                }
                return@Column
            } else {
                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = CyberCardBackground),
                border = BorderStroke(1.dp, CyberMutedBorderColor.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row {
                    Box(Modifier.width(6.dp).fillMaxHeight().background(resultSideBarColor))
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, "Risk Icon", tint = resultCardColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(currentRiskLevel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = resultCardColor, fontFamily = FontFamily.Monospace)
                                Text(currentPotentialThreat, fontSize = 14.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(currentScannedUrlToDisplay, style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace), modifier = Modifier.padding(vertical = 4.dp))
                        Text(currentApiStatus, style = MaterialTheme.typography.bodySmall.copy(color = CyberAccentCyan.copy(alpha=0.7f), fontFamily = FontFamily.Monospace), modifier = Modifier.padding(bottom=8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DETECTION_RATE:", style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${(currentThreatDetectionRate * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = resultCardColor, fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { currentThreatDetectionRate },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.small).padding(top = 4.dp),
                            color = resultCardColor,
                            trackColor = CyberDarkBackground.copy(alpha=0.8f)
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("THREAT_SUMMARY:", style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                        if (currentThreatSummary.isEmpty()) {
                             Text("• No specific threats identified by this method.", style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))
                        } else {
                            currentThreatSummary.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("SECURITY_RECOMMENDATION:", style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                        Text(currentSecurityRecommendation, style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { /* Share functionality */ },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAccentCyan.copy(alpha = 0.2f), contentColor = CyberAccentCyan),
                            border = BorderStroke(1.dp, CyberAccentCyan.copy(alpha = 0.5f))
                        ) {
                            Text("SHARE_ANALYSIS", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionMethodButton(
    modifier: Modifier = Modifier,
    methodName: String,
    description: String,
    icon: ImageVector? = null, 
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val targetBorderColor = if (isSelected) CyberAccentCyan else CyberMutedBorderColor
    val targetBackgroundColor = if (isSelected) CyberAccentCyan.copy(alpha = 0.15f) else CyberCardBackground.copy(alpha=0.5f)
    val targetTextColor = if (isSelected) CyberAccentCyan else CyberTextSecondary

    Button(
        onClick = onClick,
        modifier = modifier.height(IntrinsicSize.Min), 
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = targetBackgroundColor,
            contentColor = targetTextColor
        ),
        border = BorderStroke(1.dp, targetBorderColor),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, contentDescription = methodName, modifier = Modifier.size(16.dp), tint = targetTextColor)
                    Spacer(Modifier.width(6.dp))
                }
                Text(methodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, color = targetTextColor), maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = targetTextColor.copy(alpha=0.7f)), maxLines = 1)
        }
    }
}

@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Screen.History.icon, contentDescription = Screen.History.title, tint = CyberAccentCyan, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(16.dp))
        Text(Screen.History.title, style = MaterialTheme.typography.headlineMedium.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace))
        Spacer(Modifier.height(8.dp))
        Text("[ACCESSING SCAN LOGS...]", style = MaterialTheme.typography.bodyLarge.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))
        Text("Functionality pending system integration.", style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontFamily = FontFamily.Monospace))
        // TODO: Implement history list (e.g., from a ViewModel or database)
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Screen.Profile.icon, contentDescription = Screen.Profile.title, tint = CyberAccentCyan, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(16.dp))
        Text(Screen.Profile.title, style = MaterialTheme.typography.headlineMedium.copy(color = CyberTextPrimary, fontFamily = FontFamily.Monospace))
        Spacer(Modifier.height(16.dp))
        
        ProfileInfoRow(label = "USER_TAG:", value = "AGENT_CYBER_774")
        ProfileInfoRow(label = "EMAIL_ID:", value = "user774@securenet.local")
        ProfileInfoRow(label = "ACCESS_LVL:", value = "FIELD_OPERATIVE_II")
        ProfileInfoRow(label = "STATUS:", value = "ACTIVE_DUTY", valueColor = CyberAccentGreen)
        // TODO: Fetch and display actual user details
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, valueColor: Color = CyberTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "$label ", 
            style = MaterialTheme.typography.bodyLarge.copy(color = CyberAccentCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyLarge.copy(color = valueColor, fontFamily = FontFamily.Monospace)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun MainAppScreenPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = CyberDarkBackground) {
        MainAppScreen()
    }
}
