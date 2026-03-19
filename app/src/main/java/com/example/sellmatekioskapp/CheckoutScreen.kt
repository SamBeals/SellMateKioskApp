package com.example.sellmatekioskapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheckoutScreen(
    order: OrderDraft,
    checkout: CheckoutManager,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onReturnHome: () -> Unit
) {
    var isStartingCheckout by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val linesMap by order.linesBySlot.collectAsState()
    val lineItems = linesMap.values.sortedBy { it.name.lowercase() }
    val totalPriceCents = lineItems.sumOf { it.priceCents * it.qty }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Review Order",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (lineItems.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your order is empty.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onReturnHome) {
                        Text("Back to Inventory")
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lineItems) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Slot: ${item.slotId}")
                        Text("Price: ${formatPrice(item.priceCents)}")

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                OutlinedButton(
                                    onClick = { order.removeOne(item.slotId) },
                                    enabled = !isStartingCheckout
                                ) {
                                    Text("-")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Qty: ${item.qty}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { order.increment(item.slotId) },
                                    enabled = !isStartingCheckout && item.qty < item.quantityAvailable
                                ) {
                                    Text("+")
                                }
                            }

                            Text(
                                text = formatPrice(item.priceCents * item.qty),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Order Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatPrice(totalPriceCents),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !isStartingCheckout
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    scope.launch {
                        isStartingCheckout = true
                        errorText = null

                        val started = checkout.startCheckout(order)
                        if (!started.isSuccess) {
                            errorText = started.exceptionOrNull()?.message ?: "Unable to start checkout"
                            isStartingCheckout = false
                            return@launch
                        }

                        var attempts = 0
                        while (attempts < 60) {
                            delay(2000)

                            val status = checkout.fetchCurrentOrderStatus()?.status?.uppercase()

                            when (status) {
                                "COMPLETED",
                                "COMPLETE",
                                "VEND_COMPLETED",
                                "VEND_SUCCESS",
                                "SUCCEEDED",
                                "PAID" -> {
                                    isStartingCheckout = false
                                    onFinished()
                                    return@launch
                                }

                                "CANCELLED",
                                "FAILED",
                                "PAYMENT_FAILED",
                                "VEND_FAILED" -> {
                                    errorText = "Checkout failed: $status"
                                    isStartingCheckout = false
                                    return@launch
                                }
                            }

                            attempts++
                        }

                        errorText = "Payment started, but final status was not confirmed yet."
                        isStartingCheckout = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isStartingCheckout
            ) {
                if (isStartingCheckout) {
                    CircularProgressIndicator()
                } else {
                    Text("Finish Purchase")
                }
            }
        }
    }
}

private fun formatPrice(priceCents: Int): String {
    return "$" + "%,.2f".format(priceCents / 100.0)
}