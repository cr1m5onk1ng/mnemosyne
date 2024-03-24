package com.mnemosyne.library.core.cache

import com.mnemosyne.library.core.domain.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

open class NetworkCacheAgent<T> private constructor(
    private val scope: CoroutineScope,
    private val policy: CachePolicy<T>
) : CacheAgent, CoroutineScope by scope {
    companion object {
        fun <T> register(scope: CoroutineScope, policy: CachePolicy<T>): NetworkCacheAgent<T> {
            return NetworkCacheAgent(scope, policy)
        }
    }

    open val resource: MutableStateFlow<Resource<T>> =
        MutableStateFlow(Resource.Loading(null))

    private var commandsChannel: Channel<CacheCommand> = Channel(capacity = Channel.BUFFERED)

    init {
        scope.coroutineContext.job.invokeOnCompletion {
            commandsChannel.close()
        }

        launch {
            commandsChannel.consumeEach(::process)
        }
    }

    override fun ask(command: CacheCommand) {
        launch {
            commandsChannel.send(command)
        }
    }

    open fun process(command: CacheCommand) {
        when(command) {
            Get -> {
                launch {
                    resource.emit(Resource.Loading(null))
                    resource.emit(Resource.Success(policy.get()))
                }
            }
            Fetch -> {
                launch {
                    coroutineScope {
                        val cached = policy.get()

                        try {
                            resource.update { Resource.Loading(cached) }
                            val net = policy.fetch()
                            launch(Dispatchers.IO) {
                                policy.cache(net)
                            }
                            resource.update { Resource.Success(net) }
                        } catch (t: Throwable) {
                            if(t is CancellationException) throw t
                            resource.update { Resource.Error(cached, t) }
                        }
                    }
                }
            }
            Refresh -> {
                launch {
                    coroutineScope {
                        val current = policy.get()

                        resource.emit(Resource.Loading(current))

                        try {
                            val data = policy.fetch()
                            launch(Dispatchers.IO) {
                                policy.cache(data)
                            }
                            resource.emit(Resource.Success(data))
                        } catch (t: Throwable) {
                            if(t is CancellationException) throw t
                            resource.emit(Resource.Error(current, t))
                        }
                    }
                }
            }
            is Remove<*> -> {
                launch {
                    resource.emit(Resource.Loading(null))
                    policy.remove?.invoke(command.key)
                    resource.emit(Resource.Success(policy.get()))
                }
            }
        }
    }
}