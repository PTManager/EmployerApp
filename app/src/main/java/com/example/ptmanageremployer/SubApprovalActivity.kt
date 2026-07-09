package com.example.ptmanageremployer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployer.data.ApproveSwapRequest
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.SwapApplicationDto
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage

class SubApprovalActivity : AppCompatActivity() {

    private var swapRequestId: Long = -1
    private var applicants: List<SwapApplicationDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sub_approval)
        findViewById<View>(R.id.approval_root).applySystemBarInsets()

        swapRequestId = intent.getLongExtra(Extras.SWAP_REQUEST_ID, -1)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_approve).setOnClickListener { approve() }
        findViewById<View>(R.id.btn_reject).setOnClickListener { reject() }

        if (swapRequestId > 0) loadDetail()
    }

    private fun loadDetail() {
        launchApi {
            val detail = Network.api.getSwapRequest(swapRequestId)
            applicants = detail.applications.orEmpty()
            findViewById<TextView>(R.id.tv_req_title).text = "대타요청 #${detail.id}"
            findViewById<TextView>(R.id.tv_shift).text = detail.shift?.let {
                "${it.workDate ?: ""} ${shiftTimeRange(it.startTime, it.endTime)}"
            } ?: "근무 정보 없음"
            findViewById<TextView>(R.id.tv_reason).text = "사유 · ${detail.reason ?: "없음"}"
            val names = applicants.mapNotNull { app ->
                app.applicantName ?: app.applicantId?.let { "지원자 #$it" }
            }
            findViewById<TextView>(R.id.tv_applicants).text =
                if (names.isEmpty()) "없음" else "${names.joinToString(", ")} (${names.size}명)"
        }
    }

    /** 첫 번째 지원자를 대체 근무자로 승인한다. */
    private fun approve() {
        val applicantId = applicants.firstOrNull()?.applicantId
        if (applicantId == null) {
            toast("지원자가 없어 승인할 수 없습니다.")
            return
        }
        launchApi {
            Network.api.approveSwap(swapRequestId, ApproveSwapRequest(applicantId))
            toast("대타요청을 승인했어요")
            finish()
        }
    }

    private fun reject() {
        launchApi {
            Network.api.rejectSwap(swapRequestId)
            toast("대타요청을 거절했어요")
            finish()
        }
    }
}
