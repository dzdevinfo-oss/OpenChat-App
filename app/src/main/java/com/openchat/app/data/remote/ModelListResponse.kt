package com.openchat.app.data.remote

data class ModelListResponse(
    val data: List<ApiModelItem>? = null,
    val models: List<ApiModelItem>? = null
)

data class ApiModelItem(
    val id: String?,
    val name: String?
)
