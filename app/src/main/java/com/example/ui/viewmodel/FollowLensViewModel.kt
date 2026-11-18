package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountCategory
import com.example.data.local.AccountRecordEntity
import com.example.data.local.FollowLensDatabase
import com.example.data.local.SnapshotEntity
import com.example.data.repository.FollowLensRepository
import com.example.domain.parser.ConnectionFilter
import com.example.domain.parser.ConnectionItem
import com.example.domain.parser.SnapshotDiff
import com.example.domain.parser.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FollowLensUiState(
    val snapshots: List<SnapshotEntity> = emptyList(),
    val activeSnapshot: SnapshotEntity? = null,
    val selectedFilter: ConnectionFilter = ConnectionFilter.NOT_FOLLOWING_BACK,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val hideIgnored: Boolean = false,
    val ignoredUsernames: Set<String> = emptySet(),
    val notFollowingBack: List<ConnectionItem> = emptyList(),
    val mutuals: List<ConnectionItem> = emptyList(),
    val fans: List<ConnectionItem> = emptyList(),
    val followers: List<ConnectionItem> = emptyList(),
    val following: List<ConnectionItem> = emptyList(),
    val pendingRequests: List<ConnectionItem> = emptyList(),
    val recentlyUnfollowed: List<ConnectionItem> = emptyList(),
    val visibleAccounts: List<ConnectionItem> = emptyList(),
    val activeIgnoredInNotFollowingCount: Int = 0,
    val snapshotDiff: SnapshotDiff? = null,
    val isImporting: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
)

private data class FilterControls(
    val selectedSnapshotId: Long? = null,
    val filter: ConnectionFilter = ConnectionFilter.NOT_FOLLOWING_BACK,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val hideIgnored: Boolean = false,
)

