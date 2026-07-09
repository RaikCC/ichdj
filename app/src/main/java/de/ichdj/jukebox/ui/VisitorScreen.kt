package de.ichdj.jukebox.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.ichdj.jukebox.R
import de.ichdj.jukebox.ui.theme.OnWishContainer
import de.ichdj.jukebox.ui.theme.SpotifyGreen
import de.ichdj.jukebox.ui.theme.WishContainer

@Composable
fun VisitorScreen(
    state: JukeboxUiState,
    vm: MainViewModel,
    onOperatorRequest: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            LogoBar(onQuintupleTap = onOperatorRequest)
            if (state.syncTrouble) {
                Text(
                    stringResource(R.string.connection_trouble),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxSize()) {
                WishColumn(state, vm, Modifier.weight(1f))
                Spacer(Modifier.width(20.dp))
                QueueColumn(state, Modifier.weight(1f))
            }
        }
        if (state.search.visible) {
            SearchOverlay(state.search, vm)
        }
    }
}

/** Logo-Zeile; fünfmaliges schnelles Tippen öffnet das Veranstaltermenü. */
@Composable
private fun LogoBar(onQuintupleTap: () -> Unit) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            val now = System.currentTimeMillis()
            tapCount = if (now - lastTapAt < 800) tapCount + 1 else 1
            lastTapAt = now
            if (tapCount >= 5) {
                tapCount = 0
                onQuintupleTap()
            }
        },
    ) {
        Image(
            painterResource(R.drawable.ic_cat_logo),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.logo_text),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun WishColumn(state: JukeboxUiState, vm: MainViewModel, modifier: Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.wishes_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !state.wishesEnabled -> Text(
                stringResource(R.string.wishes_disabled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.allBoxesFull -> Text(
                stringResource(R.string.all_boxes_full),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
        ) {
            items(state.boxes.size, key = { it }) { index ->
                WishBoxCard(
                    box = state.boxes[index],
                    enabled = state.wishesEnabled && state.connected,
                    onClick = vm::openSearch,
                )
            }
        }
    }
}

@Composable
private fun WishBoxCard(box: WishBoxUi, enabled: Boolean, onClick: () -> Unit) {
    val occupied = box.wish != null
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (occupied) WishContainer else MaterialTheme.colorScheme.surface,
        border = if (occupied) null else BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .then(
                // Belegte Boxen sind nicht anklickbar, bis der Wunsch gespielt wurde
                if (!occupied && enabled) Modifier.clickable(onClick = onClick) else Modifier,
            ),
    ) {
        if (!occupied) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    stringResource(R.string.wish_empty_box),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val track = box.wish!!.track
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (box.isPlaying) {
                        PulsingPlayIcon(tint = OnWishContainer)
                    } else {
                        Text(
                            "${box.number}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = OnWishContainer,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.name,
                        fontWeight = FontWeight.Bold,
                        color = OnWishContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artists,
                        color = OnWishContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (box.isPlaying) "-${box.remainingLabel}" else box.durationLabel ?: "",
                        color = OnWishContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        box.startLabel ?: stringResource(R.string.wish_now_playing),
                        color = OnWishContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingPlayIcon(tint: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Icon(
        Icons.Default.PlayArrow,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        modifier = Modifier.size(42.dp),
    )
}

@Composable
private fun QueueColumn(state: JukeboxUiState, modifier: Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.queue_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !state.connected -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.not_connected),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.queue.isEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.queue_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                items(state.queue, key = { it.key }) { entry ->
                    QueueItemRow(entry)
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(entry: QueueEntryUi) {
    val background = when {
        entry.isWish -> WishContainer
        entry.isCurrent -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val foreground = if (entry.isWish) OnWishContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = if (entry.isCurrent) BorderStroke(2.dp, SpotifyGreen) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            if (entry.wishNumber != null) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${entry.wishNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = foreground,
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Box {
                AsyncImage(
                    model = entry.track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                if (entry.isCurrent && entry.isPlayingNow) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x66000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.track.name,
                    fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    entry.track.artists,
                    color = foreground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (entry.isCurrent) "-${entry.remainingLabel}" else entry.durationLabel,
                    color = foreground,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    entry.startLabel ?: stringResource(R.string.wish_now_playing),
                    color = if (entry.isCurrent) SpotifyGreen else foreground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
