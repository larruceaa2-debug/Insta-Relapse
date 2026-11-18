package com.example.domain.parser

data class ParsedAccount(
    val username: String,
    val profileUrl: String,
    val timestampSec: Long,
)

data class ParsedExportBundle(
    val sourceLabel: String,
    val followers: List<ParsedAccount>,
    val following: List<ParsedAccount>,
    val pendingRequests: List<ParsedAccount>,
    val recentlyUnfollowed: List<ParsedAccount>,
    val parsedFileCount: Int,
) {
    fun computeStats(): ExportAnalyticsSummary {
        val followerSet = followers.mapTo(HashSet()) { it.username.lowercase() }
        val followingSet = following.mapTo(HashSet()) { it.username.lowercase() }

        val notFollowingBack = following.filter { it.username.lowercase() !in followerSet }
        val mutuals = following.filter { it.username.lowercase() in followerSet }
        val fans = followers.filter { it.username.lowercase() !in followingSet }

        return ExportAnalyticsSummary(
            followersCount = followers.distinctBy { it.username.lowercase() }.size,
            followingCount = following.distinctBy { it.username.lowercase() }.size,
            notFollowingBackCount = notFollowingBack.distinctBy { it.username.lowercase() }.size,
            mutualCount = mutuals.distinctBy { it.username.lowercase() }.size,
            fansCount = fans.distinctBy { it.username.lowercase() }.size,
            pendingCount = pendingRequests.distinctBy { it.username.lowercase() }.size,
            recentlyUnfollowedCount = recentlyUnfollowed.distinctBy { it.username.lowercase() }.size,
        )
    }
}

data class ExportAnalyticsSummary(
    val followersCount: Int,
    val followingCount: Int,
    val notFollowingBackCount: Int,
    val mutualCount: Int,
    val fansCount: Int,
    val pendingCount: Int,
    val recentlyUnfollowedCount: Int,
)

enum class ConnectionFilter(val label: String) {
    NOT_FOLLOWING_BACK("Not Following Back"),
    MUTUALS("Mutuals"),
    FANS("Fans"),
    FOLLOWERS("Followers"),
    FOLLOWING("Following"),
    PENDING("Pending"),
    RECENTLY_UNFOLLOWED("Unfollowed"),
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    ALPHABETICAL("A–Z"),
}

data class ConnectionItem(
    val username: String,
    val profileUrl: String,
    val timestampSec: Long,
    val isIgnored: Boolean = false,
)

data class SnapshotDiff(
    val previousLabel: String,
    val previousImportedAt: Long,
    val gainedFollowers: List<String>,
    val lostFollowers: List<String>,
    val newlyFollowed: List<String>,
)
