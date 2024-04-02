package com.example.kanbun.ui.members

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.BoardRole
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.common.boardRoles
import com.example.kanbun.common.loadUserProfilePicture
import com.example.kanbun.common.workspaceRoles
import com.example.kanbun.databinding.FragmentViewAllMembersBinding
import com.example.kanbun.databinding.ItemMemberBinding
import com.example.kanbun.ui.model.ItemRole
import com.example.kanbun.ui.model.Member
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MembersBottomSheet<T> private constructor() : BottomSheetDialogFragment() {
    private var _binding: FragmentViewAllMembersBinding? = null
    private val binding: FragmentViewAllMembersBinding get() = _binding!!
    private lateinit var members: List<Member<T>>
    private var membersAdapter: AllMembersAdapter<T>? = null

    companion object {
        private const val TAG = "MembersBottomSheet"

        fun <T> init(members: List<Member<T>>): MembersBottomSheet<T> {
            return MembersBottomSheet<T>().apply {
                this.members = members
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
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun setUpAdapter() {
        membersAdapter = AllMembersAdapter<T>().apply {
            members = this@MembersBottomSheet.members
        }
        binding.rvMembers.adapter = membersAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        membersAdapter = null
    }
}

private class AllMembersAdapter<T> : RecyclerView.Adapter<AllMembersAdapter.ItemMemberViewHolder>() {
    var members: List<Member<T>> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMemberViewHolder {
        return ItemMemberViewHolder(
            binding = ItemMemberBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemMemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    class ItemMemberViewHolder(
        val binding: ItemMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            private const val TAG = "ItemMemberViewHolder"
        }

        fun <T> bind(member: Member<T>) {
            binding.apply {
                tvName.text = member.user.name
                tvTag.text = member.user.tag
                loadUserProfilePicture(
                    context = itemView.context,
                    pictureUrl = member.user.profilePicture,
                    view = ivProfilePicture
                )
                val resources = itemView.context.resources
                val (memberRole, roles) = when (member.role) {
                    is WorkspaceRole -> member.role.roleName to workspaceRoles
                    is BoardRole -> member.role.roleName to boardRoles
                    else -> return@apply
                }

                val rolesAdapter = RolesAdapter(itemView.context, roles)
                val dropDownMenu = (tfRole.editText as? AutoCompleteTextView)
                dropDownMenu?.apply {
                    setAdapter(rolesAdapter)
                    setText(memberRole, false)
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {

                            val clicked = adapter.getItem(position)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }
                    }
                }
            }
        }
    }
}

private class RolesAdapter(
    context: Context,
    roles: List<ItemRole>
) : ArrayAdapter<ItemRole>(context, 0, roles) {

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
            view?.findViewById<TextView>(R.id.tvRoleName)?.text = role.name
            view?.findViewById<TextView>(R.id.tvRoleDescription)?.text = role.description
        }

        // the view must have already been initialized if null
        return view!!
    }
}