package com.example.moonshot


import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moonshot.databinding.FragmentBtOffBinding

class BtOffFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentBtOffBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_bt_off, container, false
        )
        return binding.root
    }


}
