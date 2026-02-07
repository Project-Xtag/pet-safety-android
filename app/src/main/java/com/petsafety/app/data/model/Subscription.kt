package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    @SerialName("display_name") val displayName: String,
    val description: String? = null,
    @SerialName("price_monthly") val priceMonthly: Double,
    @SerialName("price_yearly") val priceYearly: Double,
    val currency: String,
    val features: PlanFeatures,
    @SerialName("is_popular") val isPopular: Boolean? = null
) {
    val isFree: Boolean get() = priceMonthly == 0.0

    val formattedMonthlyPrice: String
        get() = if (isFree) "Free" else "£%.2f/mo".format(priceMonthly)

    val formattedYearlyPrice: String
        get() = if (isFree) "Free" else "£%.2f/yr".format(priceYearly)
}

@Serializable
data class PlanFeatures(
    @SerialName("max_pets") val maxPets: Int? = null,
    @SerialName("max_photos_per_pet") val maxPhotosPerPet: Int,
    @SerialName("max_emergency_contacts") val maxEmergencyContacts: Int,
    @SerialName("sms_notifications") val smsNotifications: Boolean,
    @SerialName("vet_alerts") val vetAlerts: Boolean,
    @SerialName("community_alerts") val communityAlerts: Boolean,
    @SerialName("free_tag_replacement") val freeTagReplacement: Boolean,
    @SerialName("priority_support") val prioritySupport: Boolean
) {
    val maxPetsDisplay: String
        get() = maxPets?.toString() ?: "Unlimited"
}

@Serializable
data class UserSubscription(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("plan_name") val planName: String,
    val status: SubscriptionStatus,
    @SerialName("billing_period") val billingPeriod: String? = null,
    @SerialName("current_period_start") val currentPeriodStart: String? = null,
    @SerialName("current_period_end") val currentPeriodEnd: String? = null,
    @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean? = null,
    @SerialName("stripe_subscription_id") val stripeSubscriptionId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isActive: Boolean get() = status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING
    val isPaid: Boolean get() = planName.lowercase() != "starter"
    val displayStatus: String
        get() = when (status) {
            SubscriptionStatus.ACTIVE -> "Active"
            SubscriptionStatus.TRIALING -> "Trial"
            SubscriptionStatus.PAST_DUE -> "Past Due"
            SubscriptionStatus.CANCELLED -> "Cancelled"
            SubscriptionStatus.EXPIRED -> "Expired"
        }
}

@Serializable
enum class SubscriptionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("trialing") TRIALING,
    @SerialName("past_due") PAST_DUE,
    @SerialName("cancelled") CANCELLED,
    @SerialName("expired") EXPIRED
}

@Serializable
data class SubscriptionFeatures(
    @SerialName("plan_name") val planName: String,
    @SerialName("can_create_alerts") val canCreateAlerts: Boolean,
    @SerialName("can_receive_vet_alerts") val canReceiveVetAlerts: Boolean,
    @SerialName("can_receive_community_alerts") val canReceiveCommunityAlerts: Boolean,
    @SerialName("can_use_sms_notifications") val canUseSmsNotifications: Boolean,
    @SerialName("max_pets") val maxPets: Int? = null,
    @SerialName("max_photos_per_pet") val maxPhotosPerPet: Int,
    @SerialName("max_emergency_contacts") val maxEmergencyContacts: Int,
    @SerialName("free_tag_replacement") val freeTagReplacement: Boolean
) {
    val hasFullAlertFeatures: Boolean
        get() = canCreateAlerts && canReceiveVetAlerts && canReceiveCommunityAlerts
}

@Serializable
data class Invoice(
    val id: String,
    val number: String? = null,
    val status: String? = null,
    val amount: Int,
    val currency: String,
    val date: Long,
    val pdfUrl: String? = null,
    val hostedUrl: String? = null
)

@Serializable
data class ReferralCode(
    val code: String,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class Referral(
    val id: String,
    val refereeEmail: String? = null,
    val status: String,
    val redeemedAt: String? = null,
    val rewardedAt: String? = null
)
