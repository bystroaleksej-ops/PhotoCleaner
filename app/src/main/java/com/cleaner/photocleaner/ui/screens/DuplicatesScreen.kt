package com.cleaner.photocleaner.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cleaner.photocleaner.data.DuplicateGroup
import com.cleaner.photocleaner.data.PhotoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    groups: List<DuplicateGroup>,
    selectedUris: Set<Uri>,
    onToggleSelect: (Uri) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Похожие и серии (${groups.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Похожих фотографий не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups, key = { it.groupId }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedUris = selectedUris,
                        onToggleSelect = onToggleSelect
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedUris: Set<Uri>,
    onToggleSelect: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Серия из ${group.allPhotos.size} снимков",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Best photo first
                item {
                    PhotoThumbItem(
                        photo = group.bestPhoto,
                        isBest = true,
                        isSelected = selectedUris.contains(group.bestPhoto.uri),
                        onToggle = { onToggleSelect(group.bestPhoto.uri) }
                    )
                }

                // Duplicates next
                items(group.duplicates, key = { it.id }) { duplicate ->
                    PhotoThumbItem(
                        photo = duplicate,
                        isBest = false,
                        isSelected = selectedUris.contains(duplicate.uri),
                        onToggle = { onToggleSelect(duplicate.uri) }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoThumbItem(
    photo: PhotoItem,
    isBest: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(130.dp, 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.error else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Best photo badge
        if (isBest) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Лучшее", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Selection checkbox indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выбрано к удалению",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                )
            }
        }

        // Size info badge
        Text(
            text = formatFileSize(photo.size),
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
