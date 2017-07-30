package tech.minna.playjson.macros

import play.api.libs.json.{Format, OFormat}

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonMacros {
  def inlineFormat[T]: Format[T] = macro JsonInlineFormat.jsonFormatImpl[T]
}

class json extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonFormat.impl
}

class jsonDefaults extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonDefaultsFormat.impl
}

class jsonInline extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonInlineFormat.impl
}

object JsonFormat {
  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fields: List[c.universe.ValDef]): c.universe.Tree = {
    import c.universe._
    q"""play.api.libs.json.Json.format[$className]"""
  }

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

    ExtendCompanionObject.impl(c)(annottees) { (className, fields) =>
      fields.length match {
        case 0 =>
          c.abort(c.enclosingPosition, s"Cannot create JSON formatter for case class with no fields")
        case _ =>
          jsonFormat(c)(className, fields)
      }
    }
  }
}

object JsonDefaultsFormat {
  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fields: List[c.universe.ValDef]): c.universe.Tree = {
    import c.universe._
    q"""play.api.libs.json.Json.using[play.api.libs.json.Json.WithDefaultValues].format[$className]"""
  }

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    ExtendCompanionObject.impl(c)(annottees) { (className, fields) =>
      fields.length match {
        case 0 =>
          c.abort(c.enclosingPosition, s"Cannot create JSON formatter with defaults for case class with no fields")
        case _ =>
          jsonFormat(c)(className, fields)
      }
    }
  }
}

object JsonInlineFormat {
  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fieldType: c.universe.Type, fieldName: c.universe.TermName): c.universe.Tree = {
    import c.universe._
    q"""
      {
        import play.api.libs.json._
        Format(
          __.read[$fieldType].map(s => ${className.toTermName}(s)),
          new Writes[$className] {
            def writes(o: $className) = Json.toJson(o.$fieldName)
          }
        )
      }
    """
  }

  def jsonFormatImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[OFormat[T]] = {
    import c.universe._

    val typeT = weakTypeOf[T]
    val className = typeT.typeSymbol.name.toTypeName
    val fields = typeT.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m.paramLists.head.map(_.asTerm)
    }.get

    fields match {
      case List(field) =>
        c.Expr[OFormat[T]] {
          jsonFormat(c)(className, field.typeSignature, field.name)
        }
      case _ =>
        c.abort(c.enclosingPosition, s"JsonMacros.inlineFormat is only supported on case classes with a single field, found ${fields.length} fields")
    }
  }

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    ExtendCompanionObject.impl(c)(annottees) { (className, fields) =>
      fields match {
        case List(field) =>
          jsonFormat(c)(className, c.typecheck(tq"${field.tpt}", c.TYPEmode).tpe, field.name)

        case _ =>
          c.abort(c.enclosingPosition, s"Cannot create single type JSON formatter for case class with ${fields.length} fields")
      }
    }
  }
}
