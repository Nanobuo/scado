package it.scado.util

import java.util.Locale

/** Un tocco di colore: indovina un'emoji dal nome del prodotto. */
object Emoji {
    private val table = listOf(
        listOf("latte", "panna") to "🥛",
        listOf("yogurt", "yogourt", "kefir") to "🥣",
        listOf("uov") to "🥚",
        listOf("formagg", "mozzarell", "parmigian", "grana", "ricotta", "stracchino", "gorgonzola",
            "pecorino", "burrata", "scamorza", "fontina", "provola", "mascarpone", "feta", "brie") to "🧀",
        listOf("burro") to "🧈",
        listOf("prosciutt", "salame", "mortadell", "speck", "bresaola", "pancetta", "coppa", "bacon") to "🥓",
        listOf("wurstel", "würstel", "hot dog") to "🌭",
        listOf("pollo", "tacchino") to "🍗",
        listOf("carne", "manzo", "maiale", "vitello", "bistecc", "macinat", "salsicc", "hamburger") to "🥩",
        listOf("pesce", "salmone", "tonno", "merluzzo", "orata", "branzino", "sgombro") to "🐟",
        listOf("gamber", "scampi") to "🦐",
        listOf("insalata", "lattuga", "rucola", "spinaci", "bietol", "cavol", "verza") to "🥬",
        listOf("broccol") to "🥦",
        listOf("pomodor", "pelati", "passata") to "🍅",
        listOf("carot") to "🥕",
        listOf("zucchin", "cetriol") to "🥒",
        listOf("peperon") to "🫑",
        listOf("melanzan") to "🍆",
        listOf("patat") to "🥔",
        listOf("cipoll") to "🧅",
        listOf("aglio") to "🧄",
        listOf("funghi", "fungo", "champignon") to "🍄",
        listOf("avocado") to "🥑",
        listOf("mais") to "🌽",
        listOf("mela", "mele") to "🍎",
        listOf("pera", "pere") to "🍐",
        listOf("banan") to "🍌",
        listOf("fragol") to "🍓",
        listOf("uva") to "🍇",
        listOf("limon") to "🍋",
        listOf("arancia", "arance", "mandarin", "clementin") to "🍊",
        listOf("pesca", "pesche", "albicocc") to "🍑",
        listOf("ciliegi") to "🍒",
        listOf("kiwi") to "🥝",
        listOf("ananas") to "🍍",
        listOf("anguria", "melone", "cocomero") to "🍉",
        listOf("mirtill", "lampon", "frutti di bosco") to "🫐",
        listOf("pane", "panino", "baguette", "focaccia", "piadin") to "🍞",
        listOf("cornett", "brioche", "croissant") to "🥐",
        listOf("pizza") to "🍕",
        listOf("pasta", "spaghett", "tortellin", "ravioli", "gnocchi", "lasagn") to "🍝",
        listOf("riso") to "🍚",
        listOf("torta", "dolce", "tiramis") to "🍰",
        listOf("cioccolat", "nutella") to "🍫",
        listOf("gelato") to "🍦",
        listOf("biscott") to "🍪",
        listOf("succo", "spremut") to "🧃",
        listOf("birra") to "🍺",
        listOf("vino", "prosecco", "spumante") to "🍷",
        listOf("acqua") to "💧",
        listOf("hummus", "salsa", "sugo", "pesto", "maionese", "ketchup", "senape") to "🫙",
        listOf("tofu", "seitan") to "🧆",
    )

    fun forName(name: String): String {
        val n = name.lowercase(Locale.ITALIAN).trim()
        if (n.isEmpty()) return "🛒"
        val words = n.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
        for ((keys, emoji) in table) {
            for (k in keys) {
                // Le chiavi brevi (es. "uva", "mela") devono essere quasi una parola intera,
                // così "mela" non scatta dentro "melanzane" né "uva" dentro "uvetta".
                val hit = if (k.length <= 4) words.any { it == k || (it.startsWith(k) && it.length <= k.length + 1) }
                else n.contains(k)
                if (hit) return emoji
            }
        }
        return "🍽️"
    }
}
