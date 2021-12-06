package com.rarible.blockchain.entity.reducer.framework.service

import kotlinx.coroutines.flow.Flow

interface ReduceService<Id, Event, E> {
    suspend fun reduce(events: Flow<Event>): Flow<E>
}
