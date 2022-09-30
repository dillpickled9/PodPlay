package com.raywenderlich.podplay.viewmodel

import PodcastDao
import android.app.Application
import androidx.lifecycle.*
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.launch
import java.util.*

class PodcastViewModel(application: Application) :
    AndroidViewModel(application) {
    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    private val _podcastLiveData = MutableLiveData<PodcastViewData?
            >()
    val podcastLiveData: LiveData<PodcastViewData?> =
        _podcastLiveData
    val podcastDao : PodcastDao = PodPlayDatabase
        .getInstance(application, viewModelScope)
        .podcastDao()
    private var activePodcast: Podcast? = null
    var livePodcastSummaryData:
            LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null



    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )
    data class EpisodeViewData (
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    )

    //converts episodes to objects
    private fun episodesToEpisodesView(episodes: List<Episode>):
            List<EpisodeViewData> {
        return episodes.map {
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }

    private fun podcastToPodcastView(podcast: Podcast):
            PodcastViewData {
        return PodcastViewData(
            false,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        podcastSummaryViewData.feedUrl?.let { url ->
            viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let {
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.value = podcastToPodcastView(it)
                    activePodcast = it
                } ?: run {
                    _podcastLiveData.value = null
                }
            }
        } ?: run {
            _podcastLiveData.value = null
        }
    }

    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.save(it)
        }
    }

    private fun podcastToSummaryView(podcast: Podcast):
            SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl)
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null
        if (livePodcastSummaryData == null) {

            val liveData = repo.getAll()
            livePodcastSummaryData = Transformations.map(liveData)
            { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }

        return livePodcastSummaryData
    }
}