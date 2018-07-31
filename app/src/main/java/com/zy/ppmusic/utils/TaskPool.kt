package com.zy.ppmusic.utils

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author y-slience
 * @since 2018/6/29
 */
object TaskPool {
    private val AVAILABLE_SIZE = Runtime.getRuntime().availableProcessors()
    private val CORE_POOL_SIZE = Math.max(2, Math.min(AVAILABLE_SIZE - 1, 4))
    private val MAX_POOL_SIZE = AVAILABLE_SIZE * 2 + 1

    val backgroundPool: ExecutorService by lazy {
        ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 30, TimeUnit.SECONDS,
                LinkedBlockingDeque<Runnable>())
    }

}