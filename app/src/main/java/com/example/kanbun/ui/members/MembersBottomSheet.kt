package com.example.kanbun.ui.members

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.Role
import com.example.kanbun.common.loadUserProfilePicture
import com.example.kanbun.databinding.FragmentViewAllMembersBinding
import com.example.kanbun.databinding.ItemMemberBinding
import com.example.kanbun.ui.model.Member
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MembersBottomSheet private constructor() : BottomSheetDialogFragment() {
    private var _binding: FragmentViewAllMembersBinding? = null
    private val binding: FragmentViewAllMembersBinding get() = _binding!!
    private lateinit var members: List<Member>
    private lateinit var onDismissCallback: (List<Member>) -> Unit
    private var membersAdapter: AllMembersAdapter? = null
    private var saveOnDismiss = true

    companion object {
        private const val TAG = "MembersBottomSheet"

        fun  init(members: List<Member>, onDismissCallback: (List<Member>) -> Unit): MembersBottomSheet {
            return MembersBottomSheet().apply {
                this.members = members
                this.onDismissCallback = onDismissCallback
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.ModalBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewAllMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()
        binding.btnCancel.setOnClickListener {
            saveOnDismiss = false
            dismiss()
        }
    }

    private fun setUpAdapter() {
        membersAdapter = AllMembersAdapter(
            onRoleChanged = { member, role ->
                 updateMemberRole(
                     member.copy(role = role)
                 )
            }
        )
        membersAdapter?.members = this@MembersBottomSheet.members
        binding.rvMembers.adapter = membersAdapter
    }

    private fun  updateMemberRole(member: Member) {
        members = members.filterNot { it.user.id == member.user.id } + member
        Log.d(TAG, "updateMemberRole: $members")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (saveOnDismiss) {
            onDismissCallback(members)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        membersAdapter = null
    }
}

private class AllMembersAdapter(
    private val onRoleChanged: (Member, Role) -> Unit
) : RecyclerView.Adapter<AllMembersAdapter.ItemMemberViewHolder>() {
    var members: List<Member> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMemberViewHolder {
        return ItemMemberViewHolder(
            ItemMemberBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { position, role ->
            onRoleChanged(members[position], role)
        }
    }

    override fun onBindViewHolder(holder: ItemMemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    class ItemMemberViewHolder(
        val binding: ItemMemberBinding,
        val onRoleChanged: (Int, Role) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            private const val TAG = "ItemMemberViewHolder"
        }

        private lateinit var rolesAdapter: RolesAdapter

        fun  bind(member: Member) {
            binding.apply {
                tvName.text = member.user.name
                tvTag.text = itemView.context.resources.getString(
                    R.string.user_tag,
                    member.user.tag
                )
                loadUserProfilePicture(
                    context = itemView.context,
                    pictureUrl = member.user.profilePicture,
                    view = ivProfilePicture
                )

                if (member.role == null) {
                    tfRole.visibility = View.GONE
                    return@apply
                }

                val (memberRole, roles) = when(member.role) {
                    is Role.Workspace -> member.role.name to listOf(Role.Workspace.Admin, Role.Workspace.Member)
                    is Role.Board -> member.role.name to listOf(Role.Board.Admin, Role.Board.Member)
                }

                rolesAdapter = RolesAdapter(itemView.context, roles)
                val dropDownMenu = (tfRole.editText as? AutoCompleteTextView)
                dropDownMenu?.apply {
                    setAdapter(rolesAdapter)
                    setText(memberRole, false)
                    setOnItemClickListener { _, _, position, _ ->
                        rolesAdapter.getItem(position)?.let { role ->
                            Log.d(TAG, "onItemSelectedListener: $role")
                            onRoleChanged(adapterPosition, role)
                        }
                    }

                    setOnDismissListener {
                        clearFocus()
                    }
                }
            }
        }
    }
}

private class RolesAdapter(
    private val context: Context,
    roles: List<Role>
) : ArrayAdapter<Role>(context, 0, roles) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_member_role, parent, false)
        }

        val role = getItem(position)
        role?.let {
            view?.findViewById<TextView>(R.id.tvRoleName)?.text = it.name
            view?.findViewById<TextView>(R.id.tvRoleDescription)?.text = context.getString(it.description)
        }

        // the view must have already been initialized if null
        return view!!
    }
}