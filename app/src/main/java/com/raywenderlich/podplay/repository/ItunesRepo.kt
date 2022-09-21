package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.service.ItunesService

//method that accepts search terms
class ItunesRepo(private val itunesService: ItunesService) {

    suspend fun searchByTerm(term: String) =
        itunesService.searchPodcastByTerm(term)
}