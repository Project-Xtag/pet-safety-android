package com.petsafety.app

import com.petsafety.app.data.network.model.ApiEnvelope
import com.petsafety.app.data.network.model.PaymentIntentResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentIntentTests {
    @Test
    fun decodesPaymentIntentCreateResponse() {
        val json = """
        {
          "success": true,
          "data": {
            "paymentIntent": {
              "id": "pi_test_123",
              "client_secret": "secret_abc",
              "amount": 3.90,
              "currency": "gbp"
            }
          }
        }
        """.trimIndent()

        val parser = Json { ignoreUnknownKeys = true }
        val response = parser.decodeFromString(ApiEnvelope.serializer(PaymentIntentResponse.serializer()), json)
        assertEquals(true, response.success)
        assertEquals("pi_test_123", response.data?.paymentIntent?.id)
        assertEquals("secret_abc", response.data?.paymentIntent?.clientSecret)
        assertEquals(3.90, response.data?.paymentIntent?.amount)
        assertEquals("gbp", response.data?.paymentIntent?.currency)
    }
}
