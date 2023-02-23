package com.bcw.rsm.intent

import com.bcw.rsm.Async


open class AsyncResult<T>(val async: Async<T>) {
    override fun toString(): String {
        return super.toString() + "{$async}"
    }
}

class CheckNameResult(async: Async<String>) : AsyncResult<String>(async)
class SubmitResult(async: Async<Boolean>) : AsyncResult<Boolean>(async)