package com.jiangcm.base.api

import com.google.gson.JsonSyntaxException
import io.reactivex.observers.DisposableObserver
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


/**
 * @author: jcm
 * @email: jiangcm@aplus-it.cn
 * @createTime: 20-6-3
 */
abstract class BaseCallSubscriber<T> : DisposableObserver<Response<BaseResponse<T>>>() {

    override fun onNext(response: Response<BaseResponse<T>>) {
        val body = response.body()
        if (response.isSuccessful) {
            if (body?.code.equals("1")) {
                onSuccess(body)
            } else {
                onError(response.code(), body)
            }
        } else {
            handResponseErr(response.code(), body)
        }
        onEnd()
    }

    abstract fun onSuccess(baseResponse: BaseResponse<T>?)

    private fun handResponseErr(code: Int?, baseResponse: BaseResponse<T>?) {
        when (code) {
            401 -> onError(code, BaseResponse("登陆失效,请重新登陆"))
            404 -> onError(code, BaseResponse("无法连接到服务器,请检查网络连接或联系管理员"))
            500 -> onError(code, BaseResponse("系统错误，请检查网络后重试"))
            502 -> onError(code, BaseResponse("系统错误，请检查网络后重试"))
            else -> onError(code, baseResponse)
        }
    }

    /**
     * 所有的错误都会进入 的错误处理点
     * 注意：需要对 401 错误的特殊处理在其他的地方
     * @param code -1 : 网络错误
     *              -2 ：其他的错误
     */
    open fun onError(code: Int?, baseResponse: BaseResponse<T>?) {

    }


    override fun onComplete() {
        dispose()
    }

    open fun onEnd() {
        dispose()
    }

    override fun onError(t: Throwable) {
        //处理常见的几种连接错误
        when (t) {
            is SocketTimeoutException -> onError(-1, BaseResponse("网络请求超时，请检查网络连接或请稍后重试"))
            is ConnectException -> onError(-1, BaseResponse("服务器连接失败，请稍后重试"))
            is UnknownHostException -> onError(-1, BaseResponse("网络错误，请检查网络连接或请稍后重试"))
            is JsonSyntaxException -> onError(-1, BaseResponse("服务器返回异常数据,请联系管理员或稍后重试"))
            else -> onError(-2, BaseResponse("网络错误，请检查网络连接或请稍后重试"))
        }
        onEnd()
    }
}