private data class ImportFeedback(
    val isImporting: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FollowLensViewModel(
    application: Application,
    private val repository: FollowLensRepository,
) : AndroidViewModel(application) {

    private val controls = MutableStateFlow(FilterControls())
    private val feedback = MutableStateFlow(ImportFeedback())

    private val snapshotsFlow = repository.snapshots

    private val activeSnapshotFlow: StateFlow<SnapshotEntity?> = combine(
        snapshotsFlow,
        controls.map { it.selectedSnapshotId },
    ) { snapshots, selectedId ->
        snapshots.firstOrNull { it.id == selectedId } ?: snapshots.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private val recordsFlow: StateFlow<List<AccountRecordEntity>> = activeSnapshotFlow
        .flatMapLatest { snapshot ->
            if (snapshot == null) {
                flowOf(emptyList())
            } else {
                repository.observeRecordsForSnapshot(snapshot.id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val diffFlow: StateFlow<SnapshotDiff?> = combine(
        snapshotsFlow,
        activeSnapshotFlow,
    ) { snapshots, active ->
        if (active == null) {
            null
        } else {
            val activeIdx = snapshots.indexOfFirst { it.id == active.id }
            val previous = if (activeIdx >= 0 && activeIdx + 1 < snapshots.size) {
                snapshots[activeIdx + 1]
            } else {
                null
            }
            if (previous != null) {
                repository.computeDiffWithPrevious(current = active, previous = previous)
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    val uiState: StateFlow<FollowLensUiState> = combine(
        snapshotsFlow,
        activeSnapshotFlow,
        recordsFlow,
        repository.ignoredUsernames,
        controls,
        diffFlow,
        feedback,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val snapshots = values[0] as List<SnapshotEntity>
        val activeSnapshot = values[1] as SnapshotEntity?
        @Suppress("UNCHECKED_CAST")
        val records = values[2] as List<AccountRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val ignoredSet = values[3] as Set<String>
        val ctrl = values[4] as FilterControls
        val diff = values[5] as SnapshotDiff?
        val fb = values[6] as ImportFeedback

        val followersRaw = records
            .filter { it.category == AccountCategory.FOLLOWER }
            .distinctBy { it.username.lowercase() }
        val followingRaw = records
            .filter { it.category == AccountCategory.FOLLOWING }
            .distinctBy { it.username.lowercase() }
        val pendingRaw = records
            .filter { it.category == AccountCategory.PENDING }
            .distinctBy { it.username.lowercase() }
        val unfollowedRaw = records
            .filter { it.category == AccountCategory.RECENTLY_UNFOLLOWED }
            .distinctBy { it.username.lowercase() }

        val followerKeys = followersRaw.mapTo(HashSet()) { it.username.lowercase() }
        val followingKeys = followingRaw.mapTo(HashSet()) { it.username.lowercase() }

        fun AccountRecordEntity.toItem() = ConnectionItem(
            username = username,
            profileUrl = profileUrl,
            timestampSec = timestampSec,
            isIgnored = username.lowercase() in ignoredSet,
        )

        val followers = followersRaw.map { it.toItem() }
        val following = followingRaw.map { it.toItem() }
        val notFollowingBack = followingRaw
            .filter { it.username.lowercase() !in followerKeys }
            .map { it.toItem() }
        val mutuals = followingRaw
            .filter { it.username.lowercase() in followerKeys }
            .map { it.toItem() }
        val fans = followersRaw
            .filter { it.username.lowercase() !in followingKeys }
            .map { it.toItem() }
        val pending = pendingRaw.map { it.toItem() }
        val recentlyUnfollowed = unfollowedRaw.map { it.toItem() }

        val baseList = when (ctrl.filter) {
            ConnectionFilter.NOT_FOLLOWING_BACK -> notFollowingBack
            ConnectionFilter.MUTUALS -> mutuals
            ConnectionFilter.FANS -> fans
            ConnectionFilter.FOLLOWERS -> followers
            ConnectionFilter.FOLLOWING -> following
            ConnectionFilter.PENDING -> pending
            ConnectionFilter.RECENTLY_UNFOLLOWED -> recentlyUnfollowed
        }

        val query = ctrl.searchQuery.trim().removePrefix("@").lowercase()
        val filtered = baseList.asSequence()
            .filter { item ->
                if (ctrl.hideIgnored && ctrl.filter == ConnectionFilter.NOT_FOLLOWING_BACK) {
                    !item.isIgnored
                } else {
                    true
                }
            }
            .filter { item ->
                query.isEmpty() || item.username.lowercase().contains(query)
            }
            .let { seq ->
                when (ctrl.sortOrder) {
                    SortOrder.NEWEST -> seq.sortedWith(
                        compareByDescending<ConnectionItem> { it.timestampSec }
                            .thenBy { it.username.lowercase() }
                    )
                    SortOrder.OLDEST -> seq.sortedWith(
                        compareBy<ConnectionItem> { if (it.timestampSec == 0L) Long.MAX_VALUE else it.timestampSec }
                            .thenBy { it.username.lowercase() }
                    )
                    SortOrder.ALPHABETICAL -> seq.sortedBy { it.username.lowercase() }
                }
            }
            .toList()

        FollowLensUiState(
            snapshots = snapshots,
            activeSnapshot = activeSnapshot,
            selectedFilter = ctrl.filter,
            searchQuery = ctrl.searchQuery,
            sortOrder = ctrl.sortOrder,
            hideIgnored = ctrl.hideIgnored,
            ignoredUsernames = ignoredSet,
            notFollowingBack = notFollowingBack,
            mutuals = mutuals,
            fans = fans,
            followers = followers,
            following = following,
            pendingRequests = pending,
            recentlyUnfollowed = recentlyUnfollowed,
            visibleAccounts = filtered,
            activeIgnoredInNotFollowingCount = notFollowingBack.count { it.isIgnored },
            snapshotDiff = diff,
            isImporting = fb.isImporting,
            bannerMessage = fb.bannerMessage,
            errorMessage = fb.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FollowLensUiState(),
    )

    fun importFiles(uris: List<Uri>, accountHandle: String = "") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            feedback.value = ImportFeedback(isImporting = true)
            val result = repository.importFromUris(getApplication(), uris, accountHandle)
            result.fold(
                onSuccess = { snapshot ->
                    controls.update { it.copy(selectedSnapshotId = snapshot.id) }
                    feedback.value = ImportFeedback(
                        isImporting = false,
                        bannerMessage = "Imported ${snapshot.followersCount} followers & ${snapshot.followingCount} following",
                    )
                },
                onFailure = { error ->
                    feedback.value = ImportFeedback(
                        isImporting = false,
                        errorMessage = error.message ?: "Could not read selected export file.",
                    )
                },
            )
        }
    }

    fun updateAccountHandle(snapshotId: Long, handle: String) {
        viewModelScope.launch {
            repository.updateSnapshotAccountHandle(snapshotId, handle)
            val clean = handle.trim().removePrefix("@")
            feedback.update {
                it.copy(
                    bannerMessage = if (clean.isNotEmpty()) "Tagged as @$clean" else "Account tag cleared"
                )
            }
        }
    }

    fun selectSnapshot(snapshotId: Long) {
        controls.update { it.copy(selectedSnapshotId = snapshotId) }
    }

    fun setFilter(filter: ConnectionFilter) {
        controls.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        controls.update { it.copy(sortOrder = sortOrder) }
    }

    fun toggleHideIgnored() {
        controls.update { it.copy(hideIgnored = !it.hideIgnored) }
    }

    fun toggleIgnoreAccount(username: String) {
        viewModelScope.launch {
            repository.toggleIgnored(username)
        }
    }

    fun deleteSnapshot(snapshotId: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshotId)
            controls.update { current ->
                if (current.selectedSnapshotId == snapshotId) {
                    current.copy(selectedSnapshotId = null)
                } else {
                    current
                }
            }
            feedback.update { it.copy(bannerMessage = "Snapshot deleted") }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            controls.update { FilterControls() }
            feedback.update { it.copy(bannerMessage = "All local data cleared") }
        }
    }

    fun dismissMessages() {
        feedback.update { it.copy(bannerMessage = null, errorMessage = null) }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = FollowLensDatabase.getInstance(application).followLensDao()
                    val repo = FollowLensRepository(dao)
                    return FollowLensViewModel(application, repo) as T
                }
            }
        }
    }
}
