package com.mnemosyne.library

import com.mnemosyne.library.core.cache.NetworkCacheAgent
import com.mnemosyne.library.core.cache.NetworkCachePolicyBuilder
import com.mnemosyne.library.core.cache.createCachedResource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CoreUnitTest {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun it_creates_network_resource() {
        createCachedResource<String, NetworkCacheAgent<String>>(
            scope = MainScope() + Job(),
        ) {
            fetch {
                TODO()
            }

            get {
                TODO()
            }

            cache {
                TODO()
            }
        }
    }

    @Test
    fun it_fails_to_create_resource_when_builder_incomplete() {
        assertThrows(NetworkCachePolicyBuilder.MissingParameterException::class.java) {
            createCachedResource<String, NetworkCacheAgent<String>>(
                scope = MainScope() + Job(),
            ) {
                fetch {
                    TODO()
                }

                get {
                    TODO()
                }
            }
        }
    }
}