package tech.minna.playjson.macros

import play.api.libs.json.{Format, OFormat}

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonMacros {
  /**
    * Automatic [[play.api.libs.json.Format]] for case classes with a single field.
    * The serialized/deserialized JSON value will only be the class's field.
    *
    * {{{
    * case class Person(name: String)
    * val format = JsonMacros.inlineFormat[Person]
    *
    * format.writes(Person("Olle")) == JsString("Olle")
    * }}}
    */
  def inlineFormat[T]: Format[T] = macro JsonInlineFormat.jsonFormatImpl[T]
}

/**
  * Annotation for case classes to automatically create a [[play.api.libs.json.Format]] for the class.
  *
  * The formatter will be placed in the companion object, if it doesn't exist it will be created.
  *
  * {{{
  * @json case class Person(name: String, age: Int)
  *
  * Json.toJson(Person("Olle", 5)) == Json.obj("name" -> "Olle", "age" -> 5)
  * }}}
  */
class json extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonFormat.impl
}

/**
  * Annotation for case classes to automatically create a [[play.api.libs.json.Format]] for the class.
  * The formatter will use default values specified in the class fields if they are missing from the JSON value.
  *
  * The formatter will be placed in the companion object, if it doesn't exist it will be created.
  *
  * {{{
  * @jsonDefaults case class Person(name: String, age: Int = 5)
  *
  * Json.toJson(Person("Olle")) == Json.obj("name" -> "Olle", "age" -> 5)
  * }}}
  */
class jsonDefaults extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonDefaultsFormat.impl
}

/**
  * Annotation for case classes with a single field to automatically create a [[play.api.libs.json.Format]] for the class.
  * The serialized/deserialized JSON value will only be the class's field.
  *
  * The formatter will be placed in the companion object, if it doesn't exist it will be created.
  *
  * {{{
  * @jsonInline case class Person(name: String)
  *
  * Json.toJson(Person("Olle")) == JsString("Olle")
  * }}}
  */
class jsonInline extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonInlineFormat.impl
}

object JsonFormat {
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

  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fields: List[c.universe.ValDef]): c.universe.Tree = {
    import c.universe._
    q"""play.api.libs.json.Json.format[$className]"""
  }
}

object JsonDefaultsFormat {
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

  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fields: List[c.universe.ValDef]): c.universe.Tree = {
    import c.universe._
    q"""play.api.libs.json.Json.using[play.api.libs.json.Json.WithDefaultValues].format[$className]"""
  }
}

object JsonInlineFormat {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    ExtendCompanionObject.impl(c)(annottees) { (className, fields) =>
      fields match {
        case List(field) =>
          jsonFormat(c)(className, field.tpt, field.name)

        case _ =>
          c.abort(c.enclosingPosition, s"Cannot create single type JSON formatter for case class with ${fields.length} fields")
      }
    }
  }

  def jsonFormat(c: blackbox.Context)(className: c.universe.TypeName, fieldType: c.universe.Tree, fieldName: c.universe.TermName): c.universe.Tree = {
    import c.universe._
    q"""
      {
        import play.api.libs.json._
        Format(
          __.read[$fieldType].map(value => ${className.toTermName}(value)),
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
      case m: MethodSymbol if m.isPrimaryConstructor => m.paramLists.head
    }.get

    fields match {
      case List(field) =>
        val fieldName = field.name
        val fieldType = typeT.decl(fieldName).typeSignature
        c.Expr[OFormat[T]] {
          q"""
            {
              import play.api.libs.json._
              Format(
                __.read[$fieldType].map(value => ${className.toTermName}(value)),
                new Writes[$className] {
                  def writes(o: $className) = Json.toJson(o.${fieldName.toTermName})
                }
              )
            }
          """
        }
      case _ =>
        c.abort(c.enclosingPosition, s"JsonMacros.inlineFormat is only supported on case classes with a single field, found ${fields.length} fields")
    }
  }
}
