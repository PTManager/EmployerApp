package com.example.ptmanageremployer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.NotificationDto
import com.example.ptmanageremployer.data.toUserMessage

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)
        findViewById<View>(R.id.inbox_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        loadNotifications()
    }

    private fun loadNotifications() {
        val container = findViewById<LinearLayout>(R.id.noti_container)
        val empty = findViewById<TextView>(R.id.tv_noti_empty)
        launchApi {
            val items = Network.api.getNotifications(page = 0, size = 50).content
            if (items.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            val inflater = LayoutInflater.from(this@NotificationActivity)
            items.forEach { noti ->
                val row = inflater.inflate(R.layout.item_notification, container, false)
                row.findViewById<TextView>(R.id.tv_title).text = titleOf(noti)
                row.findViewById<TextView>(R.id.tv_body).text = noti.message ?: ""
                row.findViewById<TextView>(R.id.tv_time).text = formatTime(noti.createdAt)
                container.addView(row)
            }
            runCatching { Network.api.markAllNotificationsRead() }
        }
    }

    private fun titleOf(n: NotificationDto): String = when (n.type) {
        "JOIN_REQUEST" -> "🙋 가입 신청"
        "SWAP_REQUEST" -> "🔁 대타요청"
        "SWAP_APPLICATION" -> "🔁 대타 지원"
        "SWAP_RESULT" -> "🔁 대타 결과"
        "ATTENDANCE" -> "⚠️ 근태 이상"
        "SCHEDULE_CHANGED" -> "🗓 근무 편성 변경"
        "NOTICE" -> "📢 공지"
        else -> "🔔 알림"
    }

    private fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return iso.replace("T", " ").take(16)
    }
}
