package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.exception.ReduceException
import com.rarible.blockchain.entity.reducer.framework.model.RevertableEntity
import com.rarible.blockchain.entity.reducer.framework.model.Mark
import com.rarible.blockchain.entity.reducer.framework.service.MarkService
import com.rarible.blockchain.entity.reducer.framework.service.Reducer
import com.rarible.blockchain.scanner.framework.model.Log

class RecordList<Id, Event, E : RevertableEntity<Id, Event, E>>(
    events: List<Event>,
    private val markService: MarkService<Event>
) {
    private val comparator: Comparator<Event> = Comparator.comparing { it.mark }
    private val events = events.sortedWith(comparator.reversed()).toMutableList()

    fun add(event: Event) {
        val index = events.binarySearch(event, comparator.reversed())

        if (index < 0) events.add(-(index + 1), event)
        when (record.log?.status) {
            Log.Status.CONFIRMED, Log.Status.PENDING -> {
                // If we don't find a new confirmed/pending record in the list, we need to add it
                if (index < 0) events.add(-(index + 1), record)
            }
            Log.Status.REVERTED, Log.Status.DROPPED, Log.Status.INACTIVE -> {
                // If we find a reverted record in the list, we need to remove ot
                if (index >= 0) events.removeAt(index)
            }
            null -> throw ReduceException("Can't get log from record $record")
        }
    }

    fun remove(event: Event) {
        if (index >= 0) events.removeAt(index)
    }

    suspend fun apply(initial: E, reducer: Reducer<K, L, R, E>): E {
        val state =  events.fold(initial) { entity, record ->
            val result = reducer.reduce(entity, record)
            if (markService.isStableMark(record.mark)) result.withSnapshot(result) else result
        }
        return state.withLogRecords(events.removeStable())
    }

    private fun List<R>.removeStable(): List<R> = filter { record -> markService.isStableMark(record.mark).not() }

    private val Event.mark: Mark
        get() = markService.get(this)
}
