package com.example.domain.parser

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

object InstagramExportParser {

    private const val MAX_ENTRY_BYTES = 30 * 1024 * 1024 // 30 MB per JSON/HTML entry
    private const val MAX_ZIP_ENTRIES = 2000

    private val IG_HREF_REGEX = Regex(
        """href\s*=\s*["']https?://(?:www\.)?instagram\.com/(?:_u/)?([A-Za-z0-9._]+)/?["']""",
        RegexOption.IGNORE_CASE,
    )

    private val USERNAME_PATH_REGEX = Regex(
        """https?://(?:www\.)?instagram\.com/(?:_u/)?([A-Za-z0-9._]+)/?""",
        RegexOption.IGNORE_CASE,
    )

    data class NamedPayload(
        val fileName: String,
        val bytes: ByteArray,
    )

    fun parseZipStream(
        inputStream: InputStream,
        sourceLabel: String,
    ): ParsedExportBundle {
        val followers = linkedMapOf<String, ParsedAccount>()
        val following = linkedMapOf<String, ParsedAccount>()
        val pending = linkedMapOf<String, ParsedAccount>()
        val recentlyUnfollowed = linkedMapOf<String, ParsedAccount>()
        var parsedFileCount = 0

        ZipInputStream(inputStream).use { zis ->
            var entryCount = 0
            while (entryCount < MAX_ZIP_ENTRIES) {
                val entry = zis.nextEntry ?: break
                entryCount++
                if (entry.isDirectory) {
                    zis.closeEntry()
                    continue
                }
                val normalizedPath = entry.name.lowercase()
                if (normalizedPath.contains("__macosx/") || normalizedPath.startsWith(".")) {
                    zis.closeEntry()
                    continue
                }
                val fileType = classifyFileName(normalizedPath)
                if (fileType != null) {
                    val content = readBoundedUtf8(zis)
                    if (content != null) {
                        val accounts = parseFileContent(normalizedPath, content, fileType)
                        if (accounts.isNotEmpty() || normalizedPath.endsWith(".json")) {
                            parsedFileCount++
                        }
                        mergeIntoTarget(fileType, accounts, followers, following, pending, recentlyUnfollowed)
                    }
                }
                zis.closeEntry()
            }
        }

        return ParsedExportBundle(
            sourceLabel = sourceLabel,
            followers = followers.values.toList(),
            following = following.values.toList(),
            pendingRequests = pending.values.toList(),
            recentlyUnfollowed = recentlyUnfollowed.values.toList(),
            parsedFileCount = parsedFileCount,
        )
    }

    fun parseIndividualPayloads(
        payloads: List<NamedPayload>,
        sourceLabel: String,
    ): ParsedExportBundle {
        val followers = linkedMapOf<String, ParsedAccount>()
        val following = linkedMapOf<String, ParsedAccount>()
        val pending = linkedMapOf<String, ParsedAccount>()
        val recentlyUnfollowed = linkedMapOf<String, ParsedAccount>()
        var parsedFileCount = 0

        for (payload in payloads) {
            val normalizedName = payload.fileName.lowercase()
            val content = payload.bytes.toString(StandardCharsets.UTF_8)
            val fileType = classifyFileName(normalizedName) ?: inferTypeFromJsonContent(content)
            if (fileType != null) {
                val accounts = parseFileContent(normalizedName, content, fileType)
                if (accounts.isNotEmpty()) {
                    parsedFileCount++
                }
                mergeIntoTarget(fileType, accounts, followers, following, pending, recentlyUnfollowed)
            }
        }

        return ParsedExportBundle(
            sourceLabel = sourceLabel,
            followers = followers.values.toList(),
            following = following.values.toList(),
            pendingRequests = pending.values.toList(),
            recentlyUnfollowed = recentlyUnfollowed.values.toList(),
            parsedFileCount = parsedFileCount,
        )
    }

    private enum class ExportFileKind {
        FOLLOWERS,
        FOLLOWING,
        PENDING_REQUESTS,
        RECENTLY_UNFOLLOWED,
    }

    private fun classifyFileName(path: String): ExportFileKind? {
        val base = path.substringAfterLast('/')
        val isSupportedExt = base.endsWith(".json") || base.endsWith(".html") || base.endsWith(".htm")
        if (!isSupportedExt) return null

        return when {
            base.startsWith("followers") || base.contains("followers_and_following") && base.contains("follower") -> {
                if (base.contains("hashtag")) null else ExportFileKind.FOLLOWERS
            }
            base.startsWith("following") && !base.contains("hashtag") -> ExportFileKind.FOLLOWING
            base.contains("pending_follow_requests") || base.contains("follow_requests_you've_sent") -> ExportFileKind.PENDING_REQUESTS
            base.contains("recently_unfollowed") -> ExportFileKind.RECENTLY_UNFOLLOWED
            else -> null
        }
    }

