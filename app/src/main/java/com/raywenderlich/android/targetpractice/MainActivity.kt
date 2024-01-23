/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modifications Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.targetpractice

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.raywenderlich.android.targetpractice.common.helpers.*
import com.raywenderlich.android.targetpractice.common.rendering.*
//import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.Matrix
import android.widget.Button
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
//import kotlinx.android.synthetic.main.fragment_lesson.surfaceView
import kotlin.math.abs
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

  private lateinit var navController: NavController

  override fun onCreate(savedInstanceState: Bundle?){
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

    val navHostFragment = supportFragmentManager
      .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    navController = navHostFragment.navController
    setupActionBarWithNavController(navController)
  }

  override fun onSupportNavigateUp(): Boolean {
    return navController.navigateUp() || super.onSupportNavigateUp()
  }
/*
  private val TAG: String = MainActivity::class.java.simpleName

  private var installRequested = false

  private var mode: Mode = Mode.VIKING

  private var session: Session? = null

  private var selectedButton: Int = 0

  private var scaleButton: Button? = null
  private var rotateButton: Button? = null
  private var endEditButton: Button? = null
  private var textInstruction: TextView? = null



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

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.AppTheme)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    trackingStateHelper = TrackingStateHelper(this@MainActivity)
    displayRotationHelper = DisplayRotationHelper(this@MainActivity)

    installRequested = false

    textInstruction = findViewById(R.id.textInstruction)
    textInstruction?.text = ""


    scaleButton = findViewById(R.id.buttonScale)
    scaleButton?.setOnClickListener {
      selectedButton = 1
      textInstruction?.text = "Scroll up to increase scale. Or scroll down to reduce scale"
    }

    rotateButton = findViewById(R.id.buttonRotate)
    rotateButton?.setOnClickListener {
      selectedButton = 2
      textInstruction?.text = "Scroll right/left for horizontal rotation. Scroll up/down for vertical rotation"
    }

    endEditButton = findViewById(R.id.buttonEndEdit)
    endEditButton?.setOnClickListener {
      selectedButton = 0
      textInstruction?.text = ""
    }


    setupTapDetector()
    setupSurfaceView()
  }

  fun onRadioButtonClicked(view: View) {
    when (view.id) {
      R.id.radioCannon -> mode = Mode.CANNON
      R.id.radioTarget -> mode = Mode.TARGET
      else -> mode = Mode.VIKING
    }
  }

  private fun setupSurfaceView() {
    // Set up renderer.
    surfaceView.preserveEGLContextOnPause = true
    surfaceView.setEGLContextClientVersion(2)
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, maxAllocationSize, 0)
    surfaceView.setRenderer(this)
    surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    surfaceView.setWillNotDraw(false)
    surfaceView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
  }

  private fun setupTapDetector() {
    gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
      override fun onSingleTapUp(e: MotionEvent): Boolean {
        onSingleTap(e)
        return true
      }

      override fun onDown(e: MotionEvent): Boolean {
        return true
      }

      override fun onScroll(
        e1: MotionEvent,
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
      messageSnackbarHelper.showError(this@MainActivity, getString(R.string.camera_not_available))
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
      when (ArCoreApk.getInstance().requestInstall(this@MainActivity, !installRequested)) {
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
      if (!CameraPermissionHelper.hasCameraPermission(this@MainActivity)) {
        CameraPermissionHelper.requestCameraPermission(this@MainActivity)
        return false
      }

      // Create the session.
      session = Session(this@MainActivity)

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
      messageSnackbarHelper.showError(this@MainActivity, message)
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
    if (!CameraPermissionHelper.hasCameraPermission(this@MainActivity)) {
      Toast.makeText(
          this@MainActivity,
          getString(R.string.camera_permission_needed),
          Toast.LENGTH_LONG
      ).show()

      // Permission denied with checking "Do not ask again".
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this@MainActivity)) {
        CameraPermissionHelper.launchPermissionSettings(this@MainActivity)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)

    FullScreenHelper.setFullScreenOnWindowFocusChanged(this@MainActivity, hasFocus)
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(this@MainActivity)
      planeRenderer.createOnGlThread(this@MainActivity, getString(R.string.model_grid_png))
      pointCloudRenderer.createOnGlThread(this@MainActivity)

      // TODO - set up the objects
      // 1
      vikingObject.createOnGlThread(this@MainActivity, getString(R.string.model_viking_obj), getString(R.string.model_viking_png))
      cannonObject.createOnGlThread(this@MainActivity, getString(R.string.model_cannon_obj), getString(R.string.model_cannon_png))
      targetObject.createOnGlThread(this@MainActivity, getString(R.string.model_target_obj), getString(R.string.model_target_png))

      // 2
      targetObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
      vikingObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
      cannonObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)

    } catch (e: IOException) {
      Log.e(TAG, getString(R.string.failed_to_read_asset), e)
    }
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
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
          this@MainActivity, TrackingStateHelper.getTrackingFailureReasonString(camera)
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
      messageSnackbarHelper.hide(this@MainActivity)
    } else {
      messageSnackbarHelper.showMessage(
          this@MainActivity,
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


 */
}
