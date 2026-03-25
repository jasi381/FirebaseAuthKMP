package dev.jasmeetsingh.firebaseauth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.SurfaceView
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.drawable.GradientDrawable
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class CallActivity : ComponentActivity() {

    private var engine: RtcEngine? = null
    private lateinit var channelName: String
    private var isVideo = true

    private lateinit var localContainer: FrameLayout
    private lateinit var remoteContainer: FrameLayout
    private lateinit var chronometer: Chronometer
    private lateinit var statusText: TextView

    private var isMuted = false
    private var isVideoOff = false
    private var isSpeakerOn = true
    private var remoteUid = -1

    private var localSurfaceView: SurfaceView? = null
    private var localPlaceholder: TextView? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            initAgora()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        channelName = intent.getStringExtra("channel") ?: return finish()
        isVideo = intent.getBooleanExtra("isVideo", true)

        setContentView(buildUI())
        checkPermissionsAndStart()
    }

    // ── UI ────────────────────────────────────────────────────────────────

    private fun buildUI(): FrameLayout {
        val dp = resources.displayMetrics.density
        fun Int.dp() = (this * dp).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0F0F1A"))
        }

        // Remote video (full screen)
        remoteContainer = FrameLayout(this)
        root.addView(remoteContainer, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Local video PiP (top-right)
        localContainer = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                cornerRadius = 16 * dp
                setColor(Color.parseColor("#1A1A2E"))
            }
            background = bg
            clipToOutline = true
            elevation = 8 * dp
        }
        val pipParams = FrameLayout.LayoutParams(120.dp(), 170.dp()).apply {
            gravity = Gravity.END or Gravity.TOP
            marginEnd = 16.dp()
            topMargin = 60.dp()
        }
        root.addView(localContainer, pipParams)

        // Top overlay — status + timer
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 56.dp(), 0, 16.dp())
        }
        statusText = TextView(this).apply {
            text = "Connecting..."
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }
        chronometer = Chronometer(this).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        topBar.addView(statusText)
        topBar.addView(chronometer)
        root.addView(topBar, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))

        // Bottom controls bar
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                cornerRadius = 32 * dp
                setColor(Color.parseColor("#1E293B"))
            }
            background = bg
            setPadding(24.dp(), 16.dp(), 24.dp(), 16.dp())
            elevation = 12 * dp
        }

        val btnSize = 56.dp()
        val btnMargin = 12.dp()

        // Mic toggle
        val micBtn = createControlButton(
            "\uD83C\uDF99", btnSize,
            Color.parseColor("#334155"), Color.parseColor("#EF4444")
        ) {
            isMuted = !isMuted
            engine?.muteLocalAudioStream(isMuted)
            it.background = makeCircleBg(if (isMuted) "#EF4444" else "#334155")
        }

        // Video toggle (only for video calls)
        val videoBtn = createControlButton(
            "\uD83D\uDCF7", btnSize,
            Color.parseColor("#334155"), Color.parseColor("#EF4444")
        ) {
            isVideoOff = !isVideoOff
            engine?.muteLocalVideoStream(isVideoOff)
            if (isVideoOff) {
                engine?.stopPreview()
                localSurfaceView?.visibility = android.view.View.GONE
                localPlaceholder?.visibility = android.view.View.VISIBLE
            } else {
                localSurfaceView?.visibility = android.view.View.VISIBLE
                localPlaceholder?.visibility = android.view.View.GONE
                engine?.startPreview()
            }
            it.background = makeCircleBg(if (isVideoOff) "#EF4444" else "#334155")
        }

        // Flip camera
        val flipBtn = createControlButton(
            "\uD83D\uDD04", btnSize,
            Color.parseColor("#334155"), Color.parseColor("#334155")
        ) {
            engine?.switchCamera()
        }

        // Speaker toggle
        val speakerBtn = createControlButton(
            "\uD83D\uDD0A", btnSize,
            Color.parseColor("#6366F1"), Color.parseColor("#334155")
        ) {
            isSpeakerOn = !isSpeakerOn
            engine?.setEnableSpeakerphone(isSpeakerOn)
            it.background = makeCircleBg(if (isSpeakerOn) "#6366F1" else "#334155")
        }

        // End call
        val endBtn = createControlButton(
            "\uD83D\uDCDE", 64.dp(),
            Color.parseColor("#EF4444"), Color.parseColor("#EF4444")
        ) {
            leaveAndFinish()
        }

        fun addWithMargin(btn: android.view.View) {
            val lp = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = btnMargin / 2
                marginEnd = btnMargin / 2
            }
            bottomBar.addView(btn, lp)
        }

        addWithMargin(micBtn)
        if (isVideo) {
            addWithMargin(videoBtn)
            addWithMargin(flipBtn)
        }
        addWithMargin(speakerBtn)

        val endLp = LinearLayout.LayoutParams(64.dp(), 64.dp()).apply {
            marginStart = btnMargin
        }
        bottomBar.addView(endBtn, endLp)

        val bottomParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 48.dp()
        }
        root.addView(bottomBar, bottomParams)

        // Audio-only placeholder
        if (!isVideo) {
            val audioPlaceholder = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val avatar = TextView(this).apply {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#6366F1"))
                }
                background = bg
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 40f
                text = "\uD83C\uDFA7"
            }
            audioPlaceholder.addView(avatar, LinearLayout.LayoutParams(120.dp(), 120.dp()).apply {
                gravity = Gravity.CENTER
            })
            val label = TextView(this).apply {
                text = "Audio Call"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 16.dp(), 0, 0)
            }
            audioPlaceholder.addView(label)
            root.addView(audioPlaceholder, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
            localContainer.visibility = android.view.View.GONE
        }

        return root
    }

    private fun createControlButton(
        emoji: String,
        size: Int,
        normalColor: Int,
        activeColor: Int,
        onClick: (TextView) -> Unit,
    ): TextView {
        return TextView(this).apply {
            text = emoji
            textSize = 22f
            gravity = Gravity.CENTER
            background = makeCircleBg(String.format("#%06X", 0xFFFFFF and normalColor))
            elevation = 4 * resources.displayMetrics.density
            setOnClickListener { onClick(this) }
        }
    }

    private fun makeCircleBg(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(colorHex))
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────

    private fun checkPermissionsAndStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) needed.add(Manifest.permission.CAMERA)

        val notGranted = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            initAgora()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    // ── Agora ────────────────────────────────────────────────────────────

    private fun initAgora() {
        val config = RtcEngineConfig().apply {
            mContext = this@CallActivity
            mAppId = AGORA_APP_ID
            mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "Joined channel=$channel uid=$uid")
                    runOnUiThread {
                        statusText.text = "Waiting for others..."
                    }
                }

                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    android.util.Log.d("AgoraCall", "Connection state=$state reason=$reason")
                }

                private fun onRemoteConnected(uid: Int) {
                    if (remoteUid == uid) return // already handled
                    remoteUid = uid
                    statusText.text = "Connected"
                    chronometer.apply {
                        visibility = android.view.View.VISIBLE
                        base = SystemClock.elapsedRealtime()
                        start()
                    }
                    if (isVideo) {
                        val remoteView = SurfaceView(this@CallActivity).apply {
                            setZOrderOnTop(false)
                        }
                        remoteContainer.removeAllViews()
                        remoteContainer.addView(remoteView, FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                        engine?.setupRemoteVideo(
                            VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
                        )
                    }
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "onUserJoined uid=$uid")
                    runOnUiThread { onRemoteConnected(uid) }
                }

                override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "onRemoteAudioStateChanged uid=$uid state=$state")
                    if (state == 2) runOnUiThread { onRemoteConnected(uid) }
                }

                override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "onRemoteVideoStateChanged uid=$uid state=$state")
                    if (state == 2) runOnUiThread { onRemoteConnected(uid) }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread {
                        chronometer.stop()
                        statusText.text = "Call ended"
                        remoteContainer.removeAllViews()
                        remoteContainer.postDelayed({ leaveAndFinish() }, 1000)
                    }
                }
            }
        }

        engine = RtcEngine.create(config)
        engine?.setEnableSpeakerphone(true)

        if (isVideo) {
            engine?.enableVideo()

            // Local SurfaceView — setZOrderMediaOverlay so PiP renders on top of remote
            val localView = SurfaceView(this).apply {
                setZOrderMediaOverlay(true)
            }
            localSurfaceView = localView
            localContainer.addView(localView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            // Placeholder shown when camera is off
            val dp = resources.displayMetrics.density
            val placeholder = TextView(this).apply {
                text = "\uD83D\uDCF7"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                visibility = android.view.View.GONE
                setBackgroundColor(Color.parseColor("#1A1A2E"))
            }
            localPlaceholder = placeholder
            localContainer.addView(placeholder, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            engine?.setupLocalVideo(
                VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
            )
            engine?.startPreview()
        }

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishCameraTrack = isVideo
            publishMicrophoneTrack = true
        }
        engine?.joinChannel(null, channelName, 0, options)
    }

    private fun leaveAndFinish() {
        engine?.leaveChannel()
        engine?.stopPreview()
        RtcEngine.destroy()
        engine = null
        finish()
    }

    override fun onDestroy() {
        leaveAndFinish()
        super.onDestroy()
    }
}
