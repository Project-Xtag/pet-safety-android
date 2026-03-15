package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.Order
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.data.network.model.AddressDetails as NetworkAddressDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gift Order Tests — unit tests covering gift order request encoding,
 * form validation logic, and Order model gift field deserialization.
 */
class GiftOrderTests {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

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

    // ==================== Gift Order Request Encoding ====================

    @Test
    fun `gift order - encodes isGift=true with no pet names`() {
        val request = CreateTagOrderRequest(
            petNames = null,
            ownerName = "John Doe",
            email = "john@example.com",
            shippingAddress = huAddress,
            isGift = true,
            giftRecipientName = "Alice",
            giftMessage = "Happy birthday!",
            quantity = 2
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertTrue(obj["isGift"]!!.jsonPrimitive.boolean)
        assertEquals("Alice", obj["giftRecipientName"]!!.jsonPrimitive.content)
        assertEquals("Happy birthday!", obj["giftMessage"]!!.jsonPrimitive.content)
        assertEquals(2, obj["quantity"]!!.jsonPrimitive.int)
        assertTrue(!obj.containsKey("petNames") || obj["petNames"].toString() == "null")
    }

    @Test
    fun `gift order - encodes with optional pet names`() {
        val request = CreateTagOrderRequest(
            petNames = listOf("Buddy"),
            ownerName = "Jane Smith",
            email = "jane@example.com",
            shippingAddress = deAddress,
            isGift = true,
            quantity = 1
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertTrue(obj["isGift"]!!.jsonPrimitive.boolean)
        val names = obj["petNames"]!!.jsonArray
        assertEquals(1, names.size)
        assertEquals("Buddy", names[0].jsonPrimitive.content)
        assertTrue(!obj.containsKey("giftRecipientName") || obj["giftRecipientName"].toString() == "null")
        assertTrue(!obj.containsKey("giftMessage") || obj["giftMessage"].toString() == "null")
    }

    @Test
    fun `non-gift order - omits gift fields when null`() {
        val request = CreateTagOrderRequest(
            petNames = listOf("Rex", "Luna"),
            ownerName = "Bob",
            email = "bob@example.com",
            shippingAddress = huAddress,
            paymentMethod = "card"
        )

        val jsonStr = json.encodeToString(request)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        assertTrue(!obj.containsKey("isGift") || obj["isGift"].toString() == "null")
        assertTrue(!obj.containsKey("giftRecipientName") || obj["giftRecipientName"].toString() == "null")
        assertTrue(!obj.containsKey("giftMessage") || obj["giftMessage"].toString() == "null")
        assertTrue(!obj.containsKey("quantity") || obj["quantity"].toString() == "null")

        val names = obj["petNames"]!!.jsonArray
        assertEquals(2, names.size)
    }

    // ==================== Gift Order Round-Trip ====================

    @Test
    fun `gift order - round-trip preserves all fields`() {
        val original = CreateTagOrderRequest(
            petNames = null,
            ownerName = "Carol",
            email = "carol@example.com",
            shippingAddress = huAddress,
            paymentMethod = "stripe",
            isGift = true,
            giftRecipientName = "Dave",
            giftMessage = "Enjoy your new pet tag!",
            quantity = 3
        )

        val jsonStr = json.encodeToString(original)
        val decoded = json.decodeFromString<CreateTagOrderRequest>(jsonStr)

        assertNull(decoded.petNames)
        assertEquals("Carol", decoded.ownerName)
        assertEquals("carol@example.com", decoded.email)
        assertTrue(decoded.isGift == true)
        assertEquals("Dave", decoded.giftRecipientName)
        assertEquals("Enjoy your new pet tag!", decoded.giftMessage)
        assertEquals(3, decoded.quantity)
        assertEquals("HU", decoded.shippingAddress.country)
    }

    // ==================== Order Model isGift Deserialization ====================

    @Test
    fun `order model - deserializes is_gift=true`() {
        val jsonStr = """
            {
                "id": "ord-gift-1",
                "is_gift": true,
                "pet_name": "Gift",
                "total_amount": 0.0,
                "shipping_cost": 3.90,
                "payment_method": "card",
                "payment_status": "completed",
                "order_status": "processing",
                "created_at": "2026-03-15T00:00:00Z",
                "updated_at": "2026-03-15T00:00:00Z"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(jsonStr)

        assertEquals("ord-gift-1", order.id)
        assertTrue(order.isGift)
        assertEquals("Gift", order.petName)
    }

    @Test
    fun `order model - defaults isGift to false when not present`() {
        val jsonStr = """
            {
                "id": "ord-regular-1",
                "pet_name": "Buddy",
                "total_amount": 0.0,
                "shipping_cost": 3.90,
                "payment_method": "card",
                "payment_status": "completed",
                "order_status": "shipped",
                "created_at": "2026-03-15T00:00:00Z",
                "updated_at": "2026-03-15T00:00:00Z"
            }
        """.trimIndent()

        val order = json.decodeFromString<Order>(jsonStr)
        assertFalse(order.isGift)
    }

    // ==================== Form Validation Logic ====================

    @Test
    fun `gift form - valid without pet names when quantity ge 1`() {
        val isGift = true
        val petNames = listOf("") // empty pet names
        val giftQuantity = 2

        val validPetNames = petNames.filter { it.isNotBlank() }
        val isValid = if (isGift) giftQuantity >= 1 else validPetNames.isNotEmpty()

        assertTrue(isValid)
    }

    @Test
    fun `non-gift form - invalid without pet names`() {
        val isGift = false
        val petNames = listOf("") // empty pet names
        val giftQuantity = 1

        val validPetNames = petNames.filter { it.isNotBlank() }
        val isValid = if (isGift) giftQuantity >= 1 else validPetNames.isNotEmpty()

        assertFalse(isValid)
    }

    @Test
    fun `non-gift form - valid with pet names`() {
        val isGift = false
        val petNames = listOf("Buddy", "Max")
        val giftQuantity = 1

        val validPetNames = petNames.filter { it.isNotBlank() }
        val isValid = if (isGift) giftQuantity >= 1 else validPetNames.isNotEmpty()

        assertTrue(isValid)

        // Effective quantity for checkout
        val effectiveQuantity = if (isGift) giftQuantity else validPetNames.size
        assertEquals(2, effectiveQuantity)
    }

    @Test
    fun `gift quantity - effective quantity uses giftQuantity`() {
        val isGift = true
        val giftQuantity = 5
        val petNames = listOf<String>() // no pet names

        val effectiveQuantity = if (isGift) giftQuantity else petNames.size
        assertEquals(5, effectiveQuantity)
    }

    // ==================== Quantity Bounds ====================

    @Test
    fun `gift quantity - clamped to 1-20 range`() {
        assertEquals(1, maxOf(1, minOf(20, 0)))
        assertEquals(20, maxOf(1, minOf(20, 21)))
        assertEquals(10, maxOf(1, minOf(20, 10)))
        assertEquals(1, maxOf(1, minOf(20, 1)))
        assertEquals(20, maxOf(1, minOf(20, 20)))
    }

    // ==================== Gift Submit Logic ====================

    @Test
    fun `gift submit - builds correct request with no pet names`() {
        val isGift = true
        val giftQuantity = 3
        val giftRecipientName = "Alice"
        val giftMessage = "Happy birthday!"
        val petNames = listOf("", "")

        val validPetNames = petNames.filter { it.isNotBlank() }

        val request = CreateTagOrderRequest(
            petNames = if (validPetNames.isEmpty()) null else validPetNames,
            ownerName = "John",
            email = "john@example.com",
            shippingAddress = huAddress,
            paymentMethod = "stripe",
            isGift = true,
            giftRecipientName = giftRecipientName.ifBlank { null },
            giftMessage = giftMessage.ifBlank { null },
            quantity = giftQuantity
        )

        assertNull(request.petNames)
        assertTrue(request.isGift == true)
        assertEquals("Alice", request.giftRecipientName)
        assertEquals("Happy birthday!", request.giftMessage)
        assertEquals(3, request.quantity)
    }

    @Test
    fun `non-gift submit - builds correct request with pet names`() {
        val isGift = false
        val petNames = listOf("Buddy", "Max")

        val validPetNames = petNames.filter { it.isNotBlank() }

        val request = CreateTagOrderRequest(
            petNames = validPetNames,
            ownerName = "John",
            email = "john@example.com",
            shippingAddress = huAddress,
            paymentMethod = "stripe"
        )

        assertEquals(listOf("Buddy", "Max"), request.petNames)
        assertNull(request.isGift)
        assertNull(request.giftRecipientName)
        assertNull(request.giftMessage)
        assertNull(request.quantity)
    }
}
