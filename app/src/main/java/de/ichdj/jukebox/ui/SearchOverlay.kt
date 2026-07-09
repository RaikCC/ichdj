package de.ichdj.jukebox.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.ichdj.jukebox.R
import de.ichdj.jukebox.core.TimeFormat
import de.ichdj.jukebox.engine.WishResult
import de.ichdj.jukebox.model.Track

/** Vollbild-Overlay für die Songsuche inklusive Bestätigungs- und Hinweisdialogen. */
@Composable
fun SearchOverlay(search: SearchUi, vm: MainViewModel) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = vm::closeSearch) {
                    Text(stringResource(R.string.search_close))
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            when {
                search.loading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                ) { CircularProgressIndicator() }

                search.results.isEmpty() && search.query.trim().length >= 2 -> Text(
                    stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    items(search.results, key = { it.uri }) { track ->
                        SearchResultRow(track, onClick = { vm.selectTrack(track) })
                    }
                }
            }
        }
    }

    // Bestätigung: Wünsche sind nicht umkehrbar
    search.confirmTrack?.let { track ->
        AlertDialog(
            onDismissRequest = vm::dismissConfirm,
            title = { Text(stringResource(R.string.confirm_wish_title)) },
            text = {
                Column {
                    SearchResultRow(track, onClick = null)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.confirm_wish_text))
                }
            },
            confirmButton = {
                Button(onClick = vm::confirmWish, enabled = !search.submitting) {
                    Text(stringResource(R.string.confirm_wish_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissConfirm) {
                    Text(stringResource(R.string.confirm_wish_no))
                }
            },
        )
    }

    // Ablehnungshinweise (gesperrt, zu lang, ...)
    search.rejection?.let { rejection ->
        AlertDialog(
            onDismissRequest = vm::dismissRejection,
            title = { Text(stringResource(R.string.reject_title)) },
            text = { Text(rejectionText(rejection)) },
            confirmButton = {
                TextButton(onClick = vm::dismissRejection) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun rejectionText(rejection: WishResult): String = when (rejection) {
    is WishResult.TooLong ->
        stringResource(R.string.reject_too_long, rejection.maxMinutes)
    is WishResult.Locked -> stringResource(
        R.string.reject_locked,
        TimeFormat.clockCeil(rejection.allowedAgainAtMillis),
        TimeFormat.remainingWait(rejection.allowedAgainAtMillis - System.currentTimeMillis()),
    )
    is WishResult.AlreadyQueued -> stringResource(R.string.reject_already_queued)
    is WishResult.NoFreeSlot -> stringResource(R.string.reject_no_slot)
    is WishResult.NotConnected -> stringResource(R.string.reject_no_connection)
    is WishResult.NoActiveDevice -> stringResource(R.string.reject_no_active_device)
    is WishResult.Error -> stringResource(R.string.reject_generic, rejection.message ?: "?")
    is WishResult.Accepted -> "" // tritt nicht auf
}

@Composable
private fun SearchResultRow(track: Track, onClick: (() -> Unit)?) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                TimeFormat.duration(track.durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
