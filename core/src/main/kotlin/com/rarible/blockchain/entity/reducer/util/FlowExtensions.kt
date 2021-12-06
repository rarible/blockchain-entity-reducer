package com.rarible.blockchain.entity.reducer.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow

fun <T, V> Flow<T>.windowUntilChanged(keyExtractor: (T) -> V): Flow<KeyFlow<V, T>> = flow {
    var last: Pair<V, Channel<T>>? = null
    try {
        collect {
            val key = keyExtractor(it)
            if (last?.first != key) {
                val channel = Channel<T>(32)

                emit(KeyFlow(key, channel.consumeAsFlow()))

                last?.run { second.close() }
                last = key to channel
            }
            last?.second?.run { send(it) }
        }
    } finally {
        last?.run { second.close() }
    }
}

data class KeyFlow<Key, Value>(
    val key: Key,
    val flow: Flow<Value>
)
