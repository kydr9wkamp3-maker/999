package com.chromecore.camera.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "CHROMECORE X",
        description = "Профессиональная камера с хромовым дизайном.\nObsidian Chrome UI.",
        icon = "📷"
    ),
    OnboardingPage(
        title = "Умная камера",
        description = "Тап-фокус, зум щипком,\nэкспозиция, вспышка, таймер.",
        icon = "🎯"
    ),
    OnboardingPage(
        title = "Галерея",
        description = "Все фото в папке ChromecoreX.\nПросмотр, шаринг, удаление.",
        icon = "🖼"
    ),
    OnboardingPage(
        title = "Device Lab",
        description = "Диагностика камер вашего устройства.\nЭкспорт характеристик в JSON.",
        icon = "🔬"
    ),
    OnboardingPage(
        title = "Готово!",
        description = "Нажмите кнопку ниже чтобы начать.\nУдачных снимков!",
        icon = "✨"
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChromecoreColors.ObsidianBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            // Indicators + Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    onboardingPages.forEachIndexed { index, _ ->
                        val isSelected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .background(
                                    if (isSelected) ChromecoreColors.CyanAccent
                                    else ChromecoreColors.GraphiteMid,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Text("Назад", color = ChromecoreColors.ChromeSilver)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    if (pagerState.currentPage < onboardingPages.size - 1) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChromecoreColors.CyanAccent
                            )
                        ) {
                            Text("Далее", color = Color.Black)
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChromecoreColors.CyanAccent
                            )
                        ) {
                            Text("Начать", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            page.icon,
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Text(
            page.title,
            color = ChromecoreColors.CyanAccent,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            page.description,
            color = ChromecoreColors.ChromeSilver,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}
