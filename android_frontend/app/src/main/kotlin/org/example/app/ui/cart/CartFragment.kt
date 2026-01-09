package org.example.app.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.cart.CartRepository
import org.example.app.data.models.CartLine

class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter

    private lateinit var emptyState: View
    private lateinit var totalsContainer: View

    private lateinit var subtotalValue: TextView
    private lateinit var deliveryValue: TextView
    private lateinit var taxValue: TextView
    private lateinit var totalValue: TextView

    private lateinit var checkoutButton: MaterialButton
    private lateinit var checkoutHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CartAdapter(
            onInc = { line -> CartRepository.add(line.item) },
            onDec = { line -> CartRepository.updateQuantity(line.item, line.quantity - 1) },
            onRemove = { line -> CartRepository.remove(line.item) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.cartRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        emptyState = view.findViewById(R.id.emptyState)
        totalsContainer = view.findViewById(R.id.totalsContainer)

        subtotalValue = view.findViewById(R.id.subtotalValue)
        deliveryValue = view.findViewById(R.id.deliveryValue)
        taxValue = view.findViewById(R.id.taxValue)
        totalValue = view.findViewById(R.id.totalValue)

        checkoutButton = view.findViewById(R.id.checkoutButton)
        checkoutHint = view.findViewById(R.id.checkoutHint)

        checkoutButton.isEnabled = false
        checkoutHint.isVisible = true

        CartRepository.cartLines.observe(viewLifecycleOwner) { lines ->
            render(lines)
        }
    }

    private fun render(lines: List<CartLine>) {
        adapter.submitList(lines)

        val isEmpty = lines.isEmpty()
        emptyState.isVisible = isEmpty
        totalsContainer.isVisible = !isEmpty

        subtotalValue.text = Formatters.moneyFromCents(CartRepository.computeSubtotalCents())
        deliveryValue.text = Formatters.moneyFromCents(CartRepository.computeDeliveryFeeCents())
        taxValue.text = Formatters.moneyFromCents(CartRepository.computeTaxCents())
        totalValue.text = Formatters.moneyFromCents(CartRepository.computeTotalCents())
    }

    companion object {
        const val TAG = "CartFragment"

        fun newInstance(): CartFragment = CartFragment()
    }
}
