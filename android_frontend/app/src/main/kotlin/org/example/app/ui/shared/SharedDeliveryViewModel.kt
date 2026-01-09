package org.example.app.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.delivery.StoredDeliveryOrder

class SharedDeliveryViewModel : ViewModel() {
    val activeOrder: LiveData<StoredDeliveryOrder?> = DeliveryRepository.activeOrder
    val etaRemainingMs: LiveData<Long?> = DeliveryRepository.etaRemainingMs
}
