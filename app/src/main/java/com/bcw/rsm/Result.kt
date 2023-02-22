package com.bcw.rsm


abstract class Result

data class CheckNameResult(
    val inProgress: Boolean,
    val success: Boolean,
    val error: Throwable?,
) : Result() {
    companion object {
        @Suppress("FunctionName")
        fun InProgress(): CheckNameResult = CheckNameResult(inProgress = true, success = false, error = null)

        @Suppress("FunctionName")
        fun Success(): CheckNameResult =
            CheckNameResult(inProgress = false, success = true, error = null)

        @Suppress("FunctionName")
        fun Error(error: Throwable): CheckNameResult =
            CheckNameResult(inProgress = false, success = false, error = error)
    }
}

data class SubmitResult(
    val inProgress: Boolean,
    val success: Boolean,
    val error: Throwable?,
) : Result() {
    companion object {
        @Suppress("FunctionName")
        fun InProgress(): SubmitResult = SubmitResult(inProgress = true, success = false, error = null)

        @Suppress("FunctionName")
        fun Success(): SubmitResult =
            SubmitResult(inProgress = false, success = true, error = null)

        @Suppress("FunctionName")
        fun Error(error: Throwable): SubmitResult =
            SubmitResult(inProgress = false, success = false, error = error)
    }
}