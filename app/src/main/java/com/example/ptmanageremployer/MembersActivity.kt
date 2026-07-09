package com.example.ptmanageremployer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.DecisionRequest
import com.example.ptmanageremployer.data.JoinRequestDto
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UserDto
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class MembersActivity : AppCompatActivity() {

    private val workplaceId by lazy { TokenStore.workplaceId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_members)
        findViewById<View>(R.id.members_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        if (workplaceId <= 0) {
            toast("소속 매장이 없습니다.")
            return
        }
        loadInvite()
        loadPending()
        loadMembers()
    }

    /** 매장 초대코드를 조회해 표시하고 복사/공유 버튼을 연결한다. */
    private fun loadInvite() {
        val codeView = findViewById<TextView>(R.id.tv_invite_code)
        lifecycleScope.launch {
            val workplace = runCatching { Network.api.getWorkplace(workplaceId) }.getOrNull()
            val code = workplace?.inviteCode
            if (code.isNullOrBlank()) {
                codeView.text = "불러오지 못했어요"
                return@launch
            }
            codeView.text = code
            findViewById<View>(R.id.btn_copy_invite).setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("초대코드", code))
                toast("초대코드를 복사했어요")
            }
            findViewById<View>(R.id.btn_share_invite).setOnClickListener {
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
        }
    }

    private fun loadPending() {
        val container = findViewById<LinearLayout>(R.id.pending_container)
        val countLabel = findViewById<TextView>(R.id.tv_pending_count)
        launchApi {
            val requests = Network.api.getJoinRequests(workplaceId, status = "PENDING")
            countLabel.text = "가입 승인 대기 ${requests.size}"
            container.removeAllViews()
            val inflater = LayoutInflater.from(this@MembersActivity)
            requests.forEach { req ->
                val card = inflater.inflate(R.layout.item_join_request, container, false)
                card.findViewById<TextView>(R.id.tv_name).text = req.userName ?: "신청자 #${req.userId}"
                card.findViewById<TextView>(R.id.tv_sub).text = "매장 참여 신청"
                card.findViewById<View>(R.id.btn_approve).setOnClickListener {
                    decide(req, "APPROVE", card, container, countLabel)
                }
                card.findViewById<View>(R.id.btn_reject).setOnClickListener {
                    decide(req, "REJECT", card, container, countLabel)
                }
                container.addView(card)
            }
        }
    }

    private fun decide(
        req: JoinRequestDto,
        decision: String,
        card: View,
        container: LinearLayout,
        countLabel: TextView,
    ) {
        card.isEnabled = false
        lifecycleScope.launch {
            try {
                Network.api.decideJoinRequest(req.id, DecisionRequest(decision))
                container.removeView(card)
                countLabel.text = "가입 승인 대기 ${container.childCount}"
                toast(if (decision == "APPROVE") "가입을 승인했어요" else "가입을 거절했어요")
                if (decision == "APPROVE") loadMembers()
            } catch (e: Exception) {
                toast(e.toUserMessage())
                card.isEnabled = true
            }
        }
    }

    private fun loadMembers() {
        val container = findViewById<LinearLayout>(R.id.members_container)
        val countLabel = findViewById<TextView>(R.id.tv_members_count)
        launchApi {
            val members = Network.api.getMembers(workplaceId)
            countLabel.text = "멤버 ${members.size}명"
            container.removeAllViews()
            val inflater = LayoutInflater.from(this@MembersActivity)
            members.forEach { member ->
                val row = inflater.inflate(R.layout.item_member, container, false)
                row.findViewById<TextView>(R.id.tv_name).text = member.name ?: "이름 없음"
                val sub = roleLabel(member)
                if (member.role == "EMPLOYEE") {
                    row.findViewById<TextView>(R.id.tv_sub).text = "$sub · 시급 ${member.hourlyWage ?: 0}원"
                } else {
                    row.findViewById<TextView>(R.id.tv_sub).text = sub
                }
                // 자기 자신은 내보낼 수 없다.
                val deleteBtn = row.findViewById<TextView>(R.id.btn_delete)
                if (member.id != TokenStore.userId) {
                    deleteBtn.visibility = View.VISIBLE
                    deleteBtn.setOnClickListener { promptRemove(member) }
                }
                container.addView(row)
            }
        }
    }

    /** 멤버를 매장에서 내보낸다. (계정은 유지, 매장 소속만 해제) */
    private fun promptRemove(member: UserDto) {
        confirm("멤버 내보내기", "${member.name ?: "이 멤버"}님을 매장에서 내보낼까요?", "내보내기") {
            launchApi {
                Network.api.removeMember(workplaceId, member.id)
                toast("내보냈어요")
                loadMembers()
            }
        }
    }

    private fun roleLabel(user: UserDto): String {
        val base = when (user.role) {
            "EMPLOYER" -> "사장님"
            "EMPLOYEE" -> "알바"
            else -> user.email ?: ""
        }
        return if (user.id == TokenStore.userId) "$base · 나" else base
    }
}
