package com.autoroid.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.ui.navigation.AutoroidNavTab
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.FloatingBarGradient
import com.autoroid.app.ui.theme.GlassBorder
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.TextMuted
import com.autoroid.app.ui.theme.TextPrimary

@Composable
fun FloatingBottomBar(
    currentTab: AutoroidNavTab,
    onTabSelected: (AutoroidNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = CyberCyan.copy(alpha = 0.25f),
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    color = GlassBorder.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(26.dp)
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(FloatingBarGradient)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoroidNavTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    val interactionSource = remember { MutableInteractionSource() }

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) CyberCyan else TextMuted,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabTint"
                    )

                    val bubbleColor by animateColorAsState(
                        targetValue = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "bubbleColor"
                    )

                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 84.dp else 56.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pillWidth"
                    )

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .size(width = pillWidth, height = 44.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bubbleColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
