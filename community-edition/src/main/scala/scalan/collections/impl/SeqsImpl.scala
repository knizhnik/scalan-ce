package scalan.collections
package impl

import scala.collection.immutable.Seq
import scalan._
import scalan.common.Default
import scala.reflect.runtime.universe._
import scalan.common.Default

// Abs -----------------------------------
trait SeqsAbs extends Scalan with Seqs {
  self: SeqsDsl =>
  // single proxy for each type family
  implicit def proxySSeq[A](p: Rep[SSeq[A]]): SSeq[A] =
    proxyOps[SSeq[A]](p)
  // BaseTypeEx proxy
  implicit def proxySeq[A:Elem](p: Rep[Seq[A]]): SSeq[A] =
    proxyOps[SSeq[A]](p.asRep[SSeq[A]])

  implicit def defaultSSeqElem[A:Elem]: Elem[SSeq[A]] = element[SSeqImpl[A]].asElem[SSeq[A]]
  implicit def SeqElement[A:Elem:WeakTypeTag]: Elem[Seq[A]]

  abstract class SSeqElem[A, From, To <: SSeq[A]](iso: Iso[From, To]) extends ViewElem[From, To]()(iso)

  trait SSeqCompanionElem extends CompanionElem[SSeqCompanionAbs]
  implicit lazy val SSeqCompanionElem: SSeqCompanionElem = new SSeqCompanionElem {
    lazy val tag = weakTypeTag[SSeqCompanionAbs]
    protected def getDefaultRep = SSeq
  }

  abstract class SSeqCompanionAbs extends CompanionBase[SSeqCompanionAbs] with SSeqCompanion {
    override def toString = "SSeq"

    def apply[A:Elem](arr: Rep[Array[A]]): Rep[Seq[A]] =
      methodCallEx[Seq[A]](self,
        this.getClass.getMethod("apply", classOf[AnyRef], classOf[Elem[A]]),
        List(arr.asInstanceOf[AnyRef], element[A]))

    def empty[A:Elem]: Rep[Seq[A]] =
      methodCallEx[Seq[A]](self,
        this.getClass.getMethod("empty", classOf[Elem[A]]),
        List(element[A]))
  }
  def SSeq: Rep[SSeqCompanionAbs]
  implicit def proxySSeqCompanion(p: Rep[SSeqCompanion]): SSeqCompanion = {
    proxyOps[SSeqCompanion](p)
  }

  // default wrapper implementation
  abstract class SSeqImpl[A](val wrappedValueOfBaseType: Rep[Seq[A]])(implicit val eA: Elem[A]) extends SSeq[A] {
    def isEmpty: Rep[Boolean] =
      methodCallEx[Boolean](self,
        this.getClass.getMethod("isEmpty"),
        List())
  }
  trait SSeqImplCompanion
  // elem for concrete class
  class SSeqImplElem[A](iso: Iso[SSeqImplData[A], SSeqImpl[A]]) extends SSeqElem[A, SSeqImplData[A], SSeqImpl[A]](iso)

  // state representation type
  type SSeqImplData[A] = Seq[A]

  // 3) Iso for concrete class
  class SSeqImplIso[A](implicit eA: Elem[A])
    extends Iso[SSeqImplData[A], SSeqImpl[A]] {
    override def from(p: Rep[SSeqImpl[A]]) =
      unmkSSeqImpl(p) match {
        case Some((wrappedValueOfBaseType)) => wrappedValueOfBaseType
        case None => !!!
      }
    override def to(p: Rep[Seq[A]]) = {
      val wrappedValueOfBaseType = p
      SSeqImpl(wrappedValueOfBaseType)
    }
    lazy val tag = {
      weakTypeTag[SSeqImpl[A]]
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[SSeqImpl[A]]](SSeqImpl(DefaultOfSeq[A].value))
    lazy val eTo = new SSeqImplElem[A](this)
  }
  // 4) constructor and deconstructor
  abstract class SSeqImplCompanionAbs extends CompanionBase[SSeqImplCompanionAbs] with SSeqImplCompanion {
    override def toString = "SSeqImpl"

    def apply[A](wrappedValueOfBaseType: Rep[Seq[A]])(implicit eA: Elem[A]): Rep[SSeqImpl[A]] =
      mkSSeqImpl(wrappedValueOfBaseType)
    def unapply[A:Elem](p: Rep[SSeqImpl[A]]) = unmkSSeqImpl(p)
  }
  def SSeqImpl: Rep[SSeqImplCompanionAbs]
  implicit def proxySSeqImplCompanion(p: Rep[SSeqImplCompanionAbs]): SSeqImplCompanionAbs = {
    proxyOps[SSeqImplCompanionAbs](p)
  }

  class SSeqImplCompanionElem extends CompanionElem[SSeqImplCompanionAbs] {
    lazy val tag = weakTypeTag[SSeqImplCompanionAbs]
    protected def getDefaultRep = SSeqImpl
  }
  implicit lazy val SSeqImplCompanionElem: SSeqImplCompanionElem = new SSeqImplCompanionElem

  implicit def proxySSeqImpl[A](p: Rep[SSeqImpl[A]]): SSeqImpl[A] =
    proxyOps[SSeqImpl[A]](p)

