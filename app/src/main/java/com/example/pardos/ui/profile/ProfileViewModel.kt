package com.korkoor.pardos.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.korkoor.pardos.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    // Empezamos con el perfil por defecto. Más adelante aquí cargaremos los datos de Firebase.
    private val _profileState = MutableStateFlow(UserProfile())
    val profileState: StateFlow<UserProfile> = _profileState.asStateFlow()

    fun updateAvatar(newAvatarId: Int) {
        _profileState.update { currentProfile ->
            currentProfile.copy(avatarId = newAvatarId)
        }
        // TODO: Más adelante agregaremos aquí la línea para guardar en Firebase
    }

    fun updateName(newName: String) {
        if (newName.isNotBlank() && newName.length <= 15) { // Límite de 15 caracteres para que no rompa la UI
            _profileState.update { currentProfile ->
                currentProfile.copy(name = newName)
            }
            // TODO: Guardar en Firebase
        }
    }
}