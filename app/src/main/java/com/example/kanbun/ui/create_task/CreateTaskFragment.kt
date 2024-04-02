package com.example.kanbun.ui.create_task

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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.DATE_FORMAT
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.convertDateStringToTimestamp
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.AlertDialogDatetimePickerBinding
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.create_tag_dialog.CreateTagDialog
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.shared.BoardMembersViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding: FragmentCreateTaskBinding get() = _binding!!
    private val createTaskViewModel: CreateTaskViewModel by viewModels()
    private val boardMembersViewModel: BoardMembersViewModel by activityViewModels()
    private val args: CreateTaskFragmentArgs by navArgs()
    private val boardListInfo: BoardListInfo by lazy {
        BoardListInfo(args.boardList.id, args.boardList.path)
    }

    //    private var alertBinding: AlertDialogCreateTagBinding? = null
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
        super.onViewCreated(view, savedInstanceState)
        when (args.actionType) {
            TaskAction.ACTION_CREATE -> {
                setUpActionBar(binding.topAppBar.toolbar, "Create task")
            }

            TaskAction.ACTION_EDIT -> {
                setUpActionBar(binding.topAppBar.toolbar, "Edit task")
            }
        }
        createTaskViewModel.init(
            args.task,
            boardListInfo,
            args.actionType,
            boardMembersViewModel.boardMembers
        )
        setUpTagsAdapter()
        setUpAdapters()
        collectState()
    }

    override fun setUpListeners() {
        val task = args.task

        binding.apply {
            // use the task from arguments to preserve the `position` property
            when (args.actionType) {
                // TODO: Extract the `updatedTask` into a separate method if the code is similar for both creation and update
                TaskAction.ACTION_CREATE -> {
                    btnCreateTask.setOnClickListener {
                        createTaskViewModel.createTask(
                            getUpdatedTask(task),
                            boardListInfo
                        ) {
                            navController.popBackStack()
                        }
                    }
                }

                TaskAction.ACTION_EDIT -> {
                    etName.setText(task.name)
                    etDescription.setText(task.description)

                    with(btnCreateTask) {
                        isEnabled = true
                        setOnClickListener {
                            val updatedTask = getUpdatedTask(task)
                            createTaskViewModel.editTask(updatedTask, boardListInfo) {
                                navController.navigate(
                                    CreateTaskFragmentDirections.actionCreateTaskFragmentToTaskDetailsFragment(
                                        task = updatedTask,
                                        boardList = args.boardList
                                    )
                                )
                            }
                        }
                    }
                }
            }

            etName.doOnTextChanged { text, _, _, _ ->
                Log.d(TAG, "etName: textSize: ${text?.length}")
                btnCreateTask.isEnabled = !text.isNullOrEmpty()
            }

            etSearchMembers.setOnFocusChangeListener { _, isFocused ->
                if (isFocused && etSearchMembers.text?.isEmpty() == true) {
                    createTaskViewModel.resetFoundUsers()
                } else if (!isFocused) {
                    createTaskViewModel.resetFoundUsers(true)
                }
            }

            etSearchMembers.doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty() && text.length >= 3) {
                    Log.d(TAG, "searchUser: $text")
                    createTaskViewModel.searchUser(text.toString())
                } else {
                    Log.d(TAG, "searchUser: call resetFoundUsers")
                    createTaskViewModel.resetFoundUsers()
                }
            }

            tvCreateTag.setOnClickListener {
//                buildCreateTagDialog()
                val createTagDialog = CreateTagDialog(requireContext()) { tag ->
                    createTaskViewModel.createTag(tag, boardListInfo)
                }
                createTagDialog.show()
            }

            tvDateStarts.apply {
                task.dateStarts?.let {
                    setText(convertTimestampToDateString(DATE_TIME_FORMAT, it))
                }

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        Log.d(TAG, "tvDateStarts is clicked")
                        buildDateTimePickerDialog(binding.tvDateStarts)
                    }
                }
            }

            tvDateEnds.apply {
                task.dateEnds?.let {
                    setText(convertTimestampToDateString(DATE_TIME_FORMAT, it))
                }

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        Log.d(TAG, "tvDateEnds is clicked")
                        buildDateTimePickerDialog(binding.tvDateEnds)
                    }
                }
            }
        }
    }

    private fun getUpdatedTask(task: Task): Task {
        return task.copy(
            name = binding.etName.text?.trim().toString(),
            description = binding.etDescription.text?.trim().toString(),
            author = createTaskViewModel.firebaseUser!!.uid,
            tags = createTaskViewModel.createTaskState.value.tags
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
            members = createTaskViewModel.createTaskState.value.taskMembers.map { it.id }
        )
    }

    // TODO: Remove the commented code if it's not needed
