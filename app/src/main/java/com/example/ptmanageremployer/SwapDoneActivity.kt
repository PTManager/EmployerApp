package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.SwapRequestDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage

/**
 * 대타요청 화면. view=all 로 전체를 받아 대기 중(PENDING)은 상단에 탭 가능한 카드로,
 * 처리 완료(승인/거절)는 아래에 읽기 전용으로 나눠 표시한다.
 * 대기 카드를 탭하면 승인 화면(SubApprovalActivity)으로 이동한다.
 */
class SwapDoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swap_done)
        findViewById<View>(R.id.swap_done_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.swap_done_container)
        val empty = findViewById<TextView>(R.id.tv_swap_done_empty)
        container.removeAllExcept(R.id.tv_swap_done_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val all = Network.api.getSwapRequests(workplaceId, view = "all")
            val pending = all.filter { it.status == "PENDING" }
            val done = all.filter { it.status == "APPROVED" || it.status == "REJECTED" }
            empty.visibility = if (pending.isEmpty() && done.isEmpty()) View.VISIBLE else View.GONE
            val inflater = LayoutInflater.from(this@SwapDoneActivity)
            // 대기 중(직원 대타요청) — 탭하면 승인 화면으로.
            pending.forEach { req ->
                val card = inflater.inflate(R.layout.item_swap_request, container, false)
                card.findViewById<TextView>(R.id.tv_title).text = shiftTitle(req)
                card.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
                card.findViewById<TextView>(R.id.tv_badge).text = "지원자 확인 →"
                card.setOnClickListener {
                    startActivity(
                        Intent(this@SwapDoneActivity, SubApprovalActivity::class.java)
                            .putExtra(Extras.SWAP_REQUEST_ID, req.id)
                    )
                }
                container.addView(card)
            }
            // 처리 완료(승인/거절) — 읽기 전용.
            done.forEach { req ->
                val card = inflater.inflate(R.layout.item_swap_request, container, false)
                card.findViewById<TextView>(R.id.tv_title).text = shiftTitle(req)
                card.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
                val badge = card.findViewById<TextView>(R.id.tv_badge)
                badge.text = statusLabel(req.status)
                badge.setTextColor(
                    ContextCompat.getColor(
                        this@SwapDoneActivity,
                        if (req.status == "APPROVED") R.color.accent_green else R.color.accent_red,
                    )
                )
                container.addView(card)
            }
        }
    }

    private fun shiftTitle(req: SwapRequestDto): String {
        val shift = req.shift
        return if (shift != null) {
            "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}".trim()
        } else {
            "대타요청 #${req.id}"
        }
    }

    private fun statusLabel(status: String?): String = when (status) {
        "APPROVED" -> "승인"
        "REJECTED" -> "거절"
        else -> status ?: ""
    }
}
