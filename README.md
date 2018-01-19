# Play JSON macros

This library provides helpful macros for [Play JSON](https://github.com/playframework/play-json).

- `@json` adds a JSON formatter for a case class.
- `@jsonFlat` and `JsonMacros.flatFormat` creates a JSON formatter which flattens the case class with a single field to only the field value.

Inspired by [json-annotation](https://github.com/vital-software/json-annotation).

## Setup

Add the following to your `build.sbt` file:
```scala
libraryDependencies += "tech.minna" %% "play-json-macros" % "1.0.0"
resolvers += Resolver.bintrayRepo("minna-technologies", "maven")

// Needed for macro annotations
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

This library is compiled for both Scala 2.11 and 2.12.

## Documentation

This library adds a number of annotations for automatically creating JSON formatters. This is how they work:

By using `@json` or `@jsonFlat` annotations on a case class a JSON formatter will automatically be created in the companion object.

This code:
```scala
@json case class Person(name: String, age: Int)
```
Will be transformed into this:
```scala
case class Person(name: String, age: Int)

object Person {
  implicit val format = Json.format[Person]
}
```

### `@json`

```scala
@json case class Person(name: String, age: Int = 5)

Json.toJson(Person("Olle", 7)) == Json.obj("name" -> "Olle", "age" -> 7)

// Property `age` will be set to the default value
Json.obj("name" -> "Olle").as[Person] == Person("Olle", 5)
```

```scala
@json(defaultValues = false) case class Person(name: String, age: Int)

Json.toJson(Person("Olle", 7)) == Json.obj("name" -> "Olle", "age" -> 7)

// Property `age` is missing
Json.obj("name" -> "Olle").asOpt[Person] == None
```

### `@jsonFlat`

```scala
@jsonFlat case class Person(name: String)

Json.toJson(Person("Olle")) == JsString("Olle")
JsString("Olle").as[Person] == Person("Olle")
```

### `JsonMacros.flatFormat`

```scala
case class Person(name: String)
val format = JsonMacros.flatFormat[Person]

format.writes(Person("Olle")) == JsString("Olle")
format.reads(JsString("Olle")).get == Person("Olle")
```
