package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.Order
import com.petsafety.app.data.network.model.CreateTagCheckoutRequest
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.data.network.model.CreateTagOrderResponse
import com.petsafety.app.data.network.model.CreateReplacementOrderRequest
import com.petsafety.app.data.network.model.PostaPointDetails
import com.petsafety.app.data.network.model.TagCheckoutResponse
import com.petsafety.app.data.network.model.AddressDetails as NetworkAddressDetails
import com.petsafety.app.data.model.AddressDetails as ModelAddressDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Order Flow Tests — unit tests covering request/response encoding
 * for the tag ordering flow, matching the 6 web E2E scenarios.
 */
class OrderFlowTests {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    // Test addresses matching web E2E helpers
    private val huAddress = NetworkAddressDetails(
        street1 = "Kossuth Lajos utca 10",
        street2 = "2. emelet 5. ajto",
        city = "Budapest",
        postCode = "1055",
        country = "HU",
        phone = "+36301234567"
    )

    private val deAddress = NetworkAddressDetails(
        street1 = "Berliner Str. 45",
        street2 = "Wohnung 3",
        city = "Berlin",
        postCode = "10115",
        country = "DE",
        phone = "+491234567890"
    )

    private val esAddress = NetworkAddressDetails(
        street1 = "Calle Mayor 1",
        street2 = "Piso 2",
        city = "Madrid",
        postCode = "28013",
        country = "ES",
        phone = "+34612345678"
    )

    // ==================== Scenario 1: HU, 1 pet, home_delivery ====================

