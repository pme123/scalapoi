package scalapoi

import shapeless._

import cats._
import cats.data._
import cats.implicits._

import org.apache.poi.ss.usermodel.{Cell, Row}
import org.apache.poi.xssf.usermodel._

// the following code is designed to derive a PoiEncoder instance for flat case classes
object instances {
  implicit val stringEncoder: PoiEncoder[String] = PoiEncoder.pure(1)(s => List(StrCell(s)))
  implicit val booleanEncoder: PoiEncoder[Boolean] =
    PoiEncoder.pure(1)(b => List(BoolCell(b)))
  implicit val intEncoder: PoiEncoder[Int] = PoiEncoder.pure(1)(i => List(NumCell(i)))
  implicit val doubleEncoder: PoiEncoder[Double] = PoiEncoder.pure(1)(d => List(NumCell(d)))
  implicit val longEncoder: PoiEncoder[Long] = PoiEncoder.pure(1)(l => List(NumCell(l)))

  implicit def optionEncoder[A](implicit E: PoiEncoder[A]): PoiEncoder[Option[A]] =
    PoiEncoder.pure(E.width)(o => o.map(E.encode).getOrElse(List.fill(E.width)(BlankCell)))

  implicit val hnilEncoder: PoiEncoder[HNil] = PoiEncoder.pure(0)(hnil => Nil)

  implicit def hlistEncoder[H, T <: HList](
    implicit
    hEncoder: Lazy[PoiEncoder[H]],
    tEncoder: PoiEncoder[T]
  ): PoiEncoder[H :: T] =
    PoiEncoder.pure(hEncoder.value.width + tEncoder.width) {
      case h :: t => hEncoder.value.encode(h) ++ tEncoder.encode(t)
    }

  implicit val cnilEncoder: PoiEncoder[CNil] =
    PoiEncoder.pure(0)(cnil => throw new Exception("unreachable"))

  implicit def coproductEncoder[H, T <: shapeless.Coproduct](
    implicit
    hEncoder: Lazy[PoiEncoder[H]],
    tEncoder: PoiEncoder[T]
  ): PoiEncoder[H :+: T] = PoiEncoder.pure(hEncoder.value.width + tEncoder.width) {
    case Inl(h) => hEncoder.value.encode(h) ++ List.fill(tEncoder.width)(BlankCell)
    case Inr(t) => List.fill(hEncoder.value.width)(BlankCell) ++ tEncoder.encode(t)
  }

  implicit def genericEncoder[A, R](
    implicit
    gen: Generic.Aux[A, R],
    enc: Lazy[PoiEncoder[R]]
  ): PoiEncoder[A] = PoiEncoder.pure(enc.value.width)(a => enc.value.encode(gen.to(a)))
}
