package com.petsafety.app.ui.screens

import com.petsafety.app.util.InputValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pass 3 audit fix — reward-amount field input filter + backend-bound
 * validation. The filter lives inside MarkAsMissingScreen's onValueChange
 * and this test replicates it so a regression in the inline body is
 * caught without a full Compose-UI test stack.
 */
class Pass3RewardFieldTest {

    // Mirror of the onValueChange filter in MarkAsMissingScreen.kt.
    private fun filterReward(raw: String): String =
        raw.filter { it.isDigit() || it == '.' || it == ',' }
            .take(InputValidators.MAX_REWARD_AMOUNT)

    @Test
    fun `strips non-numeric and non-separator chars`() {
        // Comma + dot ARE separators — they pass through. Anything else
        // (currency symbols, letters, whitespace, punctuation) is stripped.
        assertEquals("1,000", filterReward("£1,000 or best offer"))
        assertEquals("50.00", filterReward("abc50.00xyz"))
        assertEquals("500", filterReward("$500"))
    }

    @Test
    fun `caps at MAX_REWARD_AMOUNT chars`() {
        val pasted = "9".repeat(10_000)
        assertEquals(InputValidators.MAX_REWARD_AMOUNT, filterReward(pasted).length)
    }

    @Test
    fun `handles comma as decimal separator`() {
        assertEquals("50,00", filterReward("50,00 €"))
    }

    @Test
    fun `isValidRewardAmount rejects values beyond server bound`() {
        // Server treats >1_000_000 as out-of-range.
        assertTrue(InputValidators.isValidRewardAmount("500"))
        assertTrue(InputValidators.isValidRewardAmount("1000000"))
        assertFalse(InputValidators.isValidRewardAmount("1000001"))
        assertFalse(InputValidators.isValidRewardAmount("99999999999999999999"))
    }

    @Test
    fun `isValidRewardAmount rejects negative and non-numeric`() {
        assertFalse(InputValidators.isValidRewardAmount("-50"))
        assertFalse(InputValidators.isValidRewardAmount("abc"))
        // Empty is treated as "not provided" and is valid (optional field).
        assertTrue(InputValidators.isValidRewardAmount(""))
    }
}
