package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.won
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun goTab(itemId: Int) {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.selectedItemId = itemId
        }

        view.findViewById<View>(R.id.btn_bell).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
        view.findViewById<View>(R.id.card_approval).setOnClickListener { goTab(R.id.nav_approval) }
        view.findViewById<View>(R.id.card_labor).setOnClickListener { goTab(R.id.nav_stats) }
        view.findViewById<View>(R.id.btn_make_schedule).setOnClickListener { goTab(R.id.nav_schedule) }
        view.findViewById<View>(R.id.btn_write_notice).setOnClickListener {
            startActivity(Intent(requireContext(), NoticeWriteActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_notice_list).setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_handover_list).setOnClickListener {
            startActivity(Intent(requireContext(), HandoverListActivity::class.java))
        }

        loadDashboard(view)
    }

    private fun loadDashboard(view: View) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        val today = LocalDate.now().toString()
        val yearMonth = today.substring(0, 7)

        lifecycleScope.launch {
            runCatching { Network.api.getWorkplace(workplaceId) }.getOrNull()?.let {
                view.findViewById<TextView>(R.id.tv_store_name).text = it.name ?: "매장"
            }
            // 오늘 근무 인원
            runCatching { Network.api.getShifts(workplaceId = workplaceId, from = today, to = today) }
                .getOrNull()?.let { shifts ->
                    view.findViewById<TextView>(R.id.tv_working_count).text = "${shifts.size}명"
                }
            // 승인 대기 = 가입 신청 + 대타 승인 대기
            val pendingJoins = runCatching {
                Network.api.getJoinRequests(workplaceId, status = "PENDING").size
            }.getOrDefault(0)
            val pendingSwaps = runCatching {
                Network.api.getSwapRequests(workplaceId, view = "pending").size
            }.getOrDefault(0)
            view.findViewById<TextView>(R.id.tv_approval_count).text = "${pendingJoins + pendingSwaps}건"
            // 이번 달 인건비
            runCatching { Network.api.getPayroll(workplaceId, yearMonth) }.getOrNull()?.let {
                view.findViewById<TextView>(R.id.tv_month_labor).text = won(it.totalAmount)
                view.findViewById<TextView>(R.id.tv_today_labor).text = "—"
            }
            // 안 읽은 알림 개수(GET /api/notifications/unread-count) → 종 아이콘 빨간 점.
            val unread = runCatching { Network.api.getNotificationUnreadCount().count }.getOrDefault(0)
            view.findViewById<View>(R.id.bell_dot).visibility =
                if (unread > 0) View.VISIBLE else View.GONE
        }
    }
}
