package com.mnemosyne.library.core.cache

@DslMarker
annotation class CacheDsl

interface NetworkCachePolicyBuilder<T> {

    class MissingParameterException(parameterName: String): RuntimeException("Missing parameter [$parameterName]")

    fun fetch(block: suspend () -> T)

    fun cache(block: suspend (T) -> Unit)

    fun get(block: suspend () -> T)

    fun remove(block: suspend (key: Any?) -> Unit)

    fun build(): CachePolicy<T>
}

@CacheDsl
class NetworkCachePolicyBuilderImpl<T>: NetworkCachePolicyBuilder<T> {
    private var fetch: (suspend () -> T)? = null
    private var cache: (suspend (T) -> Unit)? = null
    private var get: (suspend () -> T)? = null
    private var remove: (suspend (Any?) -> Unit)? = null

    override fun fetch(block: suspend () -> T) {
        fetch = block
    }

    override fun cache(block: suspend (T) -> Unit) {
        cache = block
    }

    override fun get(block: suspend () -> T) {
        get = block
    }

    override fun remove(block: suspend (key: Any?) -> Unit) {
        remove = block
    }

    override fun build(): CachePolicy<T> {
        val fetchAction = fetch ?: throw NetworkCachePolicyBuilder.MissingParameterException("fetch")
        val cacheAction = cache ?: throw NetworkCachePolicyBuilder.MissingParameterException("cache")
        val getAction = get ?: throw NetworkCachePolicyBuilder.MissingParameterException("get")

        return CachePolicyImpl(
            fetch = fetchAction,
            cache = cacheAction,
            get = getAction,
            remove = remove
        )
    }

}