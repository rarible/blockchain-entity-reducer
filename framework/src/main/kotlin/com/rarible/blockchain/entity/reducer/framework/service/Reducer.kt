package com.rarible.blockchain.entity.reducer.framework.service

interface Reducer<Event, E> {
    suspend fun reduce(entity: E, event: Event): E
}

