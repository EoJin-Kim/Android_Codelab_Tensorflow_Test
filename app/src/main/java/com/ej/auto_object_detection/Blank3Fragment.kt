package com.ej.auto_object_detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ej.auto_object_detection.databinding.FragmentBlank3Binding
import com.ej.auto_object_detection.tflite.ClassifierWithSupport3
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

        val imageBitmap = (binding.imageView.drawable as BitmapDrawable).bitmap
        imageHeight = imageBitmap.height
        imageWidth = imageBitmap.width

        val result = cls.classify(bitmap)
        val resultOne = result[1]

        val boundingBoxTop = if(resultOne.second[0]<0) 0f else resultOne.second[0]
        val boundingBoxLeft = if(resultOne.second[1]<0) 0f else resultOne.second[1]
        val boundingBoxBottom = if(resultOne.second[2]>1) 1f else resultOne.second[2]
        val boundingBoxRight = if(resultOne.second[3]>1) 1f else resultOne.second[3]

        val top = boundingBoxTop*imageHeight
        val left = boundingBoxLeft*imageWidth
        val bottom = boundingBoxBottom*imageHeight
        val right = boundingBoxRight * imageWidth

        val cropLeft = left.toInt()
        val cropTop = top.toInt()
        val cropWidth = (right - left).toInt()
        val cropHeight = (bottom - top).toInt()
        val croppedBitmap: Bitmap = Bitmap.createBitmap(imageBitmap, cropLeft, cropTop, cropWidth, cropHeight)

        activity?.let {
            it.runOnUiThread {
                binding.imageView2.setImageBitmap(croppedBitmap)
            }
        }
    }




    companion object {

        fun newInstance() =
            Blank3Fragment()
    }
}