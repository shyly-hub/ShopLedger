package com.stepitacademy.shopledger.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stepitacademy.shopledger.util.toTitleCase

private val CardCornerRadius = 12.dp
private val ScreenEdgePadding = 16.dp
private val CardBorderColor = Color(0xFFE5E7EB)

/**
 * initialName/initialPhone are the ONLY source of pre-fill data — they
 * come straight from the customer being edited. This screen never
 * defaults them to "" itself; that only happens because the caller
 * passes "" when opening the Add New flow. Editing vs. adding is
 * entirely the caller's responsibility (see MainActivity's routes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    initialName: String = "",
    initialPhone: String = "",
    isEditing: Boolean = false,
    onSave: (name: String, phone: String) -> Unit,
    onBack: () -> Unit
) {
    // Keyed on the incoming initial values: if this composable is ever
    // recomposed with different initialName/initialPhone (e.g. the
    // customer data arrives slightly after first composition), the saved
    // state re-initializes from the new values instead of sticking with
    // whatever was captured on the very first pass.
    var name by rememberSaveable(initialName, initialPhone) { mutableStateOf(initialName) }
    var phone by rememberSaveable(initialName, initialPhone) { mutableStateOf(initialPhone) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Customer" else "New Customer",
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
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) showError = false
                        },
                        label = { Text("Customer Name *") },
                        placeholder = { Text("Enter full name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { name = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear name")
                                }
                            }
                        },
                        isError = showError,
                        supportingText = {
                            if (showError) {
                                Text("Name is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CardBorderColor)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("Enter phone number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (phone.isNotEmpty()) {
                                IconButton(onClick = { phone = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear phone")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = CardBorderColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else if (!isSaving) {
                        isSaving = true
                        onSave(name.trim().toTitleCase(), phone.trim())
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
                        text = if (isEditing) "Update Customer" else "Save Customer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}