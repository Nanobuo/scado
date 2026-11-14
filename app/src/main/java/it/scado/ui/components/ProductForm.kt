package it.scado.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.scado.data.MAX_QUANTITY
import it.scado.util.Dates
import it.scado.util.Emoji
import java.time.LocalDate

/** Promemoria proposti come scorciatoie (in giorni prima della scadenza). */
val SuggestedReminders = listOf(0, 1, 2, 3, 7)

/** I due campi richiesti (nome + scadenza) più la scelta dei promemoria. */
@Composable
fun ProductForm(
    name: String,
    onNameChange: (String) -> Unit,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    reminders: Set<Int>,
    onRemindersChange: (Set<Int>) -> Unit,
    showDatePicker: Boolean,
    onShowDatePickerChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    nameFocus: FocusRequester = remember { FocusRequester() },
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Prodotto") },
            placeholder = { Text("es. Latte, uova, mozzarella…") },
            leadingIcon = { Text(Emoji.forName(name), fontSize = 22.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = {
                keyboard?.hide()
                onShowDatePickerChange(true)
            }),
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
        )
        QuantityStepper(quantity, onQuantityChange)
        ExpiryDateField(date, onDateChange, showDatePicker, onShowDatePickerChange)
        Text("Avvisami", style = MaterialTheme.typography.titleSmall)
        ReminderPicker(reminders, onRemindersChange)
    }
}

/** Quante confezioni uguali, con la stessa scadenza. */
@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Quantità", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Confezioni con la stessa scadenza",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > 1) {
            Icon(Icons.Default.Remove, contentDescription = "Uno in meno")
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 44.dp),
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < MAX_QUANTITY) {
            Icon(Icons.Default.Add, contentDescription = "Uno in più")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpiryDateField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val openPicker by rememberUpdatedState { onShowPickerChange(true) }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) openPicker() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = date?.let(Dates::format) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Data di scadenza") },
            placeholder = { Text("Tocca per scegliere") },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Scegli la data") },
            supportingText = if (date != null) {
                { Text(Dates.relative(date.toEpochDay())) }
            } else null,
            interactionSource = interaction,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val today = Dates.today()
        val quick = listOf(
            "Domani" to today.plusDays(1),
            "3 giorni" to today.plusDays(3),
            "1 settimana" to today.plusWeeks(1),
            "2 settimane" to today.plusWeeks(2),
            "1 mese" to today.plusMonths(1),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            quick.forEach { (label, target) ->
                FilterChip(
                    selected = date == target,
                    onClick = { onDateChange(target) },
                    label = { Text(label) },
                )
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Dates.localDateToUtcMillis(date ?: Dates.today())
        )
        DatePickerDialog(
            onDismissRequest = { onShowPickerChange(false) },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(Dates.utcMillisToLocalDate(it)) }
                    onShowPickerChange(false)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onShowPickerChange(false) }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderPicker(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val options = (SuggestedReminders + selected).toSortedSet()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEach { d ->
            val on = d in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - d else selected + d) },
                label = { Text(Dates.reminderLabel(d)) },
                leadingIcon = if (on) {
                    { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
            )
        }
        AssistChip(
            onClick = { showCustom = true },
            label = { Text("Altro…") },
            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
    }
    if (showCustom) {
        CustomReminderDialog(
            onDismiss = { showCustom = false },
            onConfirm = {
                onChange(selected + it)
                showCustom = false
            },
        )
    }
}

@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val value = text.toIntOrNull()?.takeIf { it in 0..365 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promemoria personalizzato") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { t -> text = t.filter(Char::isDigit).take(3) },
                label = { Text("Giorni prima della scadenza") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { value?.let(onConfirm) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
