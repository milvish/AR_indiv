package com.raywenderlich.android.targetpractice.fragment

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.raywenderlich.android.targetpractice.common.helpers.*
import com.raywenderlich.android.targetpractice.common.rendering.*
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.Matrix
import android.widget.Button
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.raywenderlich.android.targetpractice.R
import com.raywenderlich.android.targetpractice.common.helpers.DisplayRotationHelper
import com.raywenderlich.android.targetpractice.common.helpers.SnackbarHelper
import com.raywenderlich.android.targetpractice.common.helpers.TrackingStateHelper
import com.raywenderlich.android.targetpractice.common.rendering.BackgroundRenderer
import com.raywenderlich.android.targetpractice.common.rendering.Mode
import com.raywenderlich.android.targetpractice.common.rendering.ObjectRenderer
import com.raywenderlich.android.targetpractice.common.rendering.PlaneAttachment
import com.raywenderlich.android.targetpractice.common.rendering.PlaneRenderer
import com.raywenderlich.android.targetpractice.common.rendering.PointCloudRenderer
import kotlin.math.abs


class LessonFragment : Fragment(), GLSurfaceView.Renderer {

    private val TAG: String = LessonFragment::class.java.simpleName

    private var installRequested = false

    private var mode: Mode = Mode.VIKING

    private var session: Session? = null

    private var selectedButton: Int = 0

    private var scaleButton: Button? = null
    private var rotateButton: Button? = null
    private var endEditButton: Button? = null
    private var textInstruction: TextView? = null

    private var playVideoButton: Button? = null
    private var fileName: String = ""

    // Tap handling and UI.
    private lateinit var gestureDetector: GestureDetector
    private lateinit var trackingStateHelper: TrackingStateHelper
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val planeRenderer: PlaneRenderer = PlaneRenderer()
    private val pointCloudRenderer: PointCloudRenderer = PointCloudRenderer()

    // TODO: Declare ObjectRenderers and PlaneAttachments here

    private val vikingObject = ObjectRenderer()
    private val cannonObject = ObjectRenderer()
    private val targetObject = ObjectRenderer()

    private var vikingAttachment: PlaneAttachment? = null
    private var cannonAttachment: PlaneAttachment? = null
    private var targetAttachment: PlaneAttachment? = null


    private var vikingRotationXAngle = 0.0f
    private var cannonRotationXAngle = 0.0f
    private var targetRotationXAngle = 0.0f

    private var vikingRotationYAngle = 0.0f
    private var cannonRotationYAngle = 0.0f
    private var targetRotationYAngle = 0.0f

    // Temporary matrix allocated here to reduce number of allocations and taps for each frame.
    private val maxAllocationSize = 16
    private val anchorMatrix = FloatArray(maxAllocationSize)
    private val queuedSingleTaps = ArrayBlockingQueue<MotionEvent>(maxAllocationSize)



    private var selectedResourceName: String? = null

