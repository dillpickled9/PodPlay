package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapRegionDecoder.newInstance
import android.media.ImageReader.newInstance
import android.media.ImageWriter.newInstance
import android.net.sip.SipManager.newInstance
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo.newInstance
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest.newInstance
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo.newInstance
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest.newInstance
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.util.pool.StateVerifier.newInstance
import com.google.android.material.datepicker.MaterialCalendar.newInstance
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserFactory.newInstance
import java.lang.reflect.Array.newInstance
import java.net.URLClassLoader.newInstance
import java.security.KeyStore.Builder.newInstance
import javax.xml.datatype.DatatypeFactory.newInstance
import javax.xml.parsers.DocumentBuilderFactory.newInstance
import javax.xml.parsers.SAXParserFactory.newInstance
import javax.xml.transform.TransformerFactory.newInstance
import javax.xml.validation.SchemaFactory.newInstance
import javax.xml.xpath.XPathFactory.newInstance


class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var searchMenuItem: MenuItem
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var databinding: ActivityPodcastBinding




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding=ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        createSubscription()
        handleIntent(intent)
        addBackStackListener()

    }

    //inflates options menu and creates search widget
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (supportFragmentManager.backStackEntryCount > 0) {
            databinding.podcastRecyclerView.visibility = View.INVISIBLE
        }
        if (databinding.podcastRecyclerView.visibility ==
            View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    private fun performSearch(term: String) {
        showProgressBar()
        GlobalScope.launch {
            val results = searchViewModel.searchPodcasts(term)
            withContext(Dispatchers.Main) {
                hideProgressBar()
                databinding.toolbar.title = term
                podcastListAdapter.setSearchData(results)
            }
        }
    }

    //handles incoming intents
    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?:
            return
            performSearch(query)
        }
    }
    //overrides to receive updated intent with new search
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    //makes tookbar act as the ActionBar
    private fun setupToolbar(){
        setSupportActionBar(databinding.toolbar)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        val rssService = RssFeedService.instance
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastViewModel.podcastDao)
    }

    private fun updateControls() {
        databinding.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        databinding.podcastRecyclerView.layoutManager =
            layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            databinding.podcastRecyclerView.context,
            layoutManager.orientation)

        databinding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        databinding.podcastRecyclerView.adapter = podcastListAdapter
    }

    override fun onShowDetails(podcastSummaryViewData:
                               SearchViewModel.PodcastSummaryViewData) {
        podcastSummaryViewData.feedUrl?.let {
            showProgressBar()
            podcastViewModel.getPodcast(podcastSummaryViewData)
        }
    }

    private fun showProgressBar() {
        databinding.progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        databinding.progressBar.visibility = View.INVISIBLE
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }

    private fun createPodcastDetailsFragment():
            PodcastDetailsFragment {

        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as
                PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {

        val podcastDetailsFragment = createPodcastDetailsFragment()

        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()

        databinding.podcastRecyclerView.visibility = View.INVISIBLE

        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                databinding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun createSubscription() {
        podcastViewModel.podcastLiveData.observe(this, {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed")
            }
        })
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun showSubscribedPodcasts() {
        // 1
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            databinding.toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }


private fun setupPodcastListView() {
    podcastViewModel.getPodcasts()?.observe(this, {
        if (it != null) {
            showSubscribedPodcasts()
        }
    })
}
}