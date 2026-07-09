package com.example.ptmanageremployer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Applies system-bar insets as padding so content sits below the status bar
 *  and above the navigation bar, matching the wireframe's safe-area layout. */
fun View.applySystemBarInsets(top: Boolean = true, bottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            top = if (top) bars.top else v.paddingTop,
            bottom = if (bottom) bars.bottom else v.paddingBottom
        )
        insets
    }
}

/** 짧은 토스트. long=true 면 LENGTH_LONG. */
fun Context.toast(msg: String, long: Boolean = false) =
    Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

fun Fragment.toast(msg: String, long: Boolean = false) = requireContext().toast(msg, long)

/** 확인/취소 다이얼로그. 확인 시 [onYes] 실행. */
fun Context.confirm(title: String, message: String, positive: String, onYes: () -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positive) { _, _ -> onYes() }
        .setNegativeButton("취소", null)
        .show()
}

/** [keepId] 뷰(빈 상태 표시 등)만 남기고 자식 뷰를 모두 제거한다. */
fun ViewGroup.removeAllExcept(keepId: Int) {
    for (i in childCount - 1 downTo 0) {
        if (getChildAt(i).id != keepId) removeViewAt(i)
    }
}


/** API 호출용 코루틴: 본문 실패(Exception) 시 에러 토스트만 띄운다. */
fun AppCompatActivity.launchApi(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        try { block() } catch (e: Exception) { toast(e.toUserMessage()) }
    }
}

/** [AppCompatActivity.launchApi]의 Fragment 버전. */
fun Fragment.launchApi(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        try { block() } catch (e: Exception) { toast(e.toUserMessage()) }
    }
}
