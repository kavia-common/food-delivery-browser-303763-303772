package org.example.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.mock.MockData
import org.example.app.data.preferences.AppPreferencesRepository
import org.example.app.ui.shared.SharedAppViewModel

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter
    private lateinit var favoritesOnlySwitch: SwitchMaterial

    private lateinit var sharedAppViewModel: SharedAppViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        adapter = RestaurantAdapter(
            onClick = { restaurant -> (activity as? MainActivity)?.openMenu(restaurant.id) },
            isFavorited = { id -> FavoritesRepository.isRestaurantFavorited(id) },
            onToggleFavorite = { id -> FavoritesRepository.toggleRestaurantFavorite(id) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedAppViewModel = ViewModelProvider(requireActivity())[SharedAppViewModel::class.java]
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
        favoritesOnlySwitch = view.findViewById(R.id.favoritesOnlySwitch)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        favoritesOnlySwitch.setOnCheckedChangeListener(null)
        favoritesOnlySwitch.isChecked = sharedAppViewModel.homeFavoritesOnly.value ?: false
        favoritesOnlySwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            AppPreferencesRepository.setHomeFavoritesOnly(isChecked)
        }

        fun render() {
            val favoritesOnly = sharedAppViewModel.homeFavoritesOnly.value ?: false
            val favIds = sharedAppViewModel.favoriteRestaurantIds.value ?: emptySet()

            val list = if (favoritesOnly) {
                MockData.restaurants.filter { favIds.contains(it.id) }
            } else {
                MockData.restaurants
            }
            adapter.submitList(list)
        }

        // Update list when favorites or filter changes.
        sharedAppViewModel.favoriteRestaurantIds.observe(viewLifecycleOwner) {
            render()
            // Also refresh heart icons.
            adapter.notifyDataSetChanged()
        }
        sharedAppViewModel.homeFavoritesOnly.observe(viewLifecycleOwner) {
            render()
        }

        render()
    }

    companion object {
        const val TAG = "HomeFragment"

        fun newInstance(): HomeFragment = HomeFragment()
    }
}
