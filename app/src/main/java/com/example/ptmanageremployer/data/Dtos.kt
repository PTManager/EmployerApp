package com.example.ptmanageremployer.data

/**
 * PTManager 백엔드 API와 주고받는 DTO 모음(사장 앱).
 * 응답 객체는 백엔드가 일부 필드를 생략해도 파싱이 깨지지 않도록 nullable 로 둔다.
 * 날짜/시간은 ISO 문자열 그대로 보관한다(date=YYYY-MM-DD, time=HH:mm:ss, date-time=ISO-8601).
 */

// ---- 인증 ----
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String = "EMPLOYER",
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String? = "Bearer",
    val expiresIn: Long? = null,
    val user: UserDto,
)

data class UserDto(
    val id: Long,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val workplaceId: Long? = null,
    val hourlyWage: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

// ---- 매장 / 가입 신청 ----
data class WorkplaceDto(
    val id: Long,
    val name: String? = null,
    val address: String? = null,
    val inviteCode: String? = null,
    val createdAt: String? = null,
)

data class CreateWorkplaceRequest(
    val name: String,
    val address: String? = null,
)

data class JoinRequestDto(
    val id: Long,
    val workplaceId: Long? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

data class DecisionRequest(
    /** APPROVE | REJECT */
    val decision: String,
)

// ---- 근무(Shift) ----
data class ShiftDto(
    val id: Long,
    val workplaceId: Long? = null,
    val employeeId: Long? = null,
    val employeeName: String? = null,
    val workDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val checkedInAt: String? = null,
    val attendanceStatus: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class CreateShiftRequest(
    val workplaceId: Long,
    val employeeId: Long,
    val workDate: String,
    val startTime: String,
    val endTime: String,
)

data class UpdateShiftRequest(
    val employeeId: Long? = null,
    val workDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

// ---- 대타(SwapRequest) ----
data class SwapRequestDto(
    val id: Long,
    val workplaceId: Long? = null,
    val shiftId: Long? = null,
    val requesterId: Long? = null,
    val substituteId: Long? = null,
    val reason: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val shift: ShiftDto? = null,
)

data class SwapApplicationDto(
    val id: Long,
    val swapRequestId: Long? = null,
    val applicantId: Long? = null,
    val applicantName: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

data class SwapRequestDetailDto(
    val id: Long,
    val workplaceId: Long? = null,
    val shiftId: Long? = null,
    val requesterId: Long? = null,
    val substituteId: Long? = null,
    val reason: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val shift: ShiftDto? = null,
    val applications: List<SwapApplicationDto>? = null,
)

data class ApproveSwapRequest(
    val applicantId: Long,
)

// ---- 공지(Notice) ----
data class NoticeAttachmentDto(
    val id: Long,
    val noticeId: Long? = null,
    val fileUrl: String? = null,
    val createdAt: String? = null,
)

data class NoticeDto(
    val id: Long,
    val workplaceId: Long? = null,
    val authorId: Long? = null,
    val authorName: String? = null,
    val title: String? = null,
    val body: String? = null,
    val attachments: List<NoticeAttachmentDto>? = null,
    val createdAt: String? = null,
)

data class CreateNoticeRequest(
    val workplaceId: Long,
    val title: String,
    val body: String,
    val attachmentUrls: List<String>? = null,
)

data class UnreadFlag(val hasUnread: Boolean = false)

// ---- 알림(Notification) ----
data class NotificationDto(
    val id: Long,
    val userId: Long? = null,
    val type: String? = null,
    val message: String? = null,
    val targetType: String? = null,
    val targetId: Long? = null,
    val isRead: Boolean = false,
    val createdAt: String? = null,
)

data class UnreadCount(val count: Int = 0)

// ---- 사용자 ----
data class UpdateProfileRequest(
    val name: String,
)

data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "ANDROID",
)

data class NotificationSettingDto(
    val userId: Long? = null,
    val swapEnabled: Boolean = true,
    val noticeEnabled: Boolean = true,
    val attendanceEnabled: Boolean = true,
    val joinRequestEnabled: Boolean = true,
)

data class NotificationSettingUpdate(
    val swapEnabled: Boolean? = null,
    val noticeEnabled: Boolean? = null,
    val attendanceEnabled: Boolean? = null,
    val joinRequestEnabled: Boolean? = null,
)

// ---- 매장 운영(QR / 시급) ----
data class QrTokenResponse(val qrToken: String)

data class UpdateWageRequest(val hourlyWage: Int)

// ---- 인건비(Payroll) ----
data class PayrollItem(
    val employeeId: Long? = null,
    val employeeName: String? = null,
    val hourlyWage: Int? = null,
    val workedMinutes: Int? = null,
    val amount: Long? = null,
)

data class PayrollSummary(
    val workplaceId: Long? = null,
    val yearMonth: String? = null,
    val totalAmount: Long = 0,
    val items: List<PayrollItem> = emptyList(),
)

data class WeeklyCost(
    val week: Int = 0,
    val amount: Long = 0,
)

data class WeeklyPayrollSummary(
    val workplaceId: Long? = null,
    val yearMonth: String? = null,
    val weeks: List<WeeklyCost> = emptyList(),
)

// ---- 공통 페이지 응답 ----
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)
