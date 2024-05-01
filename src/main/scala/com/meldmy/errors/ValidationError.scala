package com.meldmy.errors

sealed trait ValidationError

object ValidationError {
  case object InvalidUUIDFormatError extends ValidationError
  case object UnknownContentTypeError extends ValidationError
  case object EmptyContentTitleError extends ValidationError
}
