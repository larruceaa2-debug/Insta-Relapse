package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.AccountCategory
import com.example.data.local.AccountRecordEntity
import com.example.data.local.FollowLensDao
import com.example.data.local.IgnoredAccountEntity
import com.example.data.local.SnapshotEntity
import com.example.domain.parser.InstagramExportParser
import com.example.domain.parser.ParsedExportBundle
import com.example.domain.parser.SnapshotDiff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class FollowLensRepository(private val dao: FollowLensDao) {

    val snapshots: Flow<List<SnapshotEntity>> = dao.observeSnapshots()

    val ignoredUsernames: Flow<Set<String>> = dao.observeIgnoredAccounts().map { list ->
        list.mapTo(HashSet()) { it.username.lowercase() }
    }

    fun observeRecordsForSnapshot(snapshotId: Long): Flow<List<AccountRecordEntity>> =
        dao.observeRecordsForSnapshot(snapshotId)

    suspend fun importFromUris(
        context: Context,
        uris: List<Uri>,
        accountHandle: String = "",
    ): Result<SnapshotEntity> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No files selected."))
        }

        try {
            val resolver = context.contentResolver
            val singleUri = uris.singleOrNull()
            val singleName = singleUri?.let { queryDisplayName(context, it) }.orEmpty()

            val bundle: ParsedExportBundle = if (
                singleUri != null &&
                (singleName.endsWith(".zip", ignoreCase = true) ||
                    resolver.getType(singleUri)?.contains("zip", ignoreCase = true) == true)
            ) {
                resolver.openInputStream(singleUri)?.use { stream ->
                    InstagramExportParser.parseZipStream(
                        inputStream = stream,
                        sourceLabel = singleName.ifBlank { "Instagram Export.zip" },
                    )
                } ?: return@withContext Result.failure(
                    IllegalStateException("Unable to open the selected ZIP file.")
                )
            } else {
                val payloads = uris.mapNotNull { uri ->
                    val name = queryDisplayName(context, uri).ifBlank { "export.json" }
                    val bytes = resolver.openInputStream(uri)?.use { readAllBytesBounded(it) }
                    if (bytes != null) {
                        InstagramExportParser.NamedPayload(fileName = name, bytes = bytes)
                    } else {
                        null
                    }
                }
                val label = when {
                    payloads.size == 1 -> payloads.first().fileName
                    payloads.isNotEmpty() -> "${payloads.size} export files"
                    else -> "Selected files"
                }
                InstagramExportParser.parseIndividualPayloads(payloads, label)
            }

            if (bundle.followers.isEmpty() && bundle.following.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "No follower or following data found. Make sure you selected an official Instagram export ZIP or followers_1.json / following.json files."
                    )
                )
            }

            val cleanHandle = Regex("""instagram\.com/(?:_u/)?([A-Za-z0-9._]+)""", RegexOption.IGNORE_CASE)
                .find(accountHandle.trim())
                ?.groupValues?.getOrNull(1)
                ?: accountHandle.trim().removePrefix("@")
            val stats = bundle.computeStats()
            val snapshotDraft = SnapshotEntity(
                importedAt = System.currentTimeMillis(),
                label = bundle.sourceLabel,
                accountHandle = cleanHandle,
                followersCount = stats.followersCount,
                followingCount = stats.followingCount,
                notFollowingBackCount = stats.notFollowingBackCount,
                mutualCount = stats.mutualCount,
                fansCount = stats.fansCount,
                pendingCount = stats.pendingCount,
                recentlyUnfollowedCount = stats.recentlyUnfollowedCount,
            )

            val saved = dao.insertSnapshotWithRecords(snapshotDraft) { snapshotId ->
                buildList {
                    bundle.followers.forEach { acc ->
                        add(
                            AccountRecordEntity(
                                snapshotId = snapshotId,
                                username = acc.username,
                                profileUrl = acc.profileUrl,
                                timestampSec = acc.timestampSec,
                                category = AccountCategory.FOLLOWER,
                            )
                        )
                    }
                    bundle.following.forEach { acc ->
                        add(
                            AccountRecordEntity(
                                snapshotId = snapshotId,
                                username = acc.username,
                                profileUrl = acc.profileUrl,
                                timestampSec = acc.timestampSec,
                                category = AccountCategory.FOLLOWING,
                            )
                        )
                    }
                    bundle.pendingRequests.forEach { acc ->
                        add(
                            AccountRecordEntity(
                                snapshotId = snapshotId,
                                username = acc.username,
                                profileUrl = acc.profileUrl,
                                timestampSec = acc.timestampSec,
                                category = AccountCategory.PENDING,
                            )
                        )
                    }
                    bundle.recentlyUnfollowed.forEach { acc ->
                        add(
                            AccountRecordEntity(
                                snapshotId = snapshotId,
                                username = acc.username,
                                profileUrl = acc.profileUrl,
                                timestampSec = acc.timestampSec,
                                category = AccountCategory.RECENTLY_UNFOLLOWED,
                            )
                        )
                    }
                }
            }

            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSnapshotAccountHandle(snapshotId: Long, handle: String) {
        dao.updateSnapshotAccountHandle(snapshotId, handle.trim().removePrefix("@"))
    }

    suspend fun computeDiffWithPrevious(
        current: SnapshotEntity,
        previous: SnapshotEntity,
    ): SnapshotDiff = withContext(Dispatchers.IO) {
        val currentRecords = dao.getRecordsForSnapshot(current.id)
        val previousRecords = dao.getRecordsForSnapshot(previous.id)

        val currentFollowers = currentRecords
            .filter { it.category == AccountCategory.FOLLOWER }
            .associate { it.username.lowercase() to it.username }
        val previousFollowers = previousRecords
            .filter { it.category == AccountCategory.FOLLOWER }
            .associate { it.username.lowercase() to it.username }

        val currentFollowing = currentRecords
            .filter { it.category == AccountCategory.FOLLOWING }
            .associate { it.username.lowercase() to it.username }
        val previousFollowing = previousRecords
            .filter { it.category == AccountCategory.FOLLOWING }
            .associate { it.username.lowercase() to it.username }

        val gained = (currentFollowers.keys - previousFollowers.keys)
            .mapNotNull { currentFollowers[it] }
            .sorted()
        val lost = (previousFollowers.keys - currentFollowers.keys)
            .mapNotNull { previousFollowers[it] }
            .sorted()
        val newlyFollowed = (currentFollowing.keys - previousFollowing.keys)
            .mapNotNull { currentFollowing[it] }
            .sorted()

        SnapshotDiff(
            previousLabel = previous.label,
            previousImportedAt = previous.importedAt,
            gainedFollowers = gained,
            lostFollowers = lost,
            newlyFollowed = newlyFollowed,
        )
    }

    suspend fun toggleIgnored(username: String) {
        val normalized = username.lowercase()
        if (dao.isIgnored(normalized)) {
            dao.deleteIgnoredAccount(normalized)
        } else {
            dao.insertIgnoredAccount(IgnoredAccountEntity(username = normalized))
        }
    }

    suspend fun deleteSnapshot(snapshotId: Long) {
        dao.deleteSnapshotById(snapshotId)
    }

    suspend fun clearAllData() {
        dao.deleteAllSnapshots()
        dao.deleteAllIgnored()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        return cursor.getString(idx).orEmpty()
                    }
                }
            }
        return uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }

    private fun readAllBytesBounded(input: java.io.InputStream): ByteArray {
        val maxBytes = 30 * 1024 * 1024
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
