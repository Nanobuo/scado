package it.scado.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scado.data.productLabel
import it.scado.ui.FridgeViewModel
import it.scado.ui.components.ProductForm
import it.scado.util.Emoji
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Il "rientro dalla spesa": chiede un prodotto alla volta (nome + scadenza),
 * lo mette nel frigo e passa subito al successivo.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingScreen(vm: FridgeViewModel, onDone: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var epochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    // null = usa i promemoria suggeriti nelle impostazioni
    var customReminders by rememberSaveable { mutableStateOf<List<Int>?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var quantity by rememberSaveable { mutableStateOf(1) }
    var added by rememberSaveable { mutableStateOf(listOf<String>()) }

    val reminders = customReminders?.toSet() ?: settings.defaultReminderDays
    val canAdd = name.isNotBlank() && epochDay != null
    val focus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(added.size) {
        delay(150)
        runCatching { focus.requestFocus() }
    }

    fun commit() {
        val day = epochDay ?: return
        val n = name.trim()
        if (n.isEmpty()) return
        vm.addProduct(n, LocalDate.ofEpochDay(day), reminders, quantity)
        val label = productLabel(n, quantity)
        added = added + label
        name = ""
        quantity = 1
        epochDay = null
        customReminders = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("${Emoji.forName(n)} $label nel frigo", duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spesa") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.Close, "Chiudi") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (canAdd) commit()
                            onDone()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(if (canAdd) "Aggiungi e finisci" else "Ho finito") }
                    Button(
                        onClick = ::commit,
                        enabled = canAdd,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Avanti") }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Prodotto n° ${added.size + 1}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Dimmi cosa hai comprato e quando scade, uno alla volta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                ProductForm(
                    name = name, onNameChange = { name = it },
                    quantity = quantity, onQuantityChange = { quantity = it },
                    date = epochDay?.let(LocalDate::ofEpochDay), onDateChange = { epochDay = it.toEpochDay() },
                    reminders = reminders, onRemindersChange = { customReminders = it.sorted() },
                    showDatePicker = showPicker, onShowDatePickerChange = { showPicker = it },
                    nameFocus = focus,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (added.isNotEmpty()) {
                Text(
                    "Già nel frigo (${added.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    added.asReversed().forEach { n ->
                        SuggestionChip(onClick = {}, label = { Text("${Emoji.forName(n)} $n") })
                    }
                }
            }
        }
    }
}
