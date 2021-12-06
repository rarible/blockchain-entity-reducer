package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.model.Identifiable
import com.rarible.blockchain.entity.reducer.framework.service.EntityEventService
import com.rarible.blockchain.entity.reducer.framework.service.EntityService
import com.rarible.blockchain.entity.reducer.framework.service.EntityTemplateProvider
import com.rarible.blockchain.entity.reducer.framework.service.ReduceService
import com.rarible.blockchain.entity.reducer.framework.service.Reducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Service used to calculate state of the entities by events from the database.
 * Events should be ordered by entity id (so events for one entity are collected together)
 * Service should be used in the long-running Task which updates state of entities
 */
open class TaskReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E>,
    private val entityEventService: EntityEventService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>
) : ReduceService<Id, Event, E> {

    //todo implemented not using windowUntilChanged, that's a more straight-forward approach I think and less error prone
    //todo windowUntilChanged implementation is pretty difficult to understand, but this approach seems much easier
    override suspend fun reduce(events: Flow<Event>): Flow<E> = flow {
        var entity: E? = null
        events.collect { event ->
            val id = entityEventService.getEntityId(event)
            val prevEntity = entity
            val currentEntity = if (prevEntity == null || prevEntity.id != id) {
                if (prevEntity != null) {
                    entityService.update(prevEntity)
                    emit(prevEntity)
                }
                templateProvider.getEntityTemplate(id)
            } else {
                prevEntity
            }
            entity = reducer.reduce(currentEntity, event)
        }
        val lastEntity = entity
        if (lastEntity != null) {
            entityService.update(lastEntity)
            emit(lastEntity)
        }
    }
}
