package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.viewmodel.SearchViewModel.PodcastSummaryViewData
import kotlinx.coroutines.launch
import java.util.*

class PodcastViewModel(application: Application) :
    AndroidViewModel(application) {

    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    val podcastLiveData: LiveData<PodcastViewData?> =
        _podcastLiveData

    val podcastDao : PodcastDao = PodPlayDatabase
        .getInstance(application, viewModelScope)
        .podcastDao()

    var activeEpisodeViewData: EpisodeViewData? = null

    private var activePodcast: Podcast? = null
    var livePodcastSummaryData: LiveData<List<PodcastSummaryViewData>>? = null

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
        var duration: String? = "",
        var isVideo: Boolean = false
    )

    //converts episodes to objects
    private fun episodesToEpisodesView(episodes: List<Episode>):
            List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(it.guid, it.title, it.description,
                it.mediaUrl, it.releaseDate, it.duration, isVideo)
        }
    }

    private fun podcastToPodcastView(podcast: Podcast):
            PodcastViewData {
        return PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    suspend fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        podcastSummaryViewData.feedUrl?.let { url ->

                podcastRepo?.getPodcast(url)?.let {
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.value = podcastToPodcastView(it)
                    activePodcast = it
                } ?: run {
                    _podcastLiveData.value = null
                }

    }?: run {
            _podcastLiveData.value = null
        }
    }

    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            it.episodes = it.episodes.drop(1)
            repo.save(it)
        }
    }

    private fun podcastToSummaryView(podcast: Podcast):
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl)
    }

     fun getPodcasts(): LiveData<List<PodcastSummaryViewData>>? {
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

    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    suspend fun setActivePodcast(feedUrl: String):
            PodcastSummaryViewData? {
        val repo = podcastRepo ?: return null
        val podcast = repo.getPodcast(feedUrl)
        if (podcast == null) {
            return null
        } else {
            _podcastLiveData.value = podcastToPodcastView(podcast)
            activePodcast = podcast
            return podcastToSummaryView(podcast)
        }
    }
}


