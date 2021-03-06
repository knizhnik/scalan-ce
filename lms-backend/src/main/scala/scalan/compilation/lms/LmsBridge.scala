package scalan.compilation.lms

import scalan.ScalanCtxExp

trait LmsBridge { self: ScalanCtxExp =>

  val lms: LmsBackend

  type SymMirror = Map[Exp[_], lms.Exp[A] forSome {type A}]
  type FuncMirror = Map[Exp[_], (lms.Exp[A] => lms.Exp[B]) forSome {type A; type B}]
  type ExpMirror = Seq[lms.Exp[_]]

  type LmsMirror = (ExpMirror, SymMirror, FuncMirror)

  type EntryTransformer = PartialFunction[TableEntry[_], LmsMirror]
  type DefTransformer = PartialFunction[Def[_], LmsMirror]

  def defTransformer[T](m: LmsMirror, g: AstGraph, e: TableEntry[T]): DefTransformer = {
    case x => !!!(s"MSBridge: Don't know how to mirror symbol ${x.self.toStringWithDefinition}")
  }

  def tableTransformer(m: LmsMirror, g: AstGraph): EntryTransformer = {
    case e => defTransformer(m, g, e)(e.rhs)
  }

  def mirrorLambdaToLmsFunc[I, R](m: LmsMirror)(lam: Lambda[I, R]): (lms.Exp[I] => lms.Exp[R]) = {
    val (expMirror, symMirror, funcMirror) = m
    val lamX = lam.x
    val f = { x: lms.Exp[I] =>
      val sched = lam.scheduleSingleLevel
      val (lamExps, _, _) = mirrorDefs((expMirror, symMirror + ((lamX, x)), funcMirror))(lam, sched)
      val res = lamExps.lastOption.getOrElse(x)
      res.asInstanceOf[lms.Exp[R]]
    }
    f
  }

  /* Mirror block */
  def mirrorBlockToLms[R](m: LmsMirror)(block: ThunkDef[_], dflt: Rep[_]): () => lms.Exp[R] = { () =>
    val (_, symMirror, _) = m
    val sched = block.scheduleSingleLevel
    val (blockExps, _, _) = mirrorDefs(m)(block, sched)
    val res = blockExps.lastOption.getOrElse(symMirror(dflt))
    res.asInstanceOf[lms.Exp[R]]
  }

  def mirrorDefs(m: LmsMirror)(fromGraph: AstGraph, defs: Seq[TableEntry[_]]): LmsMirror = {
    val (_, symMirror, funcMirror) = m
    val init: LmsMirror = (List.empty[lms.Exp[_]], symMirror, funcMirror)
    val (lmsExps, finalSymMirr, finalFuncMirr) = defs.foldLeft(init)((m, t) => tableTransformer(m, fromGraph)(t))
    (lmsExps, finalSymMirr, finalFuncMirr)
  }

  // can't just return lmsFunc: lms.Exp[A] => lms.Exp[B], since mirrorDefs needs to be called in LMS context
  def apply[A, B](g: PGraph) = { x: lms.Exp[A] =>
    val emptyMirror: LmsMirror = (Seq.empty, Map.empty, Map.empty)
    val (_, _, finalFuncMirror) = mirrorDefs(emptyMirror)(g, g.schedule)
    val lmsFunc = finalFuncMirror(g.roots.last).asInstanceOf[lms.Exp[A] => lms.Exp[B]]
    lmsFunc(x)
  }

  def createManifest[T]: PartialFunction[Elem[T], Manifest[_]] = {
    case el: ArrayBufferElem[_] => Manifest.classType(classOf[scala.collection.mutable.ArrayBuilder[_]], createManifest(el.eItem))
    case PairElem(eFst, eSnd) =>
      Manifest.classType(classOf[(_, _)], createManifest(eFst), createManifest(eSnd))
    case SumElem(eLeft, eRight) =>
      Manifest.classType(classOf[Either[_, _]], createManifest(eLeft), createManifest(eRight))
    case el: FuncElem[_, _] =>
      Manifest.classType(classOf[_ => _], createManifest(el.eDom), createManifest(el.eRange))
    case el: ArrayElem[_] =>
      Manifest.arrayType(createManifest(el.eItem))
    case el: ArrayBufferElem[_] =>
      Manifest.classType(classOf[scala.collection.mutable.ArrayBuilder[_]], createManifest(el.eItem))
    case el: ListElem[_] ⇒
      Manifest.classType(classOf[List[_]], createManifest(el.eItem))
    case el: MMapElem[_,_] =>
      Manifest.classType(classOf[java.util.HashMap[_,_]], createManifest(el.eKey), createManifest(el.eValue))
    case el: Element[_] => toManifest[T](el.tag.tpe, el.tag.mirror)
    case el => ???(s"Don't know how to create manifest for $el")
  }

  def toManifest[T](t: scala.reflect.runtime.universe.Type, m: scala.reflect.runtime.universe.Mirror): Manifest[_] = {
    import scala.reflect.runtime.universe._
    import scalan.compilation.lms.scalac.LmsType

    val args = t match {
      case api: TypeRefApi => api.args
      case _ => List.empty[Type]
    }

    simpleType.applyOrElse[Type, Manifest[_]](t, {
      case _ =>
        try {
          val c = m.runtimeClass(t)
          args.length match {
            case 0 => Manifest.classType(c)
            case 1 => Manifest.classType(c, toManifest(args(0), m))
            case n => Manifest.classType(c, toManifest(args(0), m), args.drop(1).map(toManifest(_, m)): _*)
          }
        } catch {
          case _: NoClassDefFoundError => LmsType.wildCard
        }
    })
  }

  import scala.reflect.runtime.universe.typeOf
  def simpleType : PartialFunction[scala.reflect.runtime.universe.Type, Manifest[_]] = {
    // Doesn't work for some reason, produces int instead of Int
    //    implicit val typeTag = eA.tag
    //    implicit val classTag = eA.classTag
    //    manifest[T]
    case t if t =:= typeOf[Unit] => Manifest.Unit
    case t if t =:= typeOf[Boolean] => Manifest.Boolean
    case t if t =:= typeOf[Byte] => Manifest.Byte
    case t if t =:= typeOf[Short] => Manifest.Short
    case t if t =:= typeOf[Int] => Manifest.Int
    case t if t =:= typeOf[Char] => Manifest.Char
    case t if t =:= typeOf[Long] => Manifest.Long
    case t if t =:= typeOf[Float] => Manifest.Float
    case t if t =:= typeOf[Double] => Manifest.Double
    case t if t =:= typeOf[String] => manifest[String]
  }
}
