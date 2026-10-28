package com.middes.link

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var remoteSdp: EditText
    private lateinit var localSdp: EditText
    private lateinit var videoView: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var capturer: ScreenCapturerAndroid? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var sending = false
    private var receiving = false
    private var waitingForLocalSdp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        initializeWebRtc()
        buildUi()
    }

    private fun initializeWebRtc() {
        eglBase = EglBase.create()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 24)
            setBackgroundColor(0xFF090914.toInt())
        }
        val title = TextView(this).apply {
            text = "LINK"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(0xFFEAE7FF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "Compartilhamento de tela P2P"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFFB7B2D6.toInt())
            setPadding(0, 4, 0, 14)
        }
        status = TextView(this).apply {
            text = "Escolha transmitir ou receber. Os dois celulares precisam do LINK."
            textSize = 14f
            setTextColor(0xFFEAE7FF.toInt())
            setPadding(0, 8, 0, 12)
        }
        val sendButton = Button(this).apply {
            text = "Transmitir esta tela"
            setOnClickListener {
                resetConnection()
                sending = true
                receiving = false
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                @Suppress("DEPRECATION")
                startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
            }
        }
        val receiveButton = Button(this).apply {
            text = "Receber tela de outro celular"
            setOnClickListener {
                resetConnection()
                receiving = true
                sending = false
                if (!createPeer()) {
                    status.text = "Não foi possível iniciar a conexão WebRTC."
                    return@setOnClickListener
                }
                peer?.addTransceiver(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                    RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
                )
                status.text = "Cole a oferta do celular transmissor no campo abaixo e toque em Gerar resposta."
                remoteSdp.visibility = View.VISIBLE
                localSdp.visibility = View.VISIBLE
                findViewById<Button>(ID_CREATE_ANSWER).visibility = View.VISIBLE
                findViewById<Button>(ID_APPLY_ANSWER).visibility = View.GONE
                videoView.visibility = View.VISIBLE
            }
        }
        remoteSdp = EditText(this).apply {
            hint = "Cole aqui a oferta ou resposta recebida do outro celular"
            setTextColor(0xFFEAE7FF.toInt())
            setHintTextColor(0xFF8983A9.toInt())
            textSize = 11f
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP
            setBackgroundColor(0xFF171526.toInt())
            visibility = View.GONE
        }
        localSdp = EditText(this).apply {
            hint = "O código de conexão aparecerá aqui"
            setTextColor(0xFFEAE7FF.toInt())
            setHintTextColor(0xFF8983A9.toInt())
            textSize = 11f
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP
            setBackgroundColor(0xFF171526.toInt())
            isFocusable = true
            visibility = View.GONE
        }
        val createAnswer = Button(this).apply {
            id = ID_CREATE_ANSWER
            text = "Gerar resposta"
            visibility = View.GONE
            setOnClickListener { createAnswerFromOffer() }
        }
        val applyAnswer = Button(this).apply {
            id = ID_APPLY_ANSWER
            text = "Conectar usando a resposta"
            visibility = View.GONE
            setOnClickListener { applyRemoteAnswer() }
        }
        val copy = Button(this).apply {
            text = "Copiar código exibido"
            setOnClickListener {
                val value = localSdp.text.toString().trim()
                if (value.isNotEmpty()) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("LINK SDP", value))
                    status.text = "Código copiado. Envie-o ao outro celular."
                } else {
                    status.text = "Ainda não há código para copiar."
                }
            }
        }
        val paste = Button(this).apply {
            text = "Colar código recebido"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    remoteSdp.setText(clip.getItemAt(0).coerceToText(this@MainActivity))
                    status.text = "Código colado. Continue com o botão correspondente."
                } else {
                    status.text = "A área de transferência está vazia."
                }
            }
        }
        val stop = Button(this).apply {
            text = "Encerrar conexão"
            setOnClickListener {
                resetConnection()
                status.text = "Conexão encerrada. Nenhuma tela está sendo transmitida."
            }
        }
        videoView = SurfaceViewRenderer(this).apply {
            setBackgroundColor(0xFF171526.toInt())
            visibility = View.GONE
            contentDescription = "Tela recebida do outro celular"
        }
        videoView.init(eglBase.eglBaseContext, null)
        videoView.setEnableHardwareScaler(true)

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(sendButton, matchWrap())
        root.addView(receiveButton, matchWrap())
        root.addView(remoteSdp, matchWrap())
        root.addView(createAnswer, matchWrap())
        root.addView(applyAnswer, matchWrap())
        root.addView(localSdp, matchWrap())
        root.addView(copy, matchWrap())
        root.addView(paste, matchWrap())
        root.addView(stop, matchWrap())
        root.addView(videoView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 560
        ).apply { topMargin = 12 })
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun createPeer(): Boolean {
        if (peer != null) return true
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                runOnUiThread {
                    status.text = when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> "Conectado. A transmissão P2P está ativa."
                        PeerConnection.IceConnectionState.FAILED -> "A conexão direta falhou nesta rede. Ainda não há retransmissão TURN configurada."
                        PeerConnection.IceConnectionState.DISCONNECTED -> "Conexão interrompida. Tentando recuperar…"
                        PeerConnection.IceConnectionState.CLOSED -> "Conexão encerrada."
                        else -> "Negociando conexão entre os celulares…"
                    }
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                if (state == PeerConnection.IceGatheringState.COMPLETE && waitingForLocalSdp) {
                    waitingForLocalSdp = false
                    runOnUiThread {
                        localSdp.setText(peer?.localDescription?.description.orEmpty())
                        status.text = if (sending) {
                            "Oferta pronta. Copie o código, envie ao celular receptor e depois cole aqui a resposta dele."
                        } else {
                            "Resposta pronta. Copie o código e envie de volta ao celular transmissor."
                        }
                    }
                }
            }
            override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track() as? VideoTrack
                track?.addSink(videoView)
                runOnUiThread { videoView.visibility = View.VISIBLE }
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track() as? VideoTrack
                track?.addSink(videoView)
                runOnUiThread { videoView.visibility = View.VISIBLE }
            }
        })
        return peer != null
    }

    @Deprecated("Usa o fluxo de autorização de captura do Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE) return
        if (resultCode != RESULT_OK || data == null) {
            sending = false
            status.text = "Captura cancelada. Nenhuma tela está sendo transmitida."
            return
        }
        if (!createPeer()) {
            status.text = "Não foi possível iniciar WebRTC."
            return
        }
        startForegroundService(Intent(this, LinkStreamingService::class.java))
        try {
            capturer = ScreenCapturerAndroid(data, object : android.media.projection.MediaProjection.Callback() {
                override fun onStop() {
                    runOnUiThread { status.text = "O Android encerrou a captura de tela." }
                }
            })
            surfaceTextureHelper = SurfaceTextureHelper.create("LINK-CaptureThread", eglBase.eglBaseContext)
            videoSource = factory?.createVideoSource(true)
            capturer?.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
            val metrics = resources.displayMetrics
            capturer?.startCapture(metrics.widthPixels.coerceAtMost(1280), metrics.heightPixels.coerceAtMost(1280), 15)
            videoTrack = factory?.createVideoTrack("LINK-screen-track", videoSource)
            videoTrack?.setEnabled(true)
            videoTrack?.addSink(videoView)
            videoView.visibility = View.VISIBLE
            peer?.addTrack(videoTrack, listOf("link-screen"))
            remoteSdp.visibility = View.VISIBLE
            localSdp.visibility = View.VISIBLE
            findViewById<Button>(ID_APPLY_ANSWER).visibility = View.VISIBLE
            findViewById<Button>(ID_CREATE_ANSWER).visibility = View.GONE
            createOffer()
        } catch (e: Exception) {
            status.text = "Falha ao iniciar transmissão: ${e.message ?: "erro desconhecido"}"
            resetConnection()
        }
    }

    private fun createOffer() {
        val connection = peer ?: return
        waitingForLocalSdp = true
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return
                connection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        runOnUiThread { status.text = "Preparando oferta de conexão…" }
                    }
                    override fun onSetFailure(error: String?) = showError("Falha ao definir oferta: $error")
                    override fun onCreateSuccess(description: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, description)
            }
            override fun onCreateFailure(error: String?) = showError("Falha ao criar oferta: $error")
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, org.webrtc.MediaConstraints())
    }

    private fun createAnswerFromOffer() {
        val offer = remoteSdp.text.toString().trim()
        if (!offer.startsWith("v=")) {
            status.text = "Cole uma oferta SDP completa antes de continuar."
            return
        }
        val connection = peer ?: run {
            status.text = "Toque primeiro em Receber tela de outro celular."
            return
        }
        waitingForLocalSdp = true
        val description = SessionDescription(SessionDescription.Type.OFFER, offer)
        connection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                connection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        if (answer == null) return
                        connection.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                runOnUiThread { status.text = "Preparando resposta de conexão…" }
                            }
                            override fun onSetFailure(error: String?) = showError("Falha ao definir resposta: $error")
                            override fun onCreateSuccess(description: SessionDescription?) {}
                            override fun onCreateFailure(error: String?) {}
                        }, answer)
                    }
                    override fun onCreateFailure(error: String?) = showError("Falha ao criar resposta: $error")
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                }, org.webrtc.MediaConstraints())
            }
            override fun onSetFailure(error: String?) = showError("Oferta inválida: $error")
            override fun onCreateSuccess(description: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, description)
    }

    private fun applyRemoteAnswer() {
        val answer = remoteSdp.text.toString().trim()
        if (!answer.startsWith("v=")) {
            status.text = "Cole a resposta SDP do celular receptor."
            return
        }
        peer?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                runOnUiThread { status.text = "Resposta aplicada. Aguardando conexão P2P…" }
            }
            override fun onSetFailure(error: String?) = showError("Resposta inválida: $error")
            override fun onCreateSuccess(description: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, answer))
    }

    private fun showError(message: String) {
        runOnUiThread { status.text = message }
    }

    private fun resetConnection() {
        waitingForLocalSdp = false
        try { capturer?.stopCapture() } catch (_: Exception) {}
        capturer?.dispose()
        capturer = null
        videoTrack?.dispose()
        videoTrack = null
        videoSource?.dispose()
        videoSource = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        peer?.close()
        peer?.dispose()
        peer = null
        remoteSdp.setText("")
        localSdp.setText("")
        videoView.clearImage()
        videoView.visibility = View.GONE
        stopService(Intent(this, LinkStreamingService::class.java))
    }

    override fun onDestroy() {
        resetConnection()
        videoView.release()
        factory?.dispose()
        eglBase.release()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CAPTURE = 4107
        private const val ID_CREATE_ANSWER = 5101
        private const val ID_APPLY_ANSWER = 5102
    }
}
