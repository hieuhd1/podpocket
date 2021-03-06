package com.furkanaskin.app.podpocket.ui.profile.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.lifecycle.Observer
import com.furkanaskin.app.podpocket.R
import com.furkanaskin.app.podpocket.core.BaseFragment
import com.furkanaskin.app.podpocket.core.Constants
import com.furkanaskin.app.podpocket.core.Resource
import com.furkanaskin.app.podpocket.databinding.FragmentFavoritesBinding
import com.furkanaskin.app.podpocket.db.entities.EpisodeEntity
import com.furkanaskin.app.podpocket.db.entities.FavoriteEpisodeEntity
import com.furkanaskin.app.podpocket.service.response.Podcasts
import com.furkanaskin.app.podpocket.ui.dashboard.DashboardActivity
import com.furkanaskin.app.podpocket.ui.player.PlayerActivity
import com.furkanaskin.app.podpocket.ui.profile.favorites.favorite_episodes.FavoriteEpisodesAdapter
import com.furkanaskin.app.podpocket.utils.extensions.hide
import com.furkanaskin.app.podpocket.utils.extensions.show
import com.furkanaskin.app.podpocket.utils.extensions.showKeyboard
import iammert.com.view.scalinglib.ScalingLayoutListener
import iammert.com.view.scalinglib.State
import org.jetbrains.anko.doAsync

/**
 * Created by Furkan on 2019-05-18
 */

class FavoritesFragment : BaseFragment<FavoritesViewModel, FragmentFavoritesBinding>(FavoritesViewModel::class.java) {

    var ids: ArrayList<String> = ArrayList()

    override fun getLayoutRes(): Int = R.layout.fragment_favorites

    override fun initViewModel() {
        mBinding.viewModel = viewModel
    }

    override fun init() {

        mBinding.searchView.visibility = View.INVISIBLE
        initSearchView()

        // Scaling Layout stuffs starts here..

        mBinding.scalingLayout.setListener(object : ScalingLayoutListener {

            override fun onProgress(progress: Float) {
            }

            override fun onExpanded() {
                ViewCompat.animate(mBinding.buttonSearch).alpha(0f).setDuration(350).start()
                ViewCompat.animate(mBinding.searchView).alpha(1f).setDuration(350)
                    .setListener(object : ViewPropertyAnimatorListener {

                        override fun onAnimationEnd(view: View?) {
                            mBinding.buttonSearch.visibility = View.INVISIBLE
                        }

                        override fun onAnimationCancel(view: View?) {
                        }

                        override fun onAnimationStart(view: View?) {
                            mBinding.imageViewAppLogoSmall.visibility = View.INVISIBLE
                            mBinding.textViewAppName.visibility = View.INVISIBLE
                            mBinding.searchView.visibility = View.VISIBLE
                        }
                    }).start()
            }

            override fun onCollapsed() {
                ViewCompat.animate(mBinding.buttonSearch).alpha(1f).setDuration(200).start()
                ViewCompat.animate(mBinding.searchView).alpha(0f).setDuration(200)
                    .setListener(object : ViewPropertyAnimatorListener {
                        override fun onAnimationEnd(view: View?) {
                            mBinding.searchView.visibility = View.INVISIBLE
                        }

                        override fun onAnimationCancel(view: View?) {
                        }

                        override fun onAnimationStart(view: View?) {
                            mBinding.buttonSearch.visibility = View.VISIBLE
                            mBinding.imageViewAppLogoSmall.visibility = View.VISIBLE
                            mBinding.textViewAppName.visibility = View.VISIBLE
                        }
                    }).start()
            }
        })

        mBinding.scalingLayout.setOnClickListener {
            if (mBinding.scalingLayout.state == State.COLLAPSED) {
                mBinding.scalingLayout.expand()
                (activity as? DashboardActivity)?.let { dashboardActivity ->
                    mBinding.searchView.showKeyboard(
                        dashboardActivity
                    )
                }
                mBinding.searchView.requestFocus()
            }
        }

        mBinding.rootView.setOnClickListener {
            if (mBinding.scalingLayout.state == State.EXPANDED)
                mBinding.scalingLayout.collapse()
        }

        // Scaling Layout stuffs ends here..
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FavoriteEpisodesAdapter { item, position ->

            // Get podcastId and collect all episode Ids then navigate PlayerActivity
            viewModel.getEpisodes(item.podcastId ?: "")

            if (viewModel.episodesLiveData.hasActiveObservers())
                viewModel.episodesLiveData.removeObservers(this)

            viewModel.episodesLiveData.observe(
                this,
                Observer<Resource<Podcasts>> {

                    it.data?.episodes?.forEachIndexed { _, episodesItem ->
                        ids.add(episodesItem?.id ?: "")
                    }

                    doAsync {
                        viewModel.db?.episodesDao()?.deleteAllEpisodes()
                        it.data?.episodes?.forEachIndexed { _, episode ->
                            val episodesItem = episode.let { EpisodeEntity(it) }
                            episodesItem.let { viewModel.db?.episodesDao()?.insertEpisode(it) }
                        }
                    }

                    val intent = Intent(activity, PlayerActivity::class.java)
                    intent.putStringArrayListExtra(Constants.IntentName.PLAYER_ACTIVITY_ALL_IDS, ids)
                    intent.putExtra(Constants.IntentName.PLAYER_ACTIVITY_POSITION, item.id)
                    startActivity(intent)
                }
            )
        }

        viewModel.db?.favoritesDao()?.getFavoriteEpisodes()?.removeObservers(this)
        viewModel.db?.favoritesDao()?.getFavoriteEpisodes()?.observe(
            this,
            Observer<List<FavoriteEpisodeEntity>> { t ->

                if (t.isNotEmpty()) {
                    hideAnimation()
                    mBinding.recyclerViewFavoriteEpisodes.adapter = adapter
                    (mBinding.recyclerViewFavoriteEpisodes.adapter as FavoriteEpisodesAdapter).submitList(t)
                } else {
                    showAnimation()
                }
            }
        )
    }

