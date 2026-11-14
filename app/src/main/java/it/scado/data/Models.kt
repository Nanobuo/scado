package it.scado.data

data class Product(
    val id: String,
    val name: String,
    /** Data di scadenza come giorni dall'epoch (LocalDate.toEpochDay), indipendente dal fuso. */
    val expiryEpochDay: Long,
    /** Quanti giorni prima della scadenza avvisare (0 = il giorno stesso). */
    val reminderDays: List<Int>,
    val addedBy: String?,
    val addedByName: String?,
    /** Quante confezioni uguali, con la stessa scadenza (es. 2 mozzarelle). */
    val quantity: Int = 1,
) {
    /** "Mozzarella" oppure "2× Mozzarella". */
    val label: String get() = productLabel(name, quantity)
}

fun productLabel(name: String, quantity: Int): String =
    if (quantity > 1) "$quantity× $name" else name

const val MAX_QUANTITY = 99

/** Un "frigo": il database condiviso di un nucleo familiare. */
data class Household(
    val id: String,
    val name: String,
    val members: List<String>,
    val memberNames: Map<String, String>,
    val inviteCode: String,
)

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val householdId: String?,
)

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Chiaro"),
    DARK("Scuro"),
}

data class AppSettings(
    val defaultReminderDays: Set<Int> = setOf(3, 1, 0),
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
