package com.ej.defaultcamera_gallary_test

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ej.defaultcamera_gallary_test.databinding.FragmentBlankBinding
import com.ej.defaultcamera_gallary_test.tflite.ClassifierWithSupport3
import com.ej.tensorflowlitetest.tflite.ClassifierWithSupport
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class Blank3Fragment : Fragment() {
    lateinit var binding : FragmentBlankBinding

//    lateinit var cls : ClassifierWithModel
    lateinit var cls : ClassifierWithSupport3
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(inflater,R.layout.fragment_blank,container,false)
        binding.lifecycleOwner = this.viewLifecycleOwner

//        cls = ClassifierWithModel(requireContext())
        cls = ClassifierWithSupport3(requireContext())
        try {
            cls.init()
        } catch (e : IOException) {
            e.printStackTrace()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_meal_one, BitmapFactory.Options().apply {
//            inMutable = true
//        })
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_meal_one, BitmapFactory.Options().apply {
            inMutable = true
        })
        val resultBitmap = cls.classify(bitmap)
        binding.imageView.setImageBitmap(resultBitmap)
    }




    companion object {

        fun newInstance() =
            Blank3Fragment()
    }
}