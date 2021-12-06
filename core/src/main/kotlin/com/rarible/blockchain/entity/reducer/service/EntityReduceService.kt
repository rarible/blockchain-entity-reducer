package com.rarible.blockchain.entity.reducer.service

import com.rarible.blockchain.entity.reducer.framework.model.Entity
import com.rarible.blockchain.entity.reducer.framework.service.EntityService
import com.rarible.blockchain.entity.reducer.framework.service.MarkService
import com.rarible.blockchain.entity.reducer.framework.service.ReduceService
import com.rarible.blockchain.entity.reducer.framework.service.Reducer
import com.rarible.blockchain.entity.reducer.util.windowUntilChanged
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.blockchain.scanner.framework.model.LogRecord
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map

@FlowPreview
open class EntityReduceService<K, L : Log<L>, R : LogRecord<L, R>, E : Entity<K, L, R, E>>(
    private val markService: MarkService<L, R>,
    private val entityService: EntityService<K, L, R, E>,
    private val reducer: Reducer<K, L, R, E>
) : ReduceService<K, L, R, E> {

    override suspend fun reduce(logRecords: Flow<R>): Flow<E> {
        return logRecords
            .windowUntilChanged { record ->
                entityService.getEntityId(record)
            }
            .map { (entityId, entityRecords) ->
                val entity = entityService.get(entityId)
                val currentSnapshot = entity?.snapshot ?: entityService.getEntityTemplate(entityId)
                val currentLogRecords = entity?.logRecords ?: emptyList()

                val recordList = RecordList<K, L, R, E>(currentLogRecords, markService)

                val updatedEntity = entityRecords.fold(currentSnapshot) { state, record ->
                    recordList.add(record)
                    recordList.apply(state.snapshot ?: state, reducer)
                }
                if (updatedEntity.logRecords != entity?.logRecords) {
                    entityService.update(updatedEntity)
                }
                updatedEntity
            }
    }
}