    private fun initSearchView() {
        val searchEditText: EditText = mBinding.searchView.findViewById(R.id.search_src_text)
        activity?.applicationContext?.let { ContextCompat.getColor(it, R.color.white) }
            ?.let { searchEditText.setTextColor(it) }
        activity?.applicationContext?.let { ContextCompat.getColor(it, R.color.grayTextColor) }
            ?.let { searchEditText.setHintTextColor(it) }
        searchEditText.clearFocus()
        mBinding.searchView.isActivated = true
        mBinding.searchView.setIconifiedByDefault(false)
        mBinding.searchView.isIconified = false
        val searchViewSearchIcon = mBinding.searchView.findViewById<ImageView>(R.id.search_mag_icon)
        val searchViewCloseIcon = mBinding.searchView.findViewById<ImageView>(R.id.search_close_btn)
        searchViewSearchIcon.setImageResource(R.drawable.ic_search)
        searchViewCloseIcon.setImageResource(R.color.mainBackgroundColor)
        val linearLayoutSearchView: ViewGroup = searchViewSearchIcon.parent as ViewGroup
        linearLayoutSearchView.removeView(searchViewSearchIcon)
        linearLayoutSearchView.addView(searchViewSearchIcon)
        mBinding.searchView.clearFocus()
        mBinding.fakeFocus.requestFocus()

        mBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (newText?.length ?: 0 > 1) {
                    viewModel.db?.favoritesDao()?.getFavoriteEpisodes(newText)?.removeObservers(this@FavoritesFragment)
                    viewModel.db?.favoritesDao()?.getFavoriteEpisodes(newText)?.observe(
                        this@FavoritesFragment,
                        Observer<List<FavoriteEpisodeEntity>> {
                            (mBinding.recyclerViewFavoriteEpisodes.adapter as FavoriteEpisodesAdapter).submitList(it)
                        }
                    )
                } else if (newText?.length == 0) {
                    viewModel.db?.favoritesDao()?.getFavoriteEpisodes()?.removeObservers(this@FavoritesFragment)
                    viewModel.db?.favoritesDao()?.getFavoriteEpisodes()?.observe(
                        this@FavoritesFragment,
                        Observer<List<FavoriteEpisodeEntity>> {
                            (mBinding.recyclerViewFavoriteEpisodes.adapter as FavoriteEpisodesAdapter).submitList(it)
                        }
                    )
                }

                return true
            }
        })
    }

    private fun showAnimation() {
        mBinding.scalingLayout.hide()
        mBinding.textViewMainHeading.hide()
        mBinding.textViewTagLine.hide()
        mBinding.recyclerViewFavoriteEpisodes.hide()

        mBinding.lottieAnimationView.show()
        mBinding.textViewDummyText.show()
    }

    private fun hideAnimation() {
        mBinding.scalingLayout.show()
        mBinding.textViewMainHeading.show()
        mBinding.textViewTagLine.show()
        mBinding.recyclerViewFavoriteEpisodes.show()

        mBinding.lottieAnimationView.hide()
        mBinding.textViewDummyText.hide()
    }
}