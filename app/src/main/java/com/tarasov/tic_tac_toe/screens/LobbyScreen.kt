package com.tarasov.tic_tac_toe.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LobbyScreen() {

    Column { }
}

@Composable
fun Gamer() {
    Row {
        Text(
            text = "User 1", // получаем с сервера
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { },
            modifier = Modifier
        ) {
            Text(
                text = "пригласить в игру",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}