    private lateinit var surfaceView: GLSurfaceView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lesson, container, false)

        setupViews(view)
        setupTapDetector()
        setupSurfaceView(view)

        return view

    }

    fun radioButtonGroup() {
        val radioCannon = view?.findViewById<RadioButton>(R.id.radioCannon)
        val radioTarget = view?.findViewById<RadioButton>(R.id.radioTarget)
        val radioViking = view?.findViewById<RadioButton>(R.id.radioViking)

        // Set the click listener for the radio buttons
        radioCannon?.setOnClickListener {
            mode = Mode.CANNON
            selectedResourceName = "cannon"
        }
        radioTarget?.setOnClickListener {
            mode = Mode.TARGET
            selectedResourceName = "target"
        }
        radioViking?.setOnClickListener {
            mode = Mode.VIKING
            selectedResourceName = "viking"
        }

    }

    private fun setupViews(view: View) {
        scaleButton = view.findViewById(R.id.buttonScale)
        rotateButton = view.findViewById(R.id.buttonRotate)
        endEditButton = view.findViewById(R.id.buttonEndEdit)
        textInstruction = view.findViewById(R.id.textInstruction)

        playVideoButton = view.findViewById(R.id.buttonStartPlay)

        scaleButton?.setOnClickListener {
            selectedButton = 1
            textInstruction?.text = "Scroll up to increase scale. Or scroll down to reduce scale"
        }

        rotateButton?.setOnClickListener {
            selectedButton = 2
            textInstruction?.text =
                "Scroll right/left for horizontal rotation. Scroll up/down for vertical rotation"
        }

        endEditButton?.setOnClickListener {
            selectedButton = 0
            textInstruction?.text = ""
        }

        playVideoButton?.setOnClickListener{
            selectedResourceName?.let { it1 -> VideoPlayFragment.newInstance(it1).show(requireActivity().supportFragmentManager, VideoPlayFragment.TAG) }
        }


    }

    private fun setupSurfaceView(view: View) {
        surfaceView = view.findViewById(R.id.surfaceView)
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, maxAllocationSize, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setWillNotDraw(false)
        surfaceView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        trackingStateHelper = TrackingStateHelper(requireActivity())
        displayRotationHelper = DisplayRotationHelper(requireContext())

        installRequested = false

        textInstruction = view?.findViewById(R.id.textInstruction)
        textInstruction?.text = ""


        scaleButton = view?.findViewById(R.id.buttonScale)
        scaleButton?.setOnClickListener {
            selectedButton = 1
            textInstruction?.text = "Scroll up to increase scale. Or scroll down to reduce scale"
        }

        rotateButton = view?.findViewById(R.id.buttonRotate)
        rotateButton?.setOnClickListener {
            selectedButton = 2
            textInstruction?.text = "Scroll right/left for horizontal rotation. Scroll up/down for vertical rotation"
        }

        endEditButton = view?.findViewById(R.id.buttonEndEdit)
        endEditButton?.setOnClickListener {
            selectedButton = 0
            textInstruction?.text = ""
        }


        setupTapDetector()
        radioButtonGroup()


        //view?.let { setupSurfaceView(it) }
    }


    private fun setupTapDetector() {
        gestureDetector = GestureDetector(requireActivity(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onSingleTap(e)
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (selectedButton == 1) {
                    // Обработка для кнопки "Scale"
                    // Изменяйте масштаб объекта на основе значения distanceY

                    if (distanceY > 0) {
                        // Прокрутка вниз
                        changeScale(0.003f)
                    } else {
                        // Прокрутка вверх
                        changeScale(-0.003f)
                    }
                } else if (selectedButton == 2) {
                    // Обработка для кнопки "Rotate"
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        // Горизонтальная прокрутка
                        if (distanceX > 0) {
                            // Прокрутка вправо
                            changeRotationYAngle(4.0f)
                        } else {
                            // Прокрутка влево
                            changeRotationYAngle(-4.0f)
                        }
                    } else {
                        // Вертикальная прокрутка
                        if (distanceY > 0) {
                            // Прокрутка вверх
                            changeRotationXAngle(-4.0f)
                        } else {
                            // Прокрутка вниз
                            changeRotationXAngle(4.0f)
                        }
                    }
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {

                if (selectedResourceName == "viking") {
                    Toast.makeText(requireContext(), "Viking model clicked!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    private fun changeRotationXAngle(angle: Float) {
        when (mode) {
            Mode.VIKING -> {
                vikingRotationXAngle += angle
                if (vikingRotationXAngle >= 360.0f) vikingRotationXAngle = 0.0f
                if (vikingRotationXAngle <= -360.0f) vikingRotationXAngle = 0.0f
            }
            Mode.CANNON -> {
                cannonRotationXAngle += angle
                if (cannonRotationXAngle >= 360.0f) cannonRotationXAngle = 0.0f
                if (cannonRotationXAngle <= -360.0f) cannonRotationXAngle = 0.0f
            }
            Mode.TARGET -> {
                targetRotationXAngle += angle
                if (targetRotationXAngle >= 360.0f) targetRotationXAngle = 0.0f
                if (targetRotationXAngle <= -360.0f) targetRotationXAngle = 0.0f
            }
        }
    }

    private fun changeRotationYAngle(angle: Float) {
        when (mode) {
            Mode.VIKING -> {
                vikingRotationYAngle += angle
                if (vikingRotationYAngle >= 360.0f) vikingRotationYAngle = 0.0f
                if (vikingRotationYAngle <= -360.0f) vikingRotationYAngle = 0.0f
            }
            Mode.CANNON -> {
                cannonRotationYAngle += angle
                if (cannonRotationYAngle >= 360.0f) cannonRotationYAngle = 0.0f
                if (cannonRotationYAngle <= -360.0f) cannonRotationYAngle = 0.0f
            }
            Mode.TARGET -> {
                targetRotationYAngle += angle
                if (targetRotationYAngle >= 360.0f) targetRotationYAngle = 0.0f
                if (targetRotationYAngle <= -360.0f) targetRotationYAngle = 0.0f
            }
        }
    }

    private fun changeScale(scale: Float) {
        when (mode) {
            Mode.VIKING -> {
                Mode.VIKING.scaleFactor += scale
                if (Mode.VIKING.scaleFactor < 0.1f) Mode.VIKING.scaleFactor = 0.1f
            }
            Mode.CANNON -> {
                Mode.CANNON.scaleFactor += scale
                if (Mode.CANNON.scaleFactor < 0.05f) Mode.CANNON.scaleFactor = 0.1f
            }
            Mode.TARGET -> {
                Mode.TARGET.scaleFactor += scale
                if (Mode.TARGET.scaleFactor < 0.05f) Mode.TARGET.scaleFactor = 0.1f
            }
        }
    }

    private fun onSingleTap(e: MotionEvent) {
        // Queue tap if there is space. Tap is lost if queue is full.
        queuedSingleTaps.offer(e)

    }



    override fun onResume() {
        super.onResume()

        if (session == null) {
            if (!setupSession()) {
                return
            }
        }

        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(requireActivity(), getString(R.string.camera_not_available))
            session = null
            return
        }

        surfaceView.onResume()
        displayRotationHelper.onResume()
    }

    private fun setupSession(): Boolean {
        var exception: Exception? = null
        var message: String? = null

        try {
            when (ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested)) {
                InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return false
                }
                InstallStatus.INSTALLED -> {
                }
                else -> {
                    message = getString(R.string.arcore_install_failed)
                }
            }

            // Requesting Camera Permission
            if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
                CameraPermissionHelper.requestCameraPermission(requireActivity())
                return false
            }

            // Create the session.
            session = Session(requireActivity())

        } catch (e: UnavailableArcoreNotInstalledException) {
            message = getString(R.string.please_install_arcore)
            exception = e
        } catch (e: UnavailableUserDeclinedInstallationException) {
            message = getString(R.string.please_install_arcore)
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            message = getString(R.string.please_update_arcore)
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = getString(R.string.please_update_app)
            exception = e
        } catch (e: UnavailableDeviceNotCompatibleException) {
            message = getString(R.string.arcore_not_supported)
            exception = e
        } catch (e: Exception) {
            message = getString(R.string.failed_to_create_session)
            exception = e
        }

        if (message != null) {
            messageSnackbarHelper.showError(requireActivity(), message)
            Log.e(TAG, getString(R.string.failed_to_create_session), exception)
            return false
        }

        return true
    }

    override fun onPause() {
        super.onPause()

        if (session != null) {
            displayRotationHelper.onPause()
            surfaceView.onPause()
            session!!.pause()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            Toast.makeText(
                requireActivity(),
                getString(R.string.camera_permission_needed),
                Toast.LENGTH_LONG
            ).show()

            // Permission denied with checking "Do not ask again".
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(requireActivity())) {
                CameraPermissionHelper.launchPermissionSettings(requireActivity())
            }
            //finish()
        }
    }



