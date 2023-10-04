package com.ej.defaultcamera_gallary_test.tflite

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset


class ClassifierWithSupport3 constructor(
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
    val labels = mutableListOf<String>()

    @Throws(IOException::class)
    fun init() {
        val model = FileUtil.loadMappedFile(context, MODEL_NAME)
        model.order(ByteOrder.nativeOrder())
        interpreter = Interpreter(model)
        initModelShape()

        val modelFile : MappedByteBuffer = loadModelFile(context.assets, MODEL_NAME)
        val metadata = MetadataExtractor(modelFile)

        BufferedReader(InputStreamReader(metadata.getAssociatedFile("labelmap.txt"), Charset.defaultCharset())).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                labels.add(line!!)
            }
        }
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
        val cropSize = Math.min(bitmap.width, bitmap.height)
        val imageProcessor = if (modelInputDataType == DataType.UINT8) {
            ImageProcessor.Builder()
//                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(
                    ResizeOp(
                        modelInputWidth,
                        modelInputHeight,
                        ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                    )
                )
                .add(NormalizeOp(0.0f, 255.0f))
                .add(QuantizeOp(128.0f, 1/128.0f))
                .add(CastOp(modelInputDataType))
                .build()
        } else {
            ImageProcessor.Builder()
                .add(
                    ResizeOp(
                        modelInputWidth,
                        modelInputHeight,
                        ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                    )
                )
                .add(NormalizeOp(0.0f, 255.0f))
                .add(CastOp(modelInputDataType))
                .build()
        }

        return imageProcessor.process(inputImage)
    }

    fun classify(image: Bitmap) : MutableList<Pair<String, FloatArray>>{
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

        val result = mutableListOf<Pair<String, FloatArray>>()
        for (i in outputScore[0].indices){
            if (outputScore[0][i] > 0.5) {
                val category = labels[i]
                val boundingBox = outputBox[0][i]

                val pair : Pair<String, FloatArray> = Pair(category, boundingBox)
                result.add(pair)
            }
        }
        return result
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


    @Throws(IOException::class)
    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    companion object {
        val MODEL_NAME = "efficient.tflite"

    }
}