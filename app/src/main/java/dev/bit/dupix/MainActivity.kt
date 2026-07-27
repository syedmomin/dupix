package dev.bit.dupix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.navigation.DupixNavHost
import dev.bit.dupix.ui.theme.DupixTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scanViewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DupixTheme {
                DupixNavHost(vm = scanViewModel)
            }
        }
    }
}
