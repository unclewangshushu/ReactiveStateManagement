package com.bcw.rsm.transform

import com.bcw.rsm.Async
import com.bcw.rsm.intent.Action
import com.bcw.rsm.intent.AsyncResult
import kotlinx.coroutines.flow.*

fun <T, A : Action, R : AsyncResult<T>> autoTransform(
    resultFactory: (Async<T>) -> R,
    transform: suspend (A) -> T,
): (Flow<A>) -> Flow<R> = { actions ->
    actions.flatMapLatest { action ->
        flow { emit(resultFactory(Async.Success(transform(action)))) }
            .onStart { emit(resultFactory(Async.Loading())) }
            .catch { e -> emit(resultFactory(Async.Fail(e))) }
    }
}
