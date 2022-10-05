package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.databinding.FragmentPodcastDetailsBinding
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class PodcastDetailsFragment : Fragment(),
    EpisodeListAdapter.EpisodeListAdapterListener {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var databinding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailsListener? = null


    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        databinding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        return databinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, { viewData ->
                if (viewData != null) {
                    databinding.feedTitleTextView.text = viewData.feedTitle
                    databinding.feedDescTextView.text = viewData.feedDesc
                    activity?.let { activity ->
                        Glide.with(activity).load(viewData.imageUrl).into(databinding.feedImageView)
                    }

                    databinding.feedDescTextView.movementMethod = ScrollingMovementMethod()

                    databinding.episodeRecyclerView.setHasFixedSize(true)

                    val layoutManager = LinearLayoutManager(activity)
                    databinding.episodeRecyclerView.layoutManager = layoutManager

                    val dividerItemDecoration = DividerItemDecoration(
                        databinding.episodeRecyclerView.context, layoutManager.orientation)
                    databinding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)

                    episodeListAdapter = EpisodeListAdapter(viewData.episodes, this)
                    databinding.episodeRecyclerView.adapter = episodeListAdapter

                    activity?.invalidateOptionsMenu()
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?:
        return
        databinding.feedTitleTextView.text = viewData.feedTitle
        databinding.feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->

            Glide.with(activity).load(viewData.imageUrl).into(databinding.feedImageView)
        }
    }


    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() +
                    " must implement OnPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_action -> {
                if (item.title == getString(R.string.unsubscribe)) {
                    listener?.onUnsubscribe()
                } else {
                    listener?.onSubscribe()
                }
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, { podcast ->
            if (podcast != null) {
                menu.findItem(R.id.menu_feed_action).title = if (podcast.subscribed)
                    getString(R.string.unsubscribe) else getString(R.string.subscribe)
            }
        })
        super.onPrepareOptionsMenu(menu)
    }









    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()

    }



    override fun onSelectedEpisode(episodeViewData:
                                   PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)}

}