// PlanogramModels.kt
package com.example.sellmatekioskapp

data class ProductUi(
    val name: String,
    val priceCents: Int,
    val imageUrl: String? = null
)

data class ProductTile(
    val slotId: String,
    val name: String,
    val priceCents: Int,
    val quantity: Int,
    val imageUrl: String?
)

data class SlotUi(
    val slotId: String,
    val enabled: Boolean,
    val inventory: Int,
    val motorId: String? = null,
    val i2cMask: Int? = null,
    val product: ProductUi? = null
)

data class Planogram(
    val allSlots: List<SlotUi>
)

interface PlanogramService {
    suspend fun load(machineId: String): Planogram
}
