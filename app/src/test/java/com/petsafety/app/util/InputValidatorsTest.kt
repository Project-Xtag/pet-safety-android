package com.petsafety.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorsTest {

    // MARK: - Phone Validation

    @Test
    fun `valid phone numbers pass`() {
        assertTrue(InputValidators.isValidPhone("+36301234567"))
        assertTrue(InputValidators.isValidPhone("+442071234567"))
        assertTrue(InputValidators.isValidPhone("06301234567"))
    }

    @Test
    fun `invalid phone numbers fail`() {
        assertFalse(InputValidators.isValidPhone(""))
        assertFalse(InputValidators.isValidPhone("123"))
        assertFalse(InputValidators.isValidPhone("abcdefg"))
        assertFalse(InputValidators.isValidPhone("   "))
    }

    // MARK: - Microchip Validation

    @Test
    fun `valid microchip numbers pass`() {
        assertTrue(InputValidators.isValidMicrochip("123456789012345")) // 15 digits
        assertTrue(InputValidators.isValidMicrochip("123456789")) // 9 digits
        assertTrue(InputValidators.isValidMicrochip("")) // optional
    }

    @Test
    fun `invalid microchip numbers fail`() {
        assertFalse(InputValidators.isValidMicrochip("12345")) // too short
        assertFalse(InputValidators.isValidMicrochip("123456789012345678")) // 18 digits
        assertFalse(InputValidators.isValidMicrochip("12345678901234A")) // letter
    }

    // MARK: - OTP Validation

    @Test
    fun `valid OTP codes pass`() {
        assertTrue(InputValidators.isValidOTP("123456"))
        assertTrue(InputValidators.isValidOTP("000000"))
    }

    @Test
    fun `invalid OTP codes fail`() {
        assertFalse(InputValidators.isValidOTP(""))
        assertFalse(InputValidators.isValidOTP("12345"))
        assertFalse(InputValidators.isValidOTP("1234567"))
        assertFalse(InputValidators.isValidOTP("12345a"))
        assertFalse(InputValidators.isValidOTP("      "))
    }

    // MARK: - Weight Validation

    @Test
    fun `valid weights pass`() {
        assertTrue(InputValidators.isValidWeight("5.5"))
        assertTrue(InputValidators.isValidWeight("0.1"))
        assertTrue(InputValidators.isValidWeight("500"))
        assertTrue(InputValidators.isValidWeight("")) // optional
    }

    @Test
    fun `invalid weights fail`() {
        assertFalse(InputValidators.isValidWeight("0"))
        assertFalse(InputValidators.isValidWeight("-5"))
        assertFalse(InputValidators.isValidWeight("501"))
        assertFalse(InputValidators.isValidWeight("abc"))
    }

    // MARK: - Reward Amount

    @Test
    fun `valid reward amounts pass`() {
        assertTrue(InputValidators.isValidRewardAmount("100"))
        assertTrue(InputValidators.isValidRewardAmount("50.50"))
        assertTrue(InputValidators.isValidRewardAmount("")) // optional
    }

    @Test
    fun `invalid reward amounts fail`() {
        assertFalse(InputValidators.isValidRewardAmount("0"))
        assertFalse(InputValidators.isValidRewardAmount("-100"))
        assertFalse(InputValidators.isValidRewardAmount("abc"))
        assertFalse(InputValidators.isValidRewardAmount("1000001"))
    }

    // MARK: - Coordinate Validation

    @Test
    fun `valid coordinates pass`() {
        assertTrue(InputValidators.isValidCoordinate(47.4979, 19.0402))
        assertTrue(InputValidators.isValidCoordinate(-33.8688, 151.2093))
        assertTrue(InputValidators.isValidCoordinate(90.0, 180.0))
        assertTrue(InputValidators.isValidCoordinate(-90.0, -180.0))
    }

    @Test
    fun `invalid coordinates fail`() {
        assertFalse(InputValidators.isValidCoordinate(0.0, 0.0)) // null island
        assertFalse(InputValidators.isValidCoordinate(91.0, 19.0))
        assertFalse(InputValidators.isValidCoordinate(47.0, 181.0))
    }

    // MARK: - Name Validation

    @Test
    fun `valid names pass`() {
        assertTrue(InputValidators.isValidName("John"))
        assertTrue(InputValidators.isValidName("  John Doe  "))
    }

    @Test
    fun `invalid names fail`() {
        assertFalse(InputValidators.isValidName(""))
        assertFalse(InputValidators.isValidName("   "))
        assertFalse(InputValidators.isValidName("a".repeat(101)))
    }

    // MARK: - Text Length

    @Test
    fun `isWithinLimit works correctly`() {
        assertTrue(InputValidators.isWithinLimit("hello", 10))
        assertTrue(InputValidators.isWithinLimit("", 10))
        assertFalse(InputValidators.isWithinLimit("a".repeat(101), 100))
        assertTrue(InputValidators.isWithinLimit("a".repeat(100), 100))
    }

    // MARK: - Constants

    @Test
    fun `max length constants are reasonable`() {
        assertTrue(InputValidators.MAX_PET_NAME == 100)
        assertTrue(InputValidators.MAX_MICROCHIP == 17)
        assertTrue(InputValidators.MAX_MEDICAL_NOTES == 2000)
        assertTrue(InputValidators.MAX_EMAIL == 254)
        assertTrue(InputValidators.MAX_PHONE == 20)
    }
}
