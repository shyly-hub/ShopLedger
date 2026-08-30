package com.stepitacademy.shopledger.util

/**
 * Formats a minor-unit amount for display.
 * KHR has no smaller unit, so it's a plain grouped integer: "50,000 ៛".
 * USD is divided by 100 for cents and grouped: "$1,680.00".
 */
fun formatMoney(amount: Long, currency: String): String {
    return when (currency) {
        "KHR" -> "%,d ៛".format(amount)
        "USD" -> "$%,.2f".format(amount / 100.0)
        else -> "$amount"
    }
}

/**
 * Title-cases free text a user typed — "jess" -> "Jess", "sugar, 5kg" ->
 * "Sugar, 5Kg" is avoided by only capitalizing the first letter of each
 * whitespace-separated word, not every letter after punctuation.
 * Used for customer names and order descriptions, both on save and
 * (defensively) at display time for data saved before this existed.
 */
fun String.toTitleCase(): String =
    trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { ch -> ch.uppercase() }
        }