package com.museum.presentation

import com.whitelabel.core.domain.model.ItemGroup
import com.whitelabel.core.domain.repository.ItemRepository
import com.whitelabel.core.presentation.home.ItemGrouper
import com.whitelabel.platform.data.models.CatalogItem

class CountryItemGrouper : ItemGrouper<CatalogItem> {

    override suspend fun group(
        items: List<CatalogItem>,
        repository: ItemRepository<CatalogItem>,
        languageCode: String
    ): List<ItemGroup<CatalogItem>> {
        val countryToItems = mutableMapOf<String, MutableList<CatalogItem>>()

        for (item in items) {
            val countries = item.author
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("Unknown")

            for (country in countries) {
                countryToItems.getOrPut(country) { mutableListOf() }.add(item)
            }
        }

        return countryToItems.entries
            .sortedBy { it.key }
            .map { (country, countryItems) ->
                ItemGroup(
                    key = country,
                    displayName = country,
                    items = countryItems.sortedBy { it.name }
                )
            }
    }
}
