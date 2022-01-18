package com.jiangcm.base.api

/**
 * @author: jcm
 * @email: jiangcm@aplus-it.cn
 * @createTime: 20-6-3
 */
class BaseResponse<T> {
    var msg: Array<String>? = null
    var data: T? = null
    var code: String? = null

    constructor() {}

    constructor(msg: String) {
        this.msg = arrayOf(msg)
    }
}