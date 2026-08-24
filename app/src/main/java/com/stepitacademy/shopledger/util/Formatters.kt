package com.stepitacademy.shopledger.util

fun formatMoney(amount: Long, currency: String): String {
    return when (currency) {
        "KHR" -> "%,d ៛".format(amount)
        "USD" -> "$%.2f".format(amount / 100.0)
        else -> "$amount"
    }
}