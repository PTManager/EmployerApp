package com.example.ptmanageremployer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.NotificationSettingUpdate
import com.example.ptmanageremployer.data.Push
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_my_name).text = "${TokenStore.name ?: "사장"}님"
        view.findViewById<TextView>(R.id.tv_my_sub).text = TokenStore.email ?: "사장"

        view.findViewById<View>(R.id.row_profile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }
        view.findViewById<View>(R.id.row_labor).setOnClickListener {
            startActivity(Intent(requireContext(), LaborCostActivity::class.java))
        }
        view.findViewById<View>(R.id.row_members).setOnClickListener {
            startActivity(Intent(requireContext(), MembersActivity::class.java))
        }
        view.findViewById<View>(R.id.row_invite).setOnClickListener { showInviteCode() }
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

    /** 매장 초대코드를 조회(GET /api/workplaces/{id})해 표시하고 복사/공유를 제공한다. */
    private fun showInviteCode() {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            Toast.makeText(requireContext(), "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val workplace = runCatching { Network.api.getWorkplace(workplaceId) }.getOrNull()
            val code = workplace?.inviteCode
            if (code.isNullOrBlank()) {
                Toast.makeText(requireContext(), "초대코드를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(requireContext())
                .setTitle("매장 초대코드")
                .setMessage("알바에게 이 코드를 공유하세요.\n\n$code")
                .setPositiveButton("복사") { _, _ ->
                    val clipboard =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("초대코드", code))
                    Toast.makeText(requireContext(), "초대코드를 복사했어요", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("공유") { _, _ ->
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "[${workplace.name ?: "매장"}] 초대코드: $code\n" +
                                "PTManager 직원 앱에서 이 코드로 매장에 참여하세요.",
                        )
                    }
                    startActivity(Intent.createChooser(share, "초대코드 공유"))
                }
                .setNegativeButton("닫기", null)
                .show()
        }
    }

    /** 알림 카테고리 on/off 를 불러와 다중 선택 다이얼로그로 수정한다. */
    private fun openNotificationSetting() {
        lifecycleScope.launch {
            val s = runCatching { Network.api.getNotificationSetting() }.getOrNull()
            if (s == null) {
                Toast.makeText(requireContext(), "알림 설정을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(requireContext(), "알림 설정을 저장했어요", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
