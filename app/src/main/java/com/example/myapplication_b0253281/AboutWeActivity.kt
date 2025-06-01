package com.example.myapplication_b0253281

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class AboutWeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aboutwe)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val profiles = listOf(
            Profile("陳葳桐", "首頁、收藏", "我愛程式(つ´∀｀)つ", R.drawable.avatar1,"B1203288"),
            Profile("吳佳蓁", "設定、匿名聊天室", "我好忙快要哭了(〒︿〒)", R.drawable.avatar2,"B1218307"),
            Profile("許家心", "地圖天氣、關於我們", "想行光合作用吃燕麥粥…", R.drawable.avatar3,"B0253281")
        )

        recyclerView.adapter = ProfileAdapter(profiles)
    }
}
