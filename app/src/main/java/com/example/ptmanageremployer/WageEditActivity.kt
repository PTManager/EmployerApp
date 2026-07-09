package com.example.ptmanageremployer

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UpdateWageRequest
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

/** 직원 시급 설정 화면. 통계 탭의 직원별 인건비 행에서 진입한다. */
class WageEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wage_edit)
        findViewById<View>(R.id.wage_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val employeeId = intent.getLongExtra(EXTRA_EMPLOYEE_ID, -1L)
        val workplaceId = TokenStore.workplaceId
        if (employeeId <= 0 || workplaceId <= 0) {
            finish()
            return
        }
        val name = intent.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: "직원"
        val wage = intent.getIntExtra(EXTRA_HOURLY_WAGE, 0)
        val minutes = intent.getIntExtra(EXTRA_WORKED_MINUTES, 0)

        findViewById<TextView>(R.id.tv_emp_name).text = name
        findViewById<TextView>(R.id.tv_emp_hours).text = "해당 월 ${minutes / 60}시간 근무"
        val input = findViewById<EditText>(R.id.input_wage).apply { setText(wage.toString()) }

        findViewById<TextView>(R.id.btn_save).setOnClickListener { btn ->
            val value = input.text.toString().toIntOrNull()
            if (value == null || value < 0) {
                toast("올바른 금액을 입력해 주세요.")
                return@setOnClickListener
            }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    Network.api.updateMemberWage(workplaceId, employeeId, UpdateWageRequest(value))
                    toast("시급을 저장했어요")
                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    toast(e.toUserMessage())
                    btn.isEnabled = true
                }
            }
        }
    }

    companion object {
        const val EXTRA_EMPLOYEE_ID = "employeeId"
        const val EXTRA_EMPLOYEE_NAME = "employeeName"
        const val EXTRA_HOURLY_WAGE = "hourlyWage"
        const val EXTRA_WORKED_MINUTES = "workedMinutes"
    }
}
