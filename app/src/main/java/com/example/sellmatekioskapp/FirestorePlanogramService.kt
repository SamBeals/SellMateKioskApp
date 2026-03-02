// FirestorePlanogramService.kt
package com.example.sellmatekioskapp

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Loads planogram slots + merges in inventory item data.
 *
 * Firestore paths:
 * - machines/{machineId}/planogramSlots/{slotId}
 * - machines/{machineId}/inventory/{slotId}
 */
class FirestorePlanogramService(
    private val db: FirebaseFirestore
) : PlanogramService {

    override suspend fun load(machineId: String): Planogram {
        val planSnap = db.collection("machines")
            .document(machineId)
            .collection("planogramSlots")
            .get()
            .await()

        if (planSnap.isEmpty) {
            throw IllegalStateException(
                "Firestore returned 0 docs at machines/$machineId/planogramSlots"
            )
        }

        val invSnap = db.collection("machines")
            .document(machineId)
            .collection("inventory")
            .get()
            .await()

        val invBySlotId: Map<String, DocumentSnapshot> = invSnap.documents.associateBy { doc ->
            (doc.getString("slot_id") ?: doc.getString("slotId") ?: doc.id)
        }

        val mergedSlots: List<SlotUi> = planSnap.documents.mapNotNull { planDoc ->
            val planData = planDoc.data ?: return@mapNotNull null
            val slotId = planSlotId(planDoc, planData)

            val invDoc = invBySlotId[slotId]
            val invData = invDoc?.data

            val enabled = (invData?.get("enabled") as? Boolean)
                ?: (planData["enabled"] as? Boolean)
                ?: true

            val qty = (invData?.get("qty") as? Number)?.toInt()
                ?: (invData?.get("inventory") as? Number)?.toInt()
                ?: 0

            val name = (invData?.get("name") as? String)
                ?: (invData?.get("productName") as? String)
                ?: ""

            val priceCents = (invData?.get("price_cents") as? Number)?.toInt()
                ?: (invData?.get("priceCents") as? Number)?.toInt()
                ?: 0

            val imageUrl = (invData?.get("image_url") as? String)
                ?: (invData?.get("imageUrl") as? String)
                ?: (invData?.get("image") as? String)
                ?: (invData?.get("imageUri") as? String)

            val product =
                if (name.isNotBlank()) ProductUi(name = name, priceCents = priceCents, imageUrl = imageUrl)
                else null

            val motorId: String? =
                (planData["motorId"] as? String)
                    ?: (planData["motor_id"] as? String)

            val i2cMask: Int? =
                (planData["i2cMask"] as? Number)?.toInt()
                    ?: (planData["i2c_mask"] as? Number)?.toInt()
                    ?: (planData["mask"] as? Number)?.toInt()

            SlotUi(
                slotId = slotId,
                enabled = enabled,
                inventory = qty,
                motorId = motorId,
                i2cMask = i2cMask,
                product = product
            )
        }

        return Planogram(allSlots = mergedSlots)
    }

    private fun planSlotId(planDoc: DocumentSnapshot, planData: Map<String, Any>): String {
        return (planData["slot_id"] as? String)
            ?: (planData["slotId"] as? String)
            ?: planDoc.id
    }
}
