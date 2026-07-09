package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.SwapRequestDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.handoverCategoryLabel
import com.example.ptmanageremployer.data.handoverMeta
import com.example.ptmanageremployer.data.noticeMeta
import com.example.ptmanageremployer.data.toUserMessage

class SubFragment : Fragment() {

    // 소통 탭은 각 구역의 가장 최근 1건만 미리보기로 노출한다.
    private val previewCount = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 공지 헤더 탭 → 공지 목록(작성/조회/삭제는 목록 화면 내부에 있음).
        view.findViewById<View>(R.id.btn_notice_header).setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_handover_header).setOnClickListener {
            startActivity(Intent(requireContext(), HandoverListActivity::class.java))
        }
        // 대타 헤더 탭 → 처리 완료된 대타 목록. 아래에는 대기 중(직원 요청)을 인라인 노출.
        view.findViewById<View>(R.id.btn_swap_header).setOnClickListener {
            startActivity(Intent(requireContext(), SwapDoneActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            loadNotices(it)
            loadHandovers(it)
            loadPending(it)
        }
    }

    private fun loadNotices(root: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = root.findViewById<LinearLayout>(R.id.panel_notice)
        val empty = root.findViewById<TextView>(R.id.tv_notice_empty)
        panel.removeAllExcept(R.id.tv_notice_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notices = Network.api.getNotices(workplaceId, page = 0, size = 50).content
            empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
            val inflater = LayoutInflater.from(requireContext())
            notices.take(previewCount).forEach { notice ->
                val card = inflater.inflate(R.layout.item_notice, panel, false)
                card.findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                card.findViewById<TextView>(R.id.tv_content).apply {
                    maxLines = 1
                    text = firstLine(notice.body)
                }
                card.findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                // 미리보기라 삭제는 목록 화면에서만.
                card.findViewById<View>(R.id.btn_delete).visibility = View.GONE
                card.findViewById<View>(R.id.notice_body).setOnClickListener {
                    startActivity(
                        Intent(requireContext(), NoticeDetailActivity::class.java)
                            .putExtra(Extras.NOTICE_ID, notice.id)
                    )
                }
                panel.addView(card)
            }
        }
    }

    private fun loadHandovers(root: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = root.findViewById<LinearLayout>(R.id.panel_handover)
        val empty = root.findViewById<TextView>(R.id.tv_handover_empty)
        panel.removeAllExcept(R.id.tv_handover_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notes = Network.api.getHandovers(workplaceId, null)
            empty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            val inflater = LayoutInflater.from(requireContext())
            notes.take(previewCount).forEach { note ->
                val card = inflater.inflate(R.layout.item_handover, panel, false)
                card.findViewById<TextView>(R.id.tv_category).text = handoverCategoryLabel(note.category)
                card.findViewById<TextView>(R.id.tv_title).text = note.title ?: ""
                card.findViewById<TextView>(R.id.tv_content).apply {
                    maxLines = 1
                    text = firstLine(note.content)
                }
                card.findViewById<TextView>(R.id.tv_meta).text = handoverMeta(note)
                // 미리보기라 삭제는 목록 화면에서만 — btn_delete 는 기본 gone 유지.
                card.setOnClickListener {
                    startActivity(Intent(requireContext(), HandoverListActivity::class.java))
                }
                panel.addView(card)
            }
        }
    }

    /** 미리보기 한 줄용: 줄바꿈을 공백으로 접어 첫 줄만(넘치면 …) 보이게 한다. */
    private fun firstLine(text: String?): String =
        (text ?: "").replace("\n", " ").replace("\r", " ").trim()

    /** 대기 중 대타요청 중 지원자 있는 것/없는 것을 각각 최근 1건씩 노출한다. */
    private fun loadPending(root: View) {
        val workplaceId = TokenStore.workplaceId
        val container = root.findViewById<LinearLayout>(R.id.panel_pending)
        val empty = root.findViewById<TextView>(R.id.tv_pending_empty)
        if (workplaceId <= 0) return
        launchApi {
            // 최신순(서버 정렬) 대기 요청을 훑어 지원자 유/무 각 1건을 고른다.
            // ponytail: 요청마다 지원자 목록을 조회. 스캔이 많아지면 목록 응답에 applicantCount 추가.
            val pending = Network.api.getSwapRequests(workplaceId, view = "pending")
            var withApp: Pair<SwapRequestDto, Int>? = null
            var without: SwapRequestDto? = null
            for (req in pending) {
                if (withApp != null && without != null) break
                val count = runCatching { Network.api.getSwapApplications(req.id).size }.getOrDefault(0)
                if (count > 0) { if (withApp == null) withApp = req to count }
                else if (without == null) without = req
            }
            // 빈 상태 뷰(인덱스 0)만 남기고 이전 카드 제거
            if (container.childCount > 1) container.removeViews(1, container.childCount - 1)
            if (withApp == null && without == null) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            empty.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            withApp?.let { (req, count) ->
                addSwapCaption(container, "지원자가 있는 대타요청")
                addPendingCard(container, inflater, req, "지원자 ${count}명 →")
            }
            without?.let { req ->
                addSwapCaption(container, "지원자가 없는 대타요청")
                addPendingCard(container, inflater, req, "지원자 없음")
            }
        }
    }

    private fun addPendingCard(
        container: LinearLayout,
        inflater: LayoutInflater,
        req: SwapRequestDto,
        badge: String,
    ) {
        val card = inflater.inflate(R.layout.item_swap_request, container, false)
        card.findViewById<TextView>(R.id.tv_title).text = "대타요청 #${req.id}"
        card.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
        card.findViewById<TextView>(R.id.tv_badge).text = badge
        card.setOnClickListener {
            startActivity(
                Intent(requireContext(), SubApprovalActivity::class.java)
                    .putExtra(Extras.SWAP_REQUEST_ID, req.id)
            )
        }
        container.addView(card)
    }

    /** 구역 안내 캡션(예: "지원자가 있는 대타요청")을 패널에 추가한다. */
    private fun addSwapCaption(container: LinearLayout, text: String) {
        val caption = TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_swap))
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val top = (14 * resources.displayMetrics.density).toInt()
            setPadding(0, top, 0, 0)
        }
        container.addView(caption)
    }
}
