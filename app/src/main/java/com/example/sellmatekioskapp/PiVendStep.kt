@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
// Models.kt
package com.example.sellmatekioskapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Pi Vend Sequence (optional / dev mode) ----

@Serializable
data class PiVendStep(
    val mask: Int,
    val pulses: Int,
    @SerialName("pulse_seconds") val pulseSeconds: Double,
    @SerialName("gap_seconds") val gapSeconds: Double
)

@Serializable
data class PiVendSequenceRequest(
    @SerialName("order_id") val orderId: String,
    val steps: List<PiVendStep>
)

@Serializable
data class PiVendStepEcho(
    // Pi returns mask as hex string like "0x80"
    val mask: String,
    val pulses: Int,
    @SerialName("pulse_seconds") val pulseSeconds: Double,
    @SerialName("gap_seconds") val gapSeconds: Double
)

@Serializable
data class PiVendSequenceResponse(
    val ok: Boolean,
    val mode: String,
    @SerialName("order_id") val orderId: String? = null,
    val steps: List<PiVendStepEcho>? = null
)

// ---- Server-based order flow (recommended) ----

@Serializable
data class CartLine(
    val slotId: String,          // "S01"
    val name: String,
    val priceCents: Int,
    val quantityAvailable: Int,
    val motorId: String?,
    val i2cMask: Int? = null,
    val qty: Int
)

@Serializable
data class VendSequenceItem(
    @SerialName("slot_id") val slotId: String,
    val qty: Int
)

@Serializable
data class CreateOrderRequest(
    @SerialName("machine_id") val machineId: String,
    val items: List<VendSequenceItem>,
    @SerialName("amount_cents") val amountCents: Int
)

@Serializable
data class CreateOrderResponse(
    @SerialName("order_id") val orderId: String,
    val status: String
)

@Serializable
data class StartPaymentResponse(
    val status: String,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null
)

@Serializable
data class OrderStatusResponse(
    @SerialName("order_id") val orderId: String,
    val status: String,
    val message: String? = null
    // TODO: add fields you return (vend_job_id, vend_results, etc.)
)
