package com.example.ptmanageremployer

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.CreateNoticeRequest
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class NoticeWriteActivity : AppCompatActivity() {

    private val attachmentUrls = mutableListOf<String>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) upload(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_write)
        findViewById<View>(R.id.write_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val titleInput = findViewById<EditText>(R.id.input_title)
        val bodyInput = findViewById<EditText>(R.id.input_body)

        findViewById<View>(R.id.btn_attach).setOnClickListener { pickFile.launch("*/*") }

        findViewById<View>(R.id.btn_publish).setOnClickListener { btn ->
            val title = titleInput.text.toString().trim()
            val body = bodyInput.text.toString().trim()
            if (title.isEmpty() || body.isEmpty()) {
                toast("제목과 내용을 입력해 주세요.")
                return@setOnClickListener
            }
            val workplaceId = TokenStore.workplaceId
            if (workplaceId <= 0) {
                toast("소속 매장이 없습니다.")
                return@setOnClickListener
            }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    Network.api.createNotice(
                        CreateNoticeRequest(workplaceId, title, body, attachmentUrls.toList()),
                    )
                    toast("공지를 등록했어요")
                    finish()
                } catch (e: Exception) {
                    toast(e.toUserMessage())
                    btn.isEnabled = true
                }
            }
        }
    }

    /** 선택한 파일을 업로드하고 반환된 URL을 공지 첨부 목록에 추가한다. */
    private fun upload(uri: Uri) {
        val status = findViewById<TextView>(R.id.tv_attach)
        status.text = "업로드 중…"
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("파일을 읽을 수 없습니다.")
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "upload"
                val media = contentResolver.getType(uri)?.toMediaTypeOrNull()
                val part = MultipartBody.Part.createFormData("file", name, bytes.toRequestBody(media))
                val attachment = Network.api.uploadAttachment(part)
                attachment.fileUrl?.let { attachmentUrls.add(it) }
                status.text = "첨부 ${attachmentUrls.size}개"
            } catch (e: Exception) {
                status.text = e.toUserMessage()
            }
        }
    }
}
