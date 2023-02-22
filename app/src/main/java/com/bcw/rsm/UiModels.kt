package com.bcw.rsm

data class SubmitUiModel(
    val inProgress: Boolean,
    val success: Boolean,
    val error: Throwable?
) {
    companion object {
        @Suppress("FunctionName")
        fun InProgress() = SubmitUiModel(inProgress = true, success = false, error = null)

        @Suppress("FunctionName")
        fun Success() = SubmitUiModel(inProgress = false, success = true, error = null)

        @Suppress("FunctionName")
        fun Error(error: Throwable) = SubmitUiModel(false, success = false, error = error)

        // ==================== leave it for now
        @Suppress("FunctionName")
        fun Idle() = SubmitUiModel(false, success = false, error = null)
    }
}