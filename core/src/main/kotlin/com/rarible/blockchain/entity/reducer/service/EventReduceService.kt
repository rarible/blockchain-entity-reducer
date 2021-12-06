package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.model.Identifiable
import com.rarible.blockchain.entity.reducer.framework.service.EntityEventService
import com.rarible.blockchain.entity.reducer.framework.service.EntityService
import com.rarible.blockchain.entity.reducer.framework.service.EntityTemplateProvider
import com.rarible.blockchain.entity.reducer.framework.service.Reducer

/**
 * Service to handle small amount of events for one entity
 * It takes a batch of events, loads entity, applies these events and saves entity to the database
 */
class EventReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E>,
    private val entityEventService: EntityEventService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>
) {

    /**
     * Takes all events that need to be applied to the entities,
     * groups them by entity id and applies using batch to every entity
     */
    suspend fun reduceAll(event: List<Event>) {
        event.groupBy { entityEventService.getEntityId(it) }
            .forEach { (id, events) ->
                reduce(id, events)
            }
    }

    /**
     * Takes batch of events that needs to be applied to one entity.
     * Gets entity by id, applies events and saves the entity
     */
    //todo optimistic lock?
    private suspend fun reduce(id: Id, events: List<Event>): E {
        val entity = entityService.get(id) ?: templateProvider.getEntityTemplate(id)
        val result = events.fold(entity) { e, event ->
            reducer.reduce(e, event)
        }
        entityService.update(result)
        return result
    }
}