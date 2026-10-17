package com.driveflow.driver.license

/**
 * Gatekeeper interface controlling whether screen reading and capture are authorized.
 */
interface LicenseGate {
    fun canRead(): Boolean

    companion object {
        val PERMISSIVE = object : LicenseGate {
            override fun canRead(): Boolean = true
        }
    }
}
