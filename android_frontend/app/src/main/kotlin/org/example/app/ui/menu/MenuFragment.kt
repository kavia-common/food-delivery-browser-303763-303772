package org.example.app.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.cart.CartRepository
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.mock.MockData
import org.example.app.data.models.ItemConfiguration
import org.example.app.data.models.MenuItem
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType
import org.example.app.ui.common.MotionUtils
import org.example.app.ui.ratings.ReviewEditorDialogFragment
import org.example.app.ui.ratings.ReviewsAdapter

class MenuFragment : Fragment() {

    private lateinit var title: TextView
    private lateinit var meta: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuItemAdapter

    // Ratings section
    private lateinit var addReviewButton: MaterialButton
    private lateinit var ratingsSummary: TextView
    private lateinit var noReviewsHint: TextView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter

    private var restaurantId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restaurantId = requireArguments().getString(ARG_RESTAURANT_ID).orEmpty()

        adapter = MenuItemAdapter(
            onItemClick = { item -> openOptionsAndAdd(item) },
            onAdd = { item -> openOptionsAndAdd(item) },
            onInc = { item ->
                // If item has options, increasing should go through the picker to avoid ambiguity.
                if (item.variantGroups.isNotEmpty() || item.addOnGroups.isNotEmpty()) {
                    openOptionsAndAdd(item)
                } else {
                    CartRepository.add(item)
                }
            },
            onDec = { item ->
                // For configurable items, decrementing is ambiguous (which line?). We route to cart.
                if (item.variantGroups.isNotEmpty() || item.addOnGroups.isNotEmpty()) {
                    (activity as? MainActivity)?.openCartTab()
                } else {
                    val q = CartRepository.getQuantity(item.id)
                    CartRepository.updateQuantity(item, q - 1)
                }
            },
            getQuantity = { itemId -> CartRepository.getQuantity(itemId) },
            isFavorited = { itemId -> FavoritesRepository.isMenuItemFavorited(itemId) },
            onToggleFavorite = { itemId -> FavoritesRepository.toggleMenuItemFavorite(itemId) },
            onOpenReviews = { item -> openReviewsForMenuItem(item) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.menuRestaurantTitle)
        meta = view.findViewById(R.id.menuRestaurantMeta)
        chipGroup = view.findViewById(R.id.categoryChipGroup)
        recyclerView = view.findViewById(R.id.menuRecyclerView)

        // Ratings section views live in included layout.
        val ratingsRoot = view.findViewById<View>(R.id.ratingsSection)
        addReviewButton = ratingsRoot.findViewById(R.id.addReviewButton)
        ratingsSummary = ratingsRoot.findViewById(R.id.ratingsSummary)
        noReviewsHint = ratingsRoot.findViewById(R.id.noReviewsHint)
        reviewsRecyclerView = ratingsRoot.findViewById(R.id.reviewsRecyclerView)

        val restaurantTarget = ReviewTarget(ReviewTargetType.RESTAURANT, restaurantId)

        reviewsAdapter = ReviewsAdapter(
            onEdit = { review ->
                ReviewEditorDialogFragment.newEdit(restaurantTarget, review.id)
                    .show(parentFragmentManager, "ReviewEditorDialogFragment")
            },
            onDelete = { review ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.delete_review_title))
                    .setMessage(getString(R.string.delete_review_message))
                    .setPositiveButton(R.string.delete) { _, _ -> RatingsRepository.deleteReview(review.id) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        reviewsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        reviewsRecyclerView.adapter = reviewsAdapter
        reviewsRecyclerView.itemAnimator = MotionUtils.createSubtleItemAnimator(requireContext())

        addReviewButton.setOnClickListener {
            ReviewEditorDialogFragment.newAdd(restaurantTarget)
                .show(parentFragmentManager, "ReviewEditorDialogFragment")
        }

        val restaurant = MockData.restaurantById(restaurantId)
        title.text = restaurant?.name ?: getString(R.string.menu_title)

        // Meta line keeps existing mock rating; ratings section shows user aggregate.
        meta.text = if (restaurant != null) {
            "${Formatters.ratingText(restaurant.rating)} • ${Formatters.etaText(restaurant.etaMinutesMin, restaurant.etaMinutesMax)}"
        } else ""
        meta.isVisible = restaurant != null

        // Observe ratings/reviews for this restaurant.
        RatingsRepository.getAggregate(restaurantTarget).observe(viewLifecycleOwner) { agg ->
            ratingsSummary.text = if (agg != null && agg.count > 0) {
                // "4.6 ★ • Based on 12 reviews"
                "${Formatters.ratingText(agg.average)} ★ • " +
                    getString(R.string.based_on_reviews, agg.count)
            } else {
                // Show a consistent empty state.
                getString(R.string.no_reviews_summary)
            }
        }
        RatingsRepository.getReviews(restaurantTarget).observe(viewLifecycleOwner) { reviews ->
            val latest = (reviews ?: emptyList()).take(3)
            reviewsAdapter.submitList(latest)
            noReviewsHint.isVisible = latest.isEmpty()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = MotionUtils.createSubtleItemAnimator(requireContext())

        setupCategoryChips()

        adapter.submitList(MockData.menuForRestaurant(restaurantId))

        // Refresh quantities when cart changes.
        CartRepository.cartLines.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }

        // Refresh heart icons when favorites change.
        FavoritesRepository.favoriteMenuItemIdsLive.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupCategoryChips() {
        chipGroup.removeAllViews()
        val cats = MockData.categories

        fun applyFilter(categoryIdOrNull: String?) {
            val all = MockData.menuForRestaurant(restaurantId)
            val filtered = if (categoryIdOrNull == null) all else all.filter { it.categoryId == categoryIdOrNull }
            adapter.submitList(filtered)
        }

        val allChip = layoutInflater.inflate(R.layout.item_category_chip, chipGroup, false) as Chip
        allChip.text = "All"
        allChip.isCheckable = true
        allChip.isChecked = true
        allChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) applyFilter(null)
        }
        chipGroup.addView(allChip)

        cats.forEach { c ->
            val chip = layoutInflater.inflate(R.layout.item_category_chip, chipGroup, false) as Chip
            chip.text = c.title
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) applyFilter(c.id)
            }
            chipGroup.addView(chip)
        }
    }

    private fun openOptionsAndAdd(item: MenuItem) {
        val sheet = ItemOptionsBottomSheet.newInstance()
        sheet.bind(
            item = item,
            initialConfiguration = ItemConfiguration(),
            initialQuantity = 1,
            isEditMode = false,
            initialItemNote = "",
            listener = object : ItemOptionsBottomSheet.Listener {
                override fun onConfirmed(item: MenuItem, configuration: ItemConfiguration, quantity: Int, itemNote: String) {
                    // quantity from sheet is 1 in menu flow; keep API flexible.
                    repeat(quantity.coerceAtLeast(1)) {
                        CartRepository.addConfigured(item, configuration)
                    }
                    // Apply note to the resulting line (notes do not impact totals).
                    val keyLineQty = CartRepository.getLineQuantity(item.id, configuration)
                    if (keyLineQty > 0) {
                        CartRepository.updateLineItemNote(
                            org.example.app.data.models.CartLine(item, configuration, keyLineQty),
                            itemNote
                        )
                    }
                }
            }
        )
        sheet.show(parentFragmentManager, "ItemOptionsBottomSheet")
    }

    private fun openReviewsForMenuItem(item: MenuItem) {
        val target = ReviewTarget(ReviewTargetType.MENU_ITEM, item.id)
        ReviewsListDialogFragment.newInstance(
            title = item.name,
            targetType = target.type,
            targetId = target.id
        ).show(parentFragmentManager, "ReviewsListDialogFragment")
    }

    companion object {
        const val TAG = "MenuFragment"
        private const val ARG_RESTAURANT_ID = "restaurant_id"

        fun newInstance(restaurantId: String): MenuFragment {
            return MenuFragment().apply {
                arguments = Bundle().apply { putString(ARG_RESTAURANT_ID, restaurantId) }
            }
        }
    }
}
