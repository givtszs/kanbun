package com.example.kanbun.ui.manage_members

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.Role
import com.example.kanbun.common.getColor
import com.example.kanbun.common.loadProfilePicture
import com.example.kanbun.databinding.FragmentViewAllMembersBinding
import com.example.kanbun.databinding.ItemMemberBinding
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.model.Member
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MembersBottomSheet private constructor() : BottomSheetDialogFragment() {
    private var _binding: FragmentViewAllMembersBinding? = null
    private val binding: FragmentViewAllMembersBinding get() = _binding!!
    private lateinit var members: MutableList<Member>
    private var isTaskScreen = false
    private var ownerId: String? = null
    private lateinit var onDismissCallback: (List<Member>) -> Unit
    private var membersAdapter: AllMembersAdapter? = null
    private var saveOnDismiss = true

    companion object {
        private const val TAG = "MembersBottomSheet"

        fun init(
            members: List<Member>,
            isTaskScreen: Boolean = false,
            ownerId: String? = null,
            onDismissCallback: ((List<Member>) -> Unit)? = null
        ): MembersBottomSheet {
            return MembersBottomSheet().apply {
                this.members = members.toMutableList()
                this.isTaskScreen = isTaskScreen
                this.ownerId = ownerId
                this.onDismissCallback = onDismissCallback ?: {}
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
        val currentUserRole = members.find { it.user.id == MainActivity.firebaseUser?.uid }?.role
        Log.d(TAG, "currentUserRole: $currentUserRole")
        if (currentUserRole == Role.Workspace.Admin || currentUserRole == Role.Board.Admin || isTaskScreen) {
            val swipeHandler = object : SwipeToDeleteCallback(requireContext(), currentUserRole != null) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    membersAdapter?.removeItemAt(viewHolder.adapterPosition) { member ->
                        members.remove(member)
                    }
                }
            }
            ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvMembers)
        }

        membersAdapter = AllMembersAdapter(
            members = members.toMutableList(),
            isCurrentUserAdmin = currentUserRole == Role.Workspace.Admin || currentUserRole == Role.Board.Admin,
            ownerId = ownerId,
            onRoleChanged = { member, role ->
                updateMemberRole(
                    member.copy(role = role)
                )
            }
        )

        binding.rvMembers.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        binding.rvMembers.adapter = membersAdapter
    }

    private fun updateMemberRole(member: Member) {
        members = (members.map { if (it.user.id != member.user.id) it else member }).toMutableList()
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
    private val members: MutableList<Member>,
    private val isCurrentUserAdmin: Boolean,
    private val ownerId: String?,
    private val onRoleChanged: (Member, Role) -> Unit
) : RecyclerView.Adapter<AllMembersAdapter.ItemMemberViewHolder>() {

    fun removeItemAt(position: Int, removeCallback: (Member) -> Unit) {
        removeCallback(members[position])
        members.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMemberViewHolder {
        return ItemMemberViewHolder(
            binding = ItemMemberBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            isUserAdmin = isCurrentUserAdmin,
            ownerId = ownerId
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
        val isUserAdmin: Boolean,
        val ownerId: String?,
        val onRoleChanged: (Int, Role) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            private const val TAG = "ItemMemberViewHolder"
        }

        private lateinit var rolesAdapter: RolesAdapter

        fun bind(member: Member) {
            binding.apply {
                tvName.text = member.user.name
                tvTag.text = itemView.context.resources.getString(
                    R.string.user_tag,
                    member.user.tag
                )
                loadProfilePicture(
                    context = itemView.context,
                    pictureUrl = member.user.profilePicture,
                    view = ivProfilePicture
                )

                tfRole.isEnabled = isUserAdmin && member.user.id != ownerId && MainActivity.firebaseUser?.uid != member.user.id
                if (member.role == null) {
                    tfRole.visibility = View.GONE
                    return@apply
                }

                val (memberRole, roles) = when (member.role) {
                    is Role.Workspace -> member.role.name to listOf(
                        Role.Workspace.Admin,
                        Role.Workspace.Member
                    )

                    is Role.Board -> member.role.name to listOf(Role.Board.Admin, Role.Board.Member)
                }

                val dropDownMenu = (tfRole.editText as? AutoCompleteTextView)
                dropDownMenu?.apply {
                    setText(memberRole, false)
                    if (isUserAdmin) {
                        rolesAdapter = RolesAdapter(itemView.context, roles)
                        setAdapter(rolesAdapter)
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
}

private abstract class SwipeToDeleteCallback(
    val context: Context,
    val memberHasRole: Boolean
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val background = ColorDrawable()
    private val backgroundColor = getColor(context, R.color.red_600)
    private val iconDelete =
        ContextCompat.getDrawable(context, R.drawable.ic_delete_outlined_24).also {
            it?.setTint(getColor(context, R.color.white))
        }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // this code suggests that the first element is always an owner thus preventing it from being swiped
        if (viewHolder.adapterPosition == 0 && memberHasRole) return 0
        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top // check why not to use itemView.height?
        val isCanceled = dX == 0f && !isCurrentlyActive
        Log.d(
            "ItemMemberViewHolder", "onChildDraw: itemView dimensions:    " +
                    "left: ${itemView.left}, top: ${itemView.top}, right: ${itemView.right}, bottom: ${itemView.bottom}"
        )
        Log.d("ItemMemberViewHolder", "onChildDraw: dx: $dX")

        if (isCanceled) {
            clearCanvas(
                canvas = c,
                left = itemView.right + dX,
                top = itemView.top.toFloat(),
                right = itemView.right.toFloat(),
                bottom = itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // draw the red delete background
        background.color = backgroundColor
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // calculate the position of the delete icon
        iconDelete?.let { icon ->
            val marginHorizontal = (itemHeight - icon.intrinsicHeight) / 2
            val iconLeft = itemView.right - marginHorizontal - icon.intrinsicWidth
            val iconTop = itemView.top + (itemHeight - icon.intrinsicHeight) / 2
            val iconRight = itemView.right - marginHorizontal
            val iconBottom = iconTop + icon.intrinsicHeight
            // draw the delete icon
            iconDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            iconDelete.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawRect(left, top, right, bottom, clearPaint)
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
            view?.findViewById<TextView>(R.id.tvRoleDescription)?.text =
                context.getString(it.description)
        }

        // the view must have already been initialized if null
        return view!!
    }
}