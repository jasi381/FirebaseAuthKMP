package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSTimer
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIFont
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import swiftPMImport.dev.jasmeetsingh.composeApp.AgoraRtcEngineDelegateProtocol
import swiftPMImport.dev.jasmeetsingh.composeApp.AgoraRtcEngineKit
import swiftPMImport.dev.jasmeetsingh.composeApp.AgoraRtcVideoCanvas

// ── Agora Delegate ──────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private class AgoraDelegate(
    private val engine: AgoraRtcEngineKit,
    private val remoteView: UIView,
    private val handler: CallActionHandler,
    private val isVideo: Boolean,
) : NSObject(), AgoraRtcEngineDelegateProtocol {

    override fun rtcEngine(engine: AgoraRtcEngineKit, didJoinedOfUid: ULong, elapsed: Long) {
        handler.statusLabel?.text = "Connected"
        handler.startTimer()

        if (isVideo) {
            val remoteCanvas = AgoraRtcVideoCanvas()
            remoteCanvas.view = remoteView
            remoteCanvas.renderMode = 1u // RENDER_MODE_HIDDEN
            remoteCanvas.uid = didJoinedOfUid
            engine.setupRemoteVideo(remoteCanvas)
        }
    }

    override fun rtcEngine(engine: AgoraRtcEngineKit, didOfflineOfUid: ULong, reason: ULong) {
        handler.timer?.invalidate()
        handler.statusLabel?.text = "Call ended"
        handler.timerLabel?.hidden = true
        val dismissHandler = handler
        NSTimer.scheduledTimerWithTimeInterval(1.0, repeats = false) { _ ->
            dismissHandler.endCall()
        }
    }
}

// ── Action Handler ───────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private class CallActionHandler(
    private val engine: AgoraRtcEngineKit,
    private val callVC: UIViewController,
) : NSObject() {

    var isMuted = false
    var isVideoOff = false
    var isSpeakerOn = true
    var seconds = 0
    var timer: NSTimer? = null
    var timerLabel: UILabel? = null
    var statusLabel: UILabel? = null
    var micBtn: UIButton? = null
    var videoBtn: UIButton? = null
    var speakerBtn: UIButton? = null
    var onCallEnded: (() -> Unit)? = null

    // Consistent color system: active = bright surface, inactive = dimmed surface
    private val btnActive = UIColor(white = 1.0, alpha = 0.15)
    private val btnInactive = UIColor(white = 1.0, alpha = 0.08)
    private val iconActive = UIColor(white = 1.0, alpha = 1.0)
    private val iconInactive = UIColor(white = 1.0, alpha = 0.4)

    private fun updateToggleButton(btn: UIButton?, active: Boolean) {
        btn?.backgroundColor = if (active) btnActive else btnInactive
        btn?.tintColor = if (active) iconActive else iconInactive
    }

    @ObjCAction
    fun endCall() {
        timer?.invalidate()
        engine.leaveChannel(null)
        AgoraRtcEngineKit.destroy()
        callVC.dismissViewControllerAnimated(true, completion = null)
        onCallEnded?.invoke()
    }

    @ObjCAction
    fun toggleMic() {
        isMuted = !isMuted
        engine.muteLocalAudioStream(isMuted)
        updateToggleButton(micBtn, active = !isMuted)
    }

    @ObjCAction
    fun toggleVideo() {
        isVideoOff = !isVideoOff
        engine.muteLocalVideoStream(isVideoOff)
        if (isVideoOff) engine.stopPreview() else engine.startPreview()
        updateToggleButton(videoBtn, active = !isVideoOff)
    }

    @ObjCAction
    fun flipCamera() {
        engine.switchCamera()
    }

    @ObjCAction
    fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        engine.setEnableSpeakerphone(isSpeakerOn)
        updateToggleButton(speakerBtn, active = isSpeakerOn)
    }

    @ObjCAction
    fun tick() {
        seconds++
        val m = seconds / 60
        val s = seconds % 60
        timerLabel?.text = "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    fun startTimer() {
        if (timer != null) return // already started
        statusLabel?.text = "Connected"
        timerLabel?.hidden = false
        timer = NSTimer.scheduledTimerWithTimeInterval(1.0, target = this,
            selector = NSSelectorFromString("tick"), userInfo = null, repeats = true)
    }
}

private fun colorFromHex(hex: ULong): UIColor {
    val r = ((hex shr 16) and 0xFFu).toDouble() / 255.0
    val g = ((hex shr 8) and 0xFFu).toDouble() / 255.0
    val b = (hex and 0xFFu).toDouble() / 255.0
    return UIColor(red = r, green = g, blue = b, alpha = 1.0)
}

