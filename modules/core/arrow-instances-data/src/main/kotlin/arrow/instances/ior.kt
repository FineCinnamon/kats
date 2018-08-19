package arrow.instances

import arrow.Kind
import arrow.Kind2
import arrow.core.Either
import arrow.core.Eval
import arrow.data.*
import arrow.extension
import arrow.typeclasses.*

@extension
interface IorFunctorInstance<L> : Functor<IorPartialOf<L>> {
  override fun <A, B> Kind<IorPartialOf<L>, A>.map(f: (A) -> B): Ior<L, B> = fix().map(f)
}

@extension
interface IorBifunctorInstance : Bifunctor<ForIor> {
  override fun <A, B, C, D> Kind2<ForIor, A, B>.bimap(fl: (A) -> C, fr: (B) -> D): Kind2<ForIor, C, D> =
    fix().bimap(fl, fr)
}

@extension
interface IorApplicativeInstance<L> : Applicative<IorPartialOf<L>>, IorFunctorInstance<L> {

  fun SL(): Semigroup<L>

  override fun <A> just(a: A): Ior<L, A> = Ior.Right(a)

  override fun <A, B> Kind<IorPartialOf<L>, A>.map(f: (A) -> B): Ior<L, B> = fix().map(f)

  override fun <A, B> Kind<IorPartialOf<L>, A>.ap(ff: Kind<IorPartialOf<L>, (A) -> B>): Ior<L, B> =
    fix().ap(SL(), ff)
}

@extension
interface IorMonadInstance<L> : IorApplicativeInstance<L>, Monad<IorPartialOf<L>> {

  override fun <A, B> Kind<IorPartialOf<L>, A>.map(f: (A) -> B): Ior<L, B> = fix().map(f)

  override fun <A, B> Kind<IorPartialOf<L>, A>.flatMap(f: (A) -> Kind<IorPartialOf<L>, B>): Ior<L, B> =
    fix().flatMap(SL(), { f(it).fix() })

  override fun <A, B> Kind<IorPartialOf<L>, A>.ap(ff: Kind<IorPartialOf<L>, (A) -> B>): Ior<L, B> =
    fix().ap(SL(), ff)

  override fun <A, B> tailRecM(a: A, f: (A) -> IorOf<L, Either<A, B>>): Ior<L, B> =
    Ior.tailRecM(a, f, SL())

}

@extension
interface IorFoldableInstance<L> : Foldable<IorPartialOf<L>> {

  override fun <B, C> Kind<IorPartialOf<L>, B>.foldLeft(b: C, f: (C, B) -> C): C = fix().foldLeft(b, f)

  override fun <B, C> Kind<IorPartialOf<L>, B>.foldRight(lb: Eval<C>, f: (B, Eval<C>) -> Eval<C>): Eval<C> =
    fix().foldRight(lb, f)

}

@extension
interface IorTraverseInstance<L> : IorFoldableInstance<L>, Traverse<IorPartialOf<L>> {

  override fun <G, B, C> IorOf<L, B>.traverse(AP: Applicative<G>, f: (B) -> Kind<G, C>): Kind<G, Ior<L, C>> =
    fix().traverse(AP, f)

}

@extension
interface IorEqInstance<L, R> : Eq<Ior<L, R>> {

  fun EQL(): Eq<L>

  fun EQR(): Eq<R>

  override fun Ior<L, R>.eqv(b: Ior<L, R>): Boolean = when (this) {
    is Ior.Left -> when (b) {
      is Ior.Both -> false
      is Ior.Right -> false
      is Ior.Left -> EQL().run { value.eqv(b.value) }
    }
    is Ior.Both -> when (b) {
      is Ior.Left -> false
      is Ior.Both -> EQL().run { leftValue.eqv(b.leftValue) } && EQR().run { rightValue.eqv(b.rightValue) }
      is Ior.Right -> false
    }
    is Ior.Right -> when (b) {
      is Ior.Left -> false
      is Ior.Both -> false
      is Ior.Right -> EQR().run { value.eqv(b.value) }
    }

  }
}

@extension
interface IorShowInstance<L, R> : Show<Ior<L, R>> {
  override fun Ior<L, R>.show(): String =
    toString()
}

class IorContext<L>(val SL: Semigroup<L>) : IorMonadInstance<L>, IorTraverseInstance<L> {

  override fun SL(): Semigroup<L> = SL

  override fun <A, B> Kind<IorPartialOf<L>, A>.map(f: (A) -> B): Ior<L, B> =
    fix().map(f)
}

class IorContextPartiallyApplied<L>(val SL: Semigroup<L>) {
  infix fun <A> extensions(f: IorContext<L>.() -> A): A =
    f(IorContext(SL))
}

fun <L> ForIor(SL: Semigroup<L>): IorContextPartiallyApplied<L> =
  IorContextPartiallyApplied(SL)