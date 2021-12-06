package com.rarible.blockchain.entity.reducer.framework.model

import com.rarible.blockchain.entity.reducer.framework.exception.ReduceException
import com.rarible.blockchain.entity.reducer.framework.service.Reducer

/**
 * Entity that supports reverting events (for example when chain is reorganized)
 */
interface RevertableEntity<Id, Event, E : RevertableEntity<Id, Event, E>> : Identifiable<Id> {
    /**
     * These events will always be ordered in natural order
     */
    val events: List<Event>

    /**
     * Copy and create entity with new event list
     */
    fun withEvents(events: List<Event>): E
}

interface EventRevertService<Event> {
    /**
     * Checks if this event can be reverted (it's pending or from latest n blocks)
     */
    fun canBeReverted(last: Event, current: Event): Boolean
}

class RevertableEntityReducer<Id, Event, E : RevertableEntity<Id, Event, E>>(
    private val revertService: EventRevertService<Event>,
    private val reducer: Reducer<Event, E>,
    private val reversedReducer: Reducer<Event, E>
) : Reducer<Pair<Event, Boolean>, E> {

    override suspend fun reduce(entity: E, event: Pair<Event, Boolean>): E {
        val (ev, add) = event
        return if (add) {
            //todo check here if event has been already applied
            //  for example comparing entity.events.lastOrNull with ev
            val newEvents = (entity.events + ev)
                .filter { revertService.canBeReverted(ev, it) }
            reducer.reduce(entity, ev)
                .withEvents(newEvents)
        } else {
            if (entity.events.contains(ev)) {
                reversedReducer.reduce(entity, ev)
                    .withEvents(entity.events - ev)
            } else {
                //todo here are possibly some options
                //  1. when event was already removed, then nothing should be done
                //  2. when we can't revert because we went forward too much
                throw ReduceException("Unable to revert $ev from $entity")
            }
        }
    }
}

//todo to see if everything's ok and proposed Entity is valid
sealed class ItemEvent : Comparable<ItemEvent> {
    data class ItemMintEvent(val supply: Int, val blockNumber: Long, val logIndex: Int) : ItemEvent() {
        override fun compareTo(other: ItemEvent): Int {
            TODO("Not yet implemented")
        }
    }
    data class ItemBurnEvent(val supply: Int, val blockNumber: Long, val logIndex: Int) : ItemEvent() {
        override fun compareTo(other: ItemEvent): Int {
            TODO("Not yet implemented")
        }
    }
}

//todo to see if everything's ok and proposed Entity is valid
data class Item(
    override val id: String,
    val contract: String,
    val tokenId: String,
    val supply: Int,
    override val events: List<ItemEvent>,
) : RevertableEntity<String, ItemEvent, Item> {

    override fun withEvents(events: List<ItemEvent>): Item =
        copy(events = events)

}

class ItemEventReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> entity.copy(supply = entity.supply + event.supply)
            is ItemEvent.ItemBurnEvent -> entity.copy(supply = entity.supply - event.supply)
        }
    }
}

class ItemEventReversedReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> entity.copy(supply = entity.supply - event.supply)
            is ItemEvent.ItemBurnEvent -> entity.copy(supply = entity.supply + event.supply)
        }
    }
}