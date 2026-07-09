package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.NotificationSettingUpdate
import com.example.ptmanageremployer.data.Push
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import com.example.ptmanageremployer.data.won
import kotlinx.coroutines.launch
import java.time.LocalDate

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_my_name).text = "${TokenStore.name ?: "사장"}님"
        view.findViewById<TextView>(R.id.tv_my_sub).text = TokenStore.email ?: "사장"

        loadStoreSummary(view)

        view.findViewById<View>(R.id.row_profile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }
        view.findViewById<View>(R.id.row_members).setOnClickListener {
            startActivity(Intent(requireContext(), MembersActivity::class.java))
        }
        view.findViewById<View>(R.id.row_qr).setOnClickListener {
            startActivity(Intent(requireContext(), QrDisplayActivity::class.java))
        }
        view.findViewById<View>(R.id.row_noti).setOnClickListener { openNotificationSetting() }
        view.findViewById<View>(R.id.row_logout).setOnClickListener {
            lifecycleScope.launch {
                // 이 기기의 FCM 토큰을 서버에서 먼저 제거(로그인 상태에서 호출).
                runCatching { Push.currentToken()?.let { Network.api.deleteDeviceToken(it) } }
                runCatching { Network.api.logout() }
                Push.invalidateLocalToken()
                TokenStore.clear()
                val i = Intent(requireContext(), LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
            }
        }
    }

    /** 이번 주 매장 요약: 이번 주 인건비 · 알바 수 · 이번 주 근무 건수를 채운다. */
    private fun loadStoreSummary(view: View) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        val today = LocalDate.now()
        val yearMonth = today.toString().substring(0, 7)
        val weekIndex = ((today.dayOfMonth - 1) / 7).coerceAtMost(3)
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        lifecycleScope.launch {
            val cost = runCatching {
                Network.api.getWeeklyPayroll(workplaceId, yearMonth)
                    .weeks.firstOrNull { it.week == weekIndex + 1 }?.amount ?: 0L
            }.getOrDefault(0L)
            val staff = runCatching {
                Network.api.getMembers(workplaceId, role = "EMPLOYEE").size
            }.getOrDefault(0)
            val shifts = runCatching {
                Network.api.getShifts(workplaceId, from = monday.toString(), to = sunday.toString()).size
            }.getOrDefault(0)

            view.findViewById<TextView>(R.id.tv_sum_cost).text = won(cost)
            view.findViewById<TextView>(R.id.tv_sum_staff).text = "${staff}명"
            view.findViewById<TextView>(R.id.tv_sum_shifts).text = "${shifts}건"
        }
    }

    /** 알림 카테고리 on/off 를 불러와 다중 선택 다이얼로그로 수정한다. */
    private fun openNotificationSetting() {
        lifecycleScope.launch {
            val s = runCatching { Network.api.getNotificationSetting() }.getOrNull()
            if (s == null) {
                toast("알림 설정을 불러오지 못했습니다.")
                return@launch
            }
            val labels = arrayOf("대타 알림", "공지 알림", "출퇴근 알림", "가입 신청 알림")
            val checked = booleanArrayOf(
                s.swapEnabled, s.noticeEnabled, s.attendanceEnabled, s.joinRequestEnabled,
            )
            AlertDialog.Builder(requireContext())
                .setTitle("알림 설정")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setPositiveButton("저장") { _, _ ->
                    lifecycleScope.launch {
                        runCatching {
                            Network.api.updateNotificationSetting(
                                NotificationSettingUpdate(checked[0], checked[1], checked[2], checked[3]),
                            )
                        }.onSuccess {
                            toast("알림 설정을 저장했어요")
                        }.onFailure {
                            toast(it.toUserMessage())
                        }
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
