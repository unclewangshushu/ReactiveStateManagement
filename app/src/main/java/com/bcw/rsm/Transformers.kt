package com.bcw.rsm

import kotlinx.coroutines.flow.*

@Suppress("FunctionName")
fun SubmitUserNameTransformer(
    um: UserManager
): (Flow<SubmitUserNameEvent>) -> Flow<SubmitUiModel> = { events ->
    events.mapLatest { ev -> um.setName(ev.userName) }
        .map { resp -> SubmitUiModel.Success() }
        .catch { e -> emit(SubmitUiModel.Error(e)) }
        .onStart { emit(SubmitUiModel.InProgress()) }
}


//========================== multiple requests ==========================
@Suppress("FunctionName")
fun SubmitTransformer(
    um: UserManager
): (Flow<SubmitEvent>) -> Flow<SubmitUiModel> = { events ->
    events.mapLatest { ev -> um.setName(ev.userName) }
        .map { resp -> SubmitUiModel.Success() }
        .catch { e -> emit(SubmitUiModel.Error(e)) }
        .onStart { emit(SubmitUiModel.InProgress()) }
}

@Suppress("FunctionName")
fun CheckNameTransformer(
    um: UserManager
): (Flow<CheckNameEvent>) -> Flow<SubmitUiModel> = { events ->
    events.mapLatest { ev -> um.checkName(ev.userName) }
        .map { resp -> SubmitUiModel.Success() }
        .catch { e -> emit(SubmitUiModel.Error(e)) }
        .onStart { emit(SubmitUiModel.InProgress()) }
}


//============================ with actions
@Suppress("FunctionName")
fun SubmitActionTransformer(
    um: UserManager
): (Flow<SubmitAction>) -> Flow<SubmitResult> = { actions ->
    actions.mapLatest { action -> um.setName(action.userName) }
        .map { resp -> SubmitResult.Success() }
        .catch { e -> emit(SubmitResult.Error(e)) }
        .onStart { emit(SubmitResult.InProgress()) }
}

@Suppress("FunctionName")
fun CheckNameActionTransformer(
    um: UserManager
): (Flow<CheckNameAction>) -> Flow<CheckNameResult> = { actions ->
    actions.mapLatest { action -> um.checkName(action.userName) }
        .map { resp -> CheckNameResult.Success() }
        .catch { e -> emit(CheckNameResult.Error(e)) }
        .onStart { emit(CheckNameResult.InProgress()) }
}