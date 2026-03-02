// CheckoutScreen.kt
package com.example.sellmatekioskapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    order: OrderDraft,
    checkout: CheckoutManager,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val state by checkout.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val linesBySlot by order.linesBySlot.collectAsState()
    val lines by remember(linesBySlot) {
        derivedStateOf { linesBySlot.values.sortedBy { it.name.lowercase() } }
    }

    val isBusy = state is CheckoutState.CreatingOrder ||
            state is CheckoutState.StartingPayment ||
            state is CheckoutState.AwaitingPayment ||
            state is CheckoutState.Vending

    // When state becomes Failure, show snackbar once.
    LaunchedEffect(state) {
        if (state is CheckoutState.Failure) {
            val msg = (state as CheckoutState.Failure).message
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !isBusy) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: ${formatUsd(order.totalCents())}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            val result = checkout.startCheckout(
                                amountCents = order.totalCents(),
                                items = order.toVendItems()
                            )

                            result.onSuccess {
                                order.clear()
                                onFinished()
                            }.onFailure { e ->
                                // The state flow will already move to Failure, but snackbar gives immediate feedback.
                                snackbarHostState.showSnackbar(e.message ?: "Checkout failed.")
                            }
                        }
                    },
                    enabled = lines.isNotEmpty() && !isBusy
                ) {
                    Text("Finish Purchase")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            when (val s = state) {
                is CheckoutState.Idle -> StatusCard("Ready")
                is CheckoutState.CreatingOrder -> StatusCard("Creating order…")
                is CheckoutState.StartingPayment -> StatusCard("Starting payment…")
                is CheckoutState.AwaitingPayment -> StatusCard("Awaiting payment on reader…")
                is CheckoutState.Vending -> StatusCard("Vending…")
                is CheckoutState.Success -> StatusCard("Success!")
                is CheckoutState.Failure -> {
                    StatusCard("Failed: ${s.message}")
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        TextButton(onClick = { checkout.reset() }) { Text("Reset") }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(lines) { line ->
                    ListItem(
                        headlineContent = { Text(line.name) },
                        supportingContent = { Text("Slot ${line.slotId}") },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${line.qty} × ${formatUsd(line.priceCents)}")
                                Row {
                                    TextButton(
                                        onClick = { order.removeOne(line.slotId) },
                                        enabled = !isBusy
                                    ) { Text("−") }

                                    TextButton(
                                        onClick = { order.increment(line.slotId) },
                                        enabled = !isBusy && line.qty < line.quantityAvailable
                                    ) { Text("+") }
                                }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun StatusCard(text: String) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text, Modifier.padding(16.dp))
    }
}
