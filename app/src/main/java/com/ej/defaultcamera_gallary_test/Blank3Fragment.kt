package com.ej.defaultcamera_gallary_test

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ej.defaultcamera_gallary_test.databinding.FragmentBlank3Binding
import com.ej.defaultcamera_gallary_test.tflite.ClassifierWithSupport3
import java.io.IOException


class Blank3Fragment : Fragment() {
    lateinit var binding : FragmentBlank3Binding

//    lateinit var cls : ClassifierWithModel
    lateinit var cls : ClassifierWithSupport3

    var imageHeight = 0
    var imageWidth = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(inflater,R.layout.fragment_blank3,container,false)
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
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.foreign, BitmapFactory.Options().apply {
            inMutable = true
        })

        binding.imageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 크기가 확정되면 이 리스너가 호출됩니다.

                imageHeight = binding.imageView.height
                imageWidth = binding.imageView.width

                binding.imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val result = cls.classify(bitmap)
                val resultOne = result[0]
                val yMin = resultOne.second[0]*imageHeight
                val xMin = resultOne.second[1]*imageWidth
                val yMax = resultOne.second[3]*imageHeight
                val xMax = resultOne.second[4]*imageWidth



            }
        })

    }




    companion object {

        fun newInstance() =
            Blank3Fragment()
    }
}