//    private fun buildCreateTagDialog() {
//        var tagColor = ""
//        var alertDialogBinding: AlertDialogCreateTagBinding? =
//            AlertDialogCreateTagBinding.inflate(layoutInflater, null, false)
//        var colorPickerAdapter: ColorPickerAdapter? = ColorPickerAdapter { colorId ->
//            tagColor = colorId
//        }
//
//        alertDialogBinding!!.rvTagColors.apply {
//            adapter = colorPickerAdapter
//            layoutManager = GridLayoutManager(requireContext(), 4)
//        }
//
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle("Create tag")
//            .setView(alertDialogBinding.root)
//            .setCancelable(false)
//            .setPositiveButton("Create") { _, _ ->
//                viewModel.createTag(
//                    name = alertDialogBinding!!.etName.text?.trim().toString(),
//                    color = tagColor,
//                    boardListInfo = args.boardListInfo
//                )
//                alertDialogBinding = null
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                colorPickerAdapter = null
//                alertDialogBinding = null
//                dialog.cancel()
//            }
//            .create()
//            .apply {
//                setOnShowListener {
//                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).apply {
//                        isEnabled = false
//                    }
//                    alertDialogBinding!!.etName.doOnTextChanged { _, _, _, count ->
//                        positiveButton.isEnabled = count > 0
//                    }
//                }
//            }
//            .show()
//    }

    private fun buildDateTimePickerDialog(textField: AutoCompleteTextView) {
//        fun close(dialogBinding: AlertDialogDatetimePickerBinding, dialog) {
//            alertDialogBinding = null
//            binding.tvDateStarts.clearFocus()
//            dialog.cancel()
//        }
//
        var alertDialogBinding: AlertDialogDatetimePickerBinding? =
            AlertDialogDatetimePickerBinding.inflate(layoutInflater, null, false)

        alertDialogBinding?.let { dialogBinding ->
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

            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    textField.setText(
                        resources.getString(
                            R.string.date_time,
                            dialogBinding.tvSelectedDate.text,
                            dialogBinding.tvSelectedTime.text
                        )
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setOnDismissListener {
                    alertDialogBinding = null
                    textField.clearFocus()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).also {
                            it.isEnabled = false
                        }

                        dialogBinding.apply {
                            val _isDateSelected = MutableStateFlow(tvSelectedDate.text.isNotEmpty())
                            val _isTimeSelected = MutableStateFlow(tvSelectedTime.text.isNotEmpty())
                            lifecycleScope.launch {
                                combine(
                                    _isDateSelected,
                                    _isTimeSelected
                                ) { isDateSelected, isTimeSelected ->
                                    isDateSelected && isTimeSelected
                                }.collectLatest { isDateTimeSelected ->
                                    positiveButton.isEnabled = isDateTimeSelected
                                }
                            }

                            tvSelectedDate.doAfterTextChanged {
                                Log.d(TAG, "date has changed: $it")
                                _isDateSelected.value = it?.isNotEmpty() == true
                            }

                            tvSelectedTime.doAfterTextChanged {
                                Log.d(TAG, "time has changed: $it")
                                _isTimeSelected.value = it?.isNotEmpty() == true
                            }
                        }
                    }
                }
                .show()
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
            .setInputMode(INPUT_MODE_CLOCK)

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

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                createTaskViewModel.createTaskState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.CreateTaskViewState) {
            binding.apply {
                if (loadingManager.isUpsertingTask) {
                    btnCreateTask.text = ""
                    btnCreateTask.isEnabled = false
                    pbCreatingTask.isVisible = true
                } else {
                    btnCreateTask.text = if (args.actionType == TaskAction.ACTION_CREATE) {
                        resources.getString(R.string.create_task)
                    } else {
                        resources.getString(R.string.save)
                    }
                    btnCreateTask.isEnabled =
                        etName.text?.trim().toString().isNotEmpty()
                    pbCreatingTask.isVisible = false
                }

                pbLoadingTags.isVisible = loadingManager.isLoadingTags

                if (tags.isNotEmpty()) {
                    tagsAdapter?.tags = tags
                }

                message?.let {
                    showToast(it)
                    createTaskViewModel.messageShown()
                }

                rvFoundUsers.isVisible = foundUsers != null
                foundUsers?.let { users ->
                    // TODO: DON'T FORGET TO UPDATE ME!!!
//                    searchUsersAdapter?.users = users
                }

//                searchUsersAdapter?.workspaceMembers = taskMembers.map { it.id }
                taskMembersAdapter?.members = taskMembers
            }

            message?.let {
                showToast(it)
                createTaskViewModel.messageShown()
            }
        }
    }

    private fun setUpAdapters() {
        searchUsersAdapter = SearchUsersAdapter { user ->
            showToast("Clicked on ${user.tag}")
            createTaskViewModel.addMember(user)
        }
        binding.rvFoundUsers.adapter = searchUsersAdapter

        taskMembersAdapter = MembersAdapter() { member ->
            createTaskViewModel.removeMember(member)
        }
        binding.rvMembers.adapter = taskMembersAdapter
    }

    private fun setUpTagsAdapter() {
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
