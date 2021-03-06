package com.jiangcm.base.rxjava

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Project com.daotangbill.exlib.base
 * Created by DaoTangBill on 2018/3/28/028.
 * Email:tangbaozi@daotangbill.uu.me
 *
 * @author bill
 * @version 1.0
 * @description
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class RxHandler {

    private val mCompositeDisposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    private val disposableMap: HashMap<String, Disposable> by lazy {
        HashMap<String, Disposable>()
    }

    /**
     * 这个 添加方式  会顶掉前面的 可能 会有问题
     */
    fun put(key: String, disposable: Disposable) {
        disposableMap[key] = disposable
    }

    fun putSafe(key: String, disposable: Disposable): Boolean {
        val t = disposableMap[key]
        return if (t == null) {
            disposableMap[key] = disposable
            true
        } else {
            false
        }
    }

    fun post(block: () -> Unit) {
        postDelayed(block, 0)
    }

    fun postDelayed(block: () -> Unit, time: Long) {
        val disposableObserver = object : DisposableObserver<Any>() {
            override fun onComplete() {
                dispose()
            }

            override fun onNext(t: Any) {
                block()
            }

            override fun onError(e: Throwable) {
                dispose()
            }
        }

        Observable.timer(time, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(disposableObserver)
        mCompositeDisposable.add(disposableObserver)
    }

    /**
     * @param timeLimit 计时次数
     * @param key 当前流的 名字，用于提前取消等操作
     * @param block （time 计数次数，是增加；
     *              key,当前流的名字）
     */
    fun timer(timeLimit: Long, key: String, block: (time: Long, key: String) -> Unit) {
        if (checkKey(key)) {
            return
        }

        val disposableObserver = object : DisposableObserver<Long>() {
            override fun onComplete() {
                disposableMap.remove(key)
                dispose()
            }

            override fun onNext(t: Long) {
                block(t + 1, key)
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                disposableMap.remove(key)
                dispose()
            }
        }
        disposableMap[key] = disposableObserver

        Observable.intervalRange(0, timeLimit, 0, 1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(disposableObserver)
        mCompositeDisposable.add(disposableObserver)
    }


    fun retry(key: String,
              waitTime: Long,
              block: () -> Boolean,
              success: (() -> Unit)? = null,
              error: (() -> Unit)? = null) {
        retry(key, 0, waitTime, block, success, error)
    }

    /**
     * 失败重试？或者重试
     * @param block : 返回值为  是否重试：true ,重试，false 不重试
     * @param limitime : <=0 代表无线次
     * @param waitTime : ms 重试间隔时间
     * @param success : 成功之后的 操作
     * @param error : 失败之后的 操作
     */
    fun retry(key: String,
              limitime: Int,
              waitTime: Long,
              block: () -> Boolean,
              success: (() -> Unit)? = null,
              error: (() -> Unit)? = null) {
        if (checkKey(key)) {
            return
        }
        var time = 0
        val disposableObserver = object : DisposableObserver<String>() {
            override fun onComplete() {
                success?.invoke()
                disposableMap.remove(key)
            }

            override fun onNext(t: String) {
            }

            override fun onError(e: Throwable) {
                error?.invoke()
            }
        }

        Observable
            .create(ObservableOnSubscribe<String> { e ->
                val b = block()
                if (b) {
                    time++
                    if (limitime <= 0) {
                        e.onError(Throwable("retry"))
                    } else if (limitime > 0 && limitime >= time) {
                        e.onError(Throwable("retry"))
                    } else {
                        e.onError(Throwable("More retry times"))
                    }
                } else {
                    e.onNext("Work Success")
                    e.onComplete()
                }
            })
            .retryWhen {
                it.flatMap { r->
                    val msg = r.message
                    return@flatMap if (msg == "retry") {
                        Observable.timer(waitTime, TimeUnit.MILLISECONDS)
                    } else {
                        Observable.error(r)
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(disposableObserver)
        disposableMap[key] = disposableObserver
        mCompositeDisposable.add(disposableObserver)
    }

    class NetWorkContext<T>(private val weakReference: WeakReference<RxHandler>) {

        constructor(weakReference: WeakReference<RxHandler>,
                    key: String?) : this(weakReference) {
            this.key = key
        }

        companion object {
            /**
             * 同时只有一个请求，重复请求 拒绝
             */
            const val TYPE_REFUSING_SECOND = 0

            /**
             * 同时只有一个请求，重复请求 取消前一个，换用现在的
             */
            const val TYPE_REPLACE = 1
        }

        var isEnd = false

        var key: String? = null
        var type: Int = TYPE_REFUSING_SECOND
        var subscribeOnScheduler: Scheduler = Schedulers.io()
        var observeOnScheduler: Scheduler = AndroidSchedulers.mainThread()
        var disposable: DisposableObserver<T>? = null
        var observable: Observable<T>? = null
        var beforeStart: (() -> Unit)? = null
        var afterEnd: (() -> Unit)? = null

        fun start() {
            if (isEnd) return
            val k = key ?: return
            beforeStart?.invoke()
            when (type) {
                TYPE_REFUSING_SECOND -> {
                    if (weakReference.get()?.checkKey(k) == true) {
                        return
                    }
                }
                TYPE_REPLACE -> {
                    weakReference.get()?.removeCallbacksAndMessages(key)
                }
            }
            val obs = observable
            val disp = disposable
            if (obs != null && disp != null) {
                weakReference.get()?.put(k, disp)
                obs.subscribeOn(subscribeOnScheduler)
                    .observeOn(observeOnScheduler)
                    .doFinally { afterEnd?.invoke() }
                    .subscribe(disp)
            }
        }

        fun end() {
            isEnd = true
            afterEnd?.invoke()
        }
    }

    fun <T> createNetWork(key: String): NetWorkContext<T>? {
        val c = NetWorkContext<T>(WeakReference(this), key)
        if (key.isBlank()) {
            c.end()
            return null
        }
        if (checkKey(key)) {
            c.end()
            return null
        }
        return c
    }

    fun <T> createNetWork(init: NetWorkContext<T>.() -> Unit) {
        val c = NetWorkContext<T>(WeakReference(this)).apply(init)
        val key = c.key ?: return
        if (key.isBlank()) {
            return
        }
        if (checkKey(key)) {
            return
        }
        c.start()
    }

    /**
     *
     */
    fun checkKey(key: String): Boolean = disposableMap[key]?.isDisposed == false

    fun removeCallbacksAndMessages(key: String? = null, block: ((b: Boolean) -> Unit)? = null) {
        if (key == null) {
            mCompositeDisposable.clear()
            disposableMap.clear()
        } else {
            val t = disposableMap[key]
            t?.let {
                mCompositeDisposable.remove(it)
                    .isTrue {
                        disposableMap.remove(key)
                        block?.invoke(true)
                    }.isFalse {
                        block?.invoke(false)
                    }
            }
        }
    }

    fun Boolean?.isTrue(block: () -> Unit): Boolean {
        if (this == true) {
            block()
            return true
        }
        return false
    }

    fun Boolean?.isFalse(block: () -> Unit): Boolean {
        if (this != true) {
            block()
            return true
        }
        return false
    }
}