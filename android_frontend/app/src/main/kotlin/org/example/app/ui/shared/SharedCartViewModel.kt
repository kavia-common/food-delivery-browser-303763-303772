package org.example.app.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.example.app.data.cart.CartRepository
import org.example.app.data.models.CartLine

class SharedCartViewModel : ViewModel() {
    val cartLines: LiveData<List<CartLine>> = CartRepository.cartLines
    val totalItemCount: LiveData<Int> = CartRepository.totalItemCount
}
