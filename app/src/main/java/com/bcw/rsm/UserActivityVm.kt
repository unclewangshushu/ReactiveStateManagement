package com.bcw.rsm

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bcw.rsm.data.UserManager
import com.bcw.rsm.data.UserManagerImpl
import com.bcw.rsm.databinding.ActivityUserBinding
import com.bcw.rsm.ext.launchRepeatOnLifecycle
import com.bcw.rsm.ext.toast
import com.bcw.rsm.intent.CheckNameEvent
import com.bcw.rsm.intent.SubmitEvent
import com.bcw.rsm.ext.texts
import com.bcw.rsm.ext.clicks
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserActivityVm : AppCompatActivity() {

    private val tag = "UserActivity"

    private val um: UserManager = UserManagerImpl()
    private lateinit var binding: ActivityUserBinding
    private lateinit var adapter: StringAdapter
    private val vm: UserViewModel by viewModels { UserViewModel.Factory(um) }

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


        observeUser()

        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                val submitActions: Flow<SubmitEvent> = binding.btnSubmit.clicks()
                    .map { SubmitEvent(binding.edittextName.text.toString()) }

                val checkNameActions: Flow<CheckNameEvent> = binding.edittextName.texts()
                    .map { txt -> CheckNameEvent(txt) }

                val actions = merge(submitActions, checkNameActions)
                    .onEach { log(it) }

                actions.collect { vm.uiEvents.emit(it) }
            }

            launch { vm.uiModels.collect { render(it) } }
        }
    }

    private fun observeUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                um.getUser().collect {
                    binding.user.text = it.toString()
                    Log.i(tag, "user update: $it")
                }
            }
        }
    }

    private fun render(model: SubmitUiModel) = with(binding) {
        log(model)

        Log.i(tag, "render $model")

        if (!model.inProgress) {
            if (model.success) {
            } else toast("Failed to set user name")
        }

        nameChecked.visibility = if (model.nameCheckPass != null) View.VISIBLE else View.INVISIBLE
        model.nameCheckPass?.let { pass -> nameChecked.setBackgroundColor(if (pass) green else red) }
        progressBar.isVisible = model.inProgress || model.nameCheckProgress == true
        btnSubmit.isEnabled = !model.inProgress && model.nameCheckPass == true
    }

    private fun log(msg: Any) {
        adapter.data.add(0, msg.toString())
        adapter.notifyItemInserted(0)
        binding.uiHistory.smoothScrollToPosition(0)
    }
}