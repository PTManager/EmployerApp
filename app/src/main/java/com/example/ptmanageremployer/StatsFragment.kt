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
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.WeeklyCost
import com.example.ptmanageremployer.data.won
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatsFragment : Fragment() {

    // 주차별 막대: 인덱스 0~3 = 1주~4주
    private val barIds = intArrayOf(R.id.bar_w1, R.id.bar_w2, R.id.bar_w3, R.id.bar_w4)
    private val weekLabelIds = intArrayOf(R.id.tv_w1, R.id.tv_w2, R.id.tv_w3, R.id.tv_w4)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 인건비 상세는 별도 화면에서 제공한다.
        view.findViewById<View>(R.id.tv_month_total).setOnClickListener {
            startActivity(Intent(requireContext(), LaborCostActivity::class.java))
        }
        loadStats(view)
    }

    private fun loadStats(view: View) {
        val today = LocalDate.now()
        val yearMonth = today.toString().substring(0, 7)
        view.findViewById<TextView>(R.id.tv_month_selector).text = "${today.monthValue}월 ▾"
        view.findViewById<TextView>(R.id.tv_month_label).text = "$yearMonth 실근태 기준"

        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return

        loadPayroll(view, workplaceId, yearMonth)
        loadWeekly(view, workplaceId, yearMonth, currentWeekIndex(today))
    }

    private fun loadPayroll(view: View, workplaceId: Long, yearMonth: String) {
        val container = view.findViewById<LinearLayout>(R.id.cost_container)
        val empty = view.findViewById<TextView>(R.id.tv_cost_empty)
        lifecycleScope.launch {
            val payroll = runCatching { Network.api.getPayroll(workplaceId, yearMonth) }.getOrNull()
                ?: return@launch
            view.findViewById<TextView>(R.id.tv_month_total).text = won(payroll.totalAmount)

            // 플레이스홀더/이전 결과 제거 후 실제 항목으로 채운다. (empty 뷰는 유지)
            for (i in container.childCount - 1 downTo 0) {
                if (container.getChildAt(i).id != R.id.tv_cost_empty) container.removeViewAt(i)
            }
            if (payroll.items.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launch
            }
            empty.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            payroll.items.forEach { item ->
                val row = inflater.inflate(R.layout.item_cost_row, container, false)
                row.findViewById<TextView>(R.id.tv_name).text =
                    item.employeeName ?: "직원 #${item.employeeId}"
                row.findViewById<TextView>(R.id.tv_hours).text = "${(item.workedMinutes ?: 0) / 60}h"
                row.findViewById<TextView>(R.id.tv_amount).text = won(item.amount ?: 0)
                container.addView(row)
            }
        }
    }

    private fun loadWeekly(view: View, workplaceId: Long, yearMonth: String, activeWeek: Int) {
        lifecycleScope.launch {
            val weekly = runCatching { Network.api.getWeeklyPayroll(workplaceId, yearMonth) }.getOrNull()
                ?: return@launch
            renderWeeklyChart(view, weekly.weeks, activeWeek)
        }
    }

    private fun renderWeeklyChart(view: View, weeks: List<WeeklyCost>, activeWeek: Int) {
        // week 값으로 정렬해 1~4주 순서를 보장한다.
        val amounts = LongArray(4)
        weeks.forEach { if (it.week in 1..4) amounts[it.week - 1] = it.amount }
        val max = amounts.max().coerceAtLeast(1L)
        val density = resources.displayMetrics.density

        for (i in 0..3) {
            val heightDp = MIN_BAR_DP + (MAX_BAR_DP - MIN_BAR_DP) * (amounts[i].toFloat() / max)
            val bar = view.findViewById<View>(barIds[i])
            bar.layoutParams = bar.layoutParams.apply { height = (heightDp * density).toInt() }
            val active = i == activeWeek
            bar.setBackgroundResource(if (active) R.drawable.bg_bar_active else R.drawable.bg_bar)
            view.findViewById<TextView>(weekLabelIds[i]).setTextColor(
                ContextCompat.getColor(requireContext(), if (active) R.color.brand else R.color.text_hint),
            )
        }
    }

    /** 백엔드와 동일한 버킷: 1–7→0, 8–14→1, 15–21→2, 22–말일→3 */
    private fun currentWeekIndex(date: LocalDate): Int =
        ((date.dayOfMonth - 1) / 7).coerceAtMost(3)

    companion object {
        private const val MIN_BAR_DP = 6f
        private const val MAX_BAR_DP = 68f
    }
}
