package com.mnemosyne.library.core.cache

import kotlinx.coroutines.CoroutineScope

sealed interface CacheCommand

object Get: CacheCommand

object Fetch: CacheCommand

data class FetchPeriodically(val milliseconds: Long): CacheCommand

object Refresh: CacheCommand

data class Remove<T>(val key: T): CacheCommand

object StopProcessing: CacheCommand

interface CacheAgent : CoroutineScope {
    fun ask(command: CacheCommand)
}

interface CachePolicy<T> {
    val fetch: suspend () -> T

    val cache: suspend (T) -> Unit

    val get: suspend  () -> T

    val remove: (suspend (key: Any?) -> Unit)?
}

class CachePolicyImpl<T>(
    override val fetch: suspend () -> T,
    override val cache: suspend (T) -> Unit,
    override val get: suspend () -> T,
    override val remove: (suspend (key: Any?) -> Unit)?,
) : CachePolicy<T>