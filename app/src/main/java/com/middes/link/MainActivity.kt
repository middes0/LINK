package com.middes.link

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private var captureService: ScreenCaptureService? = null
    private var bound = false

    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(serviceIntent)
            status.text = "Permissão concedida. Iniciando captura local…"
            bindService(Intent(this, ScreenCaptureService::class.java), connection, Context.BIND_AUTO_CREATE)
        } else {
            status.text = "Captura cancelada. Nenhuma tela está sendo compartilhada."
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            captureService = binder.getService()
            bound = true
            captureService?.setFrameListener { bitmap ->
                runOnUiThread {
                    preview.setImageBitmap(bitmap)
                    status.text = "Captura ativa neste aparelho. A tela não está sendo enviada pela internet."
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF090914.toInt())
        }
        val title = TextView(this).apply {
            text = "LINK"
            textSize = 32f
            setTextColor(0xFFEAE7FF.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Visualização de tela • protótipo 0.1"
            textSize = 14f
            setTextColor(0xFFB7B2D6.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }
        status = TextView(this).apply {
            text = "A captura só começa depois da autorização do Android."
            textSize = 14f
            setTextColor(0xFFEAE7FF.toInt())
            setPadding(0, 12, 0, 16)
        }
        val start = Button(this).apply {
            text = "Autorizar captura de tela"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                capturePermission.launch(manager.createScreenCaptureIntent())
            }
        }
        val stop = Button(this).apply {
            text = "Encerrar captura"
            setOnClickListener {
                captureService?.stopCapture()
                if (bound) {
                    unbindService(connection)
                    bound = false
                }
                captureService = null
                preview.setImageDrawable(null)
                status.text = "Captura encerrada."
                stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
            }
        }
        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFF171526.toInt())
            contentDescription = "Prévia local da tela capturada"
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(start, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(stop, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = 20
        })
        setContentView(root)
    }

    override fun onDestroy() {
        if (bound) {
            captureService?.setFrameListener(null)
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}
