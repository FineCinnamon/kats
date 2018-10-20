package arrow.core

interface Continuation<in T> : kotlin.coroutines.Continuation<T> {
  fun resume(value: T)

  fun resumeWithException(exception: Throwable)

  override fun resumeWith(result: SuccessOrFailure<T>) =
    result.fold(::resume, ::resumeWithException)

}
