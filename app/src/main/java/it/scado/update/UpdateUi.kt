package it.scado.update

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import it.scado.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    init {
        check(manual = false)
    }

    /** manual = true quando lo chiede l'utente: allora mostro anche "nessun aggiornamento". */
    fun check(manual: Boolean) {
        val current = _state.value
        if (current is UpdateState.Downloading || current is UpdateState.Ready) {
            _dialogVisible.value = true
            return
        }
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            if (manual) _dialogVisible.value = true
            try {
                val update = Updater.availableUpdate()
                _state.value = if (update != null) UpdateState.Available(update) else UpdateState.UpToDate
                _dialogVisible.value = update != null || manual
            } catch (e: Exception) {
                _state.value = UpdateState.Failed("Impossibile controllare gli aggiornamenti: verifica la connessione.")
                _dialogVisible.value = manual
            }
        }
    }

    fun download() {
        val info = (_state.value as? UpdateState.Available)?.info ?: return
        viewModelScope.launch {
            _state.value = UpdateState.Downloading(info, 0f)
            try {
                val file = Updater.download(getApplication(), info) { p ->
                    _state.value = UpdateState.Downloading(info, p)
                }
                _state.value = UpdateState.Ready(info, file)
                _dialogVisible.value = true
            } catch (e: Exception) {
                _state.value = UpdateState.Failed("Download non riuscito: verifica la connessione e riprova.")
            }
        }
    }

    fun dismiss() {
        _dialogVisible.value = false
    }
}

@Composable
fun UpdateDialog(vm: UpdateViewModel) {
    val visible by vm.dialogVisible.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    if (!visible) return
    val context = LocalContext.current

    when (val s = state) {
        UpdateState.Idle -> Unit

        UpdateState.Checking -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Aggiornamenti") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Controllo se c'è una nuova versione…")
                    CircularProgressIndicator()
                }
            },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text("Chiudi") } },
        )

        UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Tutto aggiornato") },
            text = { Text("Hai già l'ultima versione di Scado (${BuildConfig.VERSION_NAME}).") },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text("OK") } },
        )

        is UpdateState.Available -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Nuova versione disponibile") },
            text = {
                Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Scado ${s.info.versionName} (hai la ${BuildConfig.VERSION_NAME}).")
                    if (s.info.notes.isNotBlank()) {
                        Text("Novità:", style = MaterialTheme.typography.titleSmall)
                        Text(s.info.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::download) { Text("Aggiorna") } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("Più tardi") } },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Scarico l'aggiornamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(s.progress * 100).toInt()}%")
                }
            },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text("Continua in background") } },
        )

        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Pronto da installare") },
            text = {
                Text(
                    if (Updater.canInstall(context))
                        "Scado ${s.info.versionName} è stato scaricato. I tuoi dati restano dove sono."
                    else
                        "Android deve sapere che ti fidi di Scado: nella schermata che si apre attiva " +
                                "«Consenti da questa fonte», poi torna qui e tocca di nuovo «Installa»."
                )
            },
            confirmButton = { TextButton(onClick = { Updater.install(context, s.file) }) { Text("Installa") } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("Più tardi") } },
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Aggiornamento") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { vm.check(manual = true) }) { Text("Riprova") } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("Chiudi") } },
        )
    }
}
