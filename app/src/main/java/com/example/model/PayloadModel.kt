package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PayloadInjectMethod(val displayName: String) {
    NORMAL("Normal"),
    FRONT_INJECT("Front Inject"),
    BACK_INJECT("Back Inject"),
    SPLIT("Split"),
    DOUBLE_SPLIT("Double Split")
}

enum class PayloadRequestMethod(val verb: String) {
    CONNECT("CONNECT"),
    GET("GET"),
    POST("POST"),
    HEAD("HEAD"),
    PUT("PUT"),
    OPTIONS("OPTIONS"),
    TRACE("TRACE"),
    PATCH("PATCH")
}

@Entity(tableName = "payload_templates")
data class PayloadTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val payloadString: String,
    val injectMethod: PayloadInjectMethod = PayloadInjectMethod.NORMAL,
    val requestMethod: PayloadRequestMethod = PayloadRequestMethod.CONNECT,
    val isCustom: Boolean = true
)
