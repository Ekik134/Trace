package com.example.trace.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.data.model.TagModel

class TagAdapter(
    private var tags: List<TagModel>,
    private val onClick: (TagModel) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    fun updateData(newTags: List<TagModel>) {
        tags = newTags
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag_card, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount() = tags.size

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTagName = itemView.findViewById<TextView>(R.id.tvTagName)

        fun bind(tag: TagModel) {
            tvTagName.text = tag.name
            itemView.setOnClickListener { onClick(tag) }
        }
    }
}