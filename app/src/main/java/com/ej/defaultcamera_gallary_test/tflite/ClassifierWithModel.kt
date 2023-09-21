package com.ej.defaultcamera_gallary_test.tflite

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.io.IOException
import java.nio.ByteOrder

class ClassifierWithModel constructor(
    val context: Context
){

    lateinit var model : Model

    var modelInputWidth : Int = 0
    var modelInputHeight : Int = 0
    var modelInputChannel : Int = 0

    lateinit var inputImage : TensorImage

    @Throws(IOException::class)
    fun init() {
        model = Model.createModel(context, MODEL_NAME)
        initModelShape()

    }

    private fun initModelShape() {
        val inputTensor : Tensor = model.getInputTensor(0)
        val inputShape = inputTensor.shape()

        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        inputImage = TensorImage(inputTensor.dataType())
        return
    }

    companion object {
        val MODEL_NAME = "model.tflite"

    }
}