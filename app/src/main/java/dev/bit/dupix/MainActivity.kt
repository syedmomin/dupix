package dev.bit.dupix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.navigation.DupixNavHost
import dev.bit.dupix.ui.theme.DupixTheme
import dev.bit.dupix.util.CrashLogger

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scanViewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DupixTheme {
                DupixNavHost(vm = scanViewModel)

                // Show the previous crash (if any) so it can be copied and reported.
                val context = LocalContext.current
                var crash by remember { mutableStateOf(CrashLogger.readAndClear(context)) }
                crash?.let { text ->
                    AlertDialog(
                        onDismissRequest = { crash = null },
                        confirmButton = { TextButton(onClick = { crash = null }) { Text("Dismiss") } },
                        title = { Text("Last crash report") },
                        text = {
                            SelectionContainer {
                                Text(
                                    text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
