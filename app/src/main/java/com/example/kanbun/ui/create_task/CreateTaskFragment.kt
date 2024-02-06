package com.example.kanbun.ui.create_task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.ui.BaseFragment

class CreateTaskFragment : BaseFragment() {
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding: FragmentCreateTaskBinding get() = _binding!!

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
        setUpActionBar(binding.topAppBar.toolbar)
    }

    override fun setUpListeners() {
//        TODO("Not yet implemented")
    }
}