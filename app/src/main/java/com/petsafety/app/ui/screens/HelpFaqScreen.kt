package com.petsafety.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsafety.app.R
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.theme.TealAccent

/**
 * FAQ screen displaying frequently asked questions in an expandable accordion layout.
 * Uses existing string resources (help_faq_q1..q5, help_faq_a1..a5).
 */
@Composable
fun FaqScreen(onBack: () -> Unit) {
    val faqGroups = listOf(
        Pair(
            R.string.help_faq_group_getting_started,
            listOf(
                Pair(R.string.help_faq_q1, R.string.help_faq_a1),
                Pair(R.string.help_faq_q2, R.string.help_faq_a2),
                Pair(R.string.help_faq_q3, R.string.help_faq_a3),
                Pair(R.string.help_faq_q13, R.string.help_faq_a13)
            )
        ),
        Pair(
            R.string.help_faq_group_tags_scanning,
            listOf(
                Pair(R.string.help_faq_q4, R.string.help_faq_a4),
                Pair(R.string.help_faq_q5, R.string.help_faq_a5),
                Pair(R.string.help_faq_q6, R.string.help_faq_a6),
                Pair(R.string.help_faq_q7, R.string.help_faq_a7),
                Pair(R.string.help_faq_q25, R.string.help_faq_a25)
            )
        ),
        Pair(
            R.string.help_faq_group_missing_pets,
            listOf(
                Pair(R.string.help_faq_q8, R.string.help_faq_a8),
                Pair(R.string.help_faq_q9, R.string.help_faq_a9),
                Pair(R.string.help_faq_q14, R.string.help_faq_a14),
                Pair(R.string.help_faq_q18, R.string.help_faq_a18),
                Pair(R.string.help_faq_q23, R.string.help_faq_a23),
                Pair(R.string.help_faq_q26, R.string.help_faq_a26)
            )
        ),
        Pair(
            R.string.help_faq_group_billing_plans,
            listOf(
                Pair(R.string.help_faq_q10, R.string.help_faq_a10),
                Pair(R.string.help_faq_q11, R.string.help_faq_a11),
                Pair(R.string.help_faq_q12, R.string.help_faq_a12),
                Pair(R.string.help_faq_q21, R.string.help_faq_a21)
            )
        ),
        Pair(
            R.string.help_faq_group_privacy_account,
            listOf(
                Pair(R.string.help_faq_q15, R.string.help_faq_a15),
                Pair(R.string.help_faq_q19, R.string.help_faq_a19),
                Pair(R.string.help_faq_q22, R.string.help_faq_a22),
                Pair(R.string.help_faq_q24, R.string.help_faq_a24)
            )
        ),
        Pair(
            R.string.help_faq_group_troubleshooting,
            listOf(
                Pair(R.string.help_faq_q16, R.string.help_faq_a16),
                Pair(R.string.help_faq_q17, R.string.help_faq_a17),
                Pair(R.string.help_faq_q20, R.string.help_faq_a20),
                Pair(R.string.help_faq_q27, R.string.help_faq_a27)
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PeachBackground)
                .padding(vertical = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.faq_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // FAQ Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TealAccent
                )
                Text(
                    text = stringResource(R.string.faq),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            faqGroups.forEach { (groupTitleRes, groupItems) ->
                Text(
                    text = stringResource(groupTitleRes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        groupItems.forEachIndexed { index, (questionRes, answerRes) ->
                            FaqAccordionItem(
                                question = stringResource(questionRes),
                                answer = stringResource(answerRes)
                            )
                            if (index < groupItems.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * User Guides screen displaying guide topics.
 * Uses existing string resources (help_guide_*).
 */
@Composable
fun GuidesScreen(onBack: () -> Unit) {
    val guideItems = listOf(
        Pair(R.string.help_guide_qr_tags_title, R.string.help_guide_qr_tags_desc),
        Pair(R.string.help_guide_materials_title, R.string.help_guide_materials_desc),
        Pair(R.string.help_guide_emergency_title, R.string.help_guide_emergency_desc),
        Pair(R.string.help_guide_profile_title, R.string.help_guide_profile_desc),
        Pair(R.string.help_guide_missing_alerts_title, R.string.help_guide_missing_alerts_desc),
        Pair(R.string.help_guide_community_title, R.string.help_guide_community_desc)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PeachBackground)
                .padding(vertical = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.guides_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TealAccent
                )
                Text(
                    text = stringResource(R.string.user_guides),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    guideItems.forEachIndexed { index, (titleRes, descRes) ->
                        GuideAccordionItem(
                            title = stringResource(titleRes),
                            description = stringResource(descRes)
                        )
                        if (index < guideItems.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionItem(
    question: String,
    answer: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TealAccent
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun GuideAccordionItem(
    title: String,
    description: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TealAccent
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
