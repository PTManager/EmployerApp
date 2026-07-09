package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.LoginRequest
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.SignupRequest
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var signupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        findViewById<View>(R.id.login_root).applySystemBarInsets()

        val nameLabel = findViewById<View>(R.id.label_name)
        val nameInput = findViewById<EditText>(R.id.input_name)
        val emailInput = findViewById<EditText>(R.id.input_email)
        val passwordInput = findViewById<EditText>(R.id.input_password)
        val primaryBtn = findViewById<TextView>(R.id.btn_request_code)
        val signupBtn = findViewById<TextView>(R.id.btn_signup)

        signupBtn.setOnClickListener {
            signupMode = !signupMode
            nameLabel.visibility = if (signupMode) View.VISIBLE else View.GONE
            nameInput.visibility = if (signupMode) View.VISIBLE else View.GONE
            primaryBtn.text = if (signupMode) "회원가입" else "로그인"
            signupBtn.text = if (signupMode) "로그인으로 돌아가기" else "이메일로 회원가입"
        }

        primaryBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                toast("이메일과 비밀번호를 입력해 주세요.")
                return@setOnClickListener
            }
            if (signupMode) {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    toast("이름을 입력해 주세요.")
                    return@setOnClickListener
                }
                if (password.length < 8) {
                    toast("비밀번호는 8자 이상이어야 합니다.")
                    return@setOnClickListener
                }
                doSignup(name, email, password, primaryBtn)
            } else {
                doLogin(email, password, primaryBtn)
            }
        }

        findViewById<TextView>(R.id.btn_create_store).setOnClickListener {
            startActivity(Intent(this, CreateStoreActivity::class.java))
        }
    }

    private fun doLogin(email: String, password: String, btn: TextView) {
        btn.isEnabled = false
        lifecycleScope.launch {
            try {
                val token = Network.api.login(LoginRequest(email, password))
                TokenStore.saveSession(token)
                routeAfterAuth()
            } catch (e: Exception) {
                toast(e.toUserMessage())
                btn.isEnabled = true
            }
        }
    }

    private fun doSignup(name: String, email: String, password: String, btn: TextView) {
        btn.isEnabled = false
        lifecycleScope.launch {
            try {
                val token = Network.api.signup(SignupRequest(email, password, name, role = "EMPLOYER"))
                TokenStore.saveSession(token)
                routeAfterAuth()
            } catch (e: Exception) {
                toast(e.toUserMessage())
                btn.isEnabled = true
            }
        }
    }

    /** 매장이 있으면 대시보드로, 없으면 매장 생성 화면으로 보낸다. */
    private fun routeAfterAuth() {
        val next = if (TokenStore.workplaceId > 0) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, CreateStoreActivity::class.java)
        }
        startActivity(next)
        finish()
    }

}
