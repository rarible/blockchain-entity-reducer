package com.rarible.blockchain.entity.reducer.framework.service

import com.rarible.blockchain.entity.reducer.framework.model.Mark
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord

interface MarkService<L : Log<L>, R : LogRecord<L, R>> {
    fun get(logRecord: R): Mark

    fun isStableMark(mark: Mark): Boolean
}
