package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScanMedicationViewModel : ViewModel() {
    private val _barcode = MutableStateFlow<String?>(null)
    val barcode: StateFlow<String?> = _barcode

    fun onBarcodeScanned(barcode: String) {
        _barcode.value = barcode
    }

    // Add more scan-related logic here (e.g., backend lookup, error state, etc.)
}