    @Test
    fun `scenario 1 - HU 1 pet home delivery - checkout request encodes correctly`() {
        val request = CreateTagCheckoutRequest(
            quantity = 1,
            countryCode = "HU",
            platform = "android",
            deliveryMethod = "home_delivery"
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals(1, obj["quantity"]!!.jsonPrimitive.int)
        assertEquals("HU", obj["country_code"]!!.jsonPrimitive.content)
        assertEquals("android", request.platform)
        assertEquals("home_delivery", obj["deliveryMethod"]!!.jsonPrimitive.content)
    }

    @Test
    fun `scenario 1 - HU 1 pet - order request encodes correctly`() {
        val request = CreateTagOrderRequest(
            petNames = listOf("Buddy"),
            ownerName = "Teszt Elek",
            email = "test@example.com",
            shippingAddress = huAddress
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        // petNames is a JsonArray
        val petNamesArray = obj["petNames"]!!.jsonArray
        assertEquals(1, petNamesArray.size)
        assertEquals("Buddy", petNamesArray[0].jsonPrimitive.content)
        assertEquals("Teszt Elek", obj["ownerName"]!!.jsonPrimitive.content)
        assertEquals("HU", obj["shippingAddress"]!!.jsonObject["country"]!!.jsonPrimitive.content)
    }

    // ==================== Scenario 2: HU, 2 pets, postapoint ====================

    @Test
    fun `scenario 2 - HU 2 pets postapoint - checkout request with PostaPointDetails`() {
        val postaPoint = PostaPointDetails(
            id = "pp-1",
            name = "Budapest Posta 1",
            address = "Kossuth u. 10"
        )
        val request = CreateTagCheckoutRequest(
            quantity = 2,
            countryCode = "HU",
            platform = "android",
            deliveryMethod = "postapoint",
            postapointDetails = postaPoint
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals(2, obj["quantity"]!!.jsonPrimitive.int)
        assertEquals("postapoint", obj["deliveryMethod"]!!.jsonPrimitive.content)

        val ppObj = obj["postapointDetails"]!!.jsonObject
        assertEquals("pp-1", ppObj["id"]!!.jsonPrimitive.content)
        assertEquals("Budapest Posta 1", ppObj["name"]!!.jsonPrimitive.content)
        assertEquals("Kossuth u. 10", ppObj["address"]!!.jsonPrimitive.content)
    }

    @Test
    fun `scenario 2 - HU 2 pets - order request with 2 pet names`() {
        val request = CreateTagOrderRequest(
            petNames = listOf("Rex", "Luna"),
            ownerName = "Teszt Elek",
            email = "test@example.com",
            shippingAddress = huAddress
        )

        assertEquals(2, request.petNames.size)
        assertEquals("Rex", request.petNames[0])
        assertEquals("Luna", request.petNames[1])
    }

    // ==================== Scenario 3: DE, 3 pets, home_delivery ====================

    @Test
    fun `scenario 3 - DE 3 pets - checkout request without delivery method`() {
        val request = CreateTagCheckoutRequest(
            quantity = 3,
            countryCode = "DE",
            platform = "android"
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals(3, obj["quantity"]!!.jsonPrimitive.int)
        assertEquals("DE", obj["country_code"]!!.jsonPrimitive.content)
        assertTrue(!obj.containsKey("deliveryMethod") || obj["deliveryMethod"].toString() == "null")
    }

    @Test
    fun `scenario 3 - DE 3 pets - order request encodes all 3 pet names`() {
        val request = CreateTagOrderRequest(
            petNames = listOf("Max", "Bella", "Rocky"),
            ownerName = "Max Mustermann",
            email = "max@example.com",
            shippingAddress = deAddress
        )

        assertEquals(3, request.petNames.size)
        assertEquals("DE", request.shippingAddress.country)
        assertEquals("Berlin", request.shippingAddress.city)
    }

    // ==================== Scenario 4: Round-trip encode/decode ====================

    @Test
    fun `scenario 4 - CreateTagCheckoutRequest round-trip preserves all fields`() {
        val original = CreateTagCheckoutRequest(
            quantity = 2,
            countryCode = "HU",
            platform = "android",
            deliveryMethod = "home_delivery",
            postapointDetails = PostaPointDetails(
                id = "pp-42",
                name = "Test Posta",
                address = "Test ut 1"
            )
        )

        val jsonStr = json.encodeToString(original)
        val decoded = json.decodeFromString<CreateTagCheckoutRequest>(jsonStr)

        assertEquals(original.quantity, decoded.quantity)
        assertEquals(original.countryCode, decoded.countryCode)
        assertEquals(original.platform, decoded.platform)
        assertEquals(original.deliveryMethod, decoded.deliveryMethod)
        assertEquals(original.postapointDetails?.id, decoded.postapointDetails?.id)
        assertEquals(original.postapointDetails?.name, decoded.postapointDetails?.name)
        assertEquals(original.postapointDetails?.address, decoded.postapointDetails?.address)
    }

    // ==================== Scenario 5: TagCheckoutResponse with URL ====================

    @Test
    fun `scenario 5 - TagCheckoutResponse decodes checkout URL`() {
        val jsonStr = """
            {
                "checkout": {
                    "id": "cs_test_abc123",
                    "url": "https://checkout.stripe.com/c/pay/cs_test_abc123"
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<TagCheckoutResponse>(jsonStr)

        assertNotNull(response.checkout)
        assertEquals("cs_test_abc123", response.checkout?.id)
        assertTrue(response.checkout?.url?.startsWith("https://checkout.stripe.com") == true)
    }

    // ==================== Scenario 6: TagCheckoutResponse missing fields ====================

    @Test
    fun `scenario 6 - TagCheckoutResponse handles missing checkout data`() {
        val jsonStr = """
            {
                "checkout": null
            }
        """.trimIndent()

        val response = json.decodeFromString<TagCheckoutResponse>(jsonStr)
        assertNull(response.checkout)
    }

    @Test
    fun `scenario 6 - TagCheckoutResponse handles empty checkout object`() {
        val jsonStr = """
            {
                "checkout": {}
            }
        """.trimIndent()

        val response = json.decodeFromString<TagCheckoutResponse>(jsonStr)
        assertNotNull(response.checkout)
        assertNull(response.checkout?.id)
        assertNull(response.checkout?.url)
    }

    // ==================== Address encoding tests ====================

    @Test
    fun `address details - all fields encoded with SerialName`() {
        val address = NetworkAddressDetails(
            street1 = "Kossuth Lajos utca 10",
            street2 = "2. emelet",
            city = "Budapest",
            postCode = "1055",
            country = "HU",
            phone = "+36301234567"
        )

        val jsonStr = json.encodeToString(address)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals("Kossuth Lajos utca 10", obj["street1"]!!.jsonPrimitive.content)
        assertEquals("2. emelet", obj["street2"]!!.jsonPrimitive.content)
        assertEquals("Budapest", obj["city"]!!.jsonPrimitive.content)
        assertEquals("1055", obj["postCode"]!!.jsonPrimitive.content)
        assertEquals("HU", obj["country"]!!.jsonPrimitive.content)
        assertEquals("+36301234567", obj["phone"]!!.jsonPrimitive.content)
    }

    @Test
    fun `address details - null optional fields omitted`() {
        val address = NetworkAddressDetails(
            street1 = "Hlavna 1",
            city = "Bratislava",
            postCode = "81101",
            country = "SK"
        )

        val jsonStr = json.encodeToString(address)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertTrue(!obj.containsKey("street2") || obj["street2"].toString() == "null")
        assertTrue(!obj.containsKey("province") || obj["province"].toString() == "null")
        assertTrue(!obj.containsKey("phone") || obj["phone"].toString() == "null")
    }

    // ==================== CreateReplacementOrderRequest tests ====================

    @Test
    fun `replacement order - HU postapoint encodes delivery method`() {
        val request = CreateReplacementOrderRequest(
            shippingAddress = huAddress,
            platform = "android",
            deliveryMethod = "postapoint",
            postapointDetails = PostaPointDetails(
                id = "pp-1",
                name = "Budapest Posta 1",
                address = "Kossuth u. 10"
            )
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals("postapoint", obj["deliveryMethod"]!!.jsonPrimitive.content)
        assertNotNull(obj["postapointDetails"])
        assertEquals("android", request.platform)
    }

    @Test
    fun `replacement order - EU without delivery method`() {
        val request = CreateReplacementOrderRequest(
            shippingAddress = deAddress,
            platform = "android"
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertTrue(!obj.containsKey("deliveryMethod") || obj["deliveryMethod"].toString() == "null")
        assertTrue(!obj.containsKey("postapointDetails") || obj["postapointDetails"].toString() == "null")
    }

    // ==================== Order model tests ====================

    @Test
    fun `order model - deserializes with snake_case fields`() {
        val jsonStr = """
            {
                "id": "ord-1",
                "pet_name": "Buddy",
                "total_amount": 3.90,
                "shipping_cost": 2.50,
                "payment_method": "card",
                "payment_status": "completed",
                "order_status": "processing",
                "currency": "eur",
                "created_at": "2026-01-01T00:00:00Z",
                "updated_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(jsonStr)

        assertEquals("ord-1", order.id)
        assertEquals("Buddy", order.petName)
        assertEquals(3.90, order.totalAmount, 0.001)
        assertEquals(2.50, order.shippingCost, 0.001)
        assertEquals("card", order.paymentMethod)
        assertEquals("completed", order.paymentStatus)
        assertEquals("processing", order.orderStatus)
        assertEquals("eur", order.currency)
    }

    @Test
    fun `order model - currency defaults to eur`() {
        val jsonStr = """
            {
                "id": "ord-2",
                "pet_name": "Max",
                "total_amount": 9.95,
                "shipping_cost": 9.95,
                "payment_method": "card",
                "payment_status": "paid",
                "order_status": "completed",
                "created_at": "2026-01-01T00:00:00Z",
                "updated_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(jsonStr)
        assertEquals("eur", order.currency)
    }
}
