package com.permissionlab.app.model

data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val category: String,
    val description: String,
    var status: PermissionStatus = PermissionStatus.DENIED,
    var testResult: String? = null,
    val requiresAdb: Boolean = false,
    val adbCommand: String? = null
)
