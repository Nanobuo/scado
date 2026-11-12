package it.scado

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.scado.ui.ScadoRoot
import it.scado.ui.theme.ScadoTheme

class MainActivity : ComponentActivity() {

    /** true quando l'app è stata aperta dalla scorciatoia "Ho fatto la spesa". */
    private var openShopping by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openShopping = intent?.action == ACTION_SHOPPING
        setContent {
            ScadoTheme {
                ScadoRoot(
                    openShopping = openShopping,
                    onShoppingOpened = { openShopping = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_SHOPPING) openShopping = true
    }

    companion object {
        const val ACTION_SHOPPING = "it.scado.ACTION_SHOPPING"
    }
}
