package com.example.camerarecognition

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val REQUEST_GET_CONTENT = 1
private const val REQUEST_TAKE_PICTURE = 2

class MainActivity : AppCompatActivity() {

    var mCurrentPhotoPath: String = ""
    private val PERMISSION_REQUEST_CODE: Int = 101
    private lateinit var image: Bitmap
    private lateinit var classifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        classifier = ImageClassifier(getAssets())

        button.setOnClickListener{
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, REQUEST_GET_CONTENT)
            }
        }

        button2.setOnClickListener{
            Log.d("TEST", "CAMERA")
            if (checkPersmission())
            {
                startCamera()
            }
            else
            {
                requestPermission()
            }
        }
    }

    fun startCamera()
    {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val file: File = createFile()
        Log.d("TEST", file.absolutePath)
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.example.android.fileprovider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_TAKE_PICTURE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }

    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA), PERMISSION_REQUEST_CODE)
    }

    private fun createFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GET_CONTENT)
        {
            if (resultCode == Activity.RESULT_OK){
                imageView.setImageURI(data?.data)
                image = imageView.drawable.toBitmap()

                onImageSet()
            }
        }
        if (requestCode == REQUEST_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                val exif = ExifInterface(mCurrentPhotoPath)

                image = BitmapFactory.decodeFile(mCurrentPhotoPath)

                val orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION)!!
                if (orientation.toInt() == 6) {
                    val matrix = Matrix()
                    matrix.postRotate(90F)
                    image = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
                }

                imageView.setImageBitmap(image)

                onImageSet()
            }
        }
    }

    fun onImageSet()
    {
        val scaledImage = Bitmap.createScaledBitmap(image, 224, 224, false)
        val prediction = classifier.recognizeImage(scaledImage)
        Log.d("LOG_TEST_PRED", prediction.get(0).location.toString())
        var out_str = ""
        for (i in 0..2)
        {
            out_str += prediction[i].oneToString() + "\n"
        }
        textView.text = out_str
    }

}
