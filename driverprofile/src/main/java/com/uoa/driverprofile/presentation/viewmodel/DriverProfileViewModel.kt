package com.uoa.driverprofile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.driverprofile.domain.usecase.DeleteDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.GetDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.InsertDriverProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DriverProfileViewModel @Inject constructor(
    private val insertDriverProfileUseCase: InsertDriverProfileUseCase,
    private val getDriverProfileByEmailUseCase: GetDriverProfileByEmailUseCase,
    private val deleteDriverProfileByEmailUseCase: DeleteDriverProfileByEmailUseCase,
) : ViewModel() {
    private val _email= MutableLiveData<String>()
    val email: MutableLiveData<String> get() = _email

    private val _profile_id = MutableLiveData<String>()
    val profile_id: MutableLiveData<String> get() = _profile_id

//    fun insertDriverProfile(profileId:String, em: String) {
//        viewModelScope.launch {
//            val driverProfileEntity= DriverProfileEntity(
//                email = em,
//                driverProfileId = UUID.fromString(profileId)
//            )
//            insertDriverProfileUseCase.execute(driverProfileEntity)
//        }
//
//    }

    fun insertDriverProfile(profileId: UUID, email: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val driverProfileEntity = DriverProfileEntity(
                    email = email,
                    driverProfileId = profileId
                )
                insertDriverProfileUseCase.execute(driverProfileEntity)
            }
            withContext(Dispatchers.Main) {
                callback(result.isSuccess)
            }
        }
    }


    fun getDriverProfileByEmail() {
        viewModelScope.launch {
            getDriverProfileByEmailUseCase.execute(email.value.toString())
        }
    }

    fun deleteDriverProfileByEmail() {
        viewModelScope.launch {
            deleteDriverProfileByEmailUseCase.execute(email.value.toString())
        }
    }


}
