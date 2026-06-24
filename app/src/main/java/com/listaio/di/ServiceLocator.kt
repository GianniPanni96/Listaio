package com.listaio.di

import com.listaio.data.remote.FirestoreListRepository
import com.listaio.domain.ListRepository

/**
 * Tiny manual dependency container — keeps the app simple (no Hilt).
 * Caches a single ListRepository instance for the lifetime of the process.
 */
object ServiceLocator {

    @Volatile
    private var repository: ListRepository? = null

    fun provideListRepository(listId: String): ListRepository {
        return repository ?: synchronized(this) {
            repository ?: FirestoreListRepository(listId).also { repository = it }
        }
    }
}
