package tech.minna.playjson.macros

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

@json case class ProductDefaults(name: String, price: Double = 10.5)

@json(defaultValues = false) case class Product(name: String, price: Double)

// This case class with modifiers should compile
@json protected final case class ModifiersClass(name: String)

sealed trait Animal
@jsonObject protected case object Bird extends Animal
@jsonObject case object Mammal extends Animal
@json case class Bug(numberOfLegs: Int) extends Animal

class JsonMacrosSpec extends FlatSpec with Matchers {

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

  "@jsonObject" should "create a JSON formatter for a case object" in {
    implicit val sealedFamilyConfiguration = JsonConfiguration(
      discriminator = "name",
      typeNaming = JsonNaming { fullName =>
        fullName.split('.').last
      }
    )

    implicit val animalFormatter = Json.format[Animal]

    val mammalExpectedJson = Json.obj("name" -> "Mammal")
    Json.toJson(Mammal) shouldEqual mammalExpectedJson
    mammalExpectedJson.asOpt[Mammal.type] shouldEqual Some(Mammal)

    val bugExpectedJson = Json.obj("name" -> "Bug", "numberOfLegs" -> 6)
    val sixLegsBug = Bug(numberOfLegs = 6)
    Json.toJson(sixLegsBug) shouldEqual bugExpectedJson
    bugExpectedJson.asOpt[Bug] shouldEqual Some(sixLegsBug)
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

  "@jsonFlat" should "create a JSON formatter for a case class with a single field" in {
    @jsonFlat case class ProductFlat(name: String)
    val product = ProductFlat("Milk")
    val expectedJson = JsString("Milk")
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductFlat] shouldEqual Some(product)
  }

  it should "create a JSON formatter in a nested structure" in {
    @jsonFlat case class NameNested(text: String)
    @jsonFlat case class ProductFlatNested(name: NameNested)

    implicit val nameFormat = JsonMacros.flatFormat[NameNested]

    val product = ProductFlatNested(NameNested("Milk"))
    val expectedJson = JsString("Milk")
    Json.toJson(product) shouldEqual expectedJson
    expectedJson.asOpt[ProductFlatNested] shouldEqual Some(product)
  }

  "JsonMacros.flatFormat" should "create a JSON formatter for a case class with a single field" in {
    case class ProductFlat(name: String)
    val format = JsonMacros.flatFormat[ProductFlat]

    val product = ProductFlat("Milk")
    val expectedJson = JsString("Milk")
    format.writes(product) shouldEqual expectedJson
    format.reads(expectedJson) shouldEqual JsSuccess(product)
  }

  it should "create a JSON formatter in a nested structure" in {
    case class NameNested(text: String)
    case class ProductFlatNested(name: NameNested)

    implicit val nameFormat = JsonMacros.flatFormat[NameNested]
    val format = JsonMacros.flatFormat[ProductFlatNested]

    val product = ProductFlatNested(NameNested("Milk"))
    val expectedJson = JsString("Milk")
    format.writes(product) shouldEqual expectedJson
    format.reads(expectedJson) shouldEqual JsSuccess(product)
  }
}
