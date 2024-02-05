package com.example.kanbun.ui.task_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.databinding.FragmentTaskDetailsBinding
import com.example.kanbun.ui.BaseFragment

class TaskDetailsFragment : BaseFragment() {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding: FragmentTaskDetailsBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
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