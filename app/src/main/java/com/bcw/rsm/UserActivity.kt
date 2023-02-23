package com.bcw.rsm

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bcw.rsm.data.UserManager
import com.bcw.rsm.data.UserManagerImpl
import com.bcw.rsm.transform.*
import com.bcw.rsm.databinding.ActivityUserBinding
import com.bcw.rsm.ext.launchRepeatOnLifecycle
import com.bcw.rsm.ext.toast
import com.bcw.rsm.intent.*
import com.bcw.rsm.ext.texts
import com.bcw.rsm.ext.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserActivity : AppCompatActivity() {

    private val tag = "UserActivity"

    private lateinit var um: UserManager
    private lateinit var binding: ActivityUserBinding
    private lateinit var adapter: StringAdapter

    private val red: Int by lazy(LazyThreadSafetyMode.NONE) {
        ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, null)
    }
    private val green: Int by lazy(LazyThreadSafetyMode.NONE) {
        ResourcesCompat.getColor(resources, android.R.color.holo_green_light, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(LayoutInflater.from(this))
        adapter = StringAdapter(this)
        binding.uiHistory.layoutManager =
            LinearLayoutManager(this).apply { orientation = LinearLayoutManager.VERTICAL }
        binding.uiHistory.adapter = adapter
        setContentView(binding.root)

        um = UserManagerImpl()

        observeUser()

//        naiveClickToSubmitUserName()
//        clickToSubmitUserNameByEvent()
//        clickToSubmitUserNameByEventDecomposition()
//        clickToSubmitUserNameByEventDecompositionWithTransformer()
//        clickToSubmitUserNameBySubmitTransformer(SubmitUserNameTransformer(um))
//        submitMultipleRequests(SubmitTransformer(um), CheckNameTransformer(um))
//        submitMultipleRequestsWithActionAndResult(
//            SubmitActionTransformer(um),
//            CheckNameActionTransformer(um)
//            //CheckNameActionTransformerByAutoTransform(um)
//        )

    }

    private fun observeUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                um.getUser()
                    .flowOn(Dispatchers.IO) //TODO not needed, because it's the api owner's responsibility to ensure main-safety
                    .catch {
                    }
                    .collect {
                        binding.user.text = it.toString()
                        Log.i(tag, "user update: $it")
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
//                .flowOn(Dispatchers.IO) //TODO wbc, try me
                /**
                 * <hide>
                 * will not be call onErr */
                .onEach { binding.progressBar.hide() }
                .catch { Log.e(tag, "naiveClickToSubmitUserName err", it) }
                .collect {
                    binding.btnSubmit.isEnabled = true
                }
        }
    }

    private fun clickToSubmitUserNameByEvent() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }
                .onEach { log(it) }
                .flatMapLatest { ev ->
                    flow { emit(um.setName(ev.userName)) }
                        .map { resp -> SubmitUiModel.Success() }
                        .onStart { emit(SubmitUiModel.InProgress()) }
                        .catch { e -> emit(SubmitUiModel.Error(e)) }
                }
                .collect { model -> render(model) }
        }
    }

    private fun clickToSubmitUserNameByEventDecomposition() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            val events: Flow<SubmitUserNameEvent> = binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }
                .onEach { log(it) }

            /**
             * <hide>
             * This whole section dose NOT know anything about the UI.
             * And it can be changed into a `transformer`
             */
            val models: Flow<SubmitUiModel> = events
                .flatMapLatest { ev ->
                    flow { emit(um.setName(ev.userName)) }
                        .map { resp -> SubmitUiModel.Success() }
                        .onStart { emit(SubmitUiModel.InProgress()) }
                        .catch { e -> emit(SubmitUiModel.Error(e)) }
                }

            models.collect { model -> render(model) }
        }
    }

    private fun clickToSubmitUserNameByEventDecompositionWithTransformer() {
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            val events: Flow<SubmitUserNameEvent> = binding.btnSubmit.clicks()
                .map { SubmitUserNameEvent(userName = binding.edittextName.text.toString()) }
                .onEach { log(it) }

            // This whole section dose * NOT * know anything about the UI.
            val submit: (Flow<SubmitUserNameEvent>) -> Flow<SubmitUiModel> = { evts ->
                evts.flatMapLatest { ev ->
                    flow { emit(um.setName(ev.userName)) }
                        .map { resp -> SubmitUiModel.Success() }
                        .onStart { emit(SubmitUiModel.InProgress()) }
                        .catch { e -> emit(SubmitUiModel.Error(e)) }
                }
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
                .onEach { log(it) }
                .let { event -> submit(event) }
                .collect { model -> render(model) }
        }
    }

    private fun render(model: SubmitUiModel) = with(binding) {
        log(model)

        Log.i(tag, "render $model")
        btnSubmit.isEnabled = !model.inProgress
        progressBar.isVisible = model.inProgress

        if (!model.inProgress) {
            if (model.success) {
            } else toast("Failed to set user name")
        }


        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        if (multipleReq) {
            nameChecked.visibility =
                if (model.nameCheckPass != null) View.VISIBLE else View.INVISIBLE
            model.nameCheckPass?.let { pass -> nameChecked.setBackgroundColor(if (pass) green else red) }
            progressBar.isVisible = model.inProgress || model.nameCheckProgress == true
            btnSubmit.isEnabled = !model.inProgress && model.nameCheckPass == true
        }

        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now
        //========= leave it for now

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

            val checkNameEvents: Flow<CheckNameEvent> = binding.edittextName.texts()
                .map { txt -> CheckNameEvent(txt) }

            val events: Flow<SubmitUiEvent> = merge(submitEvents, checkNameEvents)
                .onEach { log(it) }


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


    /**
     * see [SubmitActionTransformer]
     * see [CheckNameActionTransformer]
     */
    private fun submitMultipleRequestsWithActionAndResult(
        submit: (Flow<SubmitAction>) -> Flow<SubmitResult>,
        checkName: (Flow<CheckNameAction>) -> Flow<CheckNameResult>,
    ) {
        multipleReq = true

        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            // actions
            val submitActions: Flow<SubmitAction> = binding.btnSubmit.clicks()
                .map { SubmitEvent(binding.edittextName.text.toString()) }
                .map { SubmitAction(it.userName) }

            val checkNameActions: Flow<CheckNameAction> = binding.edittextName.texts()
                .map { txt -> CheckNameEvent(txt) }
                .map { CheckNameAction(it.userName) }

            val actions: Flow<Action> = merge(submitActions, checkNameActions)
                .onEach { log(it) }


            // transformer
            val transform: (Flow<Action>) -> Flow<AsyncResult<*>> = {
                it.shareIn(
                    scope = lifecycleScope,
                    started = SharingStarted.WhileSubscribed()
                ).let { shared ->
                    merge(
                        submit(shared.filterIsInstance()),
                        checkName(shared.filterIsInstance())
                    )
                }
            }

            val results: Flow<AsyncResult<*>> = transform(actions)

            val uiModels: Flow<SubmitUiModel> = results
                // incremental state updating with `scan`
                .scan(initial = SubmitUiModel.Idle()) { state, result ->
                    when (result) {
                        is CheckNameResult -> when (result.async) {
                            is Async.Fail -> state.copy(
                                nameCheckPass = null,
                                nameCheckProgress = true
                            )
                            is Async.Loading -> state.copy(
                                nameCheckPass = true,
                                nameCheckProgress = false
                            )
                            is Async.Success -> state.copy(
                                nameCheckPass = false,
                                nameCheckProgress = false
                            )
                        }
                        is SubmitResult -> when (result.async) {
                            is Async.Fail -> state.copy(
                                success = false,
                                inProgress = false,
                                error = result.async.error
                            )
                            is Async.Loading -> state.copy(
                                success = false,
                                inProgress = true,
                                error = null
                            )
                            is Async.Success -> state.copy(
                                success = true,
                                inProgress = false,
                                error = null,
                                nameCheckPass = null
                            )
                        }
                        else -> throw UnsupportedOperationException(result.toString())
                    }
                }

            // render
            uiModels.collect { render(it) }
        }
    }

    private fun log(msg: Any) {
        adapter.data.add(0, msg.toString())
        adapter.notifyItemInserted(0)
        binding.uiHistory.smoothScrollToPosition(0)
    }


    private var multipleReq = false
}