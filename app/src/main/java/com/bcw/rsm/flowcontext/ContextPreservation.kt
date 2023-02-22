package com.bcw.rsm.flowcontext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

fun dataFlowNotAllowed(): Flow<Int> = flow {
    withContext(Dispatchers.IO) {
        while (true) {
            emit(someDataComputation())
        }
    }
}

fun dataFlow(): Flow<Int> = flow {
    while (true) {
        emit(someDataComputation())
    }
}.flowOn(Dispatchers.IO)

private fun someDataComputation(): Int {
    Thread.sleep(10_000)
    return 1
}