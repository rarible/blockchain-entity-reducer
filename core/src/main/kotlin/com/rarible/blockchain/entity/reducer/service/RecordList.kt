package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.exception.ReduceException
import com.rarible.blockchain.entity.reducer.framework.model.Mark
import com.rarible.blockchain.entity.reducer.framework.service.MarkService
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord

class RecordList<L : Log<L>, R : LogRecord<L, R>>(
    records: List<R>,
    private val markService: MarkService<L, R>
) {
    private val comparator: Comparator<R> = Comparator.comparing { record -> record.mark }
    private val records = records.sortedWith(comparator.reversed()).toMutableList()
    private val latestMark = records.firstOrNull()?.mark

    fun canBeApplied(record: R): Boolean {
        return when (record.log?.status) {
            Log.Status.CONFIRMED, Log.Status.PENDING -> recordAddIndex(record) >= 0
            Log.Status.REVERTED, Log.Status.DROPPED, Log.Status.INACTIVE -> recordRemoveIndex(record) >= 0
            null -> throw ReduceException("Can't get log from record $record")
        }
    }

    fun addOrRemove(record: R) {
        when (record.log?.status) {
            Log.Status.CONFIRMED, Log.Status.PENDING -> {
                val index = recordAddIndex(record)
                requireIndex(index >= 0) { "Can't add record $record" }
                records.add(index, record)
            }
            Log.Status.REVERTED, Log.Status.DROPPED, Log.Status.INACTIVE -> {
                val index = recordRemoveIndex(record)
                requireIndex(index > 0) { "Can't remove record $record" }
                records.removeAt(index)
            }
            null -> throw ReduceException("Can't get log from record $record")
        }
    }

    fun geList(): List<R> {
        return records.filter { record -> markService.isStableMark(record.mark) }
    }

    private fun recordAddIndex(record: R): Int {
        return if (latestMark == null || record.mark > latestMark) 0 else -records.binarySearch(record, comparator.reversed())
    }

    private fun recordRemoveIndex(record: R): Int {
        return if (record.mark == latestMark) 0 else records.binarySearch(record, comparator.reversed())
    }

    private fun requireIndex(value: Boolean, message: () -> Any) {
        if (!value) {
            throw ReduceException(message.toString())
        }
    }

    private val R.mark: Mark
        get() = markService.get(this)
}
