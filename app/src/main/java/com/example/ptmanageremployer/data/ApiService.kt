package com.example.ptmanageremployer.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * PTManager 백엔드 REST API (사장 앱이 사용하는 엔드포인트).
 * 인증 헤더는 [AuthInterceptor] 가 자동 부착한다.
 */
interface ApiService {

    // ---- Auth ----
    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): TokenResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun me(): UserDto

    // ---- Workplace / JoinRequest ----
    @POST("api/workplaces")
    suspend fun createWorkplace(@Body body: CreateWorkplaceRequest): WorkplaceDto

    @GET("api/workplaces/{id}")
    suspend fun getWorkplace(@Path("id") id: Long): WorkplaceDto

    @GET("api/workplaces/{id}/members")
    suspend fun getMembers(
        @Path("id") id: Long,
        @Query("role") role: String? = null,
    ): List<UserDto>

    @GET("api/join-requests")
    suspend fun getJoinRequests(
        @Query("workplaceId") workplaceId: Long,
        @Query("status") status: String? = null,
    ): List<JoinRequestDto>

    @PATCH("api/join-requests/{id}")
    suspend fun decideJoinRequest(
        @Path("id") id: Long,
        @Body body: DecisionRequest,
    ): JoinRequestDto

    // ---- User ----
    @PATCH("api/users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserDto

    // ---- Shift ----
    @GET("api/shifts")
    suspend fun getShifts(
        @Query("workplaceId") workplaceId: Long? = null,
        @Query("employeeId") employeeId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("status") status: String? = null,
    ): List<ShiftDto>

    @POST("api/shifts")
    suspend fun createShift(@Body body: CreateShiftRequest): ShiftDto

    @GET("api/shifts/{id}")
    suspend fun getShift(@Path("id") id: Long): ShiftDto

    @PATCH("api/shifts/{id}")
    suspend fun updateShift(@Path("id") id: Long, @Body body: UpdateShiftRequest): ShiftDto

    @DELETE("api/shifts/{id}")
    suspend fun deleteShift(@Path("id") id: Long)

    // ---- SwapRequest ----
    @GET("api/swap-requests")
    suspend fun getSwapRequests(
        @Query("workplaceId") workplaceId: Long,
        @Query("view") view: String,
        @Query("status") status: String? = null,
    ): List<SwapRequestDto>

    @GET("api/swap-requests/{id}")
    suspend fun getSwapRequest(@Path("id") id: Long): SwapRequestDetailDto

    @GET("api/swap-requests/{id}/applications")
    suspend fun getSwapApplications(@Path("id") id: Long): List<SwapApplicationDto>

    @POST("api/swap-requests/{id}/approve")
    suspend fun approveSwap(@Path("id") id: Long, @Body body: ApproveSwapRequest): SwapRequestDto

    @POST("api/swap-requests/{id}/reject")
    suspend fun rejectSwap(@Path("id") id: Long): SwapRequestDto

    // ---- Notice ----
    @GET("api/notices")
    suspend fun getNotices(
        @Query("workplaceId") workplaceId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<NoticeDto>

    @POST("api/notices")
    suspend fun createNotice(@Body body: CreateNoticeRequest): NoticeDto

    @GET("api/notices/{id}")
    suspend fun getNotice(@Path("id") id: Long): NoticeDto

    @DELETE("api/notices/{id}")
    suspend fun deleteNotice(@Path("id") id: Long)

    // ---- Notification ----
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("isRead") isRead: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<NotificationDto>

    @GET("api/notifications/unread-count")
    suspend fun getNotificationUnreadCount(): UnreadCount

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsRead()

    // ---- Payroll ----
    @GET("api/payroll")
    suspend fun getPayroll(
        @Query("workplaceId") workplaceId: Long,
        @Query("yearMonth") yearMonth: String,
    ): PayrollSummary

    @GET("api/payroll/weekly")
    suspend fun getWeeklyPayroll(
        @Query("workplaceId") workplaceId: Long,
        @Query("yearMonth") yearMonth: String,
    ): WeeklyPayrollSummary

    // ---- 매장 QR / 시급 ----
    @GET("api/workplaces/{id}/qr-token")
    suspend fun getQrToken(@Path("id") id: Long): QrTokenResponse

    @PATCH("api/workplaces/{wid}/members/{uid}/wage")
    suspend fun updateMemberWage(
        @Path("wid") wid: Long,
        @Path("uid") uid: Long,
        @Body body: UpdateWageRequest,
    ): UserDto

    // ---- 알림 설정 ----
    @GET("api/users/me/notification-setting")
    suspend fun getNotificationSetting(): NotificationSettingDto

    @PATCH("api/users/me/notification-setting")
    suspend fun updateNotificationSetting(@Body body: NotificationSettingUpdate): NotificationSettingDto

    // ---- 공지 첨부 업로드 ----
    @Multipart
    @POST("api/notices/attachments")
    suspend fun uploadAttachment(@Part file: MultipartBody.Part): NoticeAttachmentDto

    // ---- Device token (푸시 / FCM) ----
    @POST("api/users/me/device-tokens")
    suspend fun registerDeviceToken(@Body body: RegisterDeviceTokenRequest)

    @DELETE("api/users/me/device-tokens/{token}")
    suspend fun deleteDeviceToken(@Path("token") token: String)
}
