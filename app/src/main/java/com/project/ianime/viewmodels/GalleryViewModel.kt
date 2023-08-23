package com.project.ianime.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.ianime.api.data.AnimeGalleryItem
import com.project.ianime.api.error.*
import com.project.ianime.repository.AnimeDataRepository
import com.project.ianime.screens.stateholder.AnimeUiState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

class GalleryViewModel @Inject constructor(private val repository: AnimeDataRepository) : ViewModel() {

    private val _animeGalleryList = MutableLiveData<List<AnimeGalleryItem>>()
    val animeGalleryList: LiveData<List<AnimeGalleryItem>> = _animeGalleryList

    lateinit var animeUiState: MutableLiveData<AnimeUiState>

    val viewScopeSubscriptionTracker = CompositeDisposable()

    fun getAnimeGalleryList() {
        repository.getGalleryList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ galleryItems ->
                if (galleryItems.isEmpty()) {
                    animeUiState.postValue(AnimeUiState.Empty)
                } else {
                    animeUiState.postValue(AnimeUiState.Success)
                    _animeGalleryList.postValue(galleryItems)
                }
            }, {
                when (it) {
                    is UnauthorizedException -> {
                        animeUiState.postValue(AnimeUiState.Error(ErrorType.UNAUTHORIZED))
                    }
                    is NotFoundException -> {
                        animeUiState.postValue((AnimeUiState.Error(ErrorType.NOT_FOUND)))
                    }
                    is ConnectionException -> {
                        animeUiState.postValue(AnimeUiState.Error(ErrorType.CONNECTION))
                    }
                    is BadRequestException -> {
                        animeUiState.postValue(AnimeUiState.Error(ErrorType.BAD_REQUEST))
                    }
                    else -> animeUiState.postValue(AnimeUiState.Error(ErrorType.GENERIC))
                }
            }
            ).also { viewScopeSubscriptionTracker.add(it) }
    }

}