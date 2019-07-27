package com.example.moonshot.frag


import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moonshot.R
import com.example.moonshot.databinding.FragmentBtOnBinding

class BtOnFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentBtOnBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_bt_on, container, false
        )
        return binding.root
    }


}
