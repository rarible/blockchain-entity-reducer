package com.rarible.blockchain.entity.reducer.framework.service

import com.rarible.blockchain.entity.reducer.framework.model.Entity
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord
import kotlinx.coroutines.flow.Flow

interface ReduceService<K, L : Log<L>, R : LogRecord<L, R>, E : Entity<K, L, R, E>> {
    suspend fun reduce(logRecords: Flow<R>): Flow<E>
}
