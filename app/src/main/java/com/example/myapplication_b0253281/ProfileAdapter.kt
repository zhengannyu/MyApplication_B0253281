package com.example.myapplication_b0253281

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter(private val profiles: List<Profile>) :
    RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.avatarImage)
        val name: TextView = view.findViewById(R.id.nameText)
        val responsibility: TextView = view.findViewById(R.id.roleText)
        val motto: TextView = view.findViewById(R.id.mottoText)
        val studentId: TextView = view.findViewById(R.id.studentIdText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = profiles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.avatar.setImageResource(profile.imageResId)
        holder.name.text = profile.name
        holder.responsibility.text = profile.responsibility
        holder.motto.text = profile.motto
        holder.studentId.text = profile.studentId
    }
}
