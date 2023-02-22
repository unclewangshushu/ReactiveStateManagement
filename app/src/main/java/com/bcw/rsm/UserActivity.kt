package com.bcw.rsm

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bcw.rsm.databinding.ActivityUserBinding
import com.bcw.rsm.ext.launchRepeatOnLifecycle
import com.bcw.rsm.ext.toast
import com.bcw.rsm.viewflow.afterTextChanges
import com.bcw.rsm.viewflow.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserActivity : AppCompatActivity() {

    private lateinit var um: UserManager
    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
    }

    private fun observeUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                um.getUser()
                    .flowOn(Dispatchers.IO) //TODO not needed, because it's the api owner's responsibility to ensure main-safety
                    .catch {
                    }
                    .collect {
                    }
            }
        }
    }

    private fun naiveClickToSubmitUserName() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.btnSubmit.clicks()
                /**
                 * <hide>
                 * will not be call onErr */
                .onEach {
                    binding.btnSubmit.isEnabled = false
                    binding.progressBar.show()
                }
                /**
                 * <hide>
                 * With RxJava, it not thread-safe to touch UI here,
                 * maybe we have moved to a background thread.
                 * But with flow, thanks to flow's 'Context Preservation' principle,
                 * the lambda is guaranteed to be executed
                 * in the collector context (i.e. lifecycleScope).
                 */
                .mapLatest { um.setName(binding.edittextName.text.toString()) }
                /**
                 * <hide>
                 * will not be call onErr */
                .onEach { binding.progressBar.hide() }
                .catch { } // ignore err handling
                .collect {
                    finish()
                }
        }
    }

    private fun clickToSubmitUserNameByEvent() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }

                .mapLatest { ev -> um.setName(ev.userName) }
                .map { resp -> SubmitUiModel.Success() }
                .catch { e -> emit(SubmitUiModel.Error(e)) }
                .onStart { emit(SubmitUiModel.InProgress()) }

                .collect { model -> render(model) }
        }
    }

    private fun clickToSubmitUserNameByEventDecomposition() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            val events: Flow<SubmitUserNameEvent> = binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }

            /**
             * <hide>
             * This whole section dose NOT know anything about the UI.
             * And it can be changed into a `transformer`
             */
            val models: Flow<SubmitUiModel> = events
                .mapLatest { ev -> um.setName(ev.userName) }
                .map { resp -> SubmitUiModel.Success() }
                .catch { e -> emit(SubmitUiModel.Error(e)) }
                .onStart { emit(SubmitUiModel.InProgress()) }

            models.collect { model -> render(model) }
        }
    }

    private fun clickToSubmitUserNameByEventDecompositionWithTransformer() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            val events: Flow<SubmitUserNameEvent> = binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }

            // This whole section dose * NOT * know anything about the UI.
            val submit: (Flow<SubmitUserNameEvent>) -> Flow<SubmitUiModel> = { evts ->
                evts.mapLatest { ev -> um.setName(ev.userName) }
                    .map { resp -> SubmitUiModel.Success() }
                    .catch { e -> emit(SubmitUiModel.Error(e)) }
                    .onStart { emit(SubmitUiModel.InProgress()) }
            }

            submit(events).collect { model -> render(model) }
        }
    }

    /**
     * see [SubmitUserNameTransformer]
     */
    private fun clickToSubmitUserNameBySubmitTransformer(
        submit: (Flow<SubmitUserNameEvent>) -> Flow<SubmitUiModel>
    ) {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            // UI code group together to user the `submit` transformer
            binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }
                .let { event -> submit(event) }
                .collect { model -> render(model) }
        }
    }

    private fun render(model: SubmitUiModel) = with(binding) {
        btnSubmit.isEnabled = !model.inProgress
        progressBar.isVisible = model.inProgress

        if (!model.inProgress) {
            if (model.success) finish() else toast("Failed to set user name")
        }
    } // end render


    //===============================================================================
    //  What If We Have Multiple Requests ???
    //===============================================================================

    // Let's go back to there three parts from the UI

    private fun clickToSubmitInThreeParts() {
        clickToSubmitUserNameByEventDecomposition()
    }

    private fun submitMultipleRequests(
        submit: (Flow<SubmitEvent>) -> Flow<SubmitUiModel>,
        checkName: (Flow<CheckNameEvent>) -> Flow<SubmitUiModel>,
    ) {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            // events
            val submitEvents: Flow<SubmitEvent> = binding.btnSubmit.clicks()
                .map { SubmitEvent(binding.edittextName.text.toString()) }

            val checkNameEvents: Flow<CheckNameEvent> = binding.edittextName.afterTextChanges()
                .map { txt -> CheckNameEvent(txt) }

            val events: Flow<SubmitUiEvent> = merge(submitEvents, checkNameEvents)


            // transformer
            val submitUi: (Flow<SubmitUiEvent>) -> Flow<SubmitUiModel> = { evts ->
                val shared: SharedFlow<SubmitUiEvent> = evts.shareIn(
                    scope = lifecycleScope,
                    started = SharingStarted.WhileSubscribed()
                )

                merge(
                    submit(shared.filterIsInstance()),
                    checkName(shared.filterIsInstance())
                )
            }

            // render
            submitUi(events).collect { model -> render(model) }
        }
    }

    // ==========================  BUT !!!
    // There is a bug, with SubmitUiModel mapping.
    // We need to distinguish API results and UI Model.
    // see [Result]


    private fun submitMultipleRequestsWithActionAndResult(
        submit: (Flow<SubmitAction>) -> Flow<SubmitResult>,
        checkName: (Flow<CheckNameAction>) -> Flow<CheckNameResult>,
    ) {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            // actions
            val submitActions: Flow<SubmitAction> = binding.btnSubmit.clicks()
                .map { SubmitEvent(binding.edittextName.text.toString()) }
                .map { SubmitAction(it.userName) }

            val checkNameActions: Flow<CheckNameAction> = binding.edittextName.afterTextChanges()
                .map { txt -> CheckNameEvent(txt) }
                .map { CheckNameAction(it.userName) }

            val actions: Flow<Action> = merge(submitActions, checkNameActions)


            // transformer
            val transform: (Flow<Action>) -> Flow<Result> = { actions ->
                val shared: SharedFlow<Action> = actions.shareIn(
                    scope = lifecycleScope,
                    started = SharingStarted.WhileSubscribed()
                )

                merge(
                    submit(shared.filterIsInstance()),
                    checkName(shared.filterIsInstance())
                )
            }

            val uiModels: Flow<SubmitUiModel> = transform(actions)
                .scan(initial = SubmitUiModel.Idle()) { state, result ->
                    if (result == CheckNameResult.IN_PROGRESS || result == SubmitResult.IN_PROGRESS)
                        return@scan SubmitUiModel.InProgress()
                    if (result == CheckNameResult.SUCCESS)
                        return@scan SubmitUiModel.Idle()
                    if (result == SubmitResult.SUCCESS)
                        return@scan SubmitUiModel.Success()

                    //TODO handle ERROR
                    throw IllegalArgumentException("Unknown result = $result")
                }

            // render
            uiModels.collect { model -> render(model) }
        }
    }
}