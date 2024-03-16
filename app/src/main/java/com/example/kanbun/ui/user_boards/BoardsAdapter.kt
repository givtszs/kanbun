package com.example.kanbun.ui.user_boards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardBinding
import com.example.kanbun.domain.model.Workspace

class BoardsAdapter(
    private val onItemClickListener: (Workspace.BoardInfo) -> Unit
) : RecyclerView.Adapter<BoardsAdapter.ItemBoardViewHolder>() {

    class ItemBoardViewHolder(
        private val binding: ItemBoardBinding,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(boardInfo: Workspace.BoardInfo) {
            binding.apply {
                tvName.text = boardInfo.name
                // load cover image with glide
            }
        }
    }

    private var boards: List<Workspace.BoardInfo> = emptyList()

    fun setData(data: List<Workspace.BoardInfo>) {
        boards = data
        notifyDataSetChanged()
    }

    // TODO: add `addBoard` method
    fun clear() {
        boards = emptyList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBoardViewHolder {
        return ItemBoardViewHolder(
            ItemBoardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        ) { position ->
            onItemClickListener(boards[position])
        }
    }

    override fun onBindViewHolder(holder: ItemBoardViewHolder, position: Int) {
        holder.bind(boards[position])
    }

    override fun getItemCount(): Int = boards.size
}