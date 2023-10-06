package com.ej.auto_object_detection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ej.auto_object_detection.databinding.FragmentBlank4Binding
import com.ej.auto_object_detection.tflite.ClassifierWithSupport4
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class Blank4Fragment : Fragment() {
    lateinit var binding : FragmentBlank4Binding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

//    lateinit var cls : ClassifierWithModel
    lateinit var cls : ClassifierWithSupport4

    var imageHeight = 0
    var imageWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permissions



        cameraExecutor = Executors.newSingleThreadExecutor()

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(inflater,R.layout.fragment_blank4,container,false)
        binding.lifecycleOwner = this.viewLifecycleOwner

//        cls = ClassifierWithModel(requireContext())
        cls = ClassifierWithSupport4(requireContext())
        try {
            cls.init()
        } catch (e : IOException) {
            e.printStackTrace()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun analyzeImageFromTflite(bitmap: Bitmap) {
        imageHeight = bitmap.height
        imageWidth = bitmap.width

        val result = cls.classify(bitmap)

        if(result.isEmpty()) return

        val resultOne = result[0]

        val boundingBoxTop = if (resultOne.second[0] < 0) 0f else resultOne.second[0]
        val boundingBoxLeft = if (resultOne.second[1] < 0) 0f else resultOne.second[1]
        val boundingBoxBottom = if (resultOne.second[2] > 1) 1f else resultOne.second[2]
        val boundingBoxRight = if (resultOne.second[3] > 1) 1f else resultOne.second[3]

        val top = boundingBoxTop * imageHeight
        val left = boundingBoxLeft * imageWidth
        val bottom = boundingBoxBottom * imageHeight
        val right = boundingBoxRight * imageWidth

        val cropLeft = left.toInt()
        val cropTop = top.toInt()
        val cropWidth = (right - left).toInt()
        val cropHeight = (bottom - top).toInt()
        val croppedBitmap: Bitmap =
            Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)

        activity?.let {
            it.runOnUiThread {
                binding.imageView.setImageBitmap(croppedBitmap)
                binding.textView.text = resultOne.first
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireActivity(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireActivity(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        // 여기에서 비트맵 이미지를 얻을 수 있습니다.
                        val bitmap = image.toBitmap()


                        // 비트맵을 처리하거나 저장할 수 있습니다.
                        val rotationsDegrees = image.imageInfo.rotationDegrees

                        // 이미지를 회전시킬 Matrix를 생성합니다.
                        // 이미지를 회전시킬 Matrix를 생성합니다.
                        val matrix = Matrix()
                        matrix.postRotate(rotationsDegrees.toFloat())

                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        bitmap.recycle()
                        analyzeImageFromTflite(rotatedBitmap)


//                        requireActivity().runOnUiThread {
//                            binding.imageView.setImageBitmap(rotatedBitmap)
//                        }

                        image.close()
                    }
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }


    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10

        fun newInstance() =
            Blank4Fragment()

        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}


private fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}