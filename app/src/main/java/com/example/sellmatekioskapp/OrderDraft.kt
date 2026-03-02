// OrderDraft.kt
package com.example.sellmatekioskapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.util.Locale

class OrderDraft {
    private val _linesBySlot = MutableStateFlow<Map<String, CartLine>>(emptyMap())
    val linesBySlot: StateFlow<Map<String, CartLine>> = _linesBySlot

    fun add(slot: SlotUi) {
        // Mirror your Swift guards
        if (!slot.enabled) return
        if (slot.inventory <= 0) return
        val product = slot.product ?: return
        val name = product.name.trim()
        if (name.isEmpty()) return

        _linesBySlot.update { current ->
            val existing = current[slot.slotId]
            if (existing != null) {
                if (existing.qty < existing.quantityAvailable) {
                    current + (slot.slotId to existing.copy(qty = existing.qty + 1))
                } else current
            } else {
                val line = CartLine(
                    slotId = slot.slotId,
                    name = name,
                    priceCents = product.priceCents,
                    quantityAvailable = slot.inventory,
                    motorId = slot.motorId,
                    i2cMask = slot.i2cMask,
                    qty = 1
                )
                current + (slot.slotId to line)
            }
        }
    }

    fun increment(slotId: String) {
        _linesBySlot.update { current ->
            val existing = current[slotId] ?: return@update current
            if (existing.qty < existing.quantityAvailable) {
                current + (slotId to existing.copy(qty = existing.qty + 1))
            } else current
        }
    }

    fun removeOne(slotId: String) {
        _linesBySlot.update { current ->
            val existing = current[slotId] ?: return@update current
            val newQty = existing.qty - 1
            if (newQty <= 0) current - slotId
            else current + (slotId to existing.copy(qty = newQty))
        }
    }

    fun clear() {
        _linesBySlot.value = emptyMap()
    }

    fun linesSorted(): List<CartLine> =
        _linesBySlot.value.values.sortedBy { it.name.lowercase() }

    fun itemCount(): Int =
        _linesBySlot.value.values.sumOf { it.qty }

    fun totalCents(): Int =
        _linesBySlot.value.values.sumOf { it.priceCents * it.qty }

    fun toVendItems(): List<VendSequenceItem> =
        _linesBySlot.value.values
            .filter { it.qty > 0 }
            .map { VendSequenceItem(slotId = it.slotId, qty = it.qty) }
}

// Simple currency formatter (Swift formatUSD equivalent)
fun formatUsd(cents: Int): String {
    val nf = NumberFormat.getCurrencyInstance(Locale.US)
    return nf.format(cents / 100.0)
}

/**
 * This is your Swift Slot concept flattened for UI usage.
 * Replace with your real model coming from Firestore/planogram load.
 */

