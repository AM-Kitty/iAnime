package com.project.ianime.repository

import androidx.annotation.WorkerThread
import com.project.ianime.api.AnimeService
import com.project.ianime.api.error.BadRequestException
import com.project.ianime.api.error.ConnectionException
import com.project.ianime.api.error.HttpStatus
import com.project.ianime.api.error.NotFoundException
import com.project.ianime.api.error.UnauthorizedException
import com.project.ianime.api.model.AnimeApiModel
import com.project.ianime.data.AnimeDao
import com.project.ianime.data.AnimeEntity
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import javax.inject.Inject

class AnimeDataRepositoryImpl @Inject constructor(
    private val animeService: AnimeService,
    private val animeDao: AnimeDao
) : AnimeDataRepository {

    override fun getAnimeListFromNetwork(): Single<List<AnimeApiModel>> {
        return animeService.getAnimeListFromNetwork()
            .subscribeOn(Schedulers.io())
            .flatMap { response ->
                var animeList = response.animeList ?: emptyList()

                // sort anime list based on rate
                animeList = animeList.sortedByDescending {
                    it.rate
                }

                Single.just(animeList)
            }
            .onErrorResumeNext { exception ->
                if (exception is HttpException) {
                    exception.code().let {
                        when {
                            it == HttpStatus.BAD_REQUEST -> {
                                return@onErrorResumeNext Single.error(
                                    BadRequestException("Bad Request Error")
                                )
                            }

                            it == HttpStatus.UNAUTHORIZED -> {
                                return@onErrorResumeNext Single.error(
                                    UnauthorizedException("Unauthorized Error")
                                )
                            }

                            it == HttpStatus.NOT_FOUND -> {
                                return@onErrorResumeNext Single.error(
                                    NotFoundException("Not Found Error")
                                )
                            }

                            it >= 500 -> {
                                return@onErrorResumeNext Single.error(
                                    ConnectionException("Connection Error")
                                )
                            }

                            else -> {
                                return@onErrorResumeNext Single.error(exception)
                            }
                        }
                    }
                }
                Single.error(exception)
            }
    }

    override fun getAnimeDetailsById(animeId: String) = animeDao.getAnimeById(animeId)

    @WorkerThread
    override suspend fun insertAnimeIntoDatabase(animeEntity: AnimeEntity) = animeDao.insertAnime(animeEntity)

    @WorkerThread
    override suspend fun clearOfflineAnimeList(){
        animeDao.deleteAll()
        animeDao.resetPrimaryKey()
    }

    override fun isDatabaseEmpty(): Boolean {
        return animeDao.getAnimeCount() == 0
    }

    @WorkerThread
    override suspend fun getOfflineAnimeListSynchronously(): List<AnimeEntity> {
        return animeDao.getAllAnimes().firstOrNull() ?: emptyList()
    }

}