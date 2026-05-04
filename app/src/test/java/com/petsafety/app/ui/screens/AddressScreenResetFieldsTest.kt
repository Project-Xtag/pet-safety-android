package com.petsafety.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Regression — ProfileScreen.kt's `AddressScreen.resetFields()` used to set
 * `addressLine2 = ""` (literal empty string) instead of reading the value
 * back from `user?.addressLine2`. Symptom: enter edit mode, change any
 * other field, press Cancel — the persisted line-2 disappears from the
 * read-only display until the next user fetch.
 *
 * Three resetFields() functions live in the same file (PersonalInfo,
 * Address, Contacts). PersonalInfo and Contacts were always correct; only
 * Address had the bug. This test pins that the fix stays in.
 *
 * Pattern: mirror the FIXED reset behavior locally so we have a positive
 * spec to assert against, plus a source-regression grep that fails loud
 * if the real file regresses.
 */
class AddressScreenResetFieldsTest {

    private data class FakeUser(
        val address: String? = null,
        val addressLine2: String? = null,
        val city: String? = null,
        val postalCode: String? = null,
        val country: String? = null,
    )

    /** Mirror of the fixed AddressScreen.resetFields. */
    private fun resetFields(user: FakeUser?): Map<String, String> = mapOf(
        "address" to (user?.address ?: ""),
        "addressLine2" to (user?.addressLine2 ?: ""),
        "city" to (user?.city ?: ""),
        "postalCode" to (user?.postalCode ?: ""),
        "country" to (user?.country ?: ""),
    )

    @Test
    fun `resetFields restores all fields including addressLine2 from user data`() {
        val user = FakeUser(
            address = "1 Petofi Street",
            addressLine2 = "Apt 5",
            city = "Budapest",
            postalCode = "1011",
            country = "HU",
        )
        val result = resetFields(user)
        assertEquals("1 Petofi Street", result["address"])
        assertEquals("Apt 5", result["addressLine2"])
        assertEquals("Budapest", result["city"])
        assertEquals("1011", result["postalCode"])
        assertEquals("HU", result["country"])
    }

    @Test
    fun `resetFields uses empty string when user field is null`() {
        val user = FakeUser(address = "1 Petofi Street", addressLine2 = null)
        val result = resetFields(user)
        assertEquals("1 Petofi Street", result["address"])
        assertEquals("", result["addressLine2"])
    }

    @Test
    fun `resetFields handles null user (logged-out state)`() {
        val result = resetFields(null)
        assertEquals("", result["address"])
        assertEquals("", result["addressLine2"])
        assertEquals("", result["city"])
        assertEquals("", result["postalCode"])
        assertEquals("", result["country"])
    }

    /**
     * Source regression — fail if AddressScreen's resetFields ever drops
     * the addressLine2 read-back again. Walks up from the test working
     * directory to the module root, then opens the real source file.
     */
    @Test
    fun `source — AddressScreen resetFields reads addressLine2 from user`() {
        val source = readProfileScreenSource()

        // Find the AddressScreen function start, then check the next ~50
        // lines (where its local resetFields lives) contain the correct
        // assignment. Anchored grep avoids matching the PersonalInfo or
        // Contacts variants which are also in this file.
        val addressScreenStart = source.indexOf("private fun AddressScreen(")
        assert(addressScreenStart >= 0) { "AddressScreen function not found in ProfileScreen.kt" }

        val addressScreenSlice = source.substring(
            addressScreenStart,
            (addressScreenStart + 4000).coerceAtMost(source.length)
        )

        assert(addressScreenSlice.contains("addressLine2 = user?.addressLine2 ?: \"\"")) {
            "AddressScreen.resetFields must initialize addressLine2 from user data — " +
                "regression of the cancel-wipes-line-2 bug. Found slice:\n$addressScreenSlice"
        }

        // Belt-and-braces: ensure no addressLine2 = "" literal still sits
        // in this slice, which is what the bug looked like.
        val literalEmptyPattern = Regex("""addressLine2\s*=\s*"\s*"""")
        assert(!literalEmptyPattern.containsMatchIn(addressScreenSlice)) {
            "AddressScreen still contains a literal addressLine2=\"\" assignment — bug regressed."
        }
    }

    private fun readProfileScreenSource(): String {
        val candidates = listOf(
            "app/src/main/java/com/petsafety/app/ui/screens/ProfileScreen.kt",
            "src/main/java/com/petsafety/app/ui/screens/ProfileScreen.kt",
        )
        // Walk up from cwd up to 5 levels looking for the file.
        var dir: File? = File(".").canonicalFile
        repeat(6) {
            val current = dir ?: return@repeat
            for (rel in candidates) {
                val f = File(current, rel)
                if (f.exists()) return f.readText()
            }
            dir = current.parentFile
        }
        error("Could not locate ProfileScreen.kt from working dir ${File(".").canonicalPath}")
    }
}
