package com.example.kanbun.ui.manage_members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.loadUserProfilePicture
import com.example.kanbun.databinding.ItemMemberChipBinding
import com.example.kanbun.domain.model.User

class MembersAdapter(
    val ownerId: String? = null,
    private val onRemoveClicked: (User) -> Unit
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    var members: List<User> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(
            binding = ItemMemberChipBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            ownerId = ownerId
        ) { position ->
            onRemoveClicked(members[position])
        }
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(
        val ownerId: String?,
        val binding: ItemMemberChipBinding,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRemove.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(member: User) {
            binding.apply {
                tvName.text = member.name
                loadUserProfilePicture(
                    context = itemView.context,
                    pictureUrl = member.profilePicture,
                    view = ivProfilePicture
                )
                btnRemove.isVisible = member.id != ownerId
            }

        }
    }
}