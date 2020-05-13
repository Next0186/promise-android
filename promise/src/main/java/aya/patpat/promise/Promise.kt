package aya.patpat.promise

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LongSparseArray
import aya.patpat.promise.result.BaseResult
import aya.patpat.promise.result.Results
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Promise(private val mExec: (promise: Promise) -> Unit) {

    companion object {
        private val sMainHandler: Handler = Handler(Looper.getMainLooper())
        private val sExecThreadPool = Executors.newCachedThreadPool()
        private val sTimeoutThreadPool = Executors.newScheduledThreadPool(1)
        private val sPromiseMap = LongSparseArray<Promise>()

        @Synchronized
        fun handelResult(result: Results.Normal<*>) {
            if (result.isSuccess()) {
                resolve(result.id, result.data)
            } else {
                reject(result.id, result)
            }
        }

        @Synchronized
        fun resolve(id: Long, data: Any? = null) {
            sPromiseMap.get(id)?.resolve(data)
        }

        @Synchronized
        fun reject(id: Long, err: BaseResult) {
            sPromiseMap.get(id)?.reject(err)
        }
    }

    val id = SystemClock.elapsedRealtimeNanos()

    private var mCanceled = false
    private var mTimeoutFuture: ScheduledFuture<*>? = null
    private var mResolve: (data: Any?) -> Unit = {}
    private var mReject: (err: BaseResult) -> Unit = { _ -> }

    private fun close() {
        sPromiseMap.delete(id)
    }

    @Synchronized
    fun keepAlive() {
        mTimeoutFuture?.cancel(true)
        sPromiseMap.put(id, this)
    }

    @Synchronized
    fun resolve(data: Any? = null) {
        if (mCanceled) return
        cancel()
        mResolve(data)
    }

    @Synchronized
    fun reject(err: BaseResult) {
        if (mCanceled) return
        cancel()
        mReject(err)
    }

    @Synchronized
    fun cancel() {
        mCanceled = true
        mTimeoutFuture?.cancel(true)
        sPromiseMap.delete(id)
    }

    private fun actualThen(func: (data: Any?) -> Unit, onMainThread: Boolean = false): Promise {
        mResolve = { data ->
            try {
                when (onMainThread) {
                    true -> sMainHandler.post { func(data) }
                    false -> func(data)
                }
            } catch (e: Exception) {
                reject(Results.ErrInternal(e.message.toString()))
            }
        }
        return this
    }

    fun then(func: (data: Any?) -> Unit): Promise {
        return actualThen(func, false)
    }

    fun thenOnMainThread(func: (data: Any?) -> Unit): Promise {
        return actualThen(func, true)
    }

    private fun actualCatch(func: (err: BaseResult) -> Unit, onMainThread: Boolean = false): Promise {
        mReject = {
            try {
                when (onMainThread) {
                    true -> sMainHandler.post { func(it) }
                    false -> func(it)
                }
            } catch (e: Exception) { }
        }
        return this
    }

    fun catch(func: (err: BaseResult) -> Unit): Promise {
        return actualCatch(func, false)
    }

    fun catchOnMainThread(func: (err: BaseResult) -> Unit): Promise {
        return actualCatch(func, true)
    }

    private fun actualExec(timeoutMS: Long = 0L, onMainThread: Boolean = false) {
        mCanceled = false
        mTimeoutFuture = when {
            timeoutMS > 0 -> {
                sTimeoutThreadPool.schedule({
                    reject(Results.Timeout())
                }, timeoutMS, TimeUnit.MILLISECONDS)
            }
            else -> null
        }

        val run = {
            try {
                mExec(this)
            } catch (e: Exception) {
                reject(Results.ErrInternal(e.message.toString()))
            }
        }
        when (onMainThread) {
            true -> sMainHandler.post(run)
            false -> sExecThreadPool.submit(run)
        }
    }

    fun exec(timeoutMS: Long = 0L) {
        actualExec(timeoutMS, false)
    }

    fun execOnMainThread(timeoutMS: Long = 0L) {
        actualExec(timeoutMS, true)
    }
}
