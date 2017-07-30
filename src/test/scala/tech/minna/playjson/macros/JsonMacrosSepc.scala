package tech.minna.playjson.macros

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsString, JsSuccess, Json}

@json case class Product(name: String, price: Double)

@jsonDefaults case class ProductDefaults(name: String, price: Double = 10.5)

@jsonInline case class ProductInline(name: String)

case class ProductInline2(name: String)

class JsonMacrosSepc extends FlatSpec with Matchers {
  "@json" should "create a JSON formatter for a case class" in {
    val product = Product("Milk", 9.9)
    val expectedJson = Json.obj(
      "name" -> "Milk",
      "price" -> 9.9
    )
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[Product] shouldEqual Some(product)
  }

  "@jsonDefaults" should "create a JSON formatter for a case class with default value" in {
    val product = ProductDefaults("Milk", 9.9)
    val expectedJson = Json.obj(
      "name" -> "Milk",
      "price" -> 9.9
    )
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductDefaults] shouldEqual Some(product)

    Json.obj("name" -> "Milk").asOpt[ProductDefaults] shouldEqual Some(ProductDefaults("Milk", price = 10.5))
  }

  "@jsonInline" should "create a JSON formatter for a case class with a single field" in {
    val product = ProductInline("Milk")
    val expectedJson = JsString("Milk")
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductInline] shouldEqual Some(product)
  }

  "JsonMacros.inlineFormat" should "create a JSON formatter for a case class with a single field" in {
    val format = JsonMacros.inlineFormat[ProductInline2]

    val product = ProductInline2("Milk")
    val expectedJson = JsString("Milk")
    format.writes(product) shouldEqual expectedJson
    format.reads(expectedJson) shouldEqual JsSuccess(product)
  }
}
