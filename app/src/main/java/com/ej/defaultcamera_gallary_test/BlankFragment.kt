package com.ej.defaultcamera_gallary_test

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ej.defaultcamera_gallary_test.databinding.FragmentBlankBinding
import com.ej.defaultcamera_gallary_test.tflite.ClassifierWithModel
import java.io.IOException


class BlankFragment : Fragment() {
    lateinit var binding : FragmentBlankBinding

    lateinit var cls : ClassifierWithModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(inflater,R.layout.fragment_blank,container,false)
        binding.lifecycleOwner = this.viewLifecycleOwner

        cls = ClassifierWithModel(requireContext())

        try {
            cls.init()
        } catch (e : IOException) {
            e.printStackTrace()
        }

        return binding.root
    }



    companion object {

        fun newInstance() =
            BlankFragment()
    }
}