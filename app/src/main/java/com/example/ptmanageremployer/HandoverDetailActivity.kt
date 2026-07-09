package com.example.ptmanageremployer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.handoverCategoryLabel
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

/** 인수인계 노트 상세. 목록에서 넘겨받은 값을 그대로 표시한다(별도 단건 조회 API 없음). */
class HandoverDetailActivity : AppCompatActivity() {

    private var handoverId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_handover_detail)
        findViewById<View>(R.id.detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        handoverId = intent.getLongExtra(Extras.HANDOVER_ID, -1)
        val category = intent.getStringExtra(Extras.HANDOVER_CATEGORY)
        val title = intent.getStringExtra(Extras.HANDOVER_TITLE)
        val content = intent.getStringExtra(Extras.HANDOVER_CONTENT)
        val authorId = intent.getLongExtra(Extras.HANDOVER_AUTHOR_ID, -1)
        val authorName = intent.getStringExtra(Extras.HANDOVER_AUTHOR_NAME)
        val createdAt = intent.getStringExtra(Extras.HANDOVER_CREATED_AT)

        findViewById<TextView>(R.id.tv_category).text = handoverCategoryLabel(category)
        findViewById<TextView>(R.id.tv_title).text = title ?: ""
        findViewById<TextView>(R.id.tv_meta).text =
            listOf(authorName ?: "작성자", createdAt?.take(10) ?: "").filter { it.isNotBlank() }.joinToString(" · ")
        findViewById<TextView>(R.id.tv_content).text = content ?: ""

        // 본인이 쓴 노트만 삭제 가능(서버도 동일 규칙).
        if (handoverId > 0 && authorId == TokenStore.userId) {
            findViewById<TextView>(R.id.btn_delete).apply {
                visibility = View.VISIBLE
                setOnClickListener { confirmDelete() }
            }
        }
    }

    private fun confirmDelete() {
        confirm("인수인계 삭제", "이 노트를 삭제할까요?", "삭제") {
            lifecycleScope.launch {
                runCatching { Network.api.deleteHandover(handoverId) }
                    .onSuccess {
                        toast("삭제했어요")
                        finish()
                    }
                    .onFailure {
                        toast(it.toUserMessage())
                    }
            }
        }
    }
}
