package arrow.effects.internal

import arrow.core.*
import arrow.effects.IO
import arrow.effects.typeclasses.Proc
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

/* See par3 */
internal fun <A, B, C> par2(ctx: CoroutineContext, ioA: IO<A>, ioB: IO<B>, f: (A, B) -> C): Proc<C> = { cc ->
  val a: suspend () -> Either<A, B> = {
    suspendCoroutine { ca: Continuation<Either<A, B>> ->
      ioA.map { it.left() }.unsafeRunAsync {
        it.fold(ca::resumeWithException, ca::resume)
      }
    }
  }
  val b: suspend () -> Either<A, B> = {
    suspendCoroutine { ca: Continuation<Either<A, B>> ->
      ioB.map { it.right() }.unsafeRunAsync {
        it.fold(ca::resumeWithException, ca::resume)
      }
    }
  }
  val parCont = parContinuation(ctx, f, asyncIOContinuation(ctx, cc))
  a.startCoroutine(parCont)
  b.startCoroutine(parCont)
}

/* Parallelization is only provided in pairs and triples.
 * Every time you start 4+ elements, each pair or triple has to be combined with another one at the same depth.
 * Elements at higher depths that are synchronous can prevent elements at a higher depth to start.
 * Thus, we need to provide solutions for even and uneven amounts of IOs for all to be started at the same depth. */
internal fun <A, B, C, D> par3(ctx: CoroutineContext, ioA: IO<A>, ioB: IO<B>, ioC: IO<C>, f: (A, B, C) -> D): Proc<D> = { cc ->
  val a: suspend () -> Treither<A, B, C> = {
    suspendCoroutine { ca: Continuation<Treither<A, B, C>> ->
      ioA.map { Treither.Left<A, B, C>(it) }.unsafeRunAsync {
        it.fold(ca::resumeWithException, ca::resume)
      }
    }
  }
  val b: suspend () -> Treither<A, B, C> = {
    suspendCoroutine { ca: Continuation<Treither<A, B, C>> ->
      ioB.map { Treither.Middle<A, B, C>(it) }.unsafeRunAsync {
        it.fold(ca::resumeWithException, ca::resume)
      }
    }
  }
  val c: suspend () -> Treither<A, B, C> = {
    suspendCoroutine { ca: Continuation<Treither<A, B, C>> ->
      ioC.map { Treither.Right<A, B, C>(it) }.unsafeRunAsync {
        it.fold(ca::resumeWithException, ca::resume)
      }
    }
  }
  val triCont = triContinuation(ctx, f, asyncIOContinuation(ctx, cc))
  a.startCoroutine(triCont)
  b.startCoroutine(triCont)
  c.startCoroutine(triCont)
}

private fun <A, B, C> parContinuation(ctx: CoroutineContext, f: (A, B) -> C, cc: Continuation<C>): Continuation<Either<A, B>> =
  object : Continuation<Either<A, B>> {
    override val context: CoroutineContext = ctx

    var intermediate: Tuple2<A?, B?> = null toT null

    override fun resume(value: Either<A, B>) =
      synchronized(this) {
        val resA = intermediate.a
        val resB = intermediate.b
        value.fold({ a ->
          intermediate = a toT resB
          if (resB != null) {
            cc.resume(f(a, resB))
          }
        }, { b ->
          intermediate = resA toT b
          if (resA != null) {
            cc.resume(f(resA, b))
          }
        })
      }

    override fun resumeWithException(exception: Throwable) {
      cc.resumeWithException(exception)
    }
  }

private fun <A, B, C, D> triContinuation(ctx: CoroutineContext, f: (A, B, C) -> D, cc: Continuation<D>): Continuation<Treither<A, B, C>> =
  object : Continuation<Treither<A, B, C>> {
    override val context: CoroutineContext = ctx

    var intermediate: Tuple3<A?, B?, C?> = Tuple3(null, null, null)

    override fun resume(value: Treither<A, B, C>) =
      synchronized(this) {
        val resA = intermediate.a
        val resB = intermediate.b
        val resC = intermediate.c
        value.fold({ a ->
          intermediate = Tuple3(a, resB, resC)
          if (resB != null && resC != null) {
            cc.resume(f(a, resB, resC))
          }
        }, { b ->
          intermediate = Tuple3(resA, b, resC)
          if (resA != null && resC != null) {
            cc.resume(f(resA, b, resC))
          }
        }, { c ->
          intermediate = Tuple3(resA, resB, c)
          if (resA != null && resB != null) {
            cc.resume(f(resA, resB, c))
          }
        })
      }

    override fun resumeWithException(exception: Throwable) {
      cc.resumeWithException(exception)
    }
  }

private sealed class Treither<out A, out B, out C> {
  data class Left<out A, out B, out C>(val a: A) : Treither<A, B, C>() {
    override fun <D> fold(fa: (A) -> D, fb: (B) -> D, fc: (C) -> D) =
      fa(a)
  }

  data class Middle<out A, out B, out C>(val b: B) : Treither<A, B, C>() {
    override fun <D> fold(fa: (A) -> D, fb: (B) -> D, fc: (C) -> D) =
      fb(b)
  }

  data class Right<out A, out B, out C>(val c: C) : Treither<A, B, C>() {
    override fun <D> fold(fa: (A) -> D, fb: (B) -> D, fc: (C) -> D) =
      fc(c)
  }

  abstract fun <D> fold(fa: (A) -> D, fb: (B) -> D, fc: (C) -> D): D
}

private fun <A> asyncIOContinuation(ctx: CoroutineContext, cc: (Either<Throwable, A>) -> Unit): Continuation<A> =
  object : Continuation<A> {
    override val context: CoroutineContext = ctx

    override fun resume(value: A) {
      cc(value.right())
    }

    override fun resumeWithException(exception: Throwable) {
      cc(exception.left())
    }
  }
