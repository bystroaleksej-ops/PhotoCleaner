package com.cleaner.photocleaner.ui

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleaner.photocleaner.algorithms.PhotoClassifier
import com.cleaner.photocleaner.data.DuplicateGroup
import com.cleaner.photocleaner.data.GalleryRepository
import com.cleaner.photocleaner.data.PhotoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanStatus {
    object Idle : ScanStatus
    data class Scanning(val scanned: Int, val total: Int) : ScanStatus
    object Completed : ScanStatus
}

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application.applicationContext)

    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _blurryPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val blurryPhotos: StateFlow<List<PhotoItem>> = _blurryPhotos.asStateFlow()

    private val _screenshots = MutableStateFlow<List<PhotoItem>>(emptyList())
    val screenshots: StateFlow<List<PhotoItem>> = _screenshots.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _scanStatus.value = ScanStatus.Scanning(0, 0)
            _selectedUris.value = emptySet()

            val allPhotos = repository.loadAndAnalyzePhotos { scanned, total ->
                _scanStatus.value = ScanStatus.Scanning(scanned, total)
            }

            val duplicates = PhotoClassifier.findDuplicates(allPhotos)
            val blurry = PhotoClassifier.findBlurryPhotos(allPhotos)
            val screenshotsList = PhotoClassifier.findScreenshots(allPhotos)

            _duplicateGroups.value = duplicates
            _blurryPhotos.value = blurry
            _screenshots.value = screenshotsList
            _scanStatus.value = ScanStatus.Completed

            // Auto-select duplicates (excluding the best photo in each cluster)
            val autoSelected = mutableSetOf<Uri>()
            duplicates.forEach { group ->
                group.duplicates.forEach { autoSelected.add(it.uri) }
            }
            _selectedUris.value = autoSelected
        }
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    fun selectAllBlurry() {
        val current = _selectedUris.value.toMutableSet()
        _blurryPhotos.value.forEach { current.add(it.uri) }
        _selectedUris.value = current
    }

    fun selectAllScreenshots() {
        val current = _selectedUris.value.toMutableSet()
        _screenshots.value.forEach { current.add(it.uri) }
        _selectedUris.value = current
    }

    fun prepareTrashRequest(onIntentReady: (IntentSender) -> Unit) {
        val uris = _selectedUris.value.toList()
        if (uris.isEmpty()) return

        val pendingIntent = repository.createTrashOrDeleteIntent(uris)
        pendingIntent?.intentSender?.let(onIntentReady)
    }

    fun onTrashCompleted() {
        val deleted = _selectedUris.value
        _duplicateGroups.value = _duplicateGroups.value.mapNotNull { group ->
            val remainingDuplicates = group.duplicates.filter { !deleted.contains(it.uri) }
            if (remainingDuplicates.isEmpty() && deleted.contains(group.bestPhoto.uri)) {
                null
            } else if (remainingDuplicates.isEmpty()) {
                null
            } else {
                group.copy(duplicates = remainingDuplicates)
            }
        }

        _blurryPhotos.value = _blurryPhotos.value.filter { !deleted.contains(it.uri) }
        _screenshots.value = _screenshots.value.filter { !deleted.contains(it.uri) }
        _selectedUris.value = emptySet()
    }
}
