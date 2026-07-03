package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.HandoverDto
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.handoverCategoryLabel
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

/** 인수인계 노트 목록. 카테고리 필터 + 작성 진입 + 본인 노트 삭제. */
class HandoverListActivity : AppCompatActivity() {

    // 선택된 카테고리 코드(null = 전체).
    private var selectedCategory: String? = null

    // 칩 뷰 id → 카테고리 코드(전체는 null).
    private val chips: List<Pair<Int, String?>> by lazy {
        listOf(
            R.id.chip_all to null,
            R.id.chip_stock to "STOCK",
            R.id.chip_device to "DEVICE",
            R.id.chip_customer to "CUSTOMER",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_handover_list)
        findViewById<View>(R.id.handover_list_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_write).setOnClickListener {
            startActivity(Intent(this, HandoverWriteActivity::class.java))
        }
        chips.forEach { (id, category) ->
            findViewById<TextView>(id).setOnClickListener {
                selectedCategory = category
                renderChips()
                loadHandovers()
            }
        }
        renderChips()
    }

    override fun onResume() {
        super.onResume()
        // 작성/삭제 후 돌아왔을 때 갱신.
        loadHandovers()
    }

    private fun renderChips() {
        chips.forEach { (id, category) ->
            val selected = category == selectedCategory
            findViewById<TextView>(id).apply {
                setBackgroundResource(
                    if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_outline,
                )
                setTextColor(getColor(if (selected) R.color.brand else R.color.text_secondary))
            }
        }
    }

    private fun loadHandovers() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.handover_container)
        val empty = findViewById<TextView>(R.id.tv_handover_empty)
        // 기존 행 제거(빈 상태 뷰는 유지).
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.tv_handover_empty) container.removeViewAt(i)
        }
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notes = Network.api.getHandovers(workplaceId, selectedCategory)
                if (notes.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                empty.visibility = View.GONE
                val inflater = LayoutInflater.from(this@HandoverListActivity)
                notes.forEach { note ->
                    val card = inflater.inflate(R.layout.item_handover, container, false)
                    card.findViewById<TextView>(R.id.tv_category).text = handoverCategoryLabel(note.category)
                    card.findViewById<TextView>(R.id.tv_content).text = note.content ?: ""
                    card.findViewById<TextView>(R.id.tv_meta).text = handoverMeta(note)
                    val delete = card.findViewById<View>(R.id.btn_delete)
                    // 본인이 쓴 노트만 삭제 가능(서버도 동일 규칙).
                    if (note.authorId == TokenStore.userId) {
                        delete.visibility = View.VISIBLE
                        delete.setOnClickListener { confirmDelete(note) }
                    }
                    container.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(this@HandoverListActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(note: HandoverDto) {
        AlertDialog.Builder(this)
            .setTitle("인수인계 삭제")
            .setMessage("이 노트를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                lifecycleScope.launch {
                    runCatching { Network.api.deleteHandover(note.id) }
                        .onSuccess {
                            Toast.makeText(this@HandoverListActivity, "삭제했어요", Toast.LENGTH_SHORT).show()
                            loadHandovers()
                        }
                        .onFailure {
                            Toast.makeText(this@HandoverListActivity, it.toUserMessage(), Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun handoverMeta(note: HandoverDto): String {
        val author = note.authorName ?: "작성자"
        val date = note.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
