package com.cleaner.photocleaner.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleaner.photocleaner.data.DuplicateGroup
import com.cleaner.photocleaner.data.PhotoItem
import com.cleaner.photocleaner.ui.ScanStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scanStatus: ScanStatus,
    duplicateGroups: List<DuplicateGroup>,
    blurryPhotos: List<PhotoItem>,
    screenshots: List<PhotoItem>,
    selectedUris: Set<Uri>,
    onStartScan: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenBlurry: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onRequestClean: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhotoCleaner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = selectedUris.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = onRequestClean,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "В корзину (${selectedUris.size} фото)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Scan status card
            when (scanStatus) {
                is ScanStatus.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Освободите память телефона",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Найдём смазанные кадры, дубликаты и забытые скриншоты",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onStartScan,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Начать сканирование")
                            }
                        }
                    }
                }

                is ScanStatus.Scanning -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Анализ галереи…",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val progressText = if (scanStatus.total > 0) {
                                "Проверено ${scanStatus.scanned} из ${scanStatus.total} фото"
                            } else {
                                "Поиск фотографий…"
                            }
                            Text(progressText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                is ScanStatus.Completed -> {
                    // Rescan button
                    OutlinedButton(
                        onClick = onStartScan,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сканировать повторно")
                    }
                }
            }

            Text(
                "Категории очистки",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // Category 1: Duplicates & Bursts
            val duplicatesCount = duplicateGroups.sumOf { it.duplicates.size }
            CategoryItemCard(
                title = "Похожие фото и серии",
                subtitle = if (duplicateGroups.isNotEmpty()) "$duplicatesCount похожих фото в ${duplicateGroups.size} сериях" else "Дубликатов не найдено",
                icon = Icons.Default.Collections,
                count = duplicatesCount,
                badgeColor = Color(0xFF1976D2),
                onClick = onOpenDuplicates
            )

            // Category 2: Blurry
            CategoryItemCard(
                title = "Размытые и нечёткие",
                subtitle = if (blurryPhotos.isNotEmpty()) "${blurryPhotos.size} фото с низким качеством" else "Размытых фото не найдено",
                icon = Icons.Default.BrokenImage,
                count = blurryPhotos.size,
                badgeColor = Color(0xFFE65100),
                onClick = onOpenBlurry
            )

            // Category 3: Screenshots
            CategoryItemCard(
                title = "Скриншоты",
                subtitle = if (screenshots.isNotEmpty()) "${screenshots.size} снимков экрана" else "Скриншотов не найдено",
                icon = Icons.Default.Screenshot,
                count = screenshots.size,
                badgeColor = Color(0xFF7B1FA2),
                onClick = onOpenScreenshots
            )
        }
    }
}

@Composable
fun CategoryItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    count: Int,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = badgeColor)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$count",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
