package dev.jasmeetsingh.firebaseauth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.SystemClock
import android.view.SurfaceView
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.ImageView
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
    private var localPlaceholder: ImageView? = null

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
                cornerRadius = 40 * dp
                setColor(Color.argb(102, 0, 0, 0)) // black 40% alpha
            }
            background = bg
            setPadding(24.dp(), 16.dp(), 24.dp(), 16.dp())
            elevation = 12 * dp
        }

        val btnSize = 56.dp()
        val btnMargin = 12.dp()

        // Consistent button colors: active = white 15%, inactive = white 8%
        val activeBg = Color.argb(38, 255, 255, 255)   // white 15%
        val inactiveBg = Color.argb(20, 255, 255, 255)  // white 8%
        val endCallRed = Color.parseColor("#FF3B30")

        // Mic toggle — starts active (unmuted)
        val micBtn = createIconButton(R.drawable.ic_call_mic, btnSize, activeBg) {
            isMuted = !isMuted
            engine?.muteLocalAudioStream(isMuted)
            it.background = makeCircleBg(if (isMuted) inactiveBg else activeBg)
            it.alpha = if (isMuted) 0.4f else 1.0f
        }

        // Video toggle — starts active (camera on)
        val videoBtn = createIconButton(R.drawable.ic_call_videocam, btnSize, activeBg) {
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
            it.background = makeCircleBg(if (isVideoOff) inactiveBg else activeBg)
            it.alpha = if (isVideoOff) 0.4f else 1.0f
        }

        // Flip camera — always active style
        val flipBtn = createIconButton(R.drawable.ic_call_cameraswitch, btnSize, activeBg) {
            engine?.switchCamera()
        }

        // Speaker toggle — starts active (speaker on)
        val speakerBtn = createIconButton(R.drawable.ic_call_volume_up, btnSize, activeBg) {
            isSpeakerOn = !isSpeakerOn
            engine?.setEnableSpeakerphone(isSpeakerOn)
            it.background = makeCircleBg(if (isSpeakerOn) activeBg else inactiveBg)
            it.alpha = if (isSpeakerOn) 1.0f else 0.4f
        }

        // End call — the ONLY red button
        val endBtn = createIconButton(R.drawable.ic_call_end, 64.dp(), endCallRed) {
            leaveAndFinish()
        }

        fun addBtnWithMargin(btn: android.view.View, size: Int = btnSize) {
            val lp = LinearLayout.LayoutParams(size, size).apply {
                marginStart = btnMargin / 2
                marginEnd = btnMargin / 2
            }
            bottomBar.addView(btn, lp)
        }

        addBtnWithMargin(micBtn)
        if (isVideo) {
            addBtnWithMargin(videoBtn)
            addBtnWithMargin(flipBtn)
        }
        addBtnWithMargin(speakerBtn)
        addBtnWithMargin(endBtn, 64.dp())

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
            val avatarFrame = FrameLayout(this).apply {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(26, 255, 255, 255)) // white 10%
                }
                background = bg
            }
            val avatarIcon = ImageView(this).apply {
                setImageResource(R.drawable.ic_call_headset)
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val iconPad = (24 * dp).toInt()
                setPadding(iconPad, iconPad, iconPad, iconPad)
            }
            avatarFrame.addView(avatarIcon, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            val avatar = avatarFrame
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

    private fun createIconButton(
        drawableRes: Int,
        size: Int,
        bgColor: Int,
        onClick: (ImageView) -> Unit,
    ): ImageView {
        val iconPadding = (size * 0.28f).toInt()
        return ImageView(this).apply {
            setImageResource(drawableRes)
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            background = makeCircleBg(bgColor)
            elevation = 4 * resources.displayMetrics.density
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(this) }
        }
    }

    private fun makeCircleBg(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
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
            val placeholder = ImageView(this).apply {
                setImageResource(R.drawable.ic_call_videocam_off)
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.CENTER
                setBackgroundColor(Color.parseColor("#1A1A2E"))
                visibility = android.view.View.GONE
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
