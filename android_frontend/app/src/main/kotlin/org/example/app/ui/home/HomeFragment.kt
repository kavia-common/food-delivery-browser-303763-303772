package org.example.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.data.mock.MockData

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        adapter = RestaurantAdapter { restaurant ->
            (activity as? MainActivity)?.openMenu(restaurant.id)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.restaurantRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        adapter.submitList(MockData.restaurants)
    }

    companion object {
        const val TAG = "HomeFragment"

        fun newInstance(): HomeFragment = HomeFragment()
    }
}
