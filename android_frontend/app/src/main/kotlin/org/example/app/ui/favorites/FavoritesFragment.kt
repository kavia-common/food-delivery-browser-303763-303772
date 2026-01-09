package org.example.app.ui.favorites

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
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.mock.MockData
import org.example.app.ui.home.RestaurantAdapter

class FavoritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        adapter = RestaurantAdapter(
            onClick = { restaurant -> (activity as? MainActivity)?.openMenu(restaurant.id) },
            isFavorited = { id -> FavoritesRepository.isRestaurantFavorited(id) },
            onToggleFavorite = { id -> FavoritesRepository.toggleRestaurantFavorite(id) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fun render() {
            val favIds = FavoritesRepository.favoriteRestaurantIdsLive.value ?: emptySet()
            val list = MockData.restaurants.filter { favIds.contains(it.id) }
            adapter.submitList(list)
        }

        FavoritesRepository.favoriteRestaurantIdsLive.observe(viewLifecycleOwner) {
            render()
        }

        render()
    }

    companion object {
        const val TAG = "FavoritesFragment"

        fun newInstance(): FavoritesFragment = FavoritesFragment()
    }
}
