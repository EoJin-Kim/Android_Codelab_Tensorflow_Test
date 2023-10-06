package com.ej.tensorflowlitetest.tflite

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ClassifierWithSupport constructor(
    val context: Context
) {

    lateinit var interpreter : Interpreter

    var modelInputWidth : Int = 0
    var modelInputHeight : Int = 0
    var modelInputChannel : Int = 0

    lateinit var modelInputDataType : DataType

    lateinit var inputImage : TensorImage
    lateinit var outputBuffer : TensorBuffer

    lateinit var modelOutputDataType : DataType
    lateinit var modelOutputShape : IntArray

    @Throws(IOException::class)
    fun init() {
        val model = FileUtil.loadMappedFile(context, MODEL_NAME)
        model.order(ByteOrder.nativeOrder())
        interpreter = Interpreter(model)
        initModelShape()
    }

    private fun initModelShape() {
        val inputTensor : Tensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        modelInputDataType = inputTensor.dataType()
        inputImage = TensorImage(modelInputDataType)

        val outputTensor = interpreter.getOutputTensor(0)
        modelOutputDataType = outputTensor.dataType()
        modelOutputShape = outputTensor.shape()
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())

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
            .add(CastOp(modelInputDataType))
            .build()

        return imageProcessor.process(inputImage)
    }

    fun classify(image: Bitmap) : Bitmap{
        inputImage = loadImage(image)

        val height = inputImage.height
        val width = inputImage.width
        val size = outputBuffer.flatSize

        val outputMap = HashMap<Int, Any>()
        val outputBox = Array(modelOutputShape[0]){
            Array(modelOutputShape[1]){
                FloatArray(modelOutputShape[2])
            }
        }
        val outputCategory = Array(modelOutputShape[0]){
            FloatArray(modelOutputShape[1])
        }
        val outputScore = Array(modelOutputShape[0]){
            FloatArray(modelOutputShape[1])
        }
        val outputCount = FloatArray(modelOutputShape[0])

        outputMap[0] = outputBox
        outputMap[1] = outputCategory
        outputMap[2] = outputScore
        outputMap[3] = outputCount

        interpreter.runForMultipleInputsOutputs(arrayOf<Any>(inputImage.buffer), outputMap)

//        interpreter.run(inputImage.buffer, outputBuffer.buffer.rewind())
        return inputImage.bitmap
    }


    private fun argmax(map: Map<String, Float>): Pair<String, Float> {
        var maxKey = ""
        var maxVal = -1f
        for (entry in map.entries) {
            val f = entry.value
            if (f > maxVal) {
                maxKey = entry.key
                maxVal = f
            }
        }
        return Pair(maxKey, maxVal)
    }

    private fun convertBitmapToARGB8888(bitmap: Bitmap): Bitmap {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun finish() {
        interpreter.close()
    }



    companion object {
        val MODEL_NAME = "model.tflite"

    }
}