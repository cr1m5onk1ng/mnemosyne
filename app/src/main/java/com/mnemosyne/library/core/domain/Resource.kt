package com.mnemosyne.library.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

sealed interface Resource<T> {
    data class Loading<T>(val cached: T?): Resource<T>
    data class Success<T>(val data: T?): Resource<T>
    data class Error<T>(val cached: T?, val error: Throwable): Resource<T>
}

inline fun <T> resource(crossinline block: suspend () -> T): Flow<Resource<T>> {
    return flow {
        emit(Resource.Loading(null))
        try {
            Resource.Success(block())
        } catch (t: Throwable) {
            if(t is CancellationException) throw t
            emit(Resource.Error(null, t))
        }
    }
}

inline fun <T, R> Resource<T?>.map(block: (T?) -> R): Resource<R> {
    return when(this) {
        is Resource.Success -> Resource.Success(block(this.data))
        is Resource.Loading -> Resource.Loading(block(this.cached))
        is Resource.Error -> Resource.Error(block(this.cached), this.error)
    }
}

inline fun <T, E: Throwable> Resource.Error<T>.mapError(transform: (Throwable) -> E): Resource.Error<T> {
    return Resource.Error(this.cached, transform(this.error))
}

inline fun <T, D, R> Resource<T?>.or(
    other: Resource<D?>,
    combiner: (T?, D?) -> R
): Resource<R> {
    return when(this) {
        is Resource.Success -> {
            when(other) {
                is Resource.Success -> {
                    Resource.Success(combiner(this.data, other.data))
                }
                is Resource.Loading -> {
                    Resource.Loading(combiner(this.data, other.cached))
                }
                is Resource.Error -> {
                    Resource.Error(combiner(this.data, other.cached), other.error)
                }
            }
        }
        is Resource.Loading -> {
            when(other) {
                is Resource.Success -> {
                    Resource.Loading(combiner(this.cached, other.data))
                }
                is Resource.Loading -> {
                    Resource.Loading(combiner(this.cached, other.cached))
                }
                is Resource.Error -> {
                    Resource.Error(combiner(this.cached, other.cached), other.error)
                }
            }
        }
        is Resource.Error -> {
            when(other) {
                is Resource.Error -> {
                    Resource.Error(combiner(this.cached, other.cached), this.error)
                }
                is Resource.Loading -> {
                    Resource.Error(combiner(this.cached, other.cached), this.error)
                }
                is Resource.Success -> {
                    Resource.Error(combiner(this.cached, other.data), this.error)
                }
            }
        }
    }
}

