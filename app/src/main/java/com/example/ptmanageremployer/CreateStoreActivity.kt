package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.CreateWorkplaceRequest
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class CreateStoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_store)
        findViewById<View>(R.id.create_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val inputState = findViewById<View>(R.id.state_input)
        val codeState = findViewById<View>(R.id.state_code)
        val nameInput = findViewById<EditText>(R.id.input_store)
        val typeInput = findViewById<EditText>(R.id.input_type)
        val codeView = findViewById<TextView>(R.id.tv_invite_code)

        findViewById<TextView>(R.id.btn_create).setOnClickListener { btn ->
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                toast("매장 이름을 입력해 주세요.")
                return@setOnClickListener
            }
            val address = typeInput.text.toString().trim().ifBlank { null }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    val workplace = Network.api.createWorkplace(CreateWorkplaceRequest(name, address))
                    TokenStore.setWorkplaceId(workplace.id)
                    codeView.text = workplace.inviteCode ?: "------"
                    inputState.visibility = View.GONE
                    codeState.visibility = View.VISIBLE
                } catch (e: Exception) {
                    toast(e.toUserMessage())
                    btn.isEnabled = true
                }
            }
        }
        findViewById<View>(R.id.btn_enter_app).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}
