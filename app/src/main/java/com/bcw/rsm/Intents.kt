package com.bcw.rsm

data class SubmitUserNameEvent(val userName: String)


//====================== multiple requests

abstract class SubmitUiEvent
data class SubmitEvent(val userName: String) : SubmitUiEvent()
data class CheckNameEvent(val userName: String) : SubmitUiEvent()


//======================= actions
abstract class Action
data class SubmitAction(val userName: String) : Action()
data class CheckNameAction(val userName: String) : Action()