/*
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        FullScreenHelper.setFullScreenOnWindowFocusChanged(this@MainActivity, hasFocus)
    }


 */




    private fun initializeARSession() {
        session = Session(requireContext())

        val config = Config(session)
        if (!session!!.isSupported(config)) {
            messageSnackbarHelper.showMessage(
                requireActivity(),
                "ARCore is not supported on this device"
            )
            requireActivity().finish()
            return
        }

        session!!.configure(config)

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayRotationHelper.onSurfaceChanged(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels
        )

        messageSnackbarHelper.showMessage(requireActivity(), "Searching for surfaces...")

        vikingObject.createOnGlThread(requireContext(), "Viking.obj", "Viking.png")
        cannonObject.createOnGlThread(requireContext(), "Cannon.obj", "Cannon.png")
        targetObject.createOnGlThread(requireContext(), "Target.obj", "Target.png")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(requireActivity())
            planeRenderer.createOnGlThread(requireActivity(), getString(R.string.model_grid_png))
            pointCloudRenderer.createOnGlThread(requireActivity())

            // TODO - set up the objects
            // 1
            vikingObject.createOnGlThread(requireActivity(), getString(R.string.model_viking_obj), getString(R.string.model_viking_png))
            cannonObject.createOnGlThread(requireActivity(), getString(R.string.model_cannon_obj), getString(R.string.model_cannon_png))
            targetObject.createOnGlThread(requireActivity(), getString(R.string.model_target_obj), getString(R.string.model_target_png))

            // 2
            targetObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
            vikingObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
            cannonObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)

        } catch (e: IOException) {
            Log.e(TAG, getString(R.string.failed_to_read_asset), e)
        }
    }


    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        session?.let {
            // Notify ARCore session that the view size changed
            displayRotationHelper.updateSessionIfNeeded(it)

            try {
                it.setCameraTextureName(backgroundRenderer.textureId)

                val frame = it.update()
                val camera = frame.camera

                // Handle one tap per frame.
                handleTap(frame, camera)
                drawBackground(frame)

                // Keeps the screen unlocked while tracking, but allow it to lock when tracking stops.
                trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

                // If not tracking, don't draw 3D objects, show tracking failure reason instead.
                if (!isInTrackingState(camera)) return

                val projectionMatrix = computeProjectionMatrix(camera)
                val viewMatrix = computeViewMatrix(camera)
                val lightIntensity = computeLightIntensity(frame)

                visualizeTrackedPoints(frame, projectionMatrix, viewMatrix)
                checkPlaneDetected()
                visualizePlanes(camera, projectionMatrix)

                // TODO: Call drawObject() for Viking, Cannon and Target here
                drawObject(
                    vikingObject,
                    vikingAttachment,
                    Mode.VIKING.scaleFactor,
                    projectionMatrix,
                    viewMatrix,
                    lightIntensity,
                    vikingRotationXAngle,
                    vikingRotationYAngle
                )

                drawObject(
                    cannonObject,
                    cannonAttachment,
                    Mode.CANNON.scaleFactor,
                    projectionMatrix,
                    viewMatrix,
                    lightIntensity,
                    cannonRotationXAngle,
                    cannonRotationYAngle
                )

                drawObject(
                    targetObject,
                    targetAttachment,
                    Mode.TARGET.scaleFactor,
                    projectionMatrix,
                    viewMatrix,
                    lightIntensity,
                    targetRotationXAngle,
                    targetRotationYAngle
                )

            } catch (t: Throwable) {
                Log.e(TAG, getString(R.string.exception_on_opengl), t)
            }
        }
    }

    private fun isInTrackingState(camera: Camera): Boolean {
        if (camera.trackingState == TrackingState.PAUSED) {
            messageSnackbarHelper.showMessage(
                requireActivity(), TrackingStateHelper.getTrackingFailureReasonString(camera)
            )
            return false
        }

        return true
    }

    private fun drawObject(
        objectRenderer: ObjectRenderer,
        planeAttachment: PlaneAttachment?,
        scaleFactor: Float,
        projectionMatrix: FloatArray,
        viewMatrix: FloatArray,
        lightIntensity: FloatArray,
        rotationXAngle: Float,
        rotationYAngle: Float
    ) {
        if (planeAttachment?.isTracking == true) {
            planeAttachment.pose.toMatrix(anchorMatrix, 0)
            Matrix.rotateM(anchorMatrix, 0, rotationXAngle, 1f, 0f, 0f)
            Matrix.rotateM(anchorMatrix, 0, rotationYAngle, 0f, 1f, 0f)

            // Update and draw the model
            objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor)
            objectRenderer.draw(viewMatrix, projectionMatrix, lightIntensity)
        }
    }

    private fun drawBackground(frame: Frame) {
        backgroundRenderer.draw(frame)
    }

    private fun computeProjectionMatrix(camera: Camera): FloatArray {
        val projectionMatrix = FloatArray(maxAllocationSize)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

        return projectionMatrix
    }

    private fun computeViewMatrix(camera: Camera): FloatArray {
        val viewMatrix = FloatArray(maxAllocationSize)
        camera.getViewMatrix(viewMatrix, 0)

        return viewMatrix
    }

    /**
     * Compute lighting from average intensity of the image.
     */
    private fun computeLightIntensity(frame: Frame): FloatArray {
        val lightIntensity = FloatArray(4)
        frame.lightEstimate.getColorCorrection(lightIntensity, 0)

        return lightIntensity
    }

    /**
     * Visualizes tracked points.
     */
    private fun visualizeTrackedPoints(
        frame: Frame,
        projectionMatrix: FloatArray,
        viewMatrix: FloatArray
    ) {
        // Use try-with-resources to automatically release the point cloud.
        frame.acquirePointCloud().use { pointCloud ->
            pointCloudRenderer.update(pointCloud)
            pointCloudRenderer.draw(viewMatrix, projectionMatrix)
        }
    }

    /**
     *  Visualizes planes.
     */
    private fun visualizePlanes(camera: Camera, projectionMatrix: FloatArray) {
        planeRenderer.drawPlanes(
            session!!.getAllTrackables(Plane::class.java),
            camera.displayOrientedPose,
            projectionMatrix
        )
    }

    /**
     * Checks if any tracking plane exists then, hide the message UI, otherwise show searchingPlane message.
     */
    private fun checkPlaneDetected() {
        if (hasTrackingPlane()) {
            messageSnackbarHelper.hide(requireActivity())
        } else {
            messageSnackbarHelper.showMessage(
                requireActivity(),
                getString(R.string.searching_for_surfaces)
            )
        }
    }

    /**
     * Checks if we detected at least one plane.
     */
    private fun hasTrackingPlane(): Boolean {
        val allPlanes = session!!.getAllTrackables(Plane::class.java)

        for (plane in allPlanes) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }

        return false
    }

    /**
     * Handle a single tap per frame
     */
    private fun handleTap(frame: Frame, camera: Camera) {
        val tap = queuedSingleTaps.poll()

        if (tap != null && camera.trackingState == TrackingState.TRACKING) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable

                if ((trackable is Plane
                            && trackable.isPoseInPolygon(hit.hitPose)
                            && PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0)
                    || (trackable is Point
                            && trackable.orientationMode
                            == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                ) {
                    // TODO: Create an anchor if a plane or an oriented point was hit
                    when (mode) {
                        Mode.VIKING -> vikingAttachment = addSessionAnchorFromAttachment(vikingAttachment, hit)
                        Mode.CANNON -> cannonAttachment = addSessionAnchorFromAttachment(cannonAttachment, hit)
                        Mode.TARGET -> targetAttachment = addSessionAnchorFromAttachment(targetAttachment, hit)
                    }


                    break
                }
            }
        }
    }

    // TODO: Add addSessionAnchorFromAttachment() function here

    private fun addSessionAnchorFromAttachment(
        previousAttachment: PlaneAttachment?,
        hit: HitResult
    ): PlaneAttachment? {
        // 1
        previousAttachment?.anchor?.detach()

        // 2
        val plane = hit.trackable as Plane
        val anchor = session!!.createAnchor(hit.hitPose)

        // 3
        return PlaneAttachment(plane, anchor)
    }




}

