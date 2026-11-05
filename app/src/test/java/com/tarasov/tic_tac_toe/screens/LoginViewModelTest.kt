package com.tarasov.tic_tac_toe.screens

import app.cash.turbine.test
import com.tarasov.tic_tac_toe.data.LoginRepository
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

    private class FakeLoginRepository(
        var loginResult: Result<Unit> = Result.success(Unit),
        var registrationResult: Result<Unit> = Result.success(Unit),
    ) : LoginRepository {
        var loginCalls = 0
        var registrationCalls = 0
        var loginGate: CompletableDeferred<Unit>? = null

        override suspend fun login() {
            loginCalls++
            loginGate?.await()
            loginResult.getOrThrow()
        }

        override suspend fun registration() {
            registrationCalls++
            registrationResult.getOrThrow()
        }
    }

    // endregion

    // region Начальное состояние

    @Test
    fun `initial state is empty`() {
        val vm = LoginViewModel(FakeLoginRepository())

        assertEquals(LoginUiState(), vm.uiState.value)
        assertFalse(vm.uiState.value.canSubmit)
    }

    // endregion

    // region Ввод

    @Test
    fun `onLoginChange updates login field`() {
        val vm = LoginViewModel(FakeLoginRepository())

        vm.onLoginChange("player1")

        assertEquals("player1", vm.uiState.value.login)
    }

    @Test
    fun `onPasswordChange updates password field`() {
        val vm = LoginViewModel(FakeLoginRepository())

        vm.onPasswordChange("secret")

        assertEquals("secret", vm.uiState.value.password)
    }

    @Test
    fun `canSubmit is true only when both fields are filled`() {
        val vm = LoginViewModel(FakeLoginRepository())

        vm.onLoginChange("player1")
        assertFalse(vm.uiState.value.canSubmit)

        vm.onPasswordChange("secret")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `canSubmit is false for blank-only input`() {
        val vm = LoginViewModel(FakeLoginRepository())

        vm.onLoginChange("   ")
        vm.onPasswordChange("   ")

        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `typing clears previous error message`() = runTest(dispatcher) {
        val repo = FakeLoginRepository(loginResult = Result.failure(IllegalStateException("Нет сети")))
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
        val repo = FakeLoginRepository()
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
    fun `onLoginClick resets state after success`() = runTest(dispatcher) {
        val repo = FakeLoginRepository()
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals(LoginUiState(), vm.uiState.value)
    }

    @Test
    fun `onRegisterClick calls registration and invokes onSuccess`() = runTest(dispatcher) {
        val repo = FakeLoginRepository()
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
        val repo = FakeLoginRepository().apply { loginGate = gate }
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
        val repo = FakeLoginRepository().apply { loginGate = gate }
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
        val vm = LoginViewModel(FakeLoginRepository())
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
        val repo = FakeLoginRepository(
            loginResult = Result.failure(IllegalStateException("Неверный пароль")),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")
        var successCalled = false

        vm.onLoginClick { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Неверный пароль", vm.uiState.value.errorMessage)
    }

    @Test
    fun `login failure without message falls back to default text`() = runTest(dispatcher) {
        val repo = FakeLoginRepository(loginResult = Result.failure(RuntimeException()))
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onLoginClick {}
        advanceUntilIdle()

        assertEquals("Не удалось выполнить вход", vm.uiState.value.errorMessage)
    }

    @Test
    fun `login failure keeps entered credentials`() = runTest(dispatcher) {
        val repo = FakeLoginRepository(
            loginResult = Result.failure(IllegalStateException("Ошибка")),
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
        val repo = FakeLoginRepository(
            registrationResult = Result.failure(IllegalStateException("Уже существует")),
        )
        val vm = LoginViewModel(repo)
        vm.onLoginChange("player1")
        vm.onPasswordChange("secret")

        vm.onRegisterClick {}
        advanceUntilIdle()

        assertEquals("Уже существует", vm.uiState.value.errorMessage)
    }

    // endregion

    // region Отмена

    @Test
    fun `onCancelClick clears state`() = runTest(dispatcher) {
        val repo = FakeLoginRepository(
            loginResult = Result.failure(IllegalStateException("Ошибка")),
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
