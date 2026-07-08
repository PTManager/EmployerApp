package com.example.ptmanageremployer.data

import java.text.NumberFormat
import java.util.Locale

/** Intent 로 화면 간 전달하는 키 모음. */
object Extras {
    const val SHIFT_ID = "extra_shift_id"
    const val NOTICE_ID = "extra_notice_id"
    const val SWAP_REQUEST_ID = "extra_swap_request_id"
    const val HANDOVER_ID = "extra_handover_id"
    const val HANDOVER_CATEGORY = "extra_handover_category"
    const val HANDOVER_TITLE = "extra_handover_title"
    const val HANDOVER_CONTENT = "extra_handover_content"
    const val HANDOVER_AUTHOR_ID = "extra_handover_author_id"
    const val HANDOVER_AUTHOR_NAME = "extra_handover_author_name"
    const val HANDOVER_CREATED_AT = "extra_handover_created_at"
}

/** 인수인계 카테고리 코드 → 한글 태그 라벨. (STOCK/DEVICE/CUSTOMER) */
fun handoverCategoryLabel(code: String?): String = when (code) {
    "STOCK" -> "#재고"
    "DEVICE" -> "#기기오류"
    "CUSTOMER" -> "#손님"
    else -> "#기타"
}

/** "18:00:00" + "23:00:00" → "18:00 – 23:00" */
fun shiftTimeRange(start: String?, end: String?): String {
    val s = start?.take(5).orEmpty()
    val e = end?.take(5).orEmpty()
    return if (s.isBlank() && e.isBlank()) "" else "$s – $e"
}

/** 주 시작(월)~일요일 범위 라벨: "6/30 – 7/6" */
fun weekRangeLabel(monday: java.time.LocalDate): String {
    val sunday = monday.plusDays(6)
    return "${monday.monthValue}/${monday.dayOfMonth} – ${sunday.monthValue}/${sunday.dayOfMonth}"
}

/** 근무 시간(시). 자정 넘는 야간 교대(end≤start)는 +24h 보정. 파싱 실패 시 0. */
fun shiftHours(start: String?, end: String?): Double {
    val s = runCatching { java.time.LocalTime.parse(start) }.getOrNull() ?: return 0.0
    val e = runCatching { java.time.LocalTime.parse(end) }.getOrNull() ?: return 0.0
    var mins = java.time.Duration.between(s, e).toMinutes()
    if (mins <= 0) mins += 24 * 60
    return mins / 60.0
}

/** 3.5 → "3.5h", 8.0 → "8h" */
fun hoursLabel(hours: Double): String =
    if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${Math.round(hours * 10) / 10.0}h"

/** ISO 시각(오프셋 유무 모두) → "방금 / N분 전 / N시간 전 / N일 전 / M/d". 파싱 실패 시 "". */
fun relativeTime(iso: String?): String {
    val instant = runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching {
            java.time.LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault()).toInstant()
        }.getOrNull() ?: return ""
    val mins = java.time.Duration.between(instant, java.time.Instant.now()).toMinutes()
    return when {
        mins < 1 -> "방금"
        mins < 60 -> "${mins}분 전"
        mins < 60 * 24 -> "${mins / 60}시간 전"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}일 전"
        else -> java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            .let { "${it.monthValue}/${it.dayOfMonth}" }
    }
}

/** 근태 상태 한글 라벨. */
fun attendanceLabel(status: String?): String = when (status) {
    "PRESENT" -> "출근"
    "LATE" -> "지각"
    "ABSENT" -> "결근"
    "SCHEDULED" -> "예정"
    else -> status ?: ""
}

/** 1234567 → "₩1,234,567" */
fun won(amount: Long): String =
    "₩" + NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
