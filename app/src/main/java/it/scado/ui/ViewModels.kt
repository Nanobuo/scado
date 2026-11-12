package it.scado.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.scado.AppContainer
import it.scado.data.AppSettings
import it.scado.data.Household
import it.scado.data.Product
import it.scado.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NoHousehold(val uid: String, val userName: String, val email: String?) : SessionState
    data class Ready(
        val uid: String,
        val userName: String,
        val email: String?,
        val householdId: String,
    ) : SessionState
}

/** Decide quale schermata mostrare: accesso, scelta del frigo, oppure l'app. */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<SessionState> = c.authRepo.authState()
        .flatMapLatest { user ->
            if (user == null) flowOf(SessionState.SignedOut)
            else c.householdRepo.userProfile(user.uid).map { profile ->
                val name = profile?.displayName ?: user.displayName
                    ?: user.email?.substringBefore('@') ?: "Io"
                val hid = profile?.householdId
                if (hid == null) SessionState.NoHousehold(user.uid, name, user.email)
                else SessionState.Ready(user.uid, name, user.email, hid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)
}

/** Tutto ciò che riguarda il frigo corrente; vive finché si è dentro il frigo. */
class FridgeViewModel(
    private val app: Application,
    private val c: AppContainer,
    val session: SessionState.Ready,
) : ViewModel() {
    private val hid = session.householdId

    val products: StateFlow<List<Product>?> = c.productRepo.products(hid)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val household: StateFlow<Household?> = c.householdRepo.household(hid)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = c.settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        // Ogni volta che cambiano i prodotti (anche per mano dell'altro membro)
        // o le preferenze, riprogrammo le notifiche su questo telefono.
        viewModelScope.launch(Dispatchers.IO) {
            combine(products.filterNotNull(), c.settingsRepo.settings) { p, s -> p to s }
                .collectLatest { (p, s) -> runCatching { ReminderScheduler.sync(app, hid, p, s) } }
        }
    }

    fun addProduct(name: String, date: LocalDate, reminders: Set<Int>) =
        c.productRepo.add(hid, name, date.toEpochDay(), reminders, session.uid, session.userName)

    fun updateProduct(p: Product, name: String, date: LocalDate, reminders: Set<Int>) =
        c.productRepo.update(hid, p.id, name, date.toEpochDay(), reminders)

    fun deleteProduct(p: Product) = c.productRepo.delete(hid, p.id)

    fun restoreProduct(p: Product) = c.productRepo.restore(hid, p)

    fun setDefaultReminders(days: Set<Int>) {
        viewModelScope.launch { c.settingsRepo.setDefaultReminderDays(days) }
    }

    fun setNotifyTime(hour: Int, minute: Int) {
        viewModelScope.launch { c.settingsRepo.setNotifyTime(hour, minute) }
    }

    fun leaveFridge(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                c.householdRepo.leaveHousehold(session.uid, hid)
                ReminderScheduler.cancelAll(app)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Operazione non riuscita")
            }
        }
    }

    fun signOut() {
        ReminderScheduler.cancelAll(app)
        c.authRepo.signOut()
    }
}
