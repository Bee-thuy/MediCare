package com.example.medicare

import com.example.medicare.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserRoleTest {
    @Test
    fun parsesAllSupportedRoles() {
        assertEquals(UserRole.ADMIN, UserRole.fromClaim("ADMIN"))
        assertEquals(UserRole.USER, UserRole.fromClaim("user"))
        assertEquals(UserRole.USER, UserRole.fromClaim("PATIENT"))
        assertEquals(UserRole.DOCTOR, UserRole.fromClaim("DOCTOR"))
    }

    @Test
    fun rejectsUnknownRole() {
        assertNull(UserRole.fromClaim("SUPER_ADMIN"))
    }
}
