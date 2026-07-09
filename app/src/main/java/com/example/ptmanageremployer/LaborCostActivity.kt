package com.example.ptmanageremployer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import com.example.ptmanageremployer.data.won
import java.time.LocalDate

class LaborCostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_labor_cost)
        findViewById<View>(R.id.labor_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        loadPayroll()
    }

    private fun loadPayroll() {
        val workplaceId = TokenStore.workplaceId
        val yearMonth = LocalDate.now().toString().substring(0, 7)
        val container = findViewById<LinearLayout>(R.id.cost_container)
        val empty = findViewById<TextView>(R.id.tv_cost_empty)
        findViewById<TextView>(R.id.tv_subtitle).text = "$yearMonth 기준"
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val payroll = Network.api.getPayroll(workplaceId, yearMonth)
            findViewById<TextView>(R.id.tv_total).text = won(payroll.totalAmount)
            if (payroll.items.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            val inflater = LayoutInflater.from(this@LaborCostActivity)
            payroll.items.forEach { item ->
                val row = inflater.inflate(R.layout.item_cost_row, container, false)
                row.findViewById<TextView>(R.id.tv_name).text =
                    item.employeeName ?: "직원 #${item.employeeId}"
                val hours = (item.workedMinutes ?: 0) / 60
                row.findViewById<TextView>(R.id.tv_hours).text = "${hours}h"
                row.findViewById<TextView>(R.id.tv_amount).text = won(item.amount ?: 0)
                container.addView(row)
            }
        }
    }
}
