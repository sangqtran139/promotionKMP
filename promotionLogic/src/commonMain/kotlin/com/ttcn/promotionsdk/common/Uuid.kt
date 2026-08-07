package com.ttcn.promotionsdk.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Thay cho `java.util.UUID.randomUUID().toString()` của bản Android — `kotlin.uuid.Uuid` nằm trong
 * stdlib nên chạy được trên cả Android lẫn iOS, không cần thêm dependency.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun randomUuidString(): String = Uuid.random().toString()
