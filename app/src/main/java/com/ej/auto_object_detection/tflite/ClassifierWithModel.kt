package com.ej.auto_object_detection.tflite

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class ClassifierWithModel constructor(
    val context: Context
){

    lateinit var model : Model

    var modelInputWidth : Int = 0
    var modelInputHeight : Int = 0
    var modelInputChannel : Int = 0
    var modelOutputClasses : Int = 0
    lateinit var outputBuffer : TensorBuffer

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

        val outputTensor = model.getOutputTensor(0)

        val outputType = outputTensor.dataType()
        val outputShape = outputTensor.shape()
        modelOutputClasses =  outputShape[1]
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
        return
    }

    fun classify(image: Bitmap){
        inputImage = loadImage(image)

        val inputs = arrayOf<Any>(inputImage.buffer)
        val outputs : HashMap<Int, Any> = HashMap()
        outputs[0] = outputBuffer.buffer.rewind()

        model.run(inputs, outputs)
    }


    private fun loadImage(bitmap: Bitmap) : TensorImage {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap))
        } else {
            inputImage.load(bitmap)
        }
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputWidth, modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        return imageProcessor.process(inputImage)
    }

    private fun convertBitmapToARGB8888(bitmap: Bitmap): Bitmap {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    companion object {
        val MODEL_NAME = "model.tflite"

    }
}