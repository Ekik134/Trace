package com.example.trace.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.trace.R
import com.example.trace.data.model.FileModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File

class FileAdapter(
    private var files: List<FileModel>,
    private val onClick: (FileModel) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    fun updateData(newFiles: List<FileModel>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_card, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPreview = itemView.findViewById<ImageView>(R.id.ivFilePreview)
        private val tvName = itemView.findViewById<TextView>(R.id.tvFileName)
        private val tvMore = itemView.findViewById<TextView>(R.id.tvMore) // <-- Добавлено
        private val chipGroupTags = itemView.findViewById<ChipGroup>(R.id.chipGroupFileTags)

        fun bind(file: FileModel) {
            tvName.text = file.name

            // ВАЖНО: Принудительно показываем "подробнее" для каждого элемента
            // (RecyclerView переиспользует View, поэтому нужно явно сбрасывать состояние)
            tvMore.visibility = View.VISIBLE

            // Загрузка картинки
            val imagePath = file.previewPath ?: file.localUri
            if (imagePath != null && file.mimeType?.startsWith("image/") == true) {
                ivPreview.load(File(imagePath))
            } else {
                ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
            }

            // Отрисовка тегов (максимум 2, чтобы не перегружать карточку)
            chipGroupTags?.removeAllViews()
            file.tags.take(2).forEach { tag ->
                val chip = Chip(itemView.context).apply {
                    text = tag.name
                    textSize = 10f
                    setChipBackgroundColorResource(android.R.color.white)
                    setChipStrokeColorResource(R.color.green_200)
                    setChipStrokeWidth(1f)
                    isClickable = false
                    isCheckable = false
                    chipMinHeight = 24f
                }
                chipGroupTags?.addView(chip)
            }

            itemView.setOnClickListener { onClick(file) }
        }
    }
}