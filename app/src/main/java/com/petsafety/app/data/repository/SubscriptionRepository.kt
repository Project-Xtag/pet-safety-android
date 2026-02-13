package com.petsafety.app.data.repository

import com.petsafety.app.data.model.Invoice
import com.petsafety.app.data.model.Referral
import com.petsafety.app.data.model.ReferralCode
import com.petsafety.app.data.model.SubscriptionPlan
import com.petsafety.app.data.model.UserSubscription
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ApplyReferralRequest
import com.petsafety.app.data.network.model.CreateCheckoutRequest
import com.petsafety.app.data.network.model.SubscriptionFeaturesResponse

class SubscriptionRepository(private val apiService: ApiService) {

    suspend fun getPlans(): List<SubscriptionPlan> =
        apiService.getSubscriptionPlans().data?.plans ?: emptyList()

    suspend fun getMySubscription(): UserSubscription? =
        apiService.getMySubscription().data?.subscription

    suspend fun getFeatures(): SubscriptionFeaturesResponse? =
        apiService.getSubscriptionFeatures().data

    suspend fun createCheckoutSession(
        planName: String,
        billingPeriod: String,
        promoCode: String? = null,
        countryCode: String? = null
    ): String {
        val response = apiService.createSubscriptionCheckout(
            CreateCheckoutRequest(
                planName = planName,
                billingPeriod = billingPeriod,
                platform = "android",
                promoCode = promoCode,
                countryCode = countryCode
            )
        )
        return response.data?.resolvedUrl ?: error("Missing checkout URL")
    }

    suspend fun upgradeToStarter(): UserSubscription {
        val response = apiService.upgradeToStarter()
        return response.data?.subscription ?: error("Missing subscription")
    }

    suspend fun cancelSubscription(): UserSubscription {
        val response = apiService.cancelSubscription()
        return response.data?.subscription ?: error("Missing subscription")
    }

    suspend fun createPortalSession(): String {
        val response = apiService.createPortalSession()
        return response.data?.url ?: error("Missing portal URL")
    }

    suspend fun getInvoices(limit: Int = 24): List<Invoice> =
        apiService.getInvoices(limit).data?.invoices ?: emptyList()

    suspend fun generateReferralCode(): ReferralCode {
        val response = apiService.generateReferralCode()
        val data = response.data ?: error("Missing referral code")
        return ReferralCode(code = data.code, expiresAt = data.expiresAt)
    }

    suspend fun applyReferralCode(code: String): Boolean {
        val response = apiService.applyReferralCode(ApplyReferralRequest(code))
        return response.success
    }

    suspend fun getReferralStatus(): Pair<ReferralCode?, List<Referral>> {
        val response = apiService.getReferralStatus()
        val data = response.data
        val code = data?.code?.let { ReferralCode(it, data.expiresAt) }
        return Pair(code, data?.referrals ?: emptyList())
    }
}
