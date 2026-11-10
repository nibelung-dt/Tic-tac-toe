package com.tarasov.tic_tac_toe.screens

import app.cash.turbine.test
import com.tarasov.tic_tac_toe.data.ApiResult
import com.tarasov.tic_tac_toe.data.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope жёстко использует Dispatchers.Main — подменяем его тестовым
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Fake

    private class FakeAuthRepository(
        var loginResult: ApiResult<Unit> = ApiResult.Success(Unit),
        var registrationResult: ApiResult<Unit> = ApiResult.Success(Unit),
    ) : AuthRepository {
        var loginCalls = 0
        var registrationCalls = 0
        var loginGate: CompletableDeferred<Unit>? = null
        val loginRequests = mutableListOf<Pair<String, String>>()
        val registrationRequests = mutableListOf<Pair<String, String>>()

        override suspend fun login(username: String, password: String): ApiResult<Unit> {
            loginCalls++
            loginRequests += username to password
            loginGate?.await()
            return loginResult
        }

        override suspend fun registration(username: String, password: String): ApiResult<Unit> {
            registrationCalls++
            registrationRequests += username to password
            return registrationResult
        }
    }

    // endregion

    // region Начальное состояние

    @Test
    fun `initial state is empty`() {
        val vm = LoginViewModel(FakeAuthRepository())

        assertEquals(LoginUiState(), vm.uiState.value)
        assertFalse(vm.uiState.value.canSubmit)
    }

    // endregion

    // region Ввод

    @Test
    fun `onLoginChange updates login field`() {
        val vm = LoginViewModel(FakeAuthRepository())

        vm.onLoginChange("player1")

        assertEquals("player1", vm.uiState.value.login)
    }

    @Test
    fun `onPasswordChange updates password field`() {
        val vm = LoginViewModel(FakeAuthRepository())

        vm.onPasswordChange("secret")

        assertEquals("secret", vm.uiState.value.password)
    }

    @Test
    fun `canSubmit is true only when both fields are filled`() {
        val vm = LoginViewModel(FakeAuthRepository())

        vm.onLoginChange("player1")
        assertFalse(vm.uiState.value.canSubmit)

        vm.onPasswordChange("secret")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `canSubmit is false for blank-only input`() {
        val vm = LoginViewModel(FakeAuthRepository())

        vm.onLoginChange("   ")
        vm.onPasswordChange("   ")

        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `typing clears previous error message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.Unknown(IllegalStateException("Нет сети")),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        vm.onLoginClick {}
        advanceUntilIdle()
        assertEquals("Нет сети", vm.uiState.value.errorMessage)

        vm.onLoginChange("player2")

        assertNull(vm.uiState.value.errorMessage)
    }

    // endregion

    // region Успешный вход

    @Test
    fun `onLoginClick calls repository and invokes onSuccess`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        var successCalled = false

        vm.onLoginClick { successCalled = true }
        advanceUntilIdle()

        assertEquals(1, repo.loginCalls)
        assertTrue(successCalled)
    }

    @Test
    fun `login passes credentials from state to repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals(listOf("player1" to "secret"), repo.loginRequests)
    }

    @Test
    fun `registration passes credentials from state to repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onRegisterClick {}
        advanceUntilIdle()

        assertEquals(listOf("player1" to "secret"), repo.registrationRequests)
    }

    @Test
    fun `click with empty fields does not call repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals(0, repo.loginCalls)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onLoginClick resets state after success`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals(LoginUiState(), vm.uiState.value)
    }

    @Test
    fun `onRegisterClick calls registration and invokes onSuccess`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        var successCalled = false

        vm.onRegisterClick { successCalled = true }
        advanceUntilIdle()

        assertEquals(1, repo.registrationCalls)
        assertEquals(0, repo.loginCalls)
        assertTrue(successCalled)
    }

    // endregion

    // region Загрузка

    @Test
    fun `isLoading is true while request is in flight`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeAuthRepository().apply { loginGate = gate }
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.canSubmit)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `second click is ignored while first request is in flight`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeAuthRepository().apply { loginGate = gate }
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()
        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals(1, repo.loginCalls)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `ui state emits loading sequence`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.uiState.test {
            assertFalse(awaitItem().isLoading)

            vm.onLoginClick {}
            advanceUntilIdle()

            assertTrue(awaitItem().isLoading)
            assertFalse(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Ошибки

    @Test
    fun `login failure shows error message and does not call onSuccess`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.HttpError(code = 401),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        var successCalled = false

        vm.onLoginClick { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Неверный логин или пароль", vm.uiState.value.errorMessage)
    }

    @Test
    fun `invalid response shows invalid response message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.InvalidResponse)
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Некорректный ответ сервера", vm.uiState.value.errorMessage)
    }

    @Test
    fun `unknown error shows cause message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.Unknown(IllegalStateException("Не удалось выполнить вход")),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Не удалось выполнить вход", vm.uiState.value.errorMessage)
    }

    @Test
    fun `login failure keeps entered credentials`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.HttpError(code = 500),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("player1", vm.uiState.value.login)
        assertEquals("secret", vm.uiState.value.password)
    }

    @Test
    fun `registration failure shows error message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            registrationResult = ApiResult.HttpError(code = 409),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onRegisterClick {}
        advanceUntilIdle()

        assertEquals("Такой пользователь уже существует", vm.uiState.value.errorMessage)
    }

    // endregion

    // region HTTP-ошибки

    @Test
    fun `401 shows invalid credentials message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.HttpError(code = 401))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Неверный логин или пароль", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `409 shows user already exists message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(registrationResult = ApiResult.HttpError(code = 409))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onRegisterClick {}
        advanceUntilIdle()

        assertEquals("Такой пользователь уже существует", vm.uiState.value.errorMessage)
    }

    @Test
    fun `500 shows server unavailable message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.HttpError(code = 503))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Сервер недоступен, попробуйте позже", vm.uiState.value.errorMessage)
    }

    @Test
    fun `404 shows not found message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.HttpError(code = 404))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Запрошенные данные не найдены", vm.uiState.value.errorMessage)
    }

    @Test
    fun `unmapped http code falls back to generic message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.HttpError(code = 418))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Ошибка запроса (418)", vm.uiState.value.errorMessage)
    }

    @Test
    fun `io error shows no connection message`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.NoConnection,
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Нет подключения к интернету", vm.uiState.value.errorMessage)
    }

    @Test
    fun `401 does not call onSuccess`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(loginResult = ApiResult.HttpError(code = 401))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("ivan")
        vm.onPasswordChange("ivan")
        var successCalled = false

        vm.onLoginClick { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
    }

    // endregion

    // region Отмена

    @Test
    fun `onCancelClick clears state`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            loginResult = ApiResult.HttpError(code = 401),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        vm.onLoginClick {}
        advanceUntilIdle()

        vm.onCancelClick()

        assertEquals(LoginUiState(), vm.uiState.value)
    }

    // endregion
}
