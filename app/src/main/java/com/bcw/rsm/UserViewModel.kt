package com.bcw.rsm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bcw.rsm.data.UserManager
import com.bcw.rsm.intent.*
import com.bcw.rsm.transform.CheckNameActionTransformer
import com.bcw.rsm.transform.SubmitActionTransformer
import kotlinx.coroutines.flow.*

class UserViewModel(
    submit: (Flow<SubmitAction>) -> Flow<SubmitResult>,
    checkName: (Flow<CheckNameAction>) -> Flow<CheckNameResult>,
): ViewModel() {

    val uiEvents = MutableSharedFlow<SubmitUiEvent>()
    val uiModels : Flow<SubmitUiModel>

    init {
        val actions: Flow<Action> = uiEvents.map { event2action(it) }
        val transform : (Flow<Action>) -> Flow<AsyncResult<*>> = {
            it.shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed()
            ).let { shared ->
                merge(
                    submit(shared.filterIsInstance()),
                    checkName(shared.filterIsInstance()),
                )
            }
        }
        val results: Flow<AsyncResult<*>> = transform(actions)
        uiModels = results
            // incremental state updating with `scan`
            .scan(initial = SubmitUiModel.Idle()) { state, result ->
                when (result) {
                    is CheckNameResult -> when(result.async) {
                        is Async.Fail -> state.copy(nameCheckPass = null, nameCheckProgress = true)
                        is Async.Loading -> state.copy(nameCheckPass = true, nameCheckProgress = false)
                        is Async.Success -> state.copy(nameCheckPass = false, nameCheckProgress = false)
                    }
                    is SubmitResult -> when(result.async) {
                        is Async.Fail -> state.copy(success = false, inProgress = false, error = result.async.error)
                        is Async.Loading -> state.copy(success = false, inProgress = true, error = null)
                        is Async.Success -> state.copy(success = true, inProgress = false, error = null, nameCheckPass = null)
                    }
                    else -> throw UnsupportedOperationException(result.toString())
                }
            }
    }

    private fun event2action(event: SubmitUiEvent): Action = when (event) {
        is CheckNameEvent -> CheckNameAction(event.userName)
        is SubmitEvent -> SubmitAction(event.userName)
    }

    class Factory(
        um: UserManager,
    ) : ViewModelProvider.Factory {
        private val submit = SubmitActionTransformer(um)
        private val checkName = CheckNameActionTransformer(um)
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserViewModel(submit, checkName) as T
        }
    }
}