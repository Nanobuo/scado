package it.scado

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.scado.data.AuthRepository
import it.scado.data.HouseholdRepository
import it.scado.data.ProductRepository
import it.scado.data.SettingsRepository
import it.scado.notify.Notifications
import it.scado.notify.SyncWorker
import it.scado.update.UpdateCheckWorker

class ScadoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
        SyncWorker.schedule(this)
        UpdateCheckWorker.schedule(this)
    }
}

class AppContainer(context: Context) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val authRepo = AuthRepository(auth, db)
    val householdRepo = HouseholdRepository(db)
    val productRepo = ProductRepository(db)
    val settingsRepo = SettingsRepository(context.applicationContext)
}
