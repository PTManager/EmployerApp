package com.example.ptmanageremployer.data

import java.text.NumberFormat
import java.util.Locale

/** Intent 로 화면 간 전달하는 키 모음. */
object Extras {
    const val SHIFT_ID = "extra_shift_id"
    const val NOTICE_ID = "extra_notice_id"
    const val SWAP_REQUEST_ID = "extra_swap_request_id"
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
