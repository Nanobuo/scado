package it.scado.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.scado.AppContainer
import it.scado.data.AuthRepository
import it.scado.ui.SessionState
import kotlinx.coroutines.launch

@Composable
private fun Logo(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🧊", fontSize = 72.sp)
        Text(
            "Scado",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AuthScreen(repo: AuthRepository) {
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val valid = email.contains('@') && password.length >= 6 && (!register || name.isNotBlank())

    fun submit() {
        if (!valid || loading) return
        scope.launch {
            loading = true
            error = null
            try {
                if (register) repo.register(name, email, password) else repo.signIn(email, password)
            } catch (e: Exception) {
                error = AuthRepository.describe(e)
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo("Il frigo condiviso che ti ricorda le scadenze")
        Spacer(Modifier.height(16.dp))
        if (register) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Il tuo nome") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            supportingText = if (register) {
                { Text("Almeno 6 caratteri") }
            } else null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = ::submit, enabled = valid && !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(if (register) "Crea account" else "Accedi")
        }
        TextButton(onClick = { register = !register; error = null }) {
            Text(if (register) "Hai già un account? Accedi" else "Prima volta? Crea un account")
        }
    }
}

@Composable
fun HouseholdSetupScreen(container: AppContainer, session: SessionState.NoHousehold) {
    var fridgeName by rememberSaveable { mutableStateOf("Casa di ${session.userName}") }
    var code by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun run(block: suspend () -> Unit) {
        scope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Operazione non riuscita"
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo("Ciao ${session.userName}! Ogni famiglia ha il suo frigo: creane uno o entra in quello di casa.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Crea un nuovo frigo", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = fridgeName, onValueChange = { fridgeName = it },
                    label = { Text("Nome del frigo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        run { container.householdRepo.createHousehold(session.uid, session.userName, fridgeName) }
                    },
                    enabled = !loading && fridgeName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Crea") }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hai un codice invito?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(6) },
                    label = { Text("Codice di 6 caratteri") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                FilledTonalButton(
                    onClick = {
                        run { container.householdRepo.joinHousehold(session.uid, session.userName, code) }
                    },
                    enabled = !loading && code.length == 6,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Entra nel frigo") }
            }
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        TextButton(onClick = { container.authRepo.signOut() }) { Text("Esci dall'account") }
    }
}
