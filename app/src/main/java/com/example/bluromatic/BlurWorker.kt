package com.example.bluromatic

import androidx.work.workDataOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.bluromatic.R
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.workers.blurBitmap
import com.example.bluromatic.workers.makeStatusNotification
import com.example.bluromatic.workers.writeBitmapToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "BlurWorker"

class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {

        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        return try {
            require(!resourceUri.isNullOrBlank()) {
                val errorMessage = applicationContext.resources.getString(R.string.invalid_input_uri)
                Log.e(TAG, errorMessage)
                errorMessage
            }
            val resolver = applicationContext.contentResolver

            val picture = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri))
            )

            // Menampilkan notifikasi bahwa proses blurring dimulai
            makeStatusNotification(
                applicationContext.resources.getString(R.string.blurring_image),
                applicationContext
            )

            // Delay untuk memberikan efek waktu proses
            delay(DELAY_TIME_MILLIS)

            // Pindahkan tugas I/O ke thread background
            val outputUri = withContext(Dispatchers.IO) {
                // Menerapkan efek blur pada gambar
                val output = blurBitmap(picture, blurLevel)

                // Menulis gambar yang sudah di-blur ke file
                writeBitmapToFile(applicationContext, output)
            }

            // Menampilkan notifikasi dengan output URI
            makeStatusNotification(
                "Output is $outputUri",
                applicationContext
            )

            // Menyiapkan output data
            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            // Mengembalikan hasil sukses
            Result.success(outputData)

        } catch (throwable: Throwable) {
            // Menangani error dan menampilkan notifikasi error
            Log.e(
                TAG,
                applicationContext.resources.getString(R.string.error_applying_blur),
                throwable
            )
            Result.failure()
        }
    }
}