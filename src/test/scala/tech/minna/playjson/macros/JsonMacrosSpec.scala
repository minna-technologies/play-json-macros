package tech.minna.playjson.macros

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsString, JsSuccess, Json}

@json case class ProductDefaults(name: String, price: Double = 10.5)

@json(defaultValues = false) case class Product(name: String, price: Double)

class JsonMacrosSepc extends FlatSpec with Matchers {
  "@json" should "create a JSON formatter for a case class that have default values" in {
    val product = ProductDefaults("Milk", 9.9)
    val expectedJson = Json.obj(
      "name" -> "Milk",
      "price" -> 9.9
    )
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductDefaults] shouldEqual Some(product)

    Json.obj("name" -> "Milk").asOpt[ProductDefaults] shouldEqual Some(ProductDefaults("Milk", price = 10.5))
  }

  "@json(defaultValues = false)" should "create a JSON formatter for a case class without default values" in {
    val product = Product("Milk", 9.9)
    val expectedJson = Json.obj(
      "name" -> "Milk",
      "price" -> 9.9
    )
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[Product] shouldEqual Some(product)

    Json.obj("name" -> "Milk").asOpt[Product] shouldEqual None
  }

  "@jsonUnwrap" should "create a JSON formatter for a case class with a single field" in {
    @jsonUnwrap case class ProductUnwrap(name: String)
    val product = ProductUnwrap("Milk")
    val expectedJson = JsString("Milk")
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductUnwrap] shouldEqual Some(product)
  }

  it should "create a JSON formatter in a nested structure" in {
    @jsonUnwrap case class NameNested(text: String)
    @jsonUnwrap case class ProductUnwrapNested(name: NameNested)

    implicit val nameFormat = JsonMacros.unwrapFormat[NameNested]

    val product = ProductUnwrapNested(NameNested("Milk"))
    val expectedJson = JsString("Milk")
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductUnwrapNested] shouldEqual Some(product)
  }

  "JsonMacros.unwrapFormat" should "create a JSON formatter for a case class with a single field" in {
    case class ProductUnwrap(name: String)
    val format = JsonMacros.unwrapFormat[ProductUnwrap]

    val product = ProductUnwrap("Milk")
    val expectedJson = JsString("Milk")
    format.writes(product) shouldEqual expectedJson
    format.reads(expectedJson) shouldEqual JsSuccess(product)
  }

  it should "create a JSON formatter in a nested structure" in {
    case class NameNested(text: String)
    case class ProductUnwrapNested(name: NameNested)

    implicit val nameFormat = JsonMacros.unwrapFormat[NameNested]
    val format = JsonMacros.unwrapFormat[ProductUnwrapNested]

    val product = ProductUnwrapNested(NameNested("Milk"))
    val expectedJson = JsString("Milk")
    format.writes(product) shouldEqual expectedJson
    format.reads(expectedJson) shouldEqual JsSuccess(product)
  }
}
