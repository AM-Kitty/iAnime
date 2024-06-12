package com.project.ianime

import com.project.ianime.api.AnimeService
import com.project.ianime.api.error.BadRequestException
import com.project.ianime.api.error.ConnectionException
import com.project.ianime.api.error.NotFoundException
import com.project.ianime.api.error.UnauthorizedException
import com.project.ianime.api.model.AnimeApiModel
import com.project.ianime.api.model.AnimeCountryApiModel
import com.project.ianime.api.model.AnimeGenreApiModel
import com.project.ianime.api.model.AnimeListApiModel
import com.project.ianime.api.model.AnimeStatusApiModel
import com.project.ianime.data.AnimeDao
import com.project.ianime.data.AnimeEntity
import com.project.ianime.repository.AnimeDataRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AnimeDataRepositoryTest {
    private lateinit var animeService: AnimeService

    private lateinit var animeDao: AnimeDao

    private lateinit var repository: AnimeDataRepositoryImpl

    private val testDispatcher = TestScope()

    @Before
    fun setUp() {
        animeService = mockk()
        animeDao = mockk()
        repository = AnimeDataRepositoryImpl(animeService, animeDao)
    }

    @Test
    fun testGetSortedAnimeListSuccessfullyFromNetwork() {
        val animeApiData = listOf(
            AnimeApiModel(
                "01",
                "SAO",
                9.9F,
                AnimeStatusApiModel.FINISHED,
                AnimeCountryApiModel.JAP_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2012",
                "Some description",
                ""
            ),
            AnimeApiModel(
                "02",
                "Perfect World",
                10.0F,
                AnimeStatusApiModel.IN_PROGRESS,
                AnimeCountryApiModel.CHN_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2021",
                "Some description",
                ""
            )
        )

        val expectedAnimeList = listOf(
            AnimeApiModel(
                "02",
                "Perfect World",
                10.0F,
                AnimeStatusApiModel.IN_PROGRESS,
                AnimeCountryApiModel.CHN_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2021",
                "Some description",
                ""
            ),
            AnimeApiModel(
                "01",
                "SAO",
                9.9F,
                AnimeStatusApiModel.FINISHED,
                AnimeCountryApiModel.JAP_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2012",
                "Some description",
                ""
            )
        )

        every { animeService.getAnimeListFromNetwork() } returns Single.just(
            AnimeListApiModel(
                animeApiData
            )
        )

        val testObserver = repository.getAnimeListFromNetwork()
            .subscribeOn(Schedulers.trampoline())
            .test().await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(expectedAnimeList)
        verify(exactly = 1) { animeService.getAnimeListFromNetwork() }
    }

    @Test
    fun testHandleBadRequestError() {
        val errorCode = 400
        val errorMessage = "Bad Request Error"
        val errorResponseBody = errorMessage.toResponseBody("text/plain".toMediaTypeOrNull())
        val response = Response.error<Unit>(errorCode, errorResponseBody)
        val httpException = HttpException(response)

        every { animeService.getAnimeListFromNetwork() } returns Single.error(httpException)

        val testObserver = repository.getAnimeListFromNetwork()
            .test()
            .await()
        testObserver.assertNotComplete()
        testObserver.assertError {
            it is BadRequestException && it.message == errorMessage
        }
    }

    @Test
    fun testHandleUnAuthorizedError() {
        val errorCode = 401
        val errorMessage = "Unauthorized Error"
        val errorResponseBody = errorMessage.toResponseBody("text/plain".toMediaTypeOrNull())
        val response = Response.error<Unit>(errorCode, errorResponseBody)
        val httpException = HttpException(response)

        every { animeService.getAnimeListFromNetwork() } returns Single.error(httpException)

        val testObserver = repository.getAnimeListFromNetwork()
            .test()
            .await()
        testObserver.assertNotComplete()
        testObserver.assertError {
            it is UnauthorizedException && it.message == errorMessage
        }
    }

    @Test
    fun testHandleNotFoundError() {
        val errorCode = 404
        val errorMessage = "Not Found Error"
        val errorResponseBody = errorMessage.toResponseBody("text/plain".toMediaTypeOrNull())
        val response = Response.error<Unit>(errorCode, errorResponseBody)
        val httpException = HttpException(response)

        every { animeService.getAnimeListFromNetwork() } returns Single.error(httpException)

        val testObserver = repository.getAnimeListFromNetwork()
            .test()
            .await()
        testObserver.assertNotComplete()
        testObserver.assertError {
            it is NotFoundException && it.message == errorMessage
        }
    }

    @Test
    fun testHandleConnectionError() {
        val errorCode = 505
        val errorMessage = "Connection Error"
        val errorResponseBody = errorMessage.toResponseBody("text/plain".toMediaTypeOrNull())
        val response = Response.error<Unit>(errorCode, errorResponseBody)
        val httpException = HttpException(response)

        every { animeService.getAnimeListFromNetwork() } returns Single.error(httpException)

        val testObserver = repository.getAnimeListFromNetwork()
            .test()
            .await()
        testObserver.assertNotComplete()
        testObserver.assertError {
            it is ConnectionException && it.message == errorMessage
        }
    }

    @Test
    fun getAnimeById() {
        val animeId = "01"
        val expectedAnime = AnimeEntity(
            1,
            "01",
            "SAO",
            8.0F,
            AnimeStatusApiModel.FINISHED,
            AnimeCountryApiModel.JAP_ANIME,
            AnimeGenreApiModel.LEGEND,
            "2012",
            "Some description",
            ""
        )

        every { animeDao.getAnimeById(animeId) } returns flowOf(expectedAnime)

        val flow = repository.getAnimeDetailsById(animeId)
        testDispatcher.runTest {
            flow.collect { anime ->
                assertEquals(expectedAnime, anime)
            }
        }
    }

    @Test
    fun testInsertAnimeIntoDatabase() = runBlocking {
        val addAnime = AnimeEntity(
            1,
            "02",
            "Perfect World",
            10.0F,
            AnimeStatusApiModel.IN_PROGRESS,
            AnimeCountryApiModel.CHN_ANIME,
            AnimeGenreApiModel.LEGEND,
            "2021",
            "Some description",
            ""
        )

        coEvery { animeDao.insertAnime(addAnime) } just runs

        repository.insertAnimeIntoDatabase(addAnime)
        coVerify(exactly = 1) { animeDao.insertAnime(addAnime) }
    }

    @Test
    fun testClearOfflineAnimeList() = runBlocking {
        coEvery { animeDao.deleteAll() } just runs
        coEvery { animeDao.resetPrimaryKey() } just runs

        repository.clearOfflineAnimeList()
        coVerify(exactly = 1) { animeDao.deleteAll() }
        coVerify(exactly = 1) { animeDao.resetPrimaryKey() }
    }

    @Test
    fun testCheckDatabaseEmpty() {
        every { animeDao.getAnimeCount() } returns 10

        val testObserver = repository.isDatabaseEmpty()
        assertEquals(false, testObserver)
    }

    @Test
    fun testGetOfflineAnimeList() = runBlocking {
        val expectedAnimeList = listOf(
            AnimeEntity(
                1,
                "02",
                "Perfect World",
                10.0F,
                AnimeStatusApiModel.IN_PROGRESS,
                AnimeCountryApiModel.CHN_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2021",
                "Some description",
                ""
            ),
            AnimeEntity(
                2,
                "01",
                "SAO",
                9.9F,
                AnimeStatusApiModel.FINISHED,
                AnimeCountryApiModel.JAP_ANIME,
                AnimeGenreApiModel.LEGEND,
                "2012",
                "Some description",
                ""
            )
        )

        coEvery { animeDao.getAllAnimes() } returns flowOf(expectedAnimeList)

        val testObserver = repository.getOfflineAnimeListSynchronously()
        assertEquals(2, testObserver.size)
        assertEquals(expectedAnimeList[0], testObserver[0])
        assertEquals(2, testObserver[1].id)
    }

}