    private fun inferTypeFromJsonContent(content: String): ExportFileKind? {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
        return try {
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("relationships_following") -> ExportFileKind.FOLLOWING
                    obj.has("relationships_followers") -> ExportFileKind.FOLLOWERS
                    obj.has("relationships_follow_requests_sent") -> ExportFileKind.PENDING_REQUESTS
                    obj.has("relationships_unfollowed_users") -> ExportFileKind.RECENTLY_UNFOLLOWED
                    else -> null
                }
            } else {
                // Top-level JSONArray is Instagram's classic followers_1.json format
                ExportFileKind.FOLLOWERS
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFileContent(
        normalizedPath: String,
        content: String,
        kind: ExportFileKind,
    ): List<ParsedAccount> {
        return if (normalizedPath.endsWith(".html") || normalizedPath.endsWith(".htm") ||
            content.trimStart().startsWith("<")
        ) {
            parseHtmlContent(content)
        } else {
            parseJsonContent(content, kind)
        }
    }

    private fun parseJsonContent(
        jsonText: String,
        kind: ExportFileKind? = null,
    ): List<ParsedAccount> {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            if (trimmed.startsWith("[")) {
                parseAccountArray(JSONArray(trimmed))
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val preferredKeys = when (kind) {
                    ExportFileKind.FOLLOWERS -> listOf("relationships_followers", "followers")
                    ExportFileKind.FOLLOWING -> listOf("relationships_following", "following")
                    ExportFileKind.PENDING_REQUESTS -> listOf("relationships_follow_requests_sent", "pending_follow_requests")
                    ExportFileKind.RECENTLY_UNFOLLOWED -> listOf("relationships_unfollowed_users", "recently_unfollowed")
                    null -> listOf(
                        "relationships_following",
                        "relationships_followers",
                        "relationships_follow_requests_sent",
                        "relationships_unfollowed_users",
                    )
                }

                for (key in preferredKeys) {
                    if (root.has(key) && root.optJSONArray(key) != null) {
                        return parseAccountArray(root.getJSONArray(key))
                    }
                }

                // Fallback: first JSONArray property in object
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = root.optJSONArray(key)
                    if (arr != null) {
                        return parseAccountArray(arr)
                    }
                }
                emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseAccountArray(array: JSONArray): List<ParsedAccount> {
        val result = ArrayList<ParsedAccount>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val title = item.optString("title", "").trim()
            val stringList = item.optJSONArray("string_list_data")

            var value = ""
            var href = ""
            var timestamp = 0L

            if (stringList != null && stringList.length() > 0) {
                val firstEntry = stringList.optJSONObject(0)
                if (firstEntry != null) {
                    value = firstEntry.optString("value", "").trim()
                    href = firstEntry.optString("href", "").trim()
                    timestamp = firstEntry.optLong("timestamp", 0L)
                }
            }

            val usernameFromHref = extractUsernameFromUrl(href)
            val resolvedUsername = listOf(value, title, usernameFromHref)
                .firstOrNull { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }
                ?.removePrefix("@")
                ?.trim()
                .orEmpty()

            if (resolvedUsername.isNotEmpty()) {
                val resolvedUrl = if (href.isNotBlank()) {
                    href.replace("/_u/", "/")
                } else {
                    "https://www.instagram.com/$resolvedUsername/"
                }
                result.add(
                    ParsedAccount(
                        username = resolvedUsername,
                        profileUrl = resolvedUrl,
                        timestampSec = timestamp,
                    )
                )
            }
        }
        return result
    }

    internal fun parseHtmlContent(html: String): List<ParsedAccount> {
        val matches = IG_HREF_REGEX.findAll(html)
        val seen = LinkedHashMap<String, ParsedAccount>()
        for (match in matches) {
            val username = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (username.isNotEmpty() && !username.equals("p", ignoreCase = true) && !username.equals("reel", ignoreCase = true)) {
                val key = username.lowercase()
                if (!seen.containsKey(key)) {
                    seen[key] = ParsedAccount(
                        username = username,
                        profileUrl = "https://www.instagram.com/$username/",
                        timestampSec = 0L,
                    )
                }
            }
        }
        return seen.values.toList()
    }

    private fun extractUsernameFromUrl(url: String): String {
        if (url.isBlank()) return ""
        val match = USERNAME_PATH_REGEX.find(url)
        return match?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    private fun mergeIntoTarget(
        kind: ExportFileKind,
        accounts: List<ParsedAccount>,
        followers: LinkedHashMap<String, ParsedAccount>,
        following: LinkedHashMap<String, ParsedAccount>,
        pending: LinkedHashMap<String, ParsedAccount>,
        recentlyUnfollowed: LinkedHashMap<String, ParsedAccount>,
    ) {
        val target = when (kind) {
            ExportFileKind.FOLLOWERS -> followers
            ExportFileKind.FOLLOWING -> following
            ExportFileKind.PENDING_REQUESTS -> pending
            ExportFileKind.RECENTLY_UNFOLLOWED -> recentlyUnfollowed
        }
        for (account in accounts) {
            val key = account.username.lowercase()
            val existing = target[key]
            if (existing == null || account.timestampSec > existing.timestampSec) {
                target[key] = account
            }
        }
    }

    private fun readBoundedUtf8(inputStream: InputStream): String? {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            total += read
            if (total > MAX_ENTRY_BYTES) {
                return null
            }
            out.write(buffer, 0, read)
        }
        return out.toString(StandardCharsets.UTF_8.name())
    }
}
