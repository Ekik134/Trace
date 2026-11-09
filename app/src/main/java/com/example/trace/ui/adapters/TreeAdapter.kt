package com.example.trace.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trace.R
import com.example.trace.ui.TreeNodeModel

class TreeAdapter(
    private var nodes: List<TreeNodeModel>,
    private val onClick: (TreeNodeModel) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    fun updateData(newNodes: List<TreeNodeModel>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tree_node, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val node = nodes[position]

        // Сдвиг карточки вправо в зависимости от уровня (например, на 32dp)
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

        fun bind(node: TreeNodeModel, onClick: (TreeNodeModel) -> Unit) {
            tvTagName.text = node.name
            tvFileCount.text = "${node.fileCount} файлов"
            itemView.setOnClickListener { onClick(node) }
        }
    }
}