package com.tarasov.tic_tac_toe.screens.gameScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.Button
import com.tarasov.tic_tac_toe.screens.LobbyScreen
import com.tarasov.tic_tac_toe.screens.LobbyViewModel


@Composable
fun GameScreenRoute(
    onExitFromGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = koinViewModel(),
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    GameScreen(
        gameState = gameState,
        onExitFromGame = { onExitFromGame() },
        modifier = modifier
    )
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameState: GameState,
    onExitFromGame: () -> Unit,
) {

    Column {
        Row(modifier = Modifier.padding(20.dp)) {
            Text(text = "имя противника")
            Spacer(Modifier.width(15.dp))
            Text(text = "username")
        }

        Grid(
            board = gameState.board,
            onCellClick = { }
        )

        Button(
            onClick = { onExitFromGame() }
        ) {
            Text(text = "выйти из игры")
        }
    }

}

@Composable
fun Grid(
    board: List<CellType>,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(board) { index, state ->
            Cell(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f), // Каждая ячейка строго квадратная
                onClick = { onCellClick(index) }
            )
        }
    }
}




@Composable
fun Cell(
    state: CellType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(Color.LightGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        //TODO возможно, этот when нужно перенести в ViewModel
        when (state) {
            CellType.X -> Text("X", fontSize = 48.sp, color = Color.Blue)
            CellType.O -> Text("O", fontSize = 48.sp, color = Color.Red)
            CellType.EMPTY -> { /* Пусто */ }
        }
    }
}
