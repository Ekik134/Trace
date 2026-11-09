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
        // Ищем именно ImageView
        private val ivPreview = itemView.findViewById<ImageView>(R.id.ivFilePreview)
        private val tvName = itemView.findViewById<TextView>(R.id.tvFileName)

        fun bind(file: FileModel) {
            tvName.text = file.name

            // Загружаем картинку, если это изображение
            val imagePath = file.previewPath ?: file.localUri
            if (imagePath != null && file.mimeType?.startsWith("image/") == true) {
                ivPreview.load(File(imagePath))
            } else {
                // Ставим иконку-заглушку для PDF или других файлов
                ivPreview.setImageResource(R.drawable.ic_pdf_miniature)
            }

            itemView.setOnClickListener { onClick(file) }
        }
    }
}