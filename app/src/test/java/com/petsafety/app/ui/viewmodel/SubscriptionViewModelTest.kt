package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.Invoice
import com.petsafety.app.data.model.PlanFeatures
import com.petsafety.app.data.model.Referral
import com.petsafety.app.data.model.ReferralCode
import com.petsafety.app.data.model.SubscriptionPlan
import com.petsafety.app.data.model.SubscriptionStatus
import com.petsafety.app.data.model.UserSubscription
import com.petsafety.app.data.network.model.SubscriptionFeaturesResponse
import com.petsafety.app.data.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SubscriptionRepository
    private lateinit var viewModel: SubscriptionViewModel

    private val testFeatures = PlanFeatures(
        maxPets = 5,
        maxPhotosPerPet = 10,
        maxEmergencyContacts = 5,
        smsNotifications = true,
        vetAlerts = true,
        communityAlerts = true,
        freeTagReplacement = false,
        prioritySupport = false
    )

    private val starterPlan = SubscriptionPlan(
        id = "plan_0", name = "starter", displayName = "Starter",
        description = "Free forever", priceMonthly = 0.0, priceYearly = 0.0,
        currency = "GBP", features = testFeatures.copy(maxPets = 1, smsNotifications = false, vetAlerts = false)
    )

    private val standardPlan = SubscriptionPlan(
        id = "plan_1", name = "standard", displayName = "Standard",
        description = "Best for pet owners", priceMonthly = 4.95, priceYearly = 49.50,
        currency = "GBP", features = testFeatures, isPopular = true
    )

    private val testSubscription = UserSubscription(
        id = "sub_123", userId = "user_456", planId = "plan_1",
        planName = "standard", status = SubscriptionStatus.ACTIVE
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = SubscriptionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPlans - success - updates plans list`() = runTest {
        val plans = listOf(starterPlan, standardPlan)
        coEvery { repository.getPlans() } returns plans

        viewModel.loadPlans()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.plans.value.size)
        assertEquals("starter", viewModel.plans.value[0].name)
        assertEquals("standard", viewModel.plans.value[1].name)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadPlans - failure - sets error`() = runTest {
        coEvery { repository.getPlans() } throws RuntimeException("Network error")

        viewModel.loadPlans()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.plans.value.isEmpty())
        assertEquals("Network error", viewModel.error.value)
    }

    @Test
    fun `loadPlans - shows loading state`() = runTest {
        coEvery { repository.getPlans() } returns listOf(starterPlan)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())
            viewModel.loadPlans()
            assertEquals(true, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `loadSubscription - success - updates subscription`() = runTest {
        coEvery { repository.getMySubscription() } returns testSubscription

        viewModel.loadSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("standard", viewModel.subscription.value?.planName)
        assertEquals(SubscriptionStatus.ACTIVE, viewModel.subscription.value?.status)
    }

    @Test
    fun `selectPlan - free plan - upgrades directly`() = runTest {
        coEvery { repository.upgradeToStarter() } returns testSubscription.copy(planName = "starter")

        viewModel.selectPlan(starterPlan)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("starter", viewModel.subscription.value?.planName)
        assertNull(viewModel.checkoutUrl.value)
        coVerify { repository.upgradeToStarter() }
    }

    @Test
    fun `selectPlan - paid plan - creates checkout URL`() = runTest {
        coEvery { repository.createCheckoutSession("standard", "monthly", null) } returns "https://checkout.stripe.com/abc"

        viewModel.selectPlan(standardPlan, "monthly")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://checkout.stripe.com/abc", viewModel.checkoutUrl.value)
        coVerify { repository.createCheckoutSession("standard", "monthly", null) }
    }

    @Test
    fun `cancelSubscription - success`() = runTest {
        val cancelled = testSubscription.copy(status = SubscriptionStatus.CANCELLED)
        coEvery { repository.cancelSubscription() } returns cancelled

        viewModel.cancelSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionStatus.CANCELLED, viewModel.subscription.value?.status)
    }

    @Test
    fun `handleCheckoutComplete - clears URL and reloads`() = runTest {
        coEvery { repository.createCheckoutSession(any(), any(), any()) } returns "https://checkout.stripe.com/abc"
        coEvery { repository.getMySubscription() } returns testSubscription
        coEvery { repository.getFeatures() } returns null

        viewModel.selectPlan(standardPlan)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://checkout.stripe.com/abc", viewModel.checkoutUrl.value)

        viewModel.handleCheckoutComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.checkoutUrl.value)
        coVerify { repository.getMySubscription() }
    }

    @Test
    fun `loadInvoices - success`() = runTest {
        val invoices = listOf(
            Invoice(id = "inv_1", number = "INV-0001", status = "paid", amount = 495, currency = "gbp", date = 1700000000)
        )
        coEvery { repository.getInvoices(any()) } returns invoices

        viewModel.loadInvoices()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.invoices.value.size)
        assertEquals("INV-0001", viewModel.invoices.value[0].number)
    }

    @Test
    fun `generateReferralCode - success`() = runTest {
        val code = ReferralCode(code = "REF-ABCD1234", expiresAt = "2026-05-01T00:00:00Z")
        coEvery { repository.generateReferralCode() } returns code

        viewModel.generateReferralCode()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("REF-ABCD1234", viewModel.referralCode.value?.code)
    }

    @Test
    fun `loadReferralStatus - success`() = runTest {
        val code = ReferralCode("REF-XYZ")
        val referrals = listOf(
            Referral(id = "ref_1", refereeEmail = "friend@test.com", status = "subscribed")
        )
        coEvery { repository.getReferralStatus() } returns Pair(code, referrals)

        viewModel.loadReferralStatus()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("REF-XYZ", viewModel.referralCode.value?.code)
        assertEquals(1, viewModel.referrals.value.size)
        assertEquals("friend@test.com", viewModel.referrals.value[0].refereeEmail)
    }

    @Test
    fun `isOnStarterPlan - computed correctly`() = runTest {
        coEvery { repository.getMySubscription() } returns testSubscription.copy(planName = "starter")

        viewModel.loadSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isOnStarterPlan)
    }

    @Test
    fun `openPortal - success - calls callback with URL`() = runTest {
        coEvery { repository.createPortalSession() } returns "https://billing.stripe.com/portal_abc"

        var receivedUrl: String? = null
        viewModel.openPortal { receivedUrl = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://billing.stripe.com/portal_abc", receivedUrl)
    }

    @Test
    fun `openPortal - failure - sets error`() = runTest {
        coEvery { repository.createPortalSession() } throws RuntimeException("No billing account")

        viewModel.openPortal { }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("No billing account", viewModel.error.value)
    }

    @Test
    fun `clearError - clears error state`() = runTest {
        coEvery { repository.getPlans() } throws RuntimeException("fail")

        viewModel.loadPlans()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("fail", viewModel.error.value)

        viewModel.clearError()
        assertNull(viewModel.error.value)
    }
}
