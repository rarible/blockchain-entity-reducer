package com.rarible.blockchain.entity.reducer.framework.model

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord

interface Entity<K, L : Log<L>, R : LogRecord<L, R>, E : Entity<K, L, R, E>> {
    val id: K

    val logRecords: List<R>

    fun withLogRecords(records: List<R>): E
}
