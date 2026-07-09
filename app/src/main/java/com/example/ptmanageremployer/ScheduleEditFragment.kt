package com.example.ptmanageremployer

import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.CreateShiftRequest
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.ShiftDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UserDto
import com.example.ptmanageremployer.data.hoursLabel
import com.example.ptmanageremployer.data.shiftHours
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage
import com.example.ptmanageremployer.data.weekRangeLabel
import com.example.ptmanageremployer.data.won
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 스케줄 편성 탭. 한 주(월~일) 전체를 요일별로 펼쳐 보여주고, 주 단위로 이동한다.
 * - 요일 헤더의 '+ 추가'로 그날 근무를 편성(시간 프리셋 지원)
 * - '지난 주 복사'로 전주 편성을 이번 주로 복제
 * - 상단 요약: 주간 총 근무시간·예상 인건비 + 직원별 부하
 * - 신규/복사 편성은 초안이며, '발행'을 눌러야 직원에게 공개+알림된다.
 */
class ScheduleEditFragment : Fragment() {

    // 표시 중인 주의 월요일.
    private var anchorMonday: LocalDate = mondayOf(LocalDate.now())

    // 편성 대상 알바(시급 포함). loadWeek에서 캐시.
    private var members: List<UserDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_prev_week).setOnClickListener { shiftWeek(-7) }
        view.findViewById<View>(R.id.btn_next_week).setOnClickListener { shiftWeek(7) }
        view.findViewById<View>(R.id.btn_this_week).setOnClickListener {
            anchorMonday = mondayOf(LocalDate.now()); loadWeek()
        }
        view.findViewById<View>(R.id.btn_copy_week).setOnClickListener { confirmCopyLastWeek() }
        view.findViewById<View>(R.id.btn_publish).setOnClickListener { publishWeek() }
    }

    override fun onResume() {
        super.onResume()
        // 추가/수정/삭제 후 돌아왔을 때 최신 편성으로 갱신.
        loadWeek()
    }

    private fun shiftWeek(days: Long) {
        anchorMonday = anchorMonday.plusDays(days)
        loadWeek()
    }

    private fun loadWeek() {
        val view = view ?: return
        val sunday = anchorMonday.plusDays(6)
        view.findViewById<TextView>(R.id.tv_week_range).text = weekRangeLabel(anchorMonday)

        val container = view.findViewById<LinearLayout>(R.id.week_container)
        val summary = view.findViewById<TextView>(R.id.tv_week_summary)
        val empSummary = view.findViewById<LinearLayout>(R.id.emp_summary_container)
        val publishBtn = view.findViewById<TextView>(R.id.btn_publish)
        container.removeAllViews()
        empSummary.removeAllViews()

        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            summary.text = "소속 매장이 없습니다."
            return
        }

        launchApi {
            members = runCatching {
                Network.api.getMembers(workplaceId, role = "EMPLOYEE")
            }.getOrDefault(emptyList())
            val shifts = Network.api.getShifts(
                workplaceId = workplaceId,
                from = anchorMonday.toString(),
                to = sunday.toString(),
            )
            renderWeek(container, shifts)
            renderSummary(summary, empSummary, shifts)
            renderPublishButton(publishBtn, shifts)
        }
    }

    /** 월~일 7개 블록을 채운다. */
    private fun renderWeek(container: LinearLayout, shifts: List<ShiftDto>) {
        val inflater = LayoutInflater.from(requireContext())
        val byDate = shifts.groupBy { it.workDate }
        for (i in 0..6) {
            val date = anchorMonday.plusDays(i.toLong())
            val block = inflater.inflate(R.layout.item_schedule_day, container, false)
            val title = block.findViewById<TextView>(R.id.tv_day_title)
            val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            title.text = "$dow ${date.monthValue}/${date.dayOfMonth}"
            title.setTextColor(ContextCompat.getColor(requireContext(), dayTitleColor(date)))

            block.findViewById<View>(R.id.btn_day_add).setOnClickListener { showAddShiftDialog(date) }

            val dayShifts = (byDate[date.toString()] ?: emptyList()).sortedBy { it.startTime }
            val dayBox = block.findViewById<LinearLayout>(R.id.day_shifts)
            val empty = block.findViewById<TextView>(R.id.tv_day_empty)
            if (dayShifts.isEmpty()) {
                empty.visibility = View.VISIBLE
            } else {
                empty.visibility = View.GONE
                var dayHours = 0.0
                var dayCost = 0L
                dayShifts.forEach { shift ->
                    dayBox.addView(buildShiftRow(inflater, dayBox, shift))
                    dayHours += shiftHours(shift.startTime, shift.endTime)
                    dayCost += costOf(shift)
                }
                block.findViewById<TextView>(R.id.tv_day_meta).text =
                    "${hoursLabel(dayHours)} · ${won(dayCost)}"
            }
            container.addView(block)
        }
    }

    private fun buildShiftRow(inflater: LayoutInflater, parent: ViewGroup, shift: ShiftDto): View {
        val row = inflater.inflate(R.layout.item_shift_edit, parent, false)
        row.findViewById<TextView>(R.id.tv_time).text =
            shiftTimeRange(shift.startTime, shift.endTime)
        row.findViewById<TextView>(R.id.tv_worker).text =
            shift.employeeName ?: "직원 #${shift.employeeId}"

        // 초안/예정/출근을 배지로 구분. 초안은 카드도 흐리게.
        val checkedIn = shift.checkedInAt != null
        val (label, bg, fg) = when {
            !shift.published -> Triple("초안", R.color.divider, R.color.text_secondary)
            checkedIn -> Triple("출근", R.color.cat_handover_bg, R.color.cat_handover)
            else -> Triple("예정", R.color.brand_bg, R.color.brand)
        }
        val status = row.findViewById<TextView>(R.id.tv_status)
        status.text = label
        status.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bg))
        status.setTextColor(ContextCompat.getColor(requireContext(), fg))
        row.setBackgroundResource(
            if (shift.published) R.drawable.bg_schedule_active else R.drawable.bg_schedule_muted
        )

        row.setOnClickListener {
            startActivity(
                Intent(requireContext(), ShiftDetailActivity::class.java)
                    .putExtra(Extras.SHIFT_ID, shift.id)
            )
        }
        return row
    }

    /** 상단 요약: 주간 총 시간·예상 인건비 + 직원별 부하(내림차순). */
    private fun renderSummary(summary: TextView, empBox: LinearLayout, shifts: List<ShiftDto>) {
        if (shifts.isEmpty()) {
            summary.text = "이번 주 편성이 없습니다."
            return
        }
        val totalHours = shifts.sumOf { shiftHours(it.startTime, it.endTime) }
        val totalCost = shifts.sumOf { costOf(it) }
        summary.text = "이번 주 · 총 ${hoursLabel(totalHours)} · 예상 ${won(totalCost)}"

        shifts.groupBy { it.employeeId }
            .map { (id, list) ->
                val name = list.first().employeeName ?: "직원 #$id"
                val hours = list.sumOf { shiftHours(it.startTime, it.endTime) }
                Triple(name, hours, list.sumOf { costOf(it) })
            }
            .sortedByDescending { it.second }
            .forEach { (name, hours, cost) ->
                val line = TextView(requireContext()).apply {
                    text = "$name · ${hoursLabel(hours)} · ${won(cost)}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    textSize = 12f
                    val top = (6 * resources.displayMetrics.density).toInt()
                    setPadding(0, top, 0, 0)
                }
                empBox.addView(line)
            }
    }

    private fun renderPublishButton(btn: TextView, shifts: List<ShiftDto>) {
        val drafts = shifts.count { !it.published }
        btn.text = if (drafts > 0) "발행 ($drafts)" else "발행"
        btn.isEnabled = drafts > 0
        btn.alpha = if (drafts > 0) 1f else 0.4f
    }

    /** 알바 선택 → 시간 프리셋/직접입력 → 해당 날짜에 편성. */
    private fun showAddShiftDialog(date: LocalDate) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            toast("소속 매장이 없습니다.")
            return
        }
        if (members.isEmpty()) {
            toast("편성할 알바가 없습니다.")
            return
        }
        val names = members.map { it.name ?: "직원 #${it.id}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("${date.monthValue}/${date.dayOfMonth} 근무 편성 — 알바 선택")
            .setItems(names) { _, which ->
                pickTimeThenCreate(workplaceId, date, members[which].id, members[which].name ?: "알바")
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ponytail: 프리셋 시간은 하드코딩. 매장별로 바꾸려면 매장 설정에서 받아오도록 확장.
    private val presets = listOf(
        "오픈 09:00–15:00" to ("09:00:00" to "15:00:00"),
        "미들 12:00–18:00" to ("12:00:00" to "18:00:00"),
        "마감 17:00–23:00" to ("17:00:00" to "23:00:00"),
    )

    private fun pickTimeThenCreate(
        workplaceId: Long, date: LocalDate, employeeId: Long, employeeName: String,
    ) {
        val labels = (presets.map { it.first } + "직접 입력…").toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("$employeeName · 시간 선택")
            .setItems(labels) { _, which ->
                if (which < presets.size) {
                    val (start, end) = presets[which].second
                    createShift(workplaceId, date, employeeId, employeeName, start, end)
                } else {
                    pickCustomTime(workplaceId, date, employeeId, employeeName)
                }
            }
            .show()
    }

    private fun pickCustomTime(
        workplaceId: Long, date: LocalDate, employeeId: Long, employeeName: String,
    ) {
        TimePickerDialog(requireContext(), { _, startH, startM ->
            TimePickerDialog(requireContext(), { _, endH, endM ->
                val start = String.format("%02d:%02d:00", startH, startM)
                val end = String.format("%02d:%02d:00", endH, endM)
                createShift(workplaceId, date, employeeId, employeeName, start, end)
            }, 22, 0, true).apply { setTitle("종료 시간") }.show()
        }, 18, 0, true).apply { setTitle("시작 시간") }.show()
    }

    private fun createShift(
        workplaceId: Long, date: LocalDate, employeeId: Long, employeeName: String,
        start: String, end: String,
    ) {
        launchApi {
            Network.api.createShift(
                CreateShiftRequest(
                    workplaceId = workplaceId,
                    employeeId = employeeId,
                    workDate = date.toString(),
                    startTime = start,
                    endTime = end,
                )
            )
            toast("${employeeName}님 근무를 편성했어요 (${start.take(5)}–${end.take(5)})")
            loadWeek()
        }
    }

    private fun confirmCopyLastWeek() {
        requireContext().confirm(
            "지난 주 복사",
            "전주(${weekRangeLabel(anchorMonday.minusDays(7))}) 편성을 이번 주로 복사할까요? 복사본은 초안으로 추가됩니다.",
            "복사",
        ) { copyLastWeek() }
    }

    private fun copyLastWeek() {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        val prevMonday = anchorMonday.minusDays(7)
        lifecycleScope.launch {
            val source = runCatching {
                Network.api.getShifts(
                    workplaceId = workplaceId,
                    from = prevMonday.toString(),
                    to = prevMonday.plusDays(6).toString(),
                )
            }.getOrNull()
            if (source.isNullOrEmpty()) {
                toast("복사할 지난 주 편성이 없습니다.")
                return@launch
            }
            var copied = 0
            var skipped = 0
            for (shift in source) {
                val srcDate = runCatching { LocalDate.parse(shift.workDate) }.getOrNull() ?: continue
                try {
                    Network.api.createShift(
                        CreateShiftRequest(
                            workplaceId = workplaceId,
                            employeeId = shift.employeeId ?: continue,
                            workDate = srcDate.plusDays(7).toString(),
                            startTime = shift.startTime ?: continue,
                            endTime = shift.endTime ?: continue,
                        )
                    )
                    copied++
                } catch (e: Exception) {
                    // 시간 겹침(409) 등은 건너뛴다.
                    if ((e as? HttpException)?.code() == 409) skipped++ else throw e
                }
            }
            val msg = if (skipped > 0) "${copied}건 복사 (${skipped}건 건너뜀)" else "${copied}건 복사했어요"
            toast(msg)
            loadWeek()
        }
    }

    private fun publishWeek() {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        launchApi {
            val result = Network.api.publishShifts(
                workplaceId = workplaceId,
                from = anchorMonday.toString(),
                to = anchorMonday.plusDays(6).toString(),
            )
            val msg = if (result.published > 0) "${result.published}건 발행했어요" else "발행할 초안이 없습니다."
            toast(msg)
            loadWeek()
        }
    }

    private fun costOf(shift: ShiftDto): Long {
        val wage = members.find { it.id == shift.employeeId }?.hourlyWage ?: 0
        return Math.round(shiftHours(shift.startTime, shift.endTime) * wage)
    }

    private fun dayTitleColor(date: LocalDate): Int = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> R.color.weekend_blue
        DayOfWeek.SUNDAY -> R.color.weekend_red
        else -> R.color.text_primary
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}
