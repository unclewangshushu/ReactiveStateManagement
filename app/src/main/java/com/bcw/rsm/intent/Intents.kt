package com.bcw.rsm.intent

data class SubmitUserNameEvent(val userName: String)


//====================== multiple requests

sealed class SubmitUiEvent
data class SubmitEvent(val userName: String) : SubmitUiEvent()
data class CheckNameEvent(val userName: String) : SubmitUiEvent()


//======================= actions
interface  Action
data class SubmitAction(val userName: String) : Action
data class CheckNameAction(val userName: String) : Action