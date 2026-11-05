package com.tarasov.tic_tac_toe.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarasov.tic_tac_toe.ui.theme.TictactoeTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-обёртка: связывает ViewModel (из Koin) с чистым UI.
 * Activity вызывает именно её.
 */
@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    onAuthenticated: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onLoginChange = viewModel::onLoginChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { viewModel.onLoginClick(onSuccess = onAuthenticated) },
        onRegisterClick = { viewModel.onRegisterClick(onSuccess = onAuthenticated) },
        onCancelClick = viewModel::onCancelClick,
        modifier = modifier,
    )
}

/**
 * Чистый stateless UI: только входы и выходы, никакой логики и DI.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    // val submitEnabled = remember(uiState) { uiState.canSubmit }
    val submitEnabled = uiState.canSubmit

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Крестики-нолики",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.login,
            onValueChange = onLoginChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            singleLine = true,
            isError = uiState.errorMessage != null,
            label = { Text("Логин") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            singleLine = true,
            isError = uiState.errorMessage != null,
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (submitEnabled) onLoginClick()
                },
            ),
        )

        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    onLoginClick()
                },
                modifier = Modifier.weight(1f),
                enabled = submitEnabled,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Вход")
                }
            }

            OutlinedButton(
                onClick = {
                    keyboardController?.hide()
                    onCancelClick()
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading,
            ) {
                Text("Отмена")
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = {
                keyboardController?.hide()
                onRegisterClick()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = submitEnabled,
        ) {
            Text("Зарегистрировать новый аккаунт")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TictactoeTheme {
        LoginScreen(
            uiState = LoginUiState(login = "player1", password = "secret"),
            onLoginChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onRegisterClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    TictactoeTheme {
        LoginScreen(
            uiState = LoginUiState(login = "player1", password = "secret", isLoading = true),
            onLoginChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onRegisterClick = {},
            onCancelClick = {},
        )
    }
}
