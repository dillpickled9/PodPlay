package com.raywenderlich.podplay.service

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {
    // 1
    @GET("/search?media=podcast")
    // 2
    suspend fun searchPodcastByTerm(@Query("term") term: String):
            Response<PodcastResponse>
    // 3
    companion object {
        // 4
        val instance: ItunesService by lazy {
            // 5
            val retrofit = Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit.create(ItunesService::class.java)
        }
    }
}