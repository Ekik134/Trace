package com.example.trace.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.trace.R
import com.example.trace.ui.viewmodel.TreeNode
import java.io.File

class TreeAdapter(
    private var nodes: List<TreeNode>,
    private val onClick: (TreeNode) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    fun updateData(newNodes: List<TreeNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tree_node, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val node = nodes[position]

        // Сдвиг вправо в зависимости от уровня (например, 32dp на уровень)
        val density = holder.itemView.resources.displayMetrics.density
        val marginStart = (node.level * 32 * density).toInt()

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.marginStart = marginStart
        holder.itemView.layoutParams = layoutParams

        holder.bind(node, onClick)
    }

    override fun getItemCount() = nodes.size

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTagName = itemView.findViewById<TextView>(R.id.tvTagName)
        private val tvFileCount = itemView.findViewById<TextView>(R.id.tvFileCount)
        private val ivTagIcon = itemView.findViewById<ImageView>(R.id.ivTagIcon)

        fun bind(node: TreeNode, onClick: (TreeNode) -> Unit) {
            tvTagName.text = node.name
            tvFileCount.text = "${node.fileCount} файлов"

            if (!node.iconUri.isNullOrBlank()) {
                ivTagIcon.load(File(node.iconUri))
            } else {
                ivTagIcon.setImageResource(R.drawable.tag_placeholder)
            }

            itemView.setOnClickListener { onClick(node) }
        }
    }
}