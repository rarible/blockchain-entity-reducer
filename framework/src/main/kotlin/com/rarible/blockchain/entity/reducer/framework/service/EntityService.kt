package com.rarible.blockchain.entity.reducer.framework.service

import com.rarible.blockchain.entity.reducer.framework.model.Identifiable

interface EntityService<Id, E> {
    suspend fun get(id: Id): E?

    suspend fun update(entity: E): E
}

interface EntityEventService<Event, Id> {
    fun getEntityId(event: Event): Id
}

interface EntityTemplateProvider<Id, E : Identifiable<Id>> {
    fun getEntityTemplate(id: Id): E
}