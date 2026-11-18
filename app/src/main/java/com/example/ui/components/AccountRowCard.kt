package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.parser.ConnectionItem
import com.example.ui.theme.LocalFollowLensColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val AvatarPalette = listOf(
    Color(0xFFFF5E7E),
    Color(0xFFFF9F43),
    Color(0xFF2EE59D),
    Color(0xFF4CC9F0),
    Color(0xFFA78BFA),
    Color(0xFFF472B6),
)

@Composable
fun AccountRowCard(
    item: ConnectionItem,
    accentColor: Color,
    showIgnoreAction: Boolean,
    onToggleIgnore: () -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val semantics = LocalFollowLensColors.current
    val avatarColor = AvatarPalette[abs(item.username.lowercase().hashCode()) % AvatarPalette.size]
    val initial = item.username.firstOrNull()?.uppercaseChar()?.toString() ?: "@"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (item.isIgnored && showIgnoreAction) 0.58f else 1f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, semantics.subtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = avatarColor,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "@${item.username}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isIgnored && showIgnoreAction) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "Ignored",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = if (item.timestampSec > 0L) {
                        "Connected ${formatUnixSeconds(item.timestampSec)}"
                    } else {
                        "From official export"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (showIgnoreAction) {
                    IconButton(
                        onClick = onToggleIgnore,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ignore_button_${item.username}"),
                    ) {
                        Icon(
                            imageVector = if (item.isIgnored) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = stringResource(
                                if (item.isIgnored) R.string.whitelist_remove_cd else R.string.whitelist_add_cd
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        copyUsernameToClipboard(context, item.username)
                        onCopied(item.username)
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_button_${item.username}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.copy_handle_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = { openInstagramProfile(context, item.profileUrl, item.username) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_ig_button_${item.username}"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = stringResource(R.string.open_profile_cd),
                        tint = accentColor,
                    )
                }
            }
        }
    }
}

fun formatUnixSeconds(timestampSec: Long): String {
    if (timestampSec <= 0L) return "Unknown date"
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestampSec * 1000L))
}

fun formatEpochMillis(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

private fun copyUsernameToClipboard(context: Context, username: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Instagram Username", username))
}

private fun openInstagramProfile(context: Context, url: String, username: String) {
    val targetUrl = url.ifBlank { "https://www.instagram.com/$username/" }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
