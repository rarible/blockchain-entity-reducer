package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.exception.ReduceException
import com.rarible.blockchain.entity.reducer.framework.model.Entity
import com.rarible.blockchain.entity.reducer.framework.model.Mark
import com.rarible.blockchain.entity.reducer.framework.service.MarkService
import com.rarible.blockchain.entity.reducer.framework.service.Reducer
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord

class RecordList<K, L : Log<L>, R : LogRecord<L, R>, E : Entity<K, L, R, E>>(
    records: List<R>,
    private val markService: MarkService<L, R>
) {
    private val comparator: Comparator<R> = Comparator.comparing { record -> record.mark }
    private val records = records.sortedWith(comparator.reversed()).toMutableList()

    fun add(record: R) {
        val index = records.binarySearch(record, comparator.reversed())

        when (record.log?.status) {
            Log.Status.CONFIRMED, Log.Status.PENDING -> {
                // If we don't find a new confirmed/pending record in the list, we need to add it
                if (index < 0) records.add(-(index + 1), record)
            }
            Log.Status.REVERTED, Log.Status.DROPPED, Log.Status.INACTIVE -> {
                // If we find a reverted record in the list, we need to remove ot
                if (index >= 0) records.removeAt(index)
            }
            null -> throw ReduceException("Can't get log from record $record")
        }
    }

    suspend fun apply(initial: E, reducer: Reducer<K, L, R, E>): E {
        val state =  records.fold(initial) { entity, record ->
            val result = reducer.reduce(entity, record)
            if (markService.isStableMark(record.mark)) result.withSnapshot(result) else result
        }
        return state.withLogRecords(records.removeStable())
    }

    private fun List<R>.removeStable(): List<R> = filter { record -> markService.isStableMark(record.mark) }

    private val R.mark: Mark
        get() = markService.get(this)
}
