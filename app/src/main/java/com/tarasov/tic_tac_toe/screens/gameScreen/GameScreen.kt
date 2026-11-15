package com.tarasov.tic_tac_toe.screens.gameScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarasov.tic_tac_toe.screens.LoginViewModel
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme
import org.koin.androidx.compose.koinViewModel


@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = koinViewModel(),
) {
    val listState by viewModel.gameState.collectAsStateWithLifecycle()



    Column() {
        Row {
            Cell(listState[0][0])
            Cell(listState[0][1])
            Cell(listState[0][2])
        }

        Row {
            Cell(listState[1][0])
            Cell(listState[1][1])
            Cell(listState[1][2])
        }

        Row {
            Cell(listState[2][0])
            Cell(listState[2][1])
            Cell(listState[2][2])
        }
    }
}


@Composable
fun Cell(value: Int) {
    Text(
        modifier = Modifier.padding(10.dp),
        text = value.toString(),
    )
}

@Preview(showBackground = true)
@Composable
fun CellPreview(value: Int = 0) {
    Text(
        modifier = Modifier.padding(10.dp),
        text = value.toString(),
    )
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    //TODO нужно доработать эту функцию

    // listOf(listOf(1, 0, 2), listOf(0, 0, 2), listOf(1, 1, 2))

    TictactoeTheme {
        GameScreen(

        )
    }
}