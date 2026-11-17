package com.tarasov.tic_tac_toe.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarasov.tic_tac_toe.data.PresenceStatus
import com.tarasov.tic_tac_toe.data.User
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-обёртка: подписывается на LobbyViewModel из Koin.
 */
@Composable
fun LobbyRoute(
    onEnterToGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LobbyViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LobbyScreen(
        uiState = uiState,
        onInviteClick = { id -> viewModel.inviteToGame(id) },
        onRetryClick = viewModel::loadActiveUsers,
        onEnterToGame = onEnterToGame,
        modifier = modifier
    )
}

/**
 * Чистый stateless UI: загрузка / ошибка / пустой список / список игроков.
 */
@Composable
fun LobbyScreen(
    uiState: LobbyUiState,
    onInviteClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onEnterToGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Приветствие из WebSocket и ошибка сокета — над списком игроков.
        if (uiState.greeting != null || uiState.socketError != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.greeting?.let { greeting ->
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                uiState.socketError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = onRetryClick,
                )

                uiState.users.isEmpty() -> Text(
                    text = "Пока никого нет в сети",
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = uiState.users) { user ->
                        UserItem(
                            user = user,
                            onInviteClick = { onInviteClick(0L) },
                            onEnterToGame = { onEnterToGame() }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetryClick) {
            Text("Повторить")
        }
    }
}

@Composable
fun UserItem(
    modifier: Modifier = Modifier,
    user: User,
    onInviteClick: () -> Unit,
    onEnterToGame: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = user.username,
            style = MaterialTheme.typography.bodyMedium,
        )
        //TODO не нужно показывать эту кнопку для авторизованного пользователя
        Button(
            onClick = onInviteClick,
            modifier = Modifier,
        ) {
            Text(
                text = "пригласить в игру",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            modifier = Modifier,
            onClick = onEnterToGame
        ) {
            Text(
                text = "войти в игру",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(text = user.presenceStatus.value)
    }
}

@Preview(showBackground = true)
@Composable
private fun LobbyScreenPreview() {
    TictactoeTheme {
        LobbyScreen(
            uiState = LobbyUiState(
                users = listOf(
                    User(username = "User 1", presenceStatus = PresenceStatus.ONLINE),
                    User(username = "User 2", presenceStatus = PresenceStatus.OFFLINE),
                ),
            ),
            onInviteClick = {},
            onRetryClick = {},
            onEnterToGame = {}
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun LobbyScreenErrorPreview() {
    TictactoeTheme {
        LobbyScreen(
            uiState = LobbyUiState(errorMessage = "Нет подключения к интернету"),
            onInviteClick = {},
            onRetryClick = {},
            onEnterToGame = {}
        )
    }
}
