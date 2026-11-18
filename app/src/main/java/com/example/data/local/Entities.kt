package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object AccountCategory {
    const val FOLLOWER = "FOLLOWER"
    const val FOLLOWING = "FOLLOWING"
    const val PENDING = "PENDING"
    const val RECENTLY_UNFOLLOWED = "RECENTLY_UNFOLLOWED"
}

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val importedAt: Long,
    val label: String,
    val accountHandle: String = "",
    val followersCount: Int,
    val followingCount: Int,
    val notFollowingBackCount: Int,
    val mutualCount: Int,
    val fansCount: Int,
    val pendingCount: Int,
    val recentlyUnfollowedCount: Int,
)

@Entity(
    tableName = "account_records",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["snapshotId"]),
        Index(value = ["snapshotId", "category"]),
    ],
)
data class AccountRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val username: String,
    val profileUrl: String,
    val timestampSec: Long,
    val category: String,
)

@Entity(tableName = "ignored_accounts")
data class IgnoredAccountEntity(
    @PrimaryKey val username: String,
    val addedAt: Long = System.currentTimeMillis(),
)
