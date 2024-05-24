package com.project.ianime.api.model

import com.google.gson.annotations.SerializedName

/**
 * API response for get list of animes
 */
data class AnimeListApiModel(
    @SerializedName("data") val animeList: List<AnimeApiModel>?
)
