package com.tarasov.tic_tac_toe.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarasov.tic_tac_toe.data.Gamer
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-обёртка: подписывается на LobbyViewModel из Koin.
 */
@Composable
fun LobbyRoute(
    modifier: Modifier = Modifier,
    viewModel: LobbyViewModel = koinViewModel(),
) {
    val users by viewModel.listUsers.collectAsStateWithLifecycle()

    LobbyScreen(
        users = users,
        onInviteClick = { id -> viewModel.inviteToGame(id) },
        modifier = modifier,
    )
}

/**
 * Чистый stateless UI: список игроков + кнопки приглашения.
 */
@Composable
fun LobbyScreen(
    users: List<Gamer>,
    onInviteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (users.isEmpty()) {
            Text(
                text = "Пока никого нет в сети",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn {
                items(items = users, key = { it.id }) { gamer ->
                    User(
                        gamer = gamer,
                        onInviteClick = { onInviteClick(gamer.id) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun User(
    gamer: Gamer,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = gamer.username,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onInviteClick,
            modifier = Modifier,
        ) {
            Text(
                text = "пригласить в игру",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LobbyScreenPreview() {
    TictactoeTheme {
        LobbyScreen(
            users = listOf(
                Gamer(id = 1, username = "User 1"),
                Gamer(id = 2, username = "User 2"),
            ),
            onInviteClick = {},
        )
    }
}