package com.museum.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.museum.data.local.DatabaseDriverFactory
import com.museum.data.local.MuseumDatabase
import com.museum.data.Museum_item
import com.museum.utils.LocalizationManager
import com.museum.utils.SupportedLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class HeritageSiteLocalDataSource(
    driverFactory: DatabaseDriverFactory,
    private val dispatcher: CoroutineDispatcher
) {
    private val database = MuseumDatabase(driverFactory.createDriver())
    private val heritageQueries = database.heritageSiteQueries
    private val authorQueries = database.authorsQueries

    fun getAllSites(): Flow<List<Museum_item>> {
        com.museum.utils.LOG("HeritageSiteLocalDataSource.getAllSites() - CALLED, creating Flow")
        return heritageQueries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .flowOn(dispatcher)
    }

    fun getSiteById(id: Long): Flow<List<Museum_item>?> {
        com.museum.utils.LOG("HeritageSiteLocalDataSource.getSiteById() - CALLED for id=$id, creating Flow")
        return heritageQueries.selectById(id)
            .asFlow()
            .mapToList(dispatcher).onEach {
                items ->
                com.museum.utils.LOG("HeritageSiteLocalDataSource.getSiteById() - Database EMITTED ${items?.size ?: 0} items for id=$id")
            }
            .flowOn(dispatcher)
    }

    fun getFavoriteSites(): Flow<List<Museum_item>> {
        return heritageQueries.selectFavorites()
            .asFlow()
            .mapToList(dispatcher)
            .flowOn(dispatcher)
    }

    fun searchSites(query: String): Flow<List<Museum_item>> {
        val languageCode = LocalizationManager.getCurrentLanguageCode()
        val language = SupportedLanguage.fromCode(languageCode)

        val searchQuery = when (language) {
            SupportedLanguage.ROMANIAN -> heritageQueries.searchByNameRo(query, query, query, query)
            SupportedLanguage.ITALIAN -> heritageQueries.searchByNameIt(query, query, query, query)
            SupportedLanguage.SPANISH -> heritageQueries.searchByNameEs(query, query, query, query)
            SupportedLanguage.GERMAN -> heritageQueries.searchByNameDe(query, query, query, query)
            SupportedLanguage.FRENCH -> heritageQueries.searchByNameFr(query, query, query, query)
            SupportedLanguage.PORTUGUESE -> heritageQueries.searchByNamePt(query, query, query, query)
            SupportedLanguage.RUSSIAN -> heritageQueries.searchByNameRu(query, query, query, query)
            SupportedLanguage.ARABIC -> heritageQueries.searchByNameAr(query, query, query, query)
            SupportedLanguage.CHINESE -> heritageQueries.searchByNameZh(query, query, query, query)
            SupportedLanguage.JAPANESE -> heritageQueries.searchByNameJa(query, query, query, query)
            SupportedLanguage.ENGLISH -> heritageQueries.searchByName(query, query, query, query)
            SupportedLanguage.HINDI -> heritageQueries.searchByNameHi(query, query, query, query)
            SupportedLanguage.HUNGARIAN -> heritageQueries.searchByNameHu(query, query, query, query)
            SupportedLanguage.TURKISH -> heritageQueries.searchByNameTr(query, query, query, query)
            SupportedLanguage.POLISH -> heritageQueries.searchByNamePl(query, query, query, query)
        }

        return searchQuery
            .asFlow()
            .mapToList(dispatcher)
            .flowOn(dispatcher)
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        com.museum.utils.LOG("HeritageSiteLocalDataSource.updateFavorite() - CALLED id=$id, isFavorite=$isFavorite")
        withContext(dispatcher) {
            database.transaction {
                heritageQueries.updateFavorite(
                    isFavourite = if (isFavorite) "true" else "false",
                    id = id
                )
            }
        }
        com.museum.utils.LOG("HeritageSiteLocalDataSource.updateFavorite() - COMPLETE, DATABASE UPDATED")
    }

    suspend fun markAsViewed(id: Long) {
        com.museum.utils.LOG("HeritageSiteLocalDataSource.markAsViewed() - CALLED id=$id")
        withContext(dispatcher) {
            heritageQueries.updateViewed(id)
        }
        com.museum.utils.LOG("HeritageSiteLocalDataSource.markAsViewed() - COMPLETE, DATABASE UPDATED")
    }

    suspend fun getCount(): Long {
        return withContext(dispatcher) {
            heritageQueries.countAll().executeAsOne()
        }
    }

    suspend fun getCountryTranslations(countryNames: List<String>): Map<String, com.museum.data.SelectCountryTranslations> {
        return withContext(dispatcher) {
            val results = authorQueries.selectCountryTranslations(countryNames).executeAsList()
            results.associateBy { it.name ?: "" }
        }
    }
}
