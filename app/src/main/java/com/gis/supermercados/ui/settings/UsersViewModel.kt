package com.gis.supermercados.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.Validators
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.model.User
import com.gis.supermercados.domain.model.UserCredentials
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de alta/edicion de usuario. */
data class UserForm(
    val id: Long = 0L,
    val username: String = "",
    val fullName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: Role = Role.CAJERO,
    val storeId: Long? = null,
)

data class UsersUiState(
    val stores: List<Store> = emptyList(),
    val session: AuthSession? = null,
    val showForm: Boolean = false,
    val form: UserForm = UserForm(),
    val isSaving: Boolean = false,
    val resetTarget: User? = null,
    val resetPassword: String = "",
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Gestion de usuarios: roles, sucursal asignada y contrasenas. */
@HiltViewModel
class UsersViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    storeRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState

    val users: StateFlow<List<User>> = authRepository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val stores = storeRepository.getActiveStores()
            authRepository.session.collect { session ->
                _uiState.update { it.copy(stores = stores, session = session) }
            }
        }
    }

    fun openCreate() = _uiState.update {
        it.copy(showForm = true, form = UserForm(storeId = it.session?.storeId), error = null)
    }

    fun openEdit(user: User) = _uiState.update {
        it.copy(
            showForm = true,
            error = null,
            form = UserForm(id = user.id, username = user.username, fullName = user.fullName, role = user.role, storeId = user.storeId)
        )
    }

    fun closeForm() = _uiState.update { it.copy(showForm = false) }

    fun updateForm(transform: (UserForm) -> UserForm) = _uiState.update { it.copy(form = transform(it.form)) }

    /** Crea el usuario o actualiza rol/sucursal si ya existe. */
    fun save() {
        val form = _uiState.value.form
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = if (form.id == 0L) {
                val usernameCheck = Validators.username(form.username)
                val passwordCheck = Validators.password(form.password)
                val matchCheck = Validators.passwordsMatch(form.password, form.confirmPassword)
                val invalid = listOf(usernameCheck, passwordCheck, matchCheck).firstOrNull { !it.isValid }
                if (invalid != null) {
                    _uiState.update { it.copy(isSaving = false, error = invalid.errorText ?: UiText.of(R.string.users_error_invalid)) }
                    return@launch
                }
                authRepository.createUser(
                    UserCredentials(
                        username = form.username.trim(),
                        password = form.password,
                        fullName = form.fullName.trim(),
                        role = form.role,
                        storeId = form.storeId
                    ),
                    form.password
                )
            } else {
                val roleResult = authRepository.updateUserRole(form.id, form.role)
                if (roleResult.isSuccess) authRepository.updateUserStore(form.id, form.storeId) else roleResult
            }

            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        showForm = false,
                        message = UiText.of(if (form.id == 0L) R.string.users_created else R.string.users_updated)
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun toggleActive(user: User) {
        viewModelScope.launch {
            val result = authRepository.setUserActive(user.id, !user.isActive)
            _uiState.update {
                it.copy(
                    error = (result as? AppResult.Failure)?.error,
                    message = if (result.isSuccess) UiText.of(R.string.users_active_updated) else null
                )
            }
        }
    }

    fun openResetPassword(user: User) = _uiState.update { it.copy(resetTarget = user, resetPassword = "", error = null) }

    fun onResetPasswordChange(value: String) = _uiState.update { it.copy(resetPassword = value) }

    fun cancelResetPassword() = _uiState.update { it.copy(resetTarget = null) }

    /** Reestablece la contrasena de otro usuario (solo administradores). */
    fun confirmResetPassword() {
        val target = _uiState.value.resetTarget ?: return
        val password = _uiState.value.resetPassword
        val check = Validators.password(password)
        if (!check.isValid) {
            _uiState.update { it.copy(error = check.errorText ?: UiText.of(R.string.users_error_invalid)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = authRepository.resetPassword(target.id, password)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSaving = false, resetTarget = null, message = UiText.of(R.string.users_password_reset))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
