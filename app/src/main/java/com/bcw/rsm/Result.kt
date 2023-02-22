package com.bcw.rsm


open class Result(
    open val inProgress: Boolean,
    open val success: Boolean,
    open val error: Throwable?,
) {
    @Suppress("FunctionName")
    open fun InProgress(): Result = Result(inProgress = true, success = false, error = null)

    @Suppress("FunctionName")
    open fun Success(): Result = Result(inProgress = false, success = true, error = null)

    @Suppress("FunctionName")
    open fun Error(error: Throwable): Result = Result(inProgress = false, success = false, error = error)
}

data class CheckNameResult(
    override val inProgress: Boolean,
    override val success: Boolean,
    override val error: Throwable?,
) : Result(inProgress, success, error)

data class SubmitResult(
    override val inProgress: Boolean,
    override val success: Boolean,
    override val error: Throwable?,
) : Result(inProgress, success, error)