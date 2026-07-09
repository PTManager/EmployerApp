package com.example.ptmanageremployer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.ShiftDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.attendanceLabel
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage
import java.time.LocalDate

class ShiftAttendanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shift_attendance)
        findViewById<View>(R.id.att_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        loadAttendance()
    }

    private fun loadAttendance() {
        val workplaceId = TokenStore.workplaceId
        val today = LocalDate.now().toString()
        val container = findViewById<LinearLayout>(R.id.att_container)
        val empty = findViewById<TextView>(R.id.tv_att_empty)
        findViewById<TextView>(R.id.tv_date).text = "$today · 우리 매장"
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val shifts = Network.api.getShifts(workplaceId = workplaceId, from = today, to = today)
            findViewById<TextView>(R.id.tv_present).text =
                shifts.count { it.attendanceStatus == "PRESENT" }.toString()
            findViewById<TextView>(R.id.tv_late).text =
                shifts.count { it.attendanceStatus == "LATE" }.toString()
            findViewById<TextView>(R.id.tv_absent).text =
                shifts.count { it.attendanceStatus == "ABSENT" }.toString()
            if (shifts.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            val inflater = LayoutInflater.from(this@ShiftAttendanceActivity)
            shifts.forEach { shift ->
                val row = inflater.inflate(R.layout.item_attendance, container, false)
                row.findViewById<TextView>(R.id.tv_name).text =
                    shift.employeeName ?: "직원 #${shift.employeeId}"
                row.findViewById<TextView>(R.id.tv_sub).text = subOf(shift)
                row.findViewById<TextView>(R.id.tv_tag).text =
                    attendanceLabel(shift.attendanceStatus)
                container.addView(row)
            }
        }
    }

    private fun subOf(shift: ShiftDto): String {
        val range = shiftTimeRange(shift.startTime, shift.endTime)
        val checkedIn = shift.checkedInAt?.let { "출근 ${it.replace("T", " ").take(16)}" } ?: "출근 기록 없음"
        return "$range · $checkedIn"
    }
}
