package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.raywenderlich.podplay.viewmodel.SearchViewModel.PodcastSummaryViewData

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var databinding: ActivityPodcastBinding

    private val TAG = javaClass.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding=ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        //setContentView(R.layout.activity_podcast)
       //val itunesService = ItunesService.instance
        //val itunesRepo = ItunesRepo(itunesService)
        //GlobalScope.launch {
            //val results = itunesRepo.searchByTerm("Android Developer")
            //Log.i(TAG, "Results = ${results.body()}")

        //}
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)

    }

    //inflates options menu and creates search widget
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        val searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem?.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
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

    override fun onShowDetails(
        podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData
    ) {
        // Not implemented yet
    }

    private fun showProgressBar() {
        databinding.progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        databinding.progressBar.visibility = View.INVISIBLE
    }
}