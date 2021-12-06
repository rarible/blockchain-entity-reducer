package com.rarible.blockchain.entity.reducer.framework.service

import com.rarible.blockchain.entity.reducer.framework.model.Mark

interface MarkService<Event> {
    fun get(event: Event): Mark
    fun isStableMark(mark: Mark): Boolean
}
