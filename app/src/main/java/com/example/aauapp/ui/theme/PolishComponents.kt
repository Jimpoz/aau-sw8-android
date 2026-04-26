package com.example.aauapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.AndroidBackground
import com.example.aauapp.ui.theme.AndroidCard
import com.example.aauapp.ui.theme.AndroidTextSecondary
import com.example.aauapp.ui.theme.Blue50
import com.example.aauapp.ui.theme.Blue600

@Composable
fun PolishScreen(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AndroidBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -24 })
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AndroidTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        content()
    }
}

@Composable
fun PolishCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val color by animateColorAsState(
        targetValue = if (selected) Blue50 else AndroidCard,
        label = "cardColor"
    )

    val radius by animateDpAsState(
        targetValue = if (selected) 28.dp else 22.dp,
        label = "cardRadius"
    )

    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(radius),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = cardModifier
        ) {
            PolishCardContent(title, subtitle, selected)
        }
    } else {
        Card(
            shape = RoundedCornerShape(radius),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = cardModifier
        ) {
            PolishCardContent(title, subtitle, selected)
        }
    }
}

@Composable
private fun PolishCardContent(
    title: String,
    subtitle: String?,
    selected: Boolean
) {
    Column(modifier = Modifier.padding(18.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Blue600 else MaterialTheme.colorScheme.onSurface
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AndroidTextSecondary
            )
        }
    }
}