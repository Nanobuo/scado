package it.scado.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scado.BuildConfig
import it.scado.data.ThemeMode
import it.scado.ui.FridgeViewModel
import it.scado.ui.theme.LocalDarkTheme
import it.scado.ui.components.ReminderPicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: FridgeViewModel, onBack: () -> Unit, onCheckUpdates: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val household by vm.household.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var renamingFridge by rememberSaveable { mutableStateOf(false) }
    var renamingMe by rememberSaveable { mutableStateOf(false) }
    val session = vm.session
    val userName by vm.userName.collectAsStateWithLifecycle()
    val dark = LocalDarkTheme.current
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Il tuo frigo")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Kitchen, null)
                        Text(
                            household?.name ?: "…",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { renamingFridge = true }, enabled = household != null) {
                            Icon(Icons.Default.Edit, "Rinomina il frigo")
                        }
                    }
                    household?.let { h ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Group, null)
                            Text(h.memberNames.values.sorted().joinToString(", ").ifEmpty { "—" })
                        }
                        Text(
                            "Codice invito",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            h.inviteCode.chunked(3).joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = 4.sp,
                        )
                        Text(
                            "Chi ha questo codice può entrare nel frigo e vederne il contenuto. " +
                                    "Condividilo solo con chi vive con te.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Entra nel nostro frigo «${h.name}» su Scado! Codice invito: ${h.inviteCode}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(send, "Invita nel frigo"))
                            }) {
                                Icon(Icons.Default.Share, null)
                                Text("  Invita")
                            }
                            OutlinedButton(onClick = {
                                clipboard.setText(AnnotatedString(h.inviteCode))
                                scope.launch { snackbar.showSnackbar("Codice copiato") }
                            }) {
                                Icon(Icons.Default.ContentCopy, null)
                                Text("  Copia")
                            }
                        }
                    }
                }
            }

            SectionTitle("Promemoria")
            Text(
                "Avvisi proposti per ogni nuovo prodotto (puoi sempre cambiarli prodotto per prodotto).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReminderPicker(settings.defaultReminderDays, vm::setDefaultReminders)
            ListItem(
                headlineContent = { Text("Orario delle notifiche") },
                supportingContent = { Text("%02d:%02d".format(settings.notifyHour, settings.notifyMinute)) },
                leadingContent = { Icon(Icons.Default.Schedule, null) },
                colors = transparent,
                modifier = Modifier.clickable {
                    TimePickerDialog(
                        context,
                        // Orologio nativo di Android, con lo stesso tema (chiaro/scuro) dell'app
                        if (dark) android.R.style.Theme_Material_Dialog
                        else android.R.style.Theme_Material_Light_Dialog,
                        { _, h, m -> vm.setNotifyTime(h, m) },
                        settings.notifyHour,
                        settings.notifyMinute,
                        DateFormat.is24HourFormat(context),
                    ).show()
                },
            )

            SectionTitle("Aspetto")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.DarkMode, null, Modifier.padding(start = 16.dp))
                Text("Tema", style = MaterialTheme.typography.bodyLarge)
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val modes = ThemeMode.entries
                modes.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = modes.size),
                        label = { Text(mode.label) },
                    )
                }
            }

            SectionTitle("Account")
            ListItem(
                headlineContent = { Text(userName) },
                supportingContent = if (session.email != null) {
                    { Text(session.email) }
                } else null,
                leadingContent = { Icon(Icons.Default.Person, null) },
                trailingContent = { Icon(Icons.Default.Edit, "Cambia nome") },
                colors = transparent,
                modifier = Modifier.clickable { renamingMe = true },
            )
            ListItem(
                headlineContent = { Text("Esci da questo frigo") },
                supportingContent = { Text("Potrai crearne un altro o entrare con un codice") },
                leadingContent = { Icon(Icons.Default.NoMeals, null) },
                colors = transparent,
                modifier = Modifier.clickable { confirmLeave = true },
            )
            ListItem(
                headlineContent = { Text("Esci dall'account") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                colors = transparent,
                modifier = Modifier.fillMaxWidth().clickable { vm.signOut() },
            )

            SectionTitle("App")
            ListItem(
                headlineContent = { Text("Versione ${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("Tocca per cercare aggiornamenti") },
                leadingContent = { Icon(Icons.Default.SystemUpdate, null) },
                colors = transparent,
                modifier = Modifier.clickable(onClick = onCheckUpdates),
            )
        }
    }

    if (renamingFridge) {
        RenameDialog(
            title = "Rinomina il frigo",
            label = "Nome del frigo",
            initial = household?.name ?: "",
            onDismiss = { renamingFridge = false },
            onConfirm = {
                vm.renameFridge(it)
                renamingFridge = false
            },
        )
    }
    if (renamingMe) {
        RenameDialog(
            title = "Il tuo nome",
            label = "Nome",
            initial = userName,
            onDismiss = { renamingMe = false },
            onConfirm = {
                vm.renameMe(it)
                renamingMe = false
            },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Uscire dal frigo?") },
            text = { Text("Non vedrai più i prodotti e non riceverai più le notifiche di questo frigo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    vm.leaveFridge { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                }) { Text("Esci") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun RenameDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val valid = text.isNotBlank() && text.trim().length <= 60
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(60) },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = valid) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
