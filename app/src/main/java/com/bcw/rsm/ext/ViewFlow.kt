package com.bcw.rsm.ext

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun View.clicks(): Flow<View> = callbackFlow {
    setOnClickListener { trySend(it) }
    awaitClose { setOnClickListener(null) }
}

fun EditText.texts(debounce: Long = 1000): Flow<String> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            s?.toString()?.let { txt -> trySend(txt) }
        }
    }
    addTextChangedListener(watcher)
    awaitClose { removeTextChangedListener(watcher) }
}
    .debounce(debounce)
    .distinctUntilChanged()