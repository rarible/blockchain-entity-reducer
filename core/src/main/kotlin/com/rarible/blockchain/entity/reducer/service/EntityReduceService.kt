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
                val currentEntity = entityService.get(entityId) ?: entityService.getEntityTemplate(entityId)
                val records = RecordList(currentEntity.logRecords, markService)

                val updatedEntity = entityRecords.fold(currentEntity) { entity, record ->
                    if (records.canBeApplied(record)) {
                        records.addOrRemove(record)
                        reducer.reduce(entity, record)
                    } else {
                        entity
                    }
                }
                if (currentEntity != updatedEntity) {
                    entityService.update(updatedEntity.withLogRecords(records.geList()))
                }
                updatedEntity
            }
    }
}
