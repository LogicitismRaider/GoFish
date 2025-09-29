package com.example.gofish

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // This import should be correct if lifecycle-viewmodel-compose is properly synced
import com.example.gofish.ui.theme.OpenAiViewModel // Added correct import
import com.example.gofish.ui.theme.GoFIshTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoFIshTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GoFishApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GoFishApp(modifier: Modifier = Modifier, openAiViewModel: OpenAiViewModel = viewModel()) {
    val verificationResult by openAiViewModel.verificationResult.collectAsState()
    val isLoading by openAiViewModel.isLoading.collectAsState()
    val error by openAiViewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("You are a helpful assistant. Check if the following text contains any suspicious characteristics often found in phishing attempts. Do not click any links. Just analyze the text. Be concise.") }
    var localScanStatus by remember { mutableStateOf("") }
    var localScanExplanation by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("GoFish AI Scanner", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Paste message, email, or URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text("System Prompt (Instructions for AI)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (inputText.isBlank()) {
                    localScanStatus = "Info"
                    localScanExplanation = "Please enter some text to scan."
                    return@Button
                }

                // Basic local check before calling OpenAI
                if (!inputText.contains("http", ignoreCase = true) && !inputText.contains("https", ignoreCase = true)) {
                    localScanStatus = "Safe"
                    localScanExplanation = "No URL detected. Text seems safe based on local check."
                    return@Button
                }

                // Simulated fallback scanning logic (until API is implemented)
                coroutineScope.launch {
                    localScanStatus = "Scanning..."
                    localScanExplanation = "Checking with local rules."
                    if (inputText.contains("evil.com")) {
                        localScanStatus = "High Risk"
                        localScanExplanation = "This URL is known to be malicious (Simulated local response)."
                    } else if (inputText.contains("phishy.link")) {
                        localScanStatus = "Suspicious"
                        localScanExplanation = "This URL appears suspicious (Simulated local response)."
                    } else {
                        localScanStatus = "Safe"
                        localScanExplanation = "URL appears safe after local check."
                    }

                    // Then call OpenAI for deeper analysis
                    openAiViewModel.processTextWithOpenAi(inputText, systemPrompt)
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Scan & Analyze")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (localScanStatus.isNotEmpty()) {
            Text("Local Scan Status: $localScanStatus")
            Spacer(Modifier.height(8.dp))
            Text("Explanation: $localScanExplanation")
            Spacer(Modifier.height(16.dp))
        }

        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        verificationResult?.let {
            Text("OpenAI Response:", style = MaterialTheme.typography.titleMedium)
            Text(it)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "GoFish Scan Result:\nLocal Status: $localScanStatus\nExplanation: $localScanExplanation\nAI Analysis: $it")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }) {
                Text("Share Result")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GoFishAppPreview() {
    GoFIshTheme {
        GoFishApp()
    }
}
