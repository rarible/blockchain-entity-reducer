package com.rarible.blockchain.entity.reducer.framework.service

import com.rarible.blockchain.entity.reducer.framework.model.Entity
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord

interface EntityService<K, L : Log<L>, R : LogRecord<L, R>, E : Entity<K, L, R, E>> {
    suspend fun get(id: K): E?

    suspend fun update(entity: E): E

    fun getEntityTemplate(id: K): E

    fun getEntityId(logRecord: R): K
}
