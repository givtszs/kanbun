package com.example.kanbun.ui.manage_members

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.Role
import com.example.kanbun.common.loadProfilePicture
import com.example.kanbun.databinding.ItemMemberChipBinding
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.model.Member

class MembersAdapter(
    private val ownerId: String? = null,
    private val onRemoveClicked: (Member) -> Unit = {}
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    private var isCurrentUserAdmin = false

    private fun checkIsCurrentUserAdmin() {
        val currentUser = members.find { it.user.id == MainActivity.firebaseUser?.uid }
        Log.d("MembersAdapter", "currentUser: $currentUser")
        isCurrentUserAdmin =
            currentUser?.role == Role.Workspace.Admin || currentUser?.role == Role.Board.Admin
        Log.d("MembersAdapter", "isCurrentUserAdmin: $isCurrentUserAdmin")
    }

    var members: List<Member> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                checkIsCurrentUserAdmin()
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
        holder.bind(members[position], isCurrentUserAdmin)
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

        fun bind(member: Member, isAdmin: Boolean) {
            binding.apply {
                tvName.text = member.user.name
                btnRemove.isVisible = member.user.id != ownerId && isAdmin

                loadProfilePicture(
                    context = itemView.context,
                    pictureUrl = member.user.profilePicture,
                    view = ivProfilePicture
                )
            }

        }
    }
}