  implicit class ExtendedSSeqImpl[A](p: Rep[SSeqImpl[A]])(implicit eA: Elem[A]) {
    def toData: Rep[SSeqImplData[A]] = isoSSeqImpl(eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoSSeqImpl[A](implicit eA: Elem[A]): Iso[SSeqImplData[A], SSeqImpl[A]] =
    new SSeqImplIso[A]

  // 6) smart constructor and deconstructor
  def mkSSeqImpl[A](wrappedValueOfBaseType: Rep[Seq[A]])(implicit eA: Elem[A]): Rep[SSeqImpl[A]]
  def unmkSSeqImpl[A:Elem](p: Rep[SSeqImpl[A]]): Option[(Rep[Seq[A]])]
}

// Seq -----------------------------------
trait SeqsSeq extends SeqsDsl with ScalanSeq {
  self: SeqsDslSeq =>
  lazy val SSeq: Rep[SSeqCompanionAbs] = new SSeqCompanionAbs with UserTypeSeq[SSeqCompanionAbs, SSeqCompanionAbs] {
    lazy val selfType = element[SSeqCompanionAbs]

    override def apply[A:Elem](arr: Rep[Array[A]]): Rep[Seq[A]] =
      Seq.apply[A](arr: _*)

    override def empty[A:Elem]: Rep[Seq[A]] =
      Seq.empty[A]
  }

    // override proxy if we deal with BaseTypeEx
  override def proxySeq[A:Elem](p: Rep[Seq[A]]): SSeq[A] =
    proxyOpsEx[Seq[A],SSeq[A], SeqSSeqImpl[A]](p, bt => SeqSSeqImpl(bt))

    implicit def SeqElement[A:Elem:WeakTypeTag]: Elem[Seq[A]] = new SeqBaseElemEx[Seq[A], SSeq[A]](element[SSeq[A]])(weakTypeTag[Seq[A]], DefaultOfSeq[A])

  case class SeqSSeqImpl[A]
      (override val wrappedValueOfBaseType: Rep[Seq[A]])
      (implicit eA: Elem[A])
    extends SSeqImpl[A](wrappedValueOfBaseType)
        with UserTypeSeq[SSeq[A], SSeqImpl[A]] {
    lazy val selfType = element[SSeqImpl[A]].asInstanceOf[Elem[SSeq[A]]]

    override def isEmpty: Rep[Boolean] =
      wrappedValueOfBaseType.isEmpty
  }
  lazy val SSeqImpl = new SSeqImplCompanionAbs with UserTypeSeq[SSeqImplCompanionAbs, SSeqImplCompanionAbs] {
    lazy val selfType = element[SSeqImplCompanionAbs]
  }

  def mkSSeqImpl[A]
      (wrappedValueOfBaseType: Rep[Seq[A]])(implicit eA: Elem[A]) =
      new SeqSSeqImpl[A](wrappedValueOfBaseType)
  def unmkSSeqImpl[A:Elem](p: Rep[SSeqImpl[A]]) =
    Some((p.wrappedValueOfBaseType))
}

// Exp -----------------------------------
trait SeqsExp extends SeqsDsl with ScalanExp {
  self: SeqsDslExp =>
  lazy val SSeq: Rep[SSeqCompanionAbs] = new SSeqCompanionAbs with UserTypeDef[SSeqCompanionAbs, SSeqCompanionAbs] {
    lazy val selfType = element[SSeqCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  implicit def SeqElement[A:Elem:WeakTypeTag]: Elem[Seq[A]] = new ExpBaseElemEx[Seq[A], SSeq[A]](element[SSeq[A]])(weakTypeTag[Seq[A]], DefaultOfSeq[A])

  case class ExpSSeqImpl[A]
      (override val wrappedValueOfBaseType: Rep[Seq[A]])
      (implicit eA: Elem[A])
    extends SSeqImpl[A](wrappedValueOfBaseType) with UserTypeDef[SSeq[A], SSeqImpl[A]] {
    lazy val selfType = element[SSeqImpl[A]].asInstanceOf[Elem[SSeq[A]]]
    override def mirror(t: Transformer) = ExpSSeqImpl[A](t(wrappedValueOfBaseType))
  }

  lazy val SSeqImpl: Rep[SSeqImplCompanionAbs] = new SSeqImplCompanionAbs with UserTypeDef[SSeqImplCompanionAbs, SSeqImplCompanionAbs] {
    lazy val selfType = element[SSeqImplCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object SSeqImplMethods {
  }

  def mkSSeqImpl[A]
    (wrappedValueOfBaseType: Rep[Seq[A]])(implicit eA: Elem[A]) =
    new ExpSSeqImpl[A](wrappedValueOfBaseType)
  def unmkSSeqImpl[A:Elem](p: Rep[SSeqImpl[A]]) =
    Some((p.wrappedValueOfBaseType))

  object SSeqMethods {
    object isEmpty {
      def unapply(d: Def[_]): Option[Rep[SSeq[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SSeqElem[_, _, _]] && method.getName == "isEmpty" =>
          Some(receiver).asInstanceOf[Option[Rep[SSeq[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[SSeq[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SSeqCompanionMethods {
    object apply {
      def unapply(d: Def[_]): Option[Rep[Array[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, Seq(arr, _*), _) if receiver.elem.isInstanceOf[SSeqCompanionElem] && method.getName == "apply" =>
          Some(arr).asInstanceOf[Option[Rep[Array[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Array[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object empty {
      def unapply(d: Def[_]): Option[Unit forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SSeqCompanionElem] && method.getName == "empty" =>
          Some(()).asInstanceOf[Option[Unit forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}