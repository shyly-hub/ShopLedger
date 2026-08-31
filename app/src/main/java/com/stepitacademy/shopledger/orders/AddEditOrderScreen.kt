package com.stepitacademy.shopledger.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stepitacademy.shopledger.data.Currency
import com.stepitacademy.shopledger.ui.theme.components.SegmentedToggle
import com.stepitacademy.shopledger.util.toTitleCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardCornerRadius = 12.dp
private val ScreenEdgePadding = 16.dp
private val CardBorderColor = Color(0xFFE5E7EB)

/**
 * amountMinorUnits handed back to onSave is already converted to the
 * smallest unit (whole riel for KHR, cents for USD) — the caller does
 * not need to do any further conversion before writing to the DB.
 *
 * All initial* parameters are the sole source of pre-fill data. This
 * screen never resets them to blank on its own; a blank field only
 * happens because the caller passed blank defaults (the Add New flow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOrderScreen(
    customerName: String,
    initialDescription: String = "",
    initialAmountMajorUnitsText: String = "", // e.g. "20000" for riel, "3.50" for dollars
    initialCurrency: Currency = Currency.KHR,
    initialTimestamp: Long = System.currentTimeMillis(),
    initialIsPaid: Boolean = false,
    isEditing: Boolean = false,
    onSave: (description: String, amountMinorUnits: Long, currency: Currency, timestamp: Long, isPaid: Boolean) -> Unit,
    onBack: () -> Unit
) {
    // Keyed on every initial* value: if MainActivity recomposes this
    // screen with newly-arrived order data (see the loading gate in
    // AppNavigation's edit_order route), the form re-initializes from
    // the real values instead of being stuck with whatever was
    // captured on the very first, possibly-blank, composition.
    val formKey = arrayOf(initialDescription, initialAmountMajorUnitsText, initialCurrency, initialTimestamp, initialIsPaid)

    var description by rememberSaveable(*formKey) { mutableStateOf(initialDescription) }
    var amountText by rememberSaveable(*formKey) { mutableStateOf(initialAmountMajorUnitsText) }
    var currency by rememberSaveable(*formKey) { mutableStateOf(initialCurrency) }
    var timestamp by rememberSaveable(*formKey) { mutableStateOf(initialTimestamp) }
    var isPaid by rememberSaveable(*formKey) { mutableStateOf(initialIsPaid) }
    var showDatePicker by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val dateText = remember(timestamp) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenEdgePadding, vertical = ScreenEdgePadding)
        ) {
            Text(
                text = "For $customerName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            if (it.isNotBlank()) descriptionError = false
                        },
                        label = { Text("Item / Note") },
                        placeholder = { Text("e.g. Rice, 5kg") },
                        isError = descriptionError,
                        supportingText = {
                            if (descriptionError) Text("Item / note is required", color = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CardBorderColor)
                    )

                    Column {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                amountError = false
                            },
                            label = { Text(if (currency == Currency.KHR) "Amount (KHR)" else "Amount (USD)") },
                            placeholder = { Text(if (currency == Currency.KHR) "e.g. 20000" else "e.g. 3.50") },
                            trailingIcon = {
                                Text(
                                    text = if (currency == Currency.KHR) "៛" else "$",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            isError = amountError,
                            supportingText = {
                                if (amountError) Text("Enter a valid amount", color = MaterialTheme.colorScheme.error)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CardBorderColor)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedToggle(
                            leftLabel = "KHR (៛)",
                            rightLabel = "USD ($)",
                            isLeftSelected = currency == Currency.KHR,
                            onSelectLeft = { currency = Currency.KHR },
                            onSelectRight = { currency = Currency.USD },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CardBorderColor)
                    )

                    Column {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SegmentedToggle(
                            leftLabel = "Pending",
                            rightLabel = "Paid",
                            isLeftSelected = !isPaid,
                            onSelectLeft = { isPaid = false },
                            onSelectRight = { isPaid = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmedDescription = description.trim()
                    val minorUnits = parseAmountToMinorUnits(amountText, currency)

                    var hasError = false
                    if (trimmedDescription.isBlank()) {
                        descriptionError = true
                        hasError = true
                    }
                    if (minorUnits == null || minorUnits <= 0) {
                        amountError = true
                        hasError = true
                    }

                    if (!hasError && !isSaving && minorUnits != null) {
                        isSaving = true
                        onSave(trimmedDescription.toTitleCase(), minorUnits, currency, timestamp, isPaid)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(CardCornerRadius)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "Update Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}


private fun parseAmountToMinorUnits(text: String, currency: Currency): Long? {
    val value = text.toDoubleOrNull() ?: return null
    if (value <= 0) return null
    return when (currency) {
        Currency.KHR -> value.toLong()
        Currency.USD -> Math.round(value * 100)
    }
}