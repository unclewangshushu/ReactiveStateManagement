package com.bcw.rsm.transform

import com.bcw.rsm.*
import com.bcw.rsm.data.UserManager
import com.bcw.rsm.intent.*
import kotlinx.coroutines.flow.*
import java.lang.reflect.Constructor

@Suppress("FunctionName")
fun SubmitUserNameTransformer(
    um: UserManager
): (Flow<SubmitUserNameEvent>) -> Flow<SubmitUiModel> = { events ->
    events.flatMapLatest { ev ->
        flow { emit(um.setName(ev.userName)) }
            .map { resp -> SubmitUiModel.Success() }
            .onStart { emit(SubmitUiModel.InProgress()) }
            .catch { e -> emit(SubmitUiModel.Error(e)) }
    }
}


//========================== multiple requests ==========================
@Suppress("FunctionName")
fun SubmitTransformer(
    um: UserManager
): (Flow<SubmitEvent>) -> Flow<SubmitUiModel> = { events ->
    events.flatMapLatest { ev ->
        flow { emit(um.setName(ev.userName)) }
            .map { resp -> SubmitUiModel.Success() }
            .onStart { emit(SubmitUiModel.InProgress()) }
            .catch { e -> emit(SubmitUiModel.Error(e)) }
    }
}

@Suppress("FunctionName")
fun CheckNameTransformer(
    um: UserManager
): (Flow<CheckNameEvent>) -> Flow<SubmitUiModel> = { events ->
    events.flatMapLatest { ev ->
        flow { emit(um.checkName(ev.userName)) }
            .map { resp -> if (resp) SubmitUiModel.Success() else SubmitUiModel.Error(
                RuntimeException("checkName false resp")
            )
            }
            .onStart { emit(SubmitUiModel.InProgress()) }
            .catch { e -> emit(SubmitUiModel.Error(e)) }
    }
}


//============================ with actions
@Suppress("FunctionName")
fun SubmitActionTransformer(
    um: UserManager
): (Flow<SubmitAction>) -> Flow<SubmitResult> = { actions ->
    actions.flatMapLatest { action ->
        flow { emit(um.setName(action.userName)) }
            .map { SubmitResult(Async.Success(true))}
            .onStart { emit(SubmitResult(Async.Loading())) }
            .catch { e -> emit(SubmitResult(Async.Fail(e))) }
    }
}

@Suppress("FunctionName")
fun CheckNameActionTransformer(
    um: UserManager
): (Flow<CheckNameAction>) -> Flow<CheckNameResult> = { actions ->
    actions.flatMapLatest { action ->
        flow { emit(um.checkName(action.userName)) }
            .map { resp ->
                if (resp) CheckNameResult(Async.Success(action.userName))
                else CheckNameResult(Async.Fail(RuntimeException("response $resp")))
            }
            .onStart { emit(CheckNameResult(Async.Loading())) }
            .catch { e -> emit(CheckNameResult(Async.Fail(e))) }
    }
}

@Suppress("FunctionName")
fun CheckNameActionTransformerByAutoTransform(
    um: UserManager
): (Flow<CheckNameAction>) -> Flow<CheckNameResult> = autoTransform(::CheckNameResult) { action ->
    um.checkName(action.userName)
    action.userName
}