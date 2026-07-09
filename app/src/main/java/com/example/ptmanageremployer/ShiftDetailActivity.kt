package com.example.ptmanageremployer

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.ShiftDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UpdateShiftRequest
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

/** 근무 상세 화면. 근무를 수정(담당 알바·시간)하거나 삭제한다. (사장 앱) */
class ShiftDetailActivity : AppCompatActivity() {

    private var shiftId: Long = -1
    private var shift: ShiftDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shift_detail)
        findViewById<View>(R.id.shift_root).applySystemBarInsets()

        shiftId = intent.getLongExtra(Extras.SHIFT_ID, -1)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_edit).setOnClickListener { showEditShiftDialog() }
        findViewById<View>(R.id.btn_delete).setOnClickListener { confirmDelete() }

        if (shiftId > 0) loadShift()
    }

    private fun loadShift() {
        launchApi {
            val s = Network.api.getShift(shiftId).also { shift = it }
            findViewById<TextView>(R.id.tv_date).text = s.workDate ?: ""
            findViewById<TextView>(R.id.tv_time).text = shiftTimeRange(s.startTime, s.endTime)
            findViewById<TextView>(R.id.tv_worker).text =
                s.employeeName ?: "직원 #${s.employeeId}"
        }
    }

    private fun confirmDelete() {
        val s = shift ?: return
        confirm("근무 삭제", "${s.employeeName ?: "직원"}님의 ${shiftTimeRange(s.startTime, s.endTime)} 근무를 삭제할까요?", "삭제") {
            lifecycleScope.launch {
                runCatching { Network.api.deleteShift(shiftId) }
                    .onSuccess {
                        toast("근무를 삭제했어요")
                        finish()
                    }
                    .onFailure {
                        toast(it.toUserMessage())
                    }
            }
        }
    }

    /** 근무 수정: 배정 알바 변경(선택) → 시작/종료 시간 변경 → PATCH /api/shifts/{id}. */
    private fun showEditShiftDialog() {
        val s = shift ?: return
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        lifecycleScope.launch {
            val members = runCatching {
                Network.api.getMembers(workplaceId, role = "EMPLOYEE")
            }.getOrNull()
            if (members.isNullOrEmpty()) {
                toast("편성할 알바가 없습니다.")
                return@launch
            }
            val names = members.map { it.name ?: "직원 #${it.id}" }.toTypedArray()
            val currentIdx = members.indexOfFirst { it.id == s.employeeId }.coerceAtLeast(0)
            AlertDialog.Builder(this@ShiftDetailActivity)
                .setTitle("근무 수정 — 알바 선택")
                .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                    dialog.dismiss()
                    editTimeThenUpdate(members[which].id)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun editTimeThenUpdate(employeeId: Long) {
        val s = shift ?: return
        val (sh, sm) = hourMinuteOf(s.startTime, 18, 0)
        val (eh, em) = hourMinuteOf(s.endTime, 22, 0)
        TimePickerDialog(this, { _, startH, startM ->
            TimePickerDialog(this, { _, endH, endM ->
                val start = String.format("%02d:%02d:00", startH, startM)
                val end = String.format("%02d:%02d:00", endH, endM)
                launchApi {
                    Network.api.updateShift(
                        shiftId,
                        UpdateShiftRequest(
                            employeeId = employeeId,
                            workDate = s.workDate,
                            startTime = start,
                            endTime = end,
                        ),
                    )
                    toast("근무를 수정했어요")
                    loadShift()
                }
            }, eh, em, true).apply { setTitle("종료 시간") }.show()
        }, sh, sm, true).apply { setTitle("시작 시간") }.show()
    }

    /** "HH:mm:ss" → (시, 분). 파싱 실패 시 기본값. */
    private fun hourMinuteOf(time: String?, defH: Int, defM: Int): Pair<Int, Int> {
        val parts = time?.split(":") ?: return defH to defM
        val h = parts.getOrNull(0)?.toIntOrNull() ?: defH
        val m = parts.getOrNull(1)?.toIntOrNull() ?: defM
        return h to m
    }
}
