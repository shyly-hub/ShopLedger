package com.stepitacademy.shopledger.ui.theme.components

import com.stepitacademy.shopledger.util.formatMoney

/**
 * Returns only the currencies a customer actually owes, formatted.
 * A customer who owes nothing gets an empty list — callers should
 * show a "No debt" state rather than "0 ៛ | $0.00", which reads as
 * noise rather than information.
 */
fun debtParts(owedKhr: Long, owedUsd: Long): List<String> = buildList {
    if (owedKhr > 0) add(formatMoney(owedKhr, "KHR"))
    if (owedUsd > 0) add(formatMoney(owedUsd, "USD"))
}
