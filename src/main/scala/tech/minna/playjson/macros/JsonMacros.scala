package tech.minna.playjson.macros

import play.api.libs.json.{Format, OFormat}

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonMacros {
  /**
    * Automatic `play.api.libs.json.Format` for case classes with a single field.
    * The serialized/deserialized JSON value will only be the class's field.
    *
    * {{{
    * case class Person(name: String)
    * val format = JsonMacros.flatFormat[Person]
    *
    * format.writes(Person("Olle")) == JsString("Olle")
    * }}}
    */
  def flatFormat[T]: Format[T] = macro JsonFlatFormatMacro.jsonFormatImpl[T]
}

/**
  * Annotation for case classes to automatically create a `play.api.libs.json.Format` for the class.
  *
  * The formatter will be placed in the companion object, if it doesn't exist it will be created.
  *
  * {{{
  * @json case class Person(name: String, age: Int = 5)
  *
  * Json.toJson(Person("Olle")) == Json.obj("name" -> "Olle", "age" -> 5)
  * }}}
  *
  * When `defaultValues` is set to false then default values of class fields will not be used by the deserializer.
  */
class json(defaultValues: Boolean = true, snakeCase: Boolean = false) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonFormatMacro.impl
}

/**
  * Annotation for case classes with a single field to automatically create a `play.api.libs.json.Format` for the class.
  * The serializer will flatten the class and only output the field value as JSON.
  * The deserializer will wrap the value in the class.
  *
  * The formatter will be placed in the companion object, if it doesn't exist it will be created.
  *
  * {{{
  * @jsonFlat case class Person(name: String)
  *
  * Json.toJson(Person("Olle")) == JsString("Olle")
  * JsString("Olle").as[Person] == Person("Olle")
  * }}}
  */
class jsonFlat extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonFlatFormatMacro.impl
}

object JsonFormatMacro {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val (defaultValues: Boolean, snakeCase: Boolean) = c.prefix.tree match {
      case q"new json(defaultValues = $defaultValues, snakeCase = $snakeCase)" => (c.eval[Boolean](c.Expr(defaultValues)), c.eval[Boolean](c.Expr(snakeCase)))
      case q"new json(defaultValues = $defaultValues)" => (c.eval[Boolean](c.Expr(defaultValues)), false)
      case q"new json(snakeCase = $snakeCase)" => (true, c.eval[Boolean](c.Expr(snakeCase)))
      case q"new json($defaultValues)" => (c.eval[Boolean](c.Expr(defaultValues)), false)
      case q"new json()" => (true, false)
    }

    ExtendCompanionObject.impl(c)(annottees) { (className, fields) =>
      fields.length match {
        case 0 =>
          c.abort(c.enclosingPosition, s"Cannot create JSON formatter for case class with no fields")
        case _ =>
          jsonFormat(c)(defaultValues, snakeCase, className, fields)
      }
    }
  }

  def jsonFormat(c: blackbox.Context)(defaultValues: Boolean, snakeCase: Boolean, className: c.universe.TypeName, fields: List[c.universe.ValDef]): c.universe.Tree = {
    import c.universe._
    if (defaultValues && snakeCase) {
      q"""play.api.libs.json.Json.configured[play.api.libs.json.Json.WithDefaultValues](play.api.libs.json.JsonConfiguration(play.api.libs.json.JsonNaming.SnakeCase)).format[$className]"""
    } else if (defaultValues && !snakeCase) {
      q"""play.api.libs.json.Json.using[play.api.libs.json.Json.WithDefaultValues].format[$className]"""
    } else if (!defaultValues && snakeCase) {
      q"""play.api.libs.json.Json.configured[play.api.libs.json.Json.MacroOptions](play.api.libs.json.JsonConfiguration(play.api.libs.json.JsonNaming.SnakeCase)).format[$className]"""
    } else {
      q"""play.api.libs.json.Json.format[$className]"""
    }
  }
}

object JsonFlatFormatMacro {
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
        c.abort(c.enclosingPosition, s"JsonMacros.flatFormat is only supported on case classes with a single field, found ${fields.length} fields")
    }
  }
}
