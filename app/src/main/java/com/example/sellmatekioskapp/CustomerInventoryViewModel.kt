// CustomerInventoryViewModel.kt
package com.example.sellmatekioskapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomerInventoryViewModel(
    private val machineId: String,
    private val service: PlanogramService
) : ViewModel() {

    private val _slots = MutableStateFlow<List<SlotUi>>(emptyList())
    val slots: StateFlow<List<SlotUi>> = _slots

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorText.value = null
            try {
                val planogram = service.load(machineId)

                val sellable = planogram.allSlots.filter { slot ->
                    slot.enabled &&
                            slot.inventory > 0 &&
                            (slot.product?.name?.trim()?.isNotEmpty() == true)
                }

                _slots.value = sellable.sortedBy { it.product?.name?.lowercase() ?: "" }
            } catch (e: Exception) {
                _errorText.value = e.message ?: "Unknown error"
                _slots.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
