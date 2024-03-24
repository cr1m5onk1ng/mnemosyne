package com.mnemosyne.library.core.cache

import com.mnemosyne.library.core.domain.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

inline fun <T, reified Agent: CacheAgent> createCachedResource(
    scope: CoroutineScope,
    policy: NetworkCachePolicyBuilder<T>.() -> Unit
): Pair<StateFlow<Resource<T>>, Agent> {
    return when(Agent::class) {
        NetworkCacheAgent::class -> {
            val agent = NetworkCacheAgent.register(
                scope,
                NetworkCachePolicyBuilderImpl<T>()
                    .apply(policy)
                    .build()
            )
            val resourceFlow = agent.resource.asStateFlow()
            Pair(resourceFlow, agent as Agent)
        }
        else -> throw NotImplementedError()
    }
}