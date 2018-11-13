package com.angcyo.dingding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import com.angcyo.uiview.less.base.BaseAppCompatActivity
import com.angcyo.uiview.less.manager.Screenshot
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.experimental.and


class MainActivity : BaseAppCompatActivity() {

    companion object {
        val Tag = "angcyo"
        //14780977
        //vGcIcmO6OWnPcBBv9TzZryiD
        //Aa8lePlFQ8cp1py9GZUrrdkZGEyY2Tln
    }

    private var metrics: DisplayMetrics? = null
    private var display: Display? = null
    private var height: Int = 0
    private var width: Int = 0
    private var deepth: Int = 0

    private var imageRender: ImageReader? = null

    var screenshot: Screenshot? = null

    private val mainHandler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(viewHolder.v(R.id.toolbar))

        viewHolder.view(R.id.fab).setOnClickListener { view ->
            //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()

//            findViewById<ImageView>(R.id.image_view).setImageBitmap(takePicBitmap())

            val mediaProjectionManager: MediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            //startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 901)

            screenshot?.startCapture(this, 909)
        }

        metrics = DisplayMetrics()
        val WM = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        display = WM.defaultDisplay
        display?.getMetrics(metrics)

        val pixelformat = display!!.getPixelFormat()
        val localPixelFormat1 = PixelFormat()
        PixelFormat.getPixelFormatInfo(pixelformat, localPixelFormat1)

        //平板默认的是横屏
        this.width = metrics!!.widthPixels; // 屏幕的宽
        this.height = metrics!!.heightPixels; // 屏幕的高
        this.deepth = localPixelFormat1.bytesPerPixel;// 位深

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 900)

        screenshot = Screenshot.capture(this) { bitmap, filePath ->
            Log.i(
                "angcyo",
                "${bitmap.allocationByteCount.toLong()}   ${Formatter.formatFileSize(
                    this@MainActivity,
                    bitmap.allocationByteCount.toLong()
                )}"
            )
            findViewById<ImageView>(R.id.image_view).setImageBitmap(bitmap)
        }.setAlwaysCapture(true).setCaptureDelay(1_000)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        screenshot?.onActivityResult(resultCode, data)
    }

    @Throws(Exception::class)
    fun takePicBitmap(): Bitmap {
        val startTime = System.currentTimeMillis()
        Log.i(Tag, startTime.toString() + "----开始获取/dev/graphic/fb0的数据-------")
        val stream = FileInputStream(File("/dev/graphics/fb0"))
        val piex = ByteArray(height * width * deepth)
        val dStream = DataInputStream(stream)
        dStream.read(piex, 0, height * width * deepth)
        // ����ͼƬ
        val colors = IntArray(height * width)
        for (m in colors.indices) {
            val b: Int = (piex[m * 4] and 0xFF.toByte()).toInt()
            val g: Int = (piex[m * 4 + 1] and 0xFF.toByte()).toInt()
            val r: Int = (piex[m * 4 + 2] and 0xFF.toByte()).toInt()
            val a: Int = (piex[m * 4 + 3] and 0xFF.toByte()).toInt()
            colors[m] = (a shl 24) + (r shl 16) + (g shl 8) + b

            // int b = (piex[m * 4 + 1] & 0xFF);
            // int g = (piex[m * 4 + 2] & 0xFF);
            // int r = (piex[m * 4 + 3] & 0xFF);
            // int a = (piex[m * 4 ] & 0xFF);
            // colors[m] = (r << 24) + (g << 16) + (b << 8) + a;
        }
        val bitmap = Bitmap.createBitmap(
            colors, width, height,
            Bitmap.Config.ARGB_4444
        )
        val endtime = System.currentTimeMillis()
        Log.i(
            Tag, endtime.toString() + "----转码完成-----耗时" + (endtime - startTime)
                    + "毫秒------"
        )
        return bitmap
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
