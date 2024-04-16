package com.example.kanbun.ui.task_editor

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.DATE_FORMAT
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.convertDateStringToTimestamp
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.AlertDialogDatetimePickerBinding
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.create_tag_dialog.CreateTagDialog
import com.example.kanbun.ui.create_task.CreateTaskFragmentArgs
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.members.MembersBottomSheet
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.shared.SharedBoardViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
abstract class TaskEditorFragment : BaseFragment(), StateHandler {

    companion object {
        private const val TAG = "TaskEditorFragment"
    }

    private var _binding: FragmentCreateTaskBinding? = null
    protected val binding: FragmentCreateTaskBinding get() = _binding!!

    protected abstract val viewModel: TaskEditorViewModel
    private val sharedBoardViewModel: SharedBoardViewModel by hiltNavGraphViewModels(R.id.board_graph)

    abstract val boardList: BoardList
    abstract val task: Task?
    protected val boardListInfo: BoardListInfo by lazy {
        BoardListInfo(id = boardList.id, path = boardList.path)
    }

    private var tagsAdapter: TagsAdapter? = null
    private var searchUsersAdapter: SearchUsersAdapter? = null
    private var taskMembersAdapter: MembersAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setUpAdapters()
        viewModel.init(task, sharedBoardViewModel.boardMembers, sharedBoardViewModel.tags)
        collectState()
    }

    override fun setUpListeners() {
        binding.apply {
            task?.let { _task ->
                Log.d(TAG, "setUpListeners: task: $_task")
                etName.setText(_task.name)
                etDescription.setText(_task.description)
                _task.dateStarts?.let { dateStarts ->
                    tvDateStarts.setText(convertTimestampToDateString(DATE_TIME_FORMAT, dateStarts))
                }
                _task.dateEnds?.let { dateEnds ->
                    tvDateEnds.setText(convertTimestampToDateString(DATE_TIME_FORMAT, dateEnds))
                }
            }

            etName.doOnTextChanged { text, _, _, _ ->
                Log.d(TAG, "etName: textSize: ${text?.length}")
                btnCreateTask.isEnabled = !text.isNullOrEmpty()
            }

            etSearchMembers.setOnFocusChangeListener { _, isFocused ->
                viewModel.resetFoundUsers(!isFocused)
            }

            etSearchMembers.doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty() && text.length >= 3) {
                    Log.d(TAG, "searchUser: $text")
                    viewModel.searchUser(text.toString())
                } else {
                    Log.d(TAG, "searchUser: call resetFoundUsers")
                    viewModel.resetFoundUsers()
                }
            }

            tvCreateTag.setOnClickListener {
                val createTagDialog = CreateTagDialog(requireContext()) { tag ->
                    viewModel.createTag(tag, boardListInfo) { newTag ->
                        sharedBoardViewModel.tags += newTag
                    }
                }
                createTagDialog.show()
            }

            tvDateStarts.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    Log.d(TAG, "tvDateStarts is clicked")
                    buildDateTimePickerDialog(binding.tvDateStarts)
                }
            }

            tvDateEnds.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    Log.d(TAG, "tvDateEnds is clicked")
                    buildDateTimePickerDialog(binding.tvDateEnds)
                }
            }

            btnViewAllMembers.setOnClickListener {
                val membersBottomSheet =
                    MembersBottomSheet.init(
                        members = viewModel.taskEditorState.value.taskMembers.map { user ->
                            Member(user, null)
                        },
                        isTaskScreen = true
                    ) { members ->
                        viewModel.setMembers(members)
                    }
                membersBottomSheet.show(childFragmentManager, "workspace_members")
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.taskEditorState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.TaskEditorViewState) {
            Log.d(TAG, "processState: $this")
            binding.apply {
                pbCreatingTask.isVisible = isSavingTask
                if (isSavingTask) {
                    btnCreateTask.isEnabled = false
                }

                tagsAdapter?.tags = tags

                rvFoundUsers.isVisible = foundUsers != null
                foundUsers?.let { users ->
                    searchUsersAdapter?.users = users.map { user ->
                        user.copy(
                            isAdded = taskMembers.any { it.id == user.user.id }
                        )
                    }
                }
                taskMembersAdapter?.members = taskMembers.map { Member(it, null) }
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    protected fun getUpdatedTask(task: Task): Task {
        return task.copy(
            name = binding.etName.text?.trim().toString(),
            description = binding.etDescription.text?.trim().toString(),
            author = MainActivity.firebaseUser!!.uid,
            tags = viewModel.taskEditorState.value.tags
                .filter { it.isSelected }
                .map { it.tag.id },
            dateStarts = convertDateStringToTimestamp(
                DATE_TIME_FORMAT,
                binding.tvDateStarts.text.toString()
            ),
            dateEnds = convertDateStringToTimestamp(
                DATE_TIME_FORMAT,
                binding.tvDateEnds.text.toString()
            ),
            members = viewModel.taskEditorState.value.taskMembers.map { it.id }
        )
    }

    protected fun createTask(task: Task? = null): Task {
        val newTask = task ?: Task()
        return newTask.copy(
            name = binding.etName.text?.trim().toString(),
            description = binding.etDescription.text?.trim().toString(),
            author = MainActivity.firebaseUser!!.uid,
            members = viewModel.taskEditorState.value.taskMembers.map { it.id },
            tags = viewModel.taskEditorState.value.tags
                .filter { it.isSelected }
                .map { it.tag.id },
            dateStarts = convertDateStringToTimestamp(
                DATE_TIME_FORMAT,
                binding.tvDateStarts.text.toString()
            ),
            dateEnds = convertDateStringToTimestamp(
                DATE_TIME_FORMAT,
                binding.tvDateEnds.text.toString()
            )
        )
    }

    private fun buildDateTimePickerDialog(textField: AutoCompleteTextView) {
        val alertDialogBinding: AlertDialogDatetimePickerBinding =
            AlertDialogDatetimePickerBinding.inflate(layoutInflater, null, false)

        setUpDialogBinding(alertDialogBinding, textField)

        alertDialogBinding.apply {
            MaterialAlertDialogBuilder(requireContext())
                .setView(root)
                .setPositiveButton("Save") { _, _ ->
                    textField.setText(
                        resources.getString(
                            R.string.date_time,
                            tvSelectedDate.text,
                            tvSelectedTime.text
                        )
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setNeutralButton("Clear") { _, _ ->
                    textField.text.clear()
                }
                .setOnDismissListener {
//                    alertDialogBinding = null
                    textField.clearFocus()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).apply {
                            isEnabled = !tvSelectedDate.text.isNullOrEmpty()
                                    && !tvSelectedTime.text.isNullOrEmpty()
                        }

                        tvSelectedDate.doOnTextChanged { text, _, _, _ ->
                            Log.d(TAG, "date has changed: $text")
                            positiveButton.isEnabled = !text.isNullOrEmpty()
                                    && !tvSelectedTime.text.isNullOrEmpty()
                        }

                        tvSelectedTime.doOnTextChanged { text, _, _, _ ->
                            Log.d(TAG, "time has changed: $text")
                            positiveButton.isEnabled = !text.isNullOrEmpty()
                                    && !tvSelectedDate.text.isNullOrEmpty()
                        }
                    }
                }
                .show()

        }
    }

    private fun setUpDialogBinding(
        dialogBinding: AlertDialogDatetimePickerBinding,
        textField: AutoCompleteTextView
    ) {
        val date = textField.text.toString().substringBefore(",")
        val time = textField.text.toString().substringAfter(", ")

        dialogBinding.tvSelectedDate.apply {
            if (date.isNotEmpty()) {
                setText(date)
            }

            setOnClickListener {
                buildDatePicker(dialogBinding, dialogBinding.tvSelectedDate.text.toString())
            }
        }

        dialogBinding.tvSelectedTime.apply {
            if (time.isNotEmpty()) {
                setText(time)
            }

            setOnClickListener {
                buildTimePicker(dialogBinding, dialogBinding.tvSelectedTime.text.toString())
            }
        }
    }

    private fun buildDatePicker(
        alertDialogBinding: AlertDialogDatetimePickerBinding,
        date: String
    ) {
        val dateTimestamp = convertDateStringToTimestamp(DATE_FORMAT, date)

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .setSelection(dateTimestamp ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener {
            alertDialogBinding.tvSelectedDate.setText(
                convertTimestampToDateString(DATE_FORMAT, it)
            )
        }
        datePicker.show(childFragmentManager, "date_picker")
    }

    private fun buildTimePicker(
        alertDialogBinding: AlertDialogDatetimePickerBinding,
        time: String,
    ) {
        fun addZero(time: Int): String {
            return if (time < 10) {
                "0$time"
            } else {
                time.toString()
            }
        }

        val timePickerBuilder = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)

        if (time.isNotEmpty()) {
            Log.d(TAG, "buildTimePicker: initTime: $time")
            timePickerBuilder.setHour(time.substringBefore(":").toInt())
            timePickerBuilder.setMinute(time.substringAfter(":").toInt())
        }

        val timePicker = timePickerBuilder.build()
        timePicker.addOnPositiveButtonClickListener {
            val hour = addZero(timePicker.hour)
            val minute = addZero(timePicker.minute)
            alertDialogBinding.tvSelectedTime.setText("$hour:$minute")
        }

        timePicker.show(childFragmentManager, "time_picker")
    }

    private fun setUpAdapters() {
        searchUsersAdapter = SearchUsersAdapter { user ->
            showToast("Clicked on ${user.tag}")
            viewModel.addMember(user)
        }
        binding.rvFoundUsers.adapter = searchUsersAdapter

        taskMembersAdapter = MembersAdapter() { member ->
            viewModel.removeMember(member.user)
        }
        binding.rvMembers.adapter = taskMembersAdapter

        tagsAdapter = TagsAdapter(areItemsClickable = true)
        binding.rvTags.adapter = tagsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagsAdapter = null
        searchUsersAdapter = null
        taskMembersAdapter = null
        _binding = null
    }
}