package com.odc.odc_task

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.odc.odc_task.data.AppRepository
import com.odc.odc_task.databinding.ActivityBaseChatBinding
import com.odc.odc_task.ui.viewmodel.AppViewModel
import com.odc.odc_task.ui.viewmodel.AppViewModelFactory


class ConnectionActivity : AppCompatActivity() {


    private val binding by lazy {
        ActivityBaseChatBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this , AppViewModelFactory(AppRepository(this)))[AppViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.sendButton.setOnClickListener{
            if (binding.inputEdittext.text.toString().isNotEmpty()) {
                viewModel.sendString(binding.inputEdittext.text.toString())
                binding.inputEdittext.setText("")
            }
        }

        binding.contentText.text =  viewModel.text.value

        viewModel.text.observe(this){
            binding.contentText.text = it
        }

    }



}
