package com.example.ptmanageremployer

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.NoticeAttachmentDto
import com.example.ptmanageremployer.data.NoticeDto
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class NoticeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_detail)
        findViewById<View>(R.id.detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val noticeId = intent.getLongExtra(Extras.NOTICE_ID, -1)
        if (noticeId <= 0) {
            toast("공지를 찾을 수 없습니다.")
            finish()
            return
        }
        loadNotice(noticeId)
    }

    private fun loadNotice(noticeId: Long) {
        lifecycleScope.launch {
            try {
                val notice = Network.api.getNotice(noticeId)
                findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                findViewById<TextView>(R.id.tv_body).text = notice.body ?: ""
                renderAttachments(notice.attachments.orEmpty())
            } catch (e: Exception) {
                toast(e.toUserMessage())
                finish()
            }
        }
    }

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    /** 공지 첨부파일을 상세 화면에 그린다. 이미지면 썸네일을, 그 외엔 파일명 링크를 표시한다. */
    private fun renderAttachments(attachments: List<NoticeAttachmentDto>) {
        val container = findViewById<LinearLayout>(R.id.attachments_container)
        container.removeAllViews()
        attachments.forEach { attachment ->
            val fileUrl = attachment.fileUrl ?: return@forEach
            val fileName = Uri.parse(fileUrl).lastPathSegment?.substringAfterLast('-') ?: "첨부파일"
            val extension = fileName.substringAfterLast('.', "").lowercase()

            if (extension in imageExtensions) {
                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (200 * resources.displayMetrics.density).toInt(),
                    ).also { it.topMargin = (8 * resources.displayMetrics.density).toInt() }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                }
                container.addView(imageView)
                loadThumbnail(fileUrl, imageView)
            }

            val link = TextView(this).apply {
                text = "📎 $fileName"
                textSize = 13f
                setTextColor(getColor(R.color.brand))
                val top = (8 * resources.displayMetrics.density).toInt()
                setPadding(0, top, 0, 0)
                setOnClickListener { downloadFile(fileUrl, fileName) }
            }
            container.addView(link)
        }
    }

    /** 첨부파일을 기기의 '다운로드' 폴더로 내려받는다(완료 시 알림 표시). */
    private fun downloadFile(fileUrl: String, fileName: String) {
        val uri = runCatching { Uri.parse(fileUrl) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https")) {
            toast("다운로드할 수 없는 파일입니다.")
            return
        }
        try {
            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("첨부파일 다운로드 중…")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            toast("다운로드를 시작합니다.")
        } catch (e: Exception) {
            toast("다운로드에 실패했습니다.")
        }
    }

    /** 이미지 URL을 백그라운드에서 내려받아 썸네일로 표시한다. 실패해도 아래 파일명 링크는 남는다. */
    private fun loadThumbnail(fileUrl: String, imageView: ImageView) {
        lifecycleScope.launch {
            val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                runCatching {
                    URL(fileUrl).openStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.visibility = View.GONE
            }
        }
    }

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.replace("T", " ")?.take(16) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