// ── CallService ──────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
actual class CallService actual constructor() {

    // Strong references to prevent GC during call
    private var activeHandler: CallActionHandler? = null
    private var activeDelegate: AgoraDelegate? = null

    actual fun startVideoCall(channelName: String) {
        launchCall(channelName, isVideo = true)
    }

    actual fun startAudioCall(channelName: String) {
        launchCall(channelName, isVideo = false)
    }

    @Suppress("DEPRECATION")
    private fun launchCall(channelName: String, isVideo: Boolean) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val screenWidth = CGRectGetWidth(rootVC.view.bounds)
        val screenHeight = CGRectGetHeight(rootVC.view.bounds)

        // Create engine WITHOUT delegate first (we set it after creating handler)
        val engine = AgoraRtcEngineKit.sharedEngineWithAppId(AGORA_APP_ID, delegate = null)
        engine.setAudioProfile(0) // 0 = Default
        engine.setAudioScenario(0) // 0 = Default
        engine.enableAudio()
        engine.adjustPlaybackSignalVolume(400) // boost remote audio volume
        engine.setEnableSpeakerphone(true)
        engine.muteAllRemoteAudioStreams(false)
        // setDefaultMuteAllRemoteAudioStreams not available in this SDK version
        if (isVideo) {
            engine.enableVideo()
            engine.enableLocalVideo(true)
            engine.muteAllRemoteVideoStreams(false)
        }
        engine.setChannelProfile(1) // Communication
        engine.setClientRole(1) // Broadcaster

        val callVC = UIViewController()
        callVC.view.backgroundColor = colorFromHex(0xFF0F0F1Au)

        // ── Remote video (full screen) ──
        val remoteView = UIView(frame = CGRectMake(0.0, 0.0, screenWidth, screenHeight))
        callVC.view.addSubview(remoteView)

        // ── Local video PiP (top-right) ──
        val pipW = 120.0
        val pipH = 170.0
        val localView = UIView(frame = CGRectMake(screenWidth - pipW - 16.0, 60.0, pipW, pipH))
        localView.backgroundColor = colorFromHex(0xFF1A1A2Eu)
        localView.layer.cornerRadius = 16.0
        localView.clipsToBounds = true
        if (isVideo) {
            callVC.view.addSubview(localView)
            val canvas = AgoraRtcVideoCanvas()
            canvas.view = localView
            canvas.renderMode = 1u
            canvas.uid = 0u
            engine.setupLocalVideo(canvas)
            engine.startPreview()
        }

        val handler = CallActionHandler(engine, callVC)
        handler.onCallEnded = {
            activeHandler = null
            activeDelegate = null
        }
        activeHandler = handler // prevent GC

        // ── Set the delegate NOW with proper references ──
        val delegate = AgoraDelegate(engine, remoteView, handler, isVideo)
        activeDelegate = delegate // prevent GC
        engine.setDelegate(delegate)

        // ── Status label ──
        val statusLabel = UILabel(frame = CGRectMake(0.0, 56.0, screenWidth, 24.0)).apply {
            text = "Connecting..."
            textColor = UIColor.whiteColor
            font = UIFont.systemFontOfSize(17.0)
            textAlignment = 1L
        }
        callVC.view.addSubview(statusLabel)
        handler.statusLabel = statusLabel

        // ── Timer label ──
        val timerLabel = UILabel(frame = CGRectMake(0.0, 82.0, screenWidth, 20.0)).apply {
            text = "00:00"
            textColor = colorFromHex(0xFF94A3B8u)
            font = UIFont.monospacedDigitSystemFontOfSize(14.0, 0.0)
            textAlignment = 1L
            hidden = true
        }
        callVC.view.addSubview(timerLabel)
        handler.timerLabel = timerLabel

        // ── Audio-only placeholder ──
        if (!isVideo) {
            localView.hidden = true
            val avatarSize = 120.0
            val avatar = UIView(frame = CGRectMake(
                (screenWidth - avatarSize) * 0.5,
                screenHeight * 0.5 - 100.0,
                avatarSize, avatarSize
            ))
            avatar.backgroundColor = UIColor(white = 1.0, alpha = 0.1)
            avatar.layer.cornerRadius = avatarSize / 2.0
            callVC.view.addSubview(avatar)

            val iconView = UIImageView(frame = CGRectMake(
                (avatarSize - 48.0) / 2.0, (avatarSize - 48.0) / 2.0, 48.0, 48.0
            )).apply {
                image = UIImage.systemImageNamed("headphones")
                tintColor = UIColor.whiteColor
            }
            avatar.addSubview(iconView)

            val audioLabel = UILabel(frame = CGRectMake(0.0, screenHeight / 2.0 + 40.0, screenWidth, 24.0)).apply {
                text = "Audio Call"
                textColor = colorFromHex(0xFF94A3B8u)
                font = UIFont.systemFontOfSize(18.0)
                textAlignment = 1L
            }
            callVC.view.addSubview(audioLabel)
        }

        // ── Bottom controls bar ──
        val barH = 80.0
        val barY = screenHeight - barH - 48.0
        val barPadding = 24.0
        val btnSize = 56.0
        val endBtnSize = 64.0

        // Active = white 15% alpha, Inactive = white 8% alpha
        val btnActiveBg = UIColor(white = 1.0, alpha = 0.15)
        val btnInactiveBg = UIColor(white = 1.0, alpha = 0.08)
        val endCallRed = colorFromHex(0xFFFF3B30u)

        val barBg = UIView(frame = CGRectMake(barPadding, barY, screenWidth - barPadding * 2, barH))
        barBg.backgroundColor = UIColor(white = 0.0, alpha = 0.4)
        barBg.layer.cornerRadius = 40.0
        callVC.view.addSubview(barBg)

        val buttons = mutableListOf<UIButton>()

        // Mic: starts active (unmuted)
        val micBtn = makeControlButton("mic.fill", btnSize, btnActiveBg)
        micBtn.addTarget(handler, action = NSSelectorFromString("toggleMic"), forControlEvents = UIControlEventTouchUpInside)
        handler.micBtn = micBtn
        buttons.add(micBtn)

        if (isVideo) {
            // Video: starts active (camera on)
            val videoBtn = makeControlButton("video.fill", btnSize, btnActiveBg)
            videoBtn.addTarget(handler, action = NSSelectorFromString("toggleVideo"), forControlEvents = UIControlEventTouchUpInside)
            handler.videoBtn = videoBtn
            buttons.add(videoBtn)

            // Flip: always same style (not a toggle)
            val flipBtn = makeControlButton("camera.rotate.fill", btnSize, btnActiveBg)
            flipBtn.addTarget(handler, action = NSSelectorFromString("flipCamera"), forControlEvents = UIControlEventTouchUpInside)
            buttons.add(flipBtn)
        }

        // Speaker: starts active (speaker on)
        val speakerBtn = makeControlButton("speaker.wave.3.fill", btnSize, btnActiveBg)
        speakerBtn.addTarget(handler, action = NSSelectorFromString("toggleSpeaker"), forControlEvents = UIControlEventTouchUpInside)
        handler.speakerBtn = speakerBtn
        buttons.add(speakerBtn)

        // End call: the ONLY red button
        val endBtn = makeControlButton("phone.down.fill", endBtnSize, endCallRed)
        endBtn.addTarget(handler, action = NSSelectorFromString("endCall"), forControlEvents = UIControlEventTouchUpInside)
        buttons.add(endBtn)

        val totalBtns = buttons.size
        val barWidth = screenWidth - barPadding * 2
        val totalBtnWidth = buttons.fold(0.0) { acc, btn -> acc + if (btn == endBtn) endBtnSize else btnSize }
        val spacing = (barWidth - totalBtnWidth) / (totalBtns + 1).toDouble()
        var x = spacing
        for (btn in buttons) {
            val s = if (btn == endBtn) endBtnSize else btnSize
            btn.setFrame(CGRectMake(x, (barH - s) / 2.0, s, s))
            barBg.addSubview(btn)
            x += s + spacing
        }

        // ── Present & join ──
        callVC.modalPresentationStyle = UIModalPresentationFullScreen
        rootVC.presentViewController(callVC, animated = true, completion = null)

        engine.joinChannelByToken(
            null,
            channelId = channelName,
            info = null,
            uid = 0u,
        ) { _, _, _ ->
            statusLabel.text = "Waiting for others..."
        }
    }

    private fun makeControlButton(sfSymbolName: String, size: Double, bgColor: UIColor): UIButton {
        val btn = UIButton(frame = CGRectMake(0.0, 0.0, size, size))
        btn.backgroundColor = bgColor
        btn.layer.cornerRadius = size / 2.0
        val image = UIImage.systemImageNamed(sfSymbolName)
        btn.setImage(image, forState = 0u)
        btn.tintColor = UIColor.whiteColor
        return btn
    }
}
