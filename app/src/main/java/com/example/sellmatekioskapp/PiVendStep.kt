package com.example.sellmatekioskapp

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class CartLine(
    val slotId: String,
    val name: String,
    val priceCents: Int,
    val quantityAvailable: Int,
    val motorId: String?,
    val i2cMask: Int? = null,
    val qty: Int
)

data class VendSequenceItem(
    @SerializedName("slot_id") val slotId: String,
    val qty: Int
)

data class CreateOrderRequest(
    @SerializedName("machine_id") val machineId: String,
    val items: List<VendSequenceItem>,
    @SerializedName("amount_cents") val amountCents: Int
)

data class CreateOrderResponse(
    @SerializedName("order_id") val orderId: String,
    val status: String,
    @SerializedName("machine_id") val machineId: String? = null,
    @SerializedName("amount_cents") val amountCents: Int? = null
)

data class StartPaymentResponse(
    @SerializedName("order_id") val orderId: String? = null,
    val status: String,
    @SerializedName("payment_intent_id") val paymentIntentId: String? = null,
    @SerializedName("checkout_session_id") val checkoutSessionId: String? = null
)

data class OrderStatusResponse(
    @SerializedName("order_id") val orderId: String,
    val status: String,
    @SerializedName("payment_intent_id") val paymentIntentId: String? = null,
    @SerializedName("amount_cents") val amountCents: Int? = null
)
