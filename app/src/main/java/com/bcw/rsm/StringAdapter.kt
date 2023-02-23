package com.bcw.rsm

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bcw.rsm.databinding.ItemBinding

class StringAdapter(private val ctx: Context) : RecyclerView.Adapter<StringAdapter.VH>() {
    val data = mutableListOf<String>()

    private val inputColor = ResourcesCompat.getColor(ctx.resources, android.R.color.holo_purple, null)
    private val outputColor = ResourcesCompat.getColor(ctx.resources, android.R.color.black, null)

    data class VH(val bind: ItemBinding) : RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemBinding.inflate(LayoutInflater.from(ctx)))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind.textView.text = "${data.lastIndex-position}. ${data[position]}"
        holder.bind.textView.setTextColor(
            if (data[position].lowercase().contains("model")) outputColor else inputColor
        )
    }
}