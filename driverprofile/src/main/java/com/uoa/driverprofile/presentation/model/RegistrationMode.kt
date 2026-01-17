package com.uoa.driverprofile.presentation.model

enum class RegistrationMode(val wireValue: String) {
    Email("email"),
    InviteCode("invite");

    companion object {
        fun fromRoute(value: String?): RegistrationMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
                ?: Email
        }
    }
}
