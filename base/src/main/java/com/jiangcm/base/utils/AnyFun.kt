package com.jiangcm.base.utils


/**
 * @author: jcm
 * @email: jiangcm@aplus-it.cn
 * @createTime: 20-6-3
 */
inline fun <T> T.equalToFun(other: Any?, block: (T) -> Unit) {
    if (other != null && other == this) {
        block(this)
    }
}