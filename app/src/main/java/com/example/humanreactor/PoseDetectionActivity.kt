package com.example.humanreactor
import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isInvisible
import com.example.humanreactor.AI_Model.LightweightTreeClassifier

import com.example.humanreactor.customizedMove.Move
import com.example.humanreactor.customizedMove.MoveDialogManager
import com.example.humanreactor.customizedMove.NormalizedSample
import com.example.humanreactor.databases.ActionDatabaseHelper
import com.example.humanreactor.databases.Performance
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

@androidx.camera.core.ExperimentalGetImage
class PoseDetectionActivity : AppCompatActivity(), SurfaceHolder.Callback {

    //dp
    private lateinit var dbHelper: ActionDatabaseHelper
    // Configuration options - display settings can be modified in onCreate
    private var showNose = true
    private var showLeftShoulder = true
    private var showRightShoulder = true
    private var showLeftElbow = true
    private var showRightElbow = true
    private var showLeftWrist = true
    private var showRightWrist = true
    private var showLeftHip = true
    private var showRightHip = true

    // Added: Master switches for displaying red dots and coordinate text
    private var showLandmarkDots = false
    private var showCoordinatesText = false

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseDetector: PoseDetector
    private lateinit var surfaceView: SurfaceView
    private lateinit var previewView: androidx.camera.view.PreviewView
//    private lateinit var textViewStatus: TextView

    //For Terminal
    private var strStart = "sys C/User/>"
    private var terminalStr  = "Human Reactor PowerShell\n" +
            "Copyright (C) Porsugu & Lemmonie. All rights reserved.\n\n" +
            "The accuracy of the test might be vary due to your environment.\n\n" +strStart
    private var terminalSpannable: SpannableStringBuilder = SpannableStringBuilder("")
    private lateinit var InfoCover:ConstraintLayout
    private lateinit var terminalScroll:ScrollView
    private lateinit var terminalTxt: TextView
    private var isAdded = false
    private var isSampled = false
    private var isTrained = false
    private var isTested = false
    private var isSaved = false



    //For unite btn
    private lateinit var leftBTN: RelativeLayout
    private lateinit var rightBTN: RelativeLayout
    private lateinit var allBTN: ConstraintLayout
    private lateinit var allBTN_Icon: ImageView
    private lateinit var allBTN_Text: TextView
    private var START_PAGE: Int = 0
    private var current_page:Int = 0
    private var END_PAGE: Int = 6

    //For add user move
    private val usedColors = mutableSetOf<Int>()
    private val moves = mutableListOf<Move>()

    private lateinit var moveDialogManager: MoveDialogManager

    //For Collecting
    private lateinit var colorBar: View
    private lateinit var tips: TextView
    private var ref:Int = 0
    private var fac:Float = 0f
    private lateinit var progressBar: TextView
    private lateinit var msgWindow:ConstraintLayout

    //For training
    private lateinit var classifier: LightweightTreeClassifier
//    private lateinit var classifier: EnhancedTreeClassifier
    private var trainPause = 50L
    private var trainMsg = ""
    private var acc:Double = 0.0
    private var avg_Time = 0.0
    private var min_Time = 3000L
    private var max_Time = 0L

    //For reaction
    private lateinit var correctIcon: TextView
    private var testMsg = ""

    //For save
    private var categoryId = mutableSetOf<Int>()



    // Stores the latest detected pose data
    private var currentPose: Pose? = null

    //Switch tips display for user and developer
    private var isDevelop = false


    // Drawing tools
    private val paint = Paint().apply {
        color = Color.RED
        textSize = 40f
        strokeWidth = 4f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pose_detection)
//        setContentView(R.layout.activity_pose_detection2)

        //db
        dbHelper = ActionDatabaseHelper(this)
        // Initialize views
        surfaceView = findViewById(R.id.surface_view)
        previewView = findViewById(R.id.preview_view)
//        textViewStatus = findViewById(R.id.text_status)

        // Set up SurfaceView
        surfaceView.holder.addCallback(this)
        surfaceView.setZOrderOnTop(true)  // Ensure SurfaceView is above the preview
        surfaceView.holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)  // Set transparent background

        // Request camera permissions
        if (allPermissionsGranted()) {
//            startCamera()
//            stopCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

//        val options = AccuratePoseDetectorOptions.Builder()
//            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
//            .build()
        val options = AccuratePoseDetectorOptions.Builder()
//            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
//            .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU)
            .build()


        poseDetector = PoseDetection.getClient(options)


        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        //For Terminal
        InfoCover = findViewById(R.id.InfoCover)
        terminalScroll = findViewById(R.id.termimnalScroll)
        terminalTxt = findViewById(R.id.terMsgText)
//        terminalTxt.typeface = Typeface.MONOSPACE
        terminalSpannable.append(terminalStr)
        terminalTxt.text = terminalStr

        //for add moves
        moveDialogManager = MoveDialogManager(this, dbHelper, moves, usedColors,categoryId)

        colorBar = findViewById(R.id.color_bar)

        //for sampling and training
        progressBar = findViewById(R.id.progressBar2)
        msgWindow = findViewById(R.id.msgWindow)
        msgWindow.visibility = View.INVISIBLE
        tips = findViewById(R.id.tips)


        //for reaction
        correctIcon = findViewById(R.id.correctSignText)
        correctIcon.visibility = View.GONE

        //For unite btn
        allBTN_Icon = findViewById(R.id.allBTN_icon)
        allBTN_Text = findViewById(R.id.allBTN_text)
        leftBTN = findViewById(R.id.leftBTN)
        leftBTN.isInvisible = true
        leftBTN.background = ContextCompat.getDrawable(this, R.drawable.btn_background)
        leftBTN.setOnClickListener {
            current_page --
            if (current_page  == START_PAGE){
                disappearBTN(leftBTN)
            }
            if(current_page < END_PAGE - 1){
                showBTN(rightBTN)
            }
            setAllBTN(current_page)
        }

        rightBTN = findViewById(R.id.rightBTN)
        rightBTN.background = ContextCompat.getDrawable(this, R.drawable.btn_background)
        rightBTN.setOnClickListener {
            current_page ++
            if (current_page  == END_PAGE - 1){
                disappearBTN(rightBTN)
            }
            if(current_page > START_PAGE){
                showBTN(leftBTN)
            }
            setAllBTN(current_page)
        }

        moveDialogManager.setOnDialogDismissListener(object : MoveDialogManager.OnDialogDismissListener {
            override fun onDialogDismissed() {
                if(moves.size > 1 ){
                    isAdded = true
                }
                else{
                    isAdded = false
                }
                isSampled = false
                isTrained = false
                isTested = false
                isSaved = false
                terminalTxtUpdate("add", isAdded, "Please add at least 2 moves!")

            }
        })

        allBTN = findViewById(R.id.allBTN)
        allBTN.background = ContextCompat.getDrawable(this, R.drawable.btn_background)
        allBTN.setOnClickListener {
            if(current_page == 0){
                terminalTxtUpdateStart("Add...")
                moveDialogManager.showMoveManagementDialog()
            }
            else if(current_page == 1){
                terminalTxtUpdateStart("Sampling...")
                collectAllMoveSample(5, 5000)
            }
            else if(current_page == 2){
                terminalTxtUpdateStart("Train AI Model...")
                processAndTrainModel()

            }
            else if(current_page == 3){
                terminalTxtUpdateStart("Reaction Test...")
                testReaction(5)
            }
            else if(current_page == 4){
                terminalTxtUpdateStart("Save Performance...")
                confirmSave()
            }
            else if(current_page == 5){
                finish()
            }
        }


    }

    //for unite btn
    fun disappearBTN(btn:RelativeLayout){
        btn.isInvisible = true
        btn.isEnabled = false
    }

    fun showBTN(btn:RelativeLayout){
        btn.isInvisible = false
        btn.isEnabled = true
    }

    fun setAllBTN(current_page:Int){
        val icons = listOf(
            android.R.drawable.ic_menu_add,
            android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_edit,
            android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_save,
            android.R.drawable.ic_menu_revert
        )

        val texts = listOf(
            "Add Moves",
            "Sampling",
            "Train AI",
            "Reaction!!",
            "Save",
            "Back"
        )
        allBTN_Icon.setImageResource(icons[current_page])
        allBTN_Text.text = texts[current_page]

    }


    //For collecting move samples
    private fun collectAllMoveSample(secondsForEachMove: Int, samplesForEachMove: Int) {
        isSampled = false
        if(moves.size < 2){
            CoroutineScope(Dispatchers.Main).launch {
//                tips.isInvisible = false
                msgWindow.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                tips.text = "Please add at least 2 moves first."
                delay(2000)
//                tips.isInvisible = true
                msgWindow.visibility = View.INVISIBLE
                terminalTxtUpdate("sample", isSampled,"Please add at least 2 moves first.")
            }

            return
        }

        resetAllMoveSamples()

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            terminalTxtUpdateStart("Starting Camera...")
            delay(500)
            startCamera()
            InfoCover.visibility = View.INVISIBLE
            msgWindow.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            isTrained = false
            disableAllBtn()

            // Setup progress bar
//            progressBar.isInvisible = false
//            progressBar.progress = 0
            msgWindow.visibility = View.VISIBLE
//            progressBar.visibility = View.VISIBLE
            progressBar.text = progressBarStr(0)

            tips.isInvisible = false  // Make sure tips are visible
            tips.text = "Start Sampling"
            delay(2000)
            tips.text = "Old move data are cleared."
            delay(2000)

            // Constants for countdown
            val preparationTime = 3 // seconds to prepare for each move

            // Calculate total progress steps
            val totalMoves = moves.size
            // Progress allocation: 10% for setup, 90% for sample collection
            val setupProgress = 10
            val progressPerMove = 90 / totalMoves

            // Set initial progress after setup
//            progressBar.progress = setupProgress
            progressBar.text = progressBarStr(setupProgress)
            // Iterate through each move in the list
            for ((moveIndex, move) in moves.withIndex()) {
                // Preparation countdown
                tips.text = "Get ready for: ${move.name}"
                colorBar.setBackgroundColor(move.color)  // Set the color bar to match this move's color

                // Countdown for preparation
                for (i in preparationTime downTo 1) {
                    correctIcon.isInvisible = false
                    correctIcon.text = "${i}"
                    tips.text = "Ready for: ${move.name}..."
                    delay(1000)
                }
                correctIcon.text = ""
                correctIcon.isInvisible = true


                tips.text = "START ${move.name} NOW!"
                delay(500)

                // Calculate timing parameters
                val totalDurationMillis = secondsForEachMove * 1000
//                val sampleIntervalMillis = totalDurationMillis / samplesForEachMove
                val sampleIntervalMillis = 1

                // Start time for the entire collection period
                val startTime = System.currentTimeMillis()
                val endTime = startTime + totalDurationMillis

                // Collect samples
                var samplesCollected = 0

                // Calculate progress increment per sample for this move
                val progressPerSample = progressPerMove / samplesForEachMove.toFloat()

                while (samplesCollected < samplesForEachMove) {
                    // Calculate remaining time
                    val currentTime = System.currentTimeMillis()
                    val remainingMillis = Math.max(0, endTime - currentTime)
                    val remainingSeconds = (remainingMillis / 1000).toInt()

                    // Try to get the current pose
                    getCurrentPoseData()?.let { pose ->
                        move.samples.add(pose)
                        samplesCollected++

                        // Update progress in UI
//                        tips.text = "Hold your move!\nCollecting: $samplesCollected/$samplesForEachMove"
                        tips.text = "Hold your move!\n$samplesCollected/$samplesForEachMove"
                        // Update progress bar
                        val currentMoveBaseProgress = setupProgress + (moveIndex * progressPerMove)
                        val currentProgress = currentMoveBaseProgress + (samplesCollected * progressPerSample).toInt()
//                        progressBar.progress = currentProgress
                        progressBar.text = progressBarStr(currentProgress)


                        // Wait for the next sample interval
                        if (samplesCollected < samplesForEachMove) {
                            delay(sampleIntervalMillis.toLong())
                        }
                    } ?: run {
                        // No pose data available, wait a bit and try again
                        tips.text = "Hold ${move.name} for ${remainingSeconds}s more\n" +
                                "Waiting for pose data..."
                        delay(100)

                        // Check if we've run out of time
                        if (System.currentTimeMillis() > endTime) {
                            tips.text = "Time's up! Continuing to collect remaining samples..."
                        }
                    }
                }

                // Mark this move as trained
                move.isCollected = true

                // Short delay before moving to next move
//                tips.text = "Great! ${move.name} has been recorded with $samplesForEachMove samples!"
                tips.text = "Move ${move.name}:\n $samplesForEachMove samples"
                delay(1500)
            }

            // Set progress to 100% when all done
//            progressBar.progress = 100
            progressBar.text = progressBarStr(100)

            // Training session completed - CLEAR the color bar
            colorBar.setBackgroundColor(Color.TRANSPARENT)  // Clear the color
            tips.text = "Sampling success!"

            delay(1500)
//            tips.isInvisible = true
//            progressBar.isInvisible = true

            msgWindow.visibility = View.INVISIBLE
            enableAllBtn()
            stopCamera()
            terminalTxtUpdateStart("Camera Stopped.")
            InfoCover.visibility = View.VISIBLE
            isSampled = true
            terminalTxtUpdate("sample", isSampled)
        }
    }

    private fun resetAllMoveSamples(): Int {
        var movesCleared = 0

        for (move in moves) {
            if (move.samples.isNotEmpty()) {
                move.samples.clear()
                move.isTrained = false  // Reset the trained status
                movesCleared++
            }
            if (move.normalizedSamples.isNotEmpty()) {
                move.normalizedSamples.clear()
                move.isTrained = false  // Reset the trained status
                movesCleared++
            }
            move.isCollected = false
        }

        return movesCleared
    }

    //For training
    private fun processAndTrainModel() {
        var result:String = ""
        CoroutineScope(Dispatchers.Main).launch {
            isTrained = false
            msgWindow.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            disableAllBtn()
            try {
                normalizeData()
                trainModel()
            } catch (e: Exception) {
                Log.e("ProcessAndTrain", "Error during processing or training", e)
//                result = "Error during processing or training"
//                terminalTxtUpdate("train",isTrained, trainMsg)
//                tips.text = "Error: ${e.message}"
//                delay(3000)
                tips.isInvisible = true
            }
            enableAllBtn()
            msgWindow.visibility = View.INVISIBLE
            terminalTxtUpdate("train",isTrained, trainMsg)
        }

    }


    private suspend fun normalizeData() {
//        progressBar.progress = 0
        progressBar.text = progressBarStr(0)
//        tips.isInvisible = false
        if(isDevelop){
            tips.text = "Preparing normalization factors..."
        }
        else{
            tips.text = "Preparing normalizaion..."
        }


        // Calculate total work to be done
        val totalSamples = moves.sumOf { it.samples.size }
        var processedSamples = 0

        // Find stable keypoint and normalization factor
        val bestRelativePoint = withContext(Dispatchers.Default) {
            findMostStableKeypoint()
        }
        ref = bestRelativePoint

        // Update progress (10%)
//        progressBar.progress = 10
        progressBar.text = progressBarStr(10)
        delay(500)
        val bestNormFactor = withContext(Dispatchers.Default) {
            findMostStableNormalizationFactor()
        }
        fac = bestNormFactor

        // Update progress (20%)
//        progressBar.progress = 20
        progressBar.text = progressBarStr(20)

        if(isDevelop){
            tips.text = "Normalization factors calculated."
            delay(500)
        }

        var successfulNormalizations = 0

        // Process each move - remaining 80% of progress will be distributed among samples
        val progressPerSample = if (totalSamples > 0) 80.0 / totalSamples else 0.0

        // Process each move
        for ((moveIndex, move) in moves.withIndex()) {
            if(isDevelop){
                tips.text = "Clear ${move.name} previous normalized samples..."
                delay(500)
            }
//            else{
//                tips.text = "Clear ${move.name} data..."
//            }
            move.normalizedSamples.clear()

            tips.text = "Normalizing ${move.name}..."

            val normalizedResults = withContext(Dispatchers.Default) {
                val results = mutableListOf<NormalizedSample>()

                for (pose in move.samples) {
                    val normalizedFeatures = normalizePose(pose, bestRelativePoint, bestNormFactor)

                    if (normalizedFeatures != null) {
                        results.add(NormalizedSample(normalizedFeatures, move.name))
                        successfulNormalizations++
                    }

                    // Update progress for each sample
                    processedSamples++
                    withContext(Dispatchers.Main) {
//                        progressBar.progress = 20 + (processedSamples * progressPerSample).toInt()
                        progressBar.text = progressBarStr(20 + (processedSamples * progressPerSample).toInt())
                    }
                }

                results
            }

            move.normalizedSamples.addAll(normalizedResults)

            // Show progress for current move
            if(isDevelop){
                tips.text = "Processed ${move.name}: ${move.normalizedSamples.size}/${move.samples.size} samples"
                delay(500)
            }
            else{
                tips.text = "Normalized ${move.name} samples"
                delay(500)
            }

        }

        // Show final result
//        progressBar.progress = 100
        progressBar.text = progressBarStr(100)
        val successRate = if (totalSamples > 0) (successfulNormalizations * 100 / totalSamples) else 0
        if(isDevelop){
            tips.text = "Normalization complete! $successfulNormalizations/$totalSamples samples ($successRate%)"
        }
        else{
            tips.text = "Normalization complete!"
        }
        delay(1500)

//        tips.text = "All moves normalized successfully!"
//        delay(1000)

        // Hide progress bar when done
//        msgWindow.visibility = View.INVISIBLE
    }

    /**
     * Normalize a pose using the given reference point and normalization factor
     * @param pose The pose to normalize
     * @param referencePointType The reference point type for normalization
     * @param normFactor The normalization factor to use
     * @return List of normalized features, or null if normalization failed
     */
    fun normalizePose(pose: Pose, referencePointType: Int, normFactor: Float): List<Float> {
        val requiredLandmarks = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP
        )

        for (landmarkId in requiredLandmarks) {
            if (pose.getPoseLandmark(landmarkId) == null) {
                return emptyList()
            }
        }

        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)!!
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)!!
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)!!
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)!!
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)!!
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)!!
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!

        val bestRef = pose.getPoseLandmark(referencePointType)!!
        val norm = normFactor

        val features = mutableListOf<Float>()

        // 1. Calculate key angles using 3D coordinates
        // Shoulder angle (formed by left shoulder, nose, and right shoulder)
        features.add(calculate3DAngle(leftShoulder, bestRef, rightShoulder))

        // Left elbow angle (left shoulder-left elbow-left wrist)
        features.add(calculate3DAngle(leftShoulder, leftElbow, leftWrist))

        // Right elbow angle (right shoulder-right elbow-right wrist)
        features.add(calculate3DAngle(rightShoulder, rightElbow, rightWrist))

        // Left shoulder angle (left elbow-left shoulder-left hip)
        features.add(calculate3DAngle(leftElbow, leftShoulder, leftHip))

        // Right shoulder angle (right elbow-right shoulder-right hip)
        features.add(calculate3DAngle(rightElbow, rightShoulder, rightHip))

        // Left waist angle (left shoulder-left hip-right hip)
        features.add(calculate3DAngle(leftShoulder, leftHip, rightHip))

        // Right waist angle (right shoulder-right hip-left hip)
        features.add(calculate3DAngle(rightShoulder, rightHip, leftHip))

        // 2. Relative position features (normalized relative to shoulder width)

        // Height difference between wrists and shoulders
        features.add(normalize3DDistance(leftShoulder.position3D.y - leftWrist.position3D.y, norm))
        features.add(normalize3DDistance(rightShoulder.position3D.y - rightWrist.position3D.y, norm))

        // Horizontal distance between wrists and shoulders
        features.add(normalize3DDistance(leftWrist.position3D.x - leftShoulder.position3D.x, norm))
        features.add(normalize3DDistance(rightWrist.position3D.x - rightShoulder.position3D.x, norm))

        // Relative distance between elbows and shoulders
        features.add(normalize3DDistance(distance3D(leftShoulder, leftElbow), norm))
        features.add(normalize3DDistance(distance3D(rightShoulder, rightElbow), norm))

        // Relative distance between wrists and elbows
        features.add(normalize3DDistance(distance3D(leftElbow, leftWrist), norm))
        features.add(normalize3DDistance(distance3D(rightElbow, rightWrist), norm))

        // Relative distance between shoulders and hips
        features.add(normalize3DDistance(distance3D(leftShoulder, leftHip), norm))
        features.add(normalize3DDistance(distance3D(rightShoulder, rightHip), norm))

        // 3. Z-axis features (depth information)
        // Depth difference between left and right wrists
        features.add(normalize3DDistance(leftWrist.position3D.z - rightWrist.position3D.z, norm))

        // Depth difference between shoulder center point and nose
        val shoulderCenterZ = (leftShoulder.position3D.z + rightShoulder.position3D.z) / 2
        features.add(normalize3DDistance(nose.position3D.z - shoulderCenterZ, norm))

        // Shoulder tilt angle (relative to horizontal line)
        features.add(calculate3DSlope(leftShoulder, rightShoulder))

        // Hip tilt angle (relative to horizontal line)
        features.add(calculate3DSlope(leftHip, rightHip))

        // 4. Position of nose relative to shoulder center point
        val shoulderCenterX = (leftShoulder.position3D.x + rightShoulder.position3D.x) / 2
        val shoulderCenterY = (leftShoulder.position3D.y + rightShoulder.position3D.y) / 2

        features.add(normalize3DDistance(nose.position3D.x - shoulderCenterX, norm))
        features.add(normalize3DDistance(nose.position3D.y - shoulderCenterY, norm))

        return features
    }

    private fun distance3D(point1: PoseLandmark, point2: PoseLandmark): Float {
        val dx = point1.position3D.x - point2.position3D.x
        val dy = point1.position3D.y - point2.position3D.y
        val dz = point1.position3D.z - point2.position3D.z
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun calculate3DAngle(point1: PoseLandmark, point2: PoseLandmark, point3: PoseLandmark): Float {
        // Calculate vector A (point1 to point2)
        val ax = point1.position3D.x - point2.position3D.x
        val ay = point1.position3D.y - point2.position3D.y
        val az = point1.position3D.z - point2.position3D.z

        // Calculate vector B (point3 to point2)
        val bx = point3.position3D.x - point2.position3D.x
        val by = point3.position3D.y - point2.position3D.y
        val bz = point3.position3D.z - point2.position3D.z

        // Calculate the dot product of the vectors
        val dotProduct = ax * bx + ay * by + az * bz

        // Calculate the magnitude of the vectors
        val magnitudeA = Math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        val magnitudeB = Math.sqrt((bx * bx + by * by + bz * bz).toDouble()).toFloat()

        // Calculate the cosine of the angle between the vectors
        val cosAngle = dotProduct / (magnitudeA * magnitudeB)

        // Calculate the angle in radians and convert to 0-1 range
        val angle = Math.acos(cosAngle.coerceIn(-1.0f, 1.0f).toDouble())
        return (angle / Math.PI).toFloat()
    }

    private fun calculate3DSlope(point1: PoseLandmark, point2: PoseLandmark): Float {
        // Calculate the slope angle in 3D space
        val dx = point2.position3D.x - point1.position3D.x
        val dy = point2.position3D.y - point1.position3D.y
        val dz = point2.position3D.z - point1.position3D.z

        // Calculate the horizontal angle in the xz plane
        val horizontalAngle = Math.atan2(dz.toDouble(), dx.toDouble())

        // Calculate the vertical angle with the horizontal plane
        val verticalAngle = Math.atan2(dy.toDouble(),
            Math.sqrt((dx * dx + dz * dz).toDouble()))

        // Return the normalized vertical angle as a measure of slope
        return (verticalAngle / Math.PI).toFloat()
    }

    private fun normalize3DDistance(distance: Float, referenceLength: Float): Float {
        return if (referenceLength > 0) distance / referenceLength else 0f
    }
    /**
     * Find the most stable keypoint across all moves
     * @param moves The list of moves to analyze
     * @return The most stable keypoint type (PoseLandmark.XXX)
     */
    fun findMostStableKeypoint(): Int {
        // Keypoint types to analyze
        val keypointTypes = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP
        )

        // Total CV for each keypoint across all moves
        val totalKeypointCVs = mutableMapOf<Int, Float>()
        keypointTypes.forEach { totalKeypointCVs[it] = 0f }

        // Calculate CV for each keypoint in each move
        for (move in moves) {

            // Calculate CVs for this move
            val moveKeypointCVs = calculateKeypointCVsForMove(move, keypointTypes)

            // Add to total CVs
            for ((keypointType, cv) in moveKeypointCVs) {
                totalKeypointCVs[keypointType] =
                    totalKeypointCVs[keypointType]!! + cv
            }
        }

        // Find the keypoint with minimum total CV
        val mostStable = totalKeypointCVs.minByOrNull { it.value }?.key

        // Default to nose if no keypoint is found
        return mostStable ?: PoseLandmark.NOSE
    }

    /**
     * Calculate the CV of each keypoint for a single move
     */
    private fun calculateKeypointCVsForMove(move: Move, keypointTypes: List<Int>): Map<Int, Float> {
        val keypointCVs = mutableMapOf<Int, Float>()

        for (keypointType in keypointTypes) {
            val xCoordinates = mutableListOf<Float>()
            val yCoordinates = mutableListOf<Float>()
            val zCoordinates = mutableListOf<Float>()

            for (pose in move.samples) {
                pose.getPoseLandmark(keypointType)?.let {
                    xCoordinates.add(it.position.x)
                    yCoordinates.add(it.position.y)

                    try {
                        zCoordinates.add(it.position3D.z)
                    } catch (e: Exception) {
                        zCoordinates.add(0f)
                        Log.d("PoseDetection", "3D z-coordinate not available for ${keypointType}, using default")
                    }
                }
            }

            if (xCoordinates.size < move.samples.size) {
                keypointCVs[keypointType] = Float.MAX_VALUE
                continue
            }

            val xCV = calculateCV(xCoordinates)
            val yCV = calculateCV(yCoordinates)
            val zCV = calculateCV(zCoordinates)

            keypointCVs[keypointType] = (xCV * 0.4f + yCV * 0.4f + zCV * 0.2f)
        }

        return keypointCVs
    }

    /**
     * Find the most stable normalization factor value across all pose samples
     * @param moves The list of moves to analyze
     * @return The most stable normalization factor value
     */
    fun findMostStableNormalizationFactor(): Float {
        // Collect measurement results from different normalization methods
        val shoulderWidths = mutableListOf<Float>()
        val hipWidths = mutableListOf<Float>()
        val shoulderDepths = mutableListOf<Float>() // Shoulder depth differences
        val spineHeights = mutableListOf<Float>()   // Spine height
        val bboxDiagonals = mutableListOf<Float>()

        // Analyze all samples
        for (move in moves) {
            for (pose in move.samples) {
                // Shoulder width
                val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

                if (leftShoulder != null && rightShoulder != null) {
                    // 3D distance (considering depth)
                    val shoulderDepth = Math.abs(leftShoulder.position3D.z - rightShoulder.position3D.z)
                    shoulderDepths.add(shoulderDepth)
                }

                // Hip width
                calculateDistance(pose, PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_HIP)?.let { hipWidths.add(it) }

                // Spine height (distance from nose to hip center)
                val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
                val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

                if (nose != null && leftHip != null && rightHip != null) {
                    val hipCenterX = (leftHip.position3D.x + rightHip.position3D.x) / 2
                    val hipCenterY = (leftHip.position3D.y + rightHip.position3D.y) / 2
                    val hipCenterZ = (leftHip.position3D.z + rightHip.position3D.z) / 2

                    val dx = nose.position3D.x - hipCenterX
                    val dy = nose.position3D.y - hipCenterY
                    val dz = nose.position3D.z - hipCenterZ

                    val spineHeight = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
                    spineHeights.add(spineHeight)
                }

                // Bounding box diagonal
                calculateBboxDiagonal(pose)?.let { bboxDiagonals.add(it) }
            }
        }

        // Calculate coefficient of variation (CV) for each method
        val shoulderWidthCV = calculateCV(shoulderWidths)
        val hipWidthCV = calculateCV(hipWidths)
        val shoulderDepthCV = calculateCV(shoulderDepths)
        val spineHeightCV = calculateCV(spineHeights)
        val bboxDiagonalCV = calculateCV(bboxDiagonals)

        // Log analysis results
        Log.d("Normalization", "Coefficient of Variation Analysis:")
        Log.d("Normalization", "- Shoulder Width: $shoulderWidthCV (${shoulderWidths.size} samples)")
        Log.d("Normalization", "- Hip Width: $hipWidthCV (${hipWidths.size} samples)")
        Log.d("Normalization", "- Shoulder Depth: $shoulderDepthCV (${shoulderDepths.size} samples)")
        Log.d("Normalization", "- Spine Height: $spineHeightCV (${spineHeights.size} samples)")
        Log.d("Normalization", "- Bounding Box Diagonal: $bboxDiagonalCV (${bboxDiagonals.size} samples)")

        // Only consider methods with sufficient sample coverage
        var bestCV = Float.MAX_VALUE
        var bestFactorAverage = 0f

        // Check and select the most stable normalization factor
        if (shoulderWidthCV < bestCV && shoulderWidths.isNotEmpty()) {
            bestCV = shoulderWidthCV
            bestFactorAverage = shoulderWidths.average().toFloat()
        }

        if (hipWidthCV < bestCV && hipWidths.isNotEmpty()) {
            bestCV = hipWidthCV
            bestFactorAverage = hipWidths.average().toFloat()
        }

        if (shoulderDepthCV < bestCV && shoulderDepths.isNotEmpty()) {
            bestCV = shoulderDepthCV
            bestFactorAverage = shoulderDepths.average().toFloat()
        }

        if (spineHeightCV < bestCV && spineHeights.isNotEmpty()) {
            bestCV = spineHeightCV
            bestFactorAverage = spineHeights.average().toFloat()
        }

        // Bounding box is always available as a fallback
        if (bboxDiagonalCV < bestCV || bestFactorAverage == 0f) {
            bestCV = bboxDiagonalCV
            bestFactorAverage = bboxDiagonals.average().toFloat()
        }

        Log.d("Normalization", "Selected best normalization factor: $bestFactorAverage (CV: $bestCV)")

        return bestFactorAverage
    }

    /**
     * Calculate distance between two landmarks in a pose
     * @param pose The pose to analyze
     * @param firstLandmarkType First landmark type
     * @param secondLandmarkType Second landmark type
     * @return Distance between the landmarks, or null if either landmark is missing
     */
    private fun calculateDistance(pose: Pose, firstLandmarkType: Int, secondLandmarkType: Int): Float? {
        val firstLandmark = pose.getPoseLandmark(firstLandmarkType)
        val secondLandmark = pose.getPoseLandmark(secondLandmarkType)

        if (firstLandmark != null && secondLandmark != null) {
            val dx = secondLandmark.position.x - firstLandmark.position.x
            val dy = secondLandmark.position.y - firstLandmark.position.y
            return sqrt(dx * dx + dy * dy)
        }

        return null
    }

    /**
     * Calculate the bounding box diagonal for the upper body landmarks
     */
    private fun calculateBboxDiagonal(pose: Pose): Float? {
        // List of upper body key points to consider
        val landmarkTypes = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP
        )

        // Get all available key points
        val landmarks = landmarkTypes.mapNotNull { pose.getPoseLandmark(it) }

        // Return null if there are not enough key points
        if (landmarks.size < 3) return null

        // Find the minimum and maximum values of x, y, z coordinates
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (landmark in landmarks) {
            minX = minOf(minX, landmark.position3D.x)
            minY = minOf(minY, landmark.position3D.y)
            minZ = minOf(minZ, landmark.position3D.z)
            maxX = maxOf(maxX, landmark.position3D.x)
            maxY = maxOf(maxY, landmark.position3D.y)
            maxZ = maxOf(maxZ, landmark.position3D.z)
        }

        // Calculate the diagonal length of the 3D bounding box
        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ
        return Math.sqrt((width * width + height * height + depth * depth).toDouble()).toFloat()
    }

    private fun calculateCV(values: List<Float>): Float {
        if (values.isEmpty() || values.size < 3) return Float.MAX_VALUE

        val mean = values.average().toFloat()
        if (mean == 0f) return Float.MAX_VALUE

        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance)

        return stdDev / mean
    }
    // Interface: Get current detected pose data
    fun getCurrentPoseData(): Pose? {
        return currentPose
    }

    private suspend fun trainModel() {
        disableAllBtn()
        progressBar.text = progressBarStr(0)
        if(isDevelop){
            tips.text = "Preparing to train classifier..."
        }
        else{
            tips.text = "Load data..."
        }


        try {
            // Check if we have samples (10% progress)
//                progressBar.progress = 10
            for (i in 1..10) {
                progressBar.text = progressBarStr(i)
                delay(trainPause)
            }
//            progressBar.text = progressBarStr(10)
            val totalSamples = moves.sumOf { it.normalizedSamples.size }
            if (totalSamples == 0) {
                tips.text = "Cannot train classifier: No normalized samples available"

                trainMsg = "No normalized data available."
                delay(trainPause)
                tips.isInvisible = true
                progressBar.isInvisible = true
                enableAllBtn()
                return
            }
            if (isDevelop){
                tips.text = "Training classifier with $totalSamples samples..."
            }
            else{
                tips.text = "Training AI model..."
            }

//                progressBar.progress = 20

            progressBar.text = progressBarStr(20)
            for (i in 10..20) {
                progressBar.text = progressBarStr(i)
                delay(trainPause)
            }
//            delay(500)

            // Collect all samples (30% progress)
            val allSamples = withContext(Dispatchers.Default) {
                moves.flatMap { it.normalizedSamples }
            }
//                progressBar.progress = 50
//            progressBar.text = progressBarStr(50)
            for (i in 20..50) {
                progressBar.text = progressBarStr(i)
                delay(trainPause)
            }
//            delay(500)
            // Initialize classifier
//                classifier = RuleBasedClassifier()
//                classifier = ImprovedRuleBasedClassifier()
            classifier = LightweightTreeClassifier()
//            classifier = EnhancedTreeClassifier()
            if (isDevelop){
                tips.text = "Processing feature ranges for each move..."
            }
            else{
                tips.text = "Analyzing each move..."
            }

            // Train the classifier (takes most time - 50% progress)
            val success = withContext(Dispatchers.Default) {
                // You could update progress inside train method for more granular updates
                classifier.train(allSamples)

            }
//                progressBar.progress = 80

            for (i in 50..100) {
                progressBar.text = progressBarStr(i)
                delay(trainPause)
            }
//            progressBar.text = progressBarStr(80)
//            delay(500)
////                progressBar.progress = 100
//            progressBar.text = progressBarStr(100)

            // Show results
            if (success) {
//                    val trainedMoves = classifier.getTrainedMoves().joinToString(", ")
                if (isDevelop){
                    tips.text = "Classifier trained successfully!\n" +
                            "Total samples: $totalSamples"
                }
                else{
                    tips.text = "Train success!"
                }
                isTrained = true
            } else {
                if(isDevelop){
                    tips.text = "Classifier training failed. Please try again."
                }
                else{
                    tips.text = "Train failed."
                    trainMsg = "Low sampling quality."
                }
            }

            delay(1000)

        } catch (e: Exception) {
            Log.e("ModelTraining", "Error during classifier training", e)
            tips.text = "Training error: ${e.message}"
            trainMsg = "${e.message}"
            delay(1000)

        }

        enableAllBtn()
    }

    /**
     * Test user reaction time and accuracy
     */
    private fun testReaction(numTest: Int) {
        if (!::classifier.isInitialized || !classifier.isTrained() || !isTrained) {
//            Toast.makeText(this, "Please train the classifier first", Toast.LENGTH_SHORT).show()
            testMsg = "The AI model is not trained."
            terminalTxtUpdate("test", isTested,testMsg)
            return
        }

        // Define constants
        val UNKNOWN_ACTION = "unknown"
//        val CONFIDENCE_THRESHOLD = classifier.getConfidenceThreshold()

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            terminalTxtUpdateStart("Starting Camera...")
            delay(500)
            startCamera()
            InfoCover.visibility = View.INVISIBLE
            isSaved = false
            msgWindow.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
            disableAllBtn()
            try {
                correctIcon.isInvisible = false
//                tips.isInvisible = false
//                tips.text = "Get ready for reaction test!\nThe color bar will change colors.\nMake the corresponding move as quickly as possible."
                tips.text = "Reaction test!"
                colorBar.setBackgroundColor(Color.GRAY)
                delay(3000)

                // Prepare test data
                val moveOptions = moves.filter { it.normalizedSamples.size > 0 }
                if (moveOptions.isEmpty()) {
                    tips.text = "No trained moves available for testing"
                    delay(3000)
                    tips.isInvisible = true
                    return@launch
                }

                // Randomly select test moves
                val testMoves = List(numTest) { moveOptions.random() }

                // Statistics variables
                val reactionTimes = mutableListOf<Long>()
                var correctCount = 0

                // Prediction window settings
                val WINDOW_SIZE = 5
                val REQUIRED_CONSENSUS = 4

                // Find best reference point and normalization factor
//            val bestReferencePoint = findMostStableKeypoint(moves)
//            val bestNormFactor = findMostStableNormalizationFactor(moves)

                val bestReferencePoint = ref
                val bestNormFactor = fac

                // Run tests
                for (i in 0 until numTest) {
                    // Select current test move
                    val currentMove = testMoves[i]

                    // Countdown
                    for (count in 3 downTo 1) {
                        tips.text = "Next test in $count..."
                        delay(1000)
                    }

                    // Show prompt
                    tips.text = "DO ${currentMove.name} NOW!"
                    colorBar.setBackgroundColor(currentMove.color)

                    // Record start time
                    val startTime = System.currentTimeMillis()

                    // Detection variables
                    var detected = false
                    var detectedMove = ""
                    var confidence = 0f
                    var endTime: Long = 0

                    // Real-time display variables
                    var lastUpdateTime = 0L
                    val UPDATE_INTERVAL = 200L // Real-time update interval (milliseconds)

                    // Display variables
                    var currentPrediction = UNKNOWN_ACTION
                    var currentConfidence = 0f

                    // Prediction window
                    val predictionWindow = mutableListOf<Pair<String, Float>>()

                    // Detection loop with timeout
                    withTimeoutOrNull(3000) { // 3 seconds timeout
                        while (!detected) {
                            val currentTime = System.currentTimeMillis()

                            // Get current pose
                            val currentPose = getCurrentPoseData()

                            if (currentPose != null) {
                                // Normalize pose
                                val features = normalizePose(currentPose, bestReferencePoint, bestNormFactor)

                                if (features != null) {
                                    // Predict move
                                    val prediction = classifier.predict(features)
                                    currentPrediction = prediction.first
                                    currentConfidence = prediction.second

                                    // Real-time display of current detection and confidence
                                    if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
                                        val remainingTime = (3000 - (currentTime - startTime)) / 1000 // +1 to round up
                                        correctIcon.text = "$remainingTime"
                                        correctIcon.setTextColor(Color.WHITE)
                                        val confidencePercent = (currentConfidence * 100).toInt()
                                        if (isDevelop){
                                            tips.text = "Target: ${currentMove.name}\n" +
                                                    "Current: $currentPrediction ($confidencePercent%)\n"
//                                                    "Threshold: ${(CONFIDENCE_THRESHOLD * 100).toInt()}%"
                                        }
                                        else{
                                            tips.text = "Target: ${currentMove.name}\n Current: $currentPrediction"
                                        }

                                        lastUpdateTime = currentTime

                                        // Use color to indicate confidence
//                                        if(isDevelop){
//                                            when {
//                                                currentConfidence < 0.5f -> tips.setTextColor(Color.RED)
//                                                currentConfidence < CONFIDENCE_THRESHOLD -> tips.setTextColor(Color.YELLOW)
//                                                else -> tips.setTextColor(Color.GREEN)
//                                            }
//                                        }

                                    }

                                    // Add to prediction window
                                    predictionWindow.add(prediction)

                                    // Keep window at fixed size
                                    if (predictionWindow.size > WINDOW_SIZE) {
                                        predictionWindow.removeAt(0)
                                    }

                                    // Check for sufficient consensus and confidence above threshold
                                    val result = classifier.predictWithWindow(
                                        features,
                                        predictionWindow.dropLast(1),
                                        REQUIRED_CONSENSUS
                                    )

//                                            && result.second >= CONFIDENCE_THRESHOLD
                                    if (result.first != UNKNOWN_ACTION && result.second >= classifier.getConfidenceThreshold()) {

                                        detectedMove = result.first
                                        confidence = result.second
                                        endTime = System.currentTimeMillis()
                                        detected = true

                                        // Once a move is detected, immediately exit the loop
                                        break
                                    }
                                }
                            }

                            // Short delay
                            delay(50)
                        }
                    }

                    // Reset text color
                    tips.setTextColor(Color.WHITE)

                    // Check results
                    if (detected) {
                        val reactionTime = endTime - startTime
                        reactionTimes.add(reactionTime)
                        if(reactionTime > max_Time){
                            max_Time = reactionTime
                        }
                        if(reactionTime < min_Time){
                            min_Time = reactionTime
                        }

                        val isCorrect = detectedMove == currentMove.name
                        val confidencePercent = (confidence * 100).toInt()

                        if (isCorrect) {
                            correctCount++
                            if(isDevelop){
                                tips.text = "✓ Correct! '${detectedMove}' ($confidencePercent%)\n" +
                                        "Reaction time: ${String.format("%.2f", reactionTime/1000.0)}s"
                            }
                            else{
                                correctIcon.text = "✓"
                                correctIcon.setTextColor(Color.GREEN)
                                correctIcon.isInvisible = false
                                tips.text = "Correct! Reaction time: ${String.format("%.2f", reactionTime/1000.0)}s"
                            }

                        } else {
                            if(isDevelop){
                                correctIcon.setTextColor(Color.RED)
                                tips.text = "Incorrect! You did '${detectedMove}' ($confidencePercent%)\n" +
                                        "instead of '${currentMove.name}'\n" +
                                        "Reaction time: ${String.format("%.2f", reactionTime/1000.0)}s"
                            }
                            else{
                                correctIcon.text = "✗"
                                correctIcon.isInvisible = false
                                correctIcon.setTextColor(Color.RED)
//                                tips.text = "Incorrect! You did '${detectedMove} " +
//                                        "instead of '${currentMove.name}'\n" +
//                                        "Reaction time: ${String.format("%.2f", reactionTime/1000.0)}s"
                                tips.text = "Incorrect! You did '${detectedMove} " +
                                        "instead of '${currentMove.name}'\n"
                            }
                        }
                    } else {
                        if(isDevelop){
                            tips.text = "No move detected with sufficient confidence\n" +
                                    "Last prediction: $currentPrediction (${(currentConfidence * 100).toInt()}%)"
                        }
                        else{
                            tips.text = "No move detected!"
                        }
                    }

                    // Reset color bar
                    colorBar.setBackgroundColor(Color.GRAY)

                    // Display result (give user enough time to read)
                    delay(3000)
                    correctIcon.text = ""


                    // Clearly indicate to user that this test is complete
                    if(i+1 < numTest){
                        tips.text = "Test ${i+1}/${numTest} completed."
                    }
                    else{
                        tips.text = "All ${i+1} tests completed."
                    }

//                    tips.text = "Test ${i+1}/${numTest} completed. Get ready for next test..."

                    delay(1500)
                }

                // Calculate final statistics
                val accuracy = if (numTest > 0) correctCount.toFloat() / numTest else 0f
//                val avgReactionTime = if (reactionTimes.isNotEmpty())
//                    reactionTimes.average() / 1000.0 else 0.0
                val avgReactionTime = if (reactionTimes.isNotEmpty())
                    reactionTimes.average() else 0.0
                //save to global
                acc = accuracy.toDouble()
                avg_Time = avgReactionTime

//                tips.text = "Test complete!\n" +
//                        "Accuracy: ${(accuracy * 100).toInt()}% ($correctCount/$numTest)\n" +
//                        "Average reaction time: ${String.format("%.2f", avgReactionTime)}s"
//                tips.text = "Accuracy: ${(accuracy * 100).toInt()}%" +
//                        "Avg Reaction time: ${String.format("%.2f", avgReactionTime)}s"
//
//                // Hide tips after showing results
//                delay(5000)
//                tips.isInvisible = true
                colorBar.setBackgroundColor(Color.TRANSPARENT)
            } catch (e: Exception) {
                Log.e("ReactionTest", "Error during reaction test", e)
                tips.text = "Test error: ${e.message}"
                delay(3000)
//                tips.isInvisible = true
            }
            correctIcon.isInvisible = true
//            stopCamera()
            enableAllBtn()
            correctIcon.setTextColor(Color.WHITE)
            msgWindow.visibility = View.INVISIBLE
            InfoCover.visibility = View.VISIBLE
            isTested = true
            stopCamera()
            terminalTxtUpdateStart("Camera Stopped.")
            terminalTxtUpdate("test",isTested)
        }
    }

    private fun confirmSave() {
        try {
            var msg = ""
            var accTxt = String.format("%.0f", acc * 100.0)+"%"
            var responseTimeTxt = String.format("%.4g", avg_Time/1000.0)

            if (categoryId.isNotEmpty()) {
                val firstCategoryId = categoryId.first()

                val categories = dbHelper.getAllCategories()
                val categoryName = categories.find { it.id == firstCategoryId }?.name ?: "Unknown"


                if (!isTested) {
                    accTxt = "N/A"
                    responseTimeTxt = "N/A"
                }

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Save Performance Data")

                val message = StringBuilder()
                    .append("Category: $categoryName\n")
                    .append("Accuracy: $accTxt\n")
                    .append("Avg Reaction Time: $responseTimeTxt s")
                    .toString()

                builder.setMessage(message)

                builder.setPositiveButton("Save") { dialog, _ ->
                    if (!isTested) {
                        terminalTxtUpdate("save", false, "No valid data.")
                        dialog.dismiss()
                        return@setPositiveButton
                    } else if (isSaved) {
                        terminalTxtUpdate("save", false, "Already saved!")
                        dialog.dismiss()
                        return@setPositiveButton
                    }

                    try {
//                        val performance = Performance(
//                            categoryId = firstCategoryId,
//                            avgResponseTime = avg_Time.toDouble(),
//                            accuracy = acc.toDouble()
//                        )

//                        val result = dbHelper.addOrUpdatePerformance(performance)
//                        Log.d("saveRecord", avg_Time.toString())
                        val result = dbHelper.addPerformanceRecord(firstCategoryId, avg_Time, acc)

                        if (result != -1L) {
                            isSaved = true
                            msg = "Success to save into the database."
                        } else {
                            msg = "Failed to save into the database."
                        }

                        terminalTxtUpdate("save", true, msg)
                    } catch (e: Exception) {
                        Log.e("ConfirmSave", "Error saving data: ${e.message}", e)
                        terminalTxtUpdate("save", false, "Error: ${e.message}")
                    }

                    dialog.dismiss()
                }

                builder.setNegativeButton("Cancel") { dialog, _ ->
                    terminalTxtUpdate("save", false, "Canceled by user.")
                    dialog.dismiss()
                }

                builder.create().show()
            } else {
                terminalTxtUpdate("save", false, "No category ID available.")
            }
        } catch (e: Exception) {
            Log.e("ConfirmSave", "Unexpected error: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    fun disableAllBtn(){
        leftBTN.isEnabled = false
        allBTN.isEnabled = false
        rightBTN.isEnabled = false
    }

    fun enableAllBtn(){
        leftBTN.isEnabled = true
        allBTN.isEnabled = true
        rightBTN.isEnabled = true
    }

    // Interface: Get specific coordinates
    fun getLandmarkPosition(landmarkType: Int): Pair<Float, Float>? {
        val landmark = currentPose?.getPoseLandmark(landmarkType)
        return if (landmark != null) {
            Pair(landmark.position.x, landmark.position.y)
        } else {
            null
        }
    }

    // Added: Method to toggle the display of red dots
    fun toggleLandmarkDotsDisplay(show: Boolean) {
        showLandmarkDots = show
        // If there is a current pose, redraw immediately
        currentPose?.let { processPose(it) }
    }

    // Added: Method to toggle the display of coordinate text
    fun toggleCoordinatesTextDisplay(show: Boolean) {
        showCoordinatesText = show
        // If there is a current pose, redraw immediately
        currentPose?.let { processPose(it) }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            cameraProvider = cameraProviderFuture.get()
            // Set up preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Set up image analysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer())
                }

            // Select front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                // Unbind all existing bindings
                cameraProvider.unbindAll()

                // Bind use cases to the camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

//                textViewStatus.text = "Camera started"
//                Toast.makeText(this, "Camera started", Toast.LENGTH_SHORT).show()
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
//                textViewStatus.text = "Camera start failed: ${exc.message}"
                Toast.makeText(this, "Camera start failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        if (::cameraProvider.isInitialized) {
            // This will unbind all use cases and effectively stop the camera
            cameraProvider.unbindAll()
            previewView.removeAllViews()
//            Toast.makeText(this, "Camera stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processPose(pose: Pose) {
        // Update current pose data
        currentPose = pose

        // Draw key points
        val holder = surfaceView.holder
        val canvas = holder.lockCanvas() ?: return

        try {
            // Clear canvas
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Call the drawing function regardless of the master switch, the function will determine what to draw based on switch states
            drawPoseLandmarks(canvas, pose)

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawPoseLandmarks(canvas: Canvas, pose: Pose) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val landmarks = arrayOf(
            Pair(PoseLandmark.NOSE, showNose),
            Pair(PoseLandmark.LEFT_SHOULDER, showLeftShoulder),
            Pair(PoseLandmark.RIGHT_SHOULDER, showRightShoulder),
            Pair(PoseLandmark.LEFT_ELBOW, showLeftElbow),
            Pair(PoseLandmark.RIGHT_ELBOW, showRightElbow),
            Pair(PoseLandmark.LEFT_WRIST, showLeftWrist),
            Pair(PoseLandmark.RIGHT_WRIST, showRightWrist),
            Pair(PoseLandmark.LEFT_HIP, showLeftHip),
            Pair(PoseLandmark.RIGHT_HIP, showRightHip)
        )

        for ((landmarkType, isVisible) in landmarks) {
            if (!isVisible) continue

            val landmark = pose.getPoseLandmark(landmarkType)
            if (landmark != null) {
                // Only draw red dots when showLandmarkDots is true
                if (showLandmarkDots) {
                    canvas.drawCircle(
                        landmark.position.x / 2, // Adjust scale to fit screen
                        landmark.position.y / 2,
                        10f,
                        paint
                    )
                }

                // Only show coordinate information when showCoordinatesText is true
                if (showCoordinatesText) {
                    val landmarkName = getLandmarkName(landmarkType)
                    val coordsText = "$landmarkName: (${landmark.position.x.toInt()}, ${landmark.position.y.toInt()})"

                    // Determine text position based on landmark type
                    val textY = when (landmarkType) {
                        PoseLandmark.NOSE -> 50f
                        PoseLandmark.LEFT_SHOULDER -> 100f
                        PoseLandmark.RIGHT_SHOULDER -> 150f
                        PoseLandmark.LEFT_ELBOW -> 200f
                        PoseLandmark.RIGHT_ELBOW -> 250f
                        PoseLandmark.LEFT_WRIST -> 300f
                        PoseLandmark.RIGHT_WRIST -> 350f
                        PoseLandmark.LEFT_HIP -> 400f
                        PoseLandmark.RIGHT_HIP -> 450f
                        else -> 500f
                    }

                    canvas.drawText(coordsText, 20f, textY, paint)
                }
            } else if (showCoordinatesText) {
                // Only show "unable to capture" information when showCoordinatesText is true
                val landmarkName = getLandmarkName(landmarkType)
                val textY = when (landmarkType) {
                    PoseLandmark.NOSE -> 50f
                    PoseLandmark.LEFT_SHOULDER -> 100f
                    PoseLandmark.RIGHT_SHOULDER -> 150f
                    PoseLandmark.LEFT_ELBOW -> 200f
                    PoseLandmark.RIGHT_ELBOW -> 250f
                    PoseLandmark.LEFT_WRIST -> 300f
                    PoseLandmark.RIGHT_WRIST -> 350f
                    PoseLandmark.LEFT_HIP -> 400f
                    PoseLandmark.RIGHT_HIP -> 450f
                    else -> 500f
                }

                canvas.drawText("$landmarkName: Unable to capture", 20f, textY, paint)
            }
        }
    }

    private fun getLandmarkName(landmarkType: Int): String {
        return when (landmarkType) {
            PoseLandmark.NOSE -> "Nose"
            PoseLandmark.LEFT_SHOULDER -> "Left Shoulder"
            PoseLandmark.RIGHT_SHOULDER -> "Right Shoulder"
            PoseLandmark.LEFT_ELBOW -> "Left Elbow"
            PoseLandmark.RIGHT_ELBOW -> "Right Elbow"
            PoseLandmark.LEFT_WRIST -> "Left Wrist"
            PoseLandmark.RIGHT_WRIST -> "Right Wrist"
            PoseLandmark.LEFT_HIP -> "Left Hip"
            PoseLandmark.RIGHT_HIP -> "Right Hip"
            else -> "Unknown"
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Callback when Surface is created
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Callback when Surface changes
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Callback when Surface is destroyed
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
//                startCamera()
            } else {
//                textViewStatus.text = "Camera permission is required to run this app"
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // Image analyzer
    @androidx.camera.core.ExperimentalGetImage
    private inner class PoseAnalyzer : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                // Perform pose detection
                poseDetector.process(image)
                    .addOnSuccessListener { pose ->
                        processPose(pose)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Pose detection failed: $e")
                    }
                    .addOnCompleteListener {
                        // Release image
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    companion object {
        private const val TAG = "PoseDetectionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    fun progressBarStr(percentage: Int, totalBlocks: Int = 10): String {
        val validPercentage = percentage.coerceIn(0, 100)

        val filledBlocks = (validPercentage.toFloat() / 100 * totalBlocks).toInt()

        val progressBar = StringBuilder()

        progressBar.append("[")

        for (i in 0 until totalBlocks) {
            if (i < filledBlocks) {
                progressBar.append("■")
            } else {
                progressBar.append("□")
            }

            if (i < totalBlocks - 1) {
                progressBar.append(" ")
            }
        }
        progressBar.append("] $validPercentage%")

        return progressBar.toString()
    }

    fun reactionTimeFigure(
        minTime: Long,
        maxTime: Long,
        avgTime: Long,
        segments: Int = 10,
        filledChar: Char = '|',
        emptyChar: Char = '-'
    ): String {
        if (minTime >= maxTime) {
            return "[${filledChar.toString().repeat(segments)}]"
        }

        val range = maxTime - minTime
        val segmentSize = range / segments

        val segmentIndex = ((avgTime - minTime) / segmentSize)
            .toInt()
            .coerceIn(0, segments - 1)

        val visualization = StringBuilder("[")

        for (i in 0 until segments) {
            visualization.append(if (i == segmentIndex) filledChar else emptyChar)
            if (i < segments - 1) visualization.append(" ")
        }

        visualization.append("]")
        visualization.append(" "+String.format("%.4g", avg_Time/1000.0)+"s")
        return visualization.toString()
    }

    fun terminalTxtUpdateStart(type: String){
//        terminalStr += " ${type}\n" + strStart
//        terminalTxt.text = terminalStr

        terminalSpannable.append(" ${type}\n$strStart")
        terminalTxt.text = terminalSpannable
        terminalScroll.post {
            terminalScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun terminalTxtUpdate(type: String, success: Boolean, extra:String = ""){

        if(type == "add"){
            if(success){
                terminalSpannable.append(" Update success!\n\n$strStart")
//                terminalStr += " Update success!\n\n"+strStart
            }
            else{
                terminalSpannable.append(" Failed! ${extra}\n\n$strStart")
//                terminalStr += " Failed! ${extra}\n\n"+strStart
            }
        }
        else if(type == "sample"){
            if(success){
                terminalSpannable.append(" Sampling success!\n\n$strStart")
//                terminalStr += " Sampling success!\n\n"+strStart
            }
            else{
                terminalSpannable.append(" Failed! Please add moves first!\n\n$strStart")
//                terminalStr += " Failed! Please add moves first!\n\n"+strStart
            }
        }
        else if(type == "train"){
            if (success){
                terminalSpannable.append(" AI Training success!\n\n$strStart")
//                terminalStr += " AI Training success!\n\n"+strStart
            }
            else{
                terminalSpannable.append(" Failed! ${trainMsg}\n\n$strStart")
//                terminalStr += " Failed! ${trainMsg}\n\n"+strStart
            }
        }
        else if(type == "test"){
            if(success){
                val color = ContextCompat.getColor(this, R.color.fat_orange)
                val table = buildSpannedString {
                    append(" Result:")
                    append("\nAccuracy:\n")
                    color(color) {
                        append(progressBarStr((acc * 100.0).toInt(), 10))
                    }
                    append("\nAvg reaction time:\n")
                    color(color) {
//                        append(String.format("%.4g", avg_Time))
                        append(reactionTimeFigure(min_Time, max_Time, (avg_Time/1000.0).toLong()))
                    }
                    append("\n\n")
                    append(strStart)
                }
                terminalSpannable.append(table)
//                terminalStr = spannedString.toString()+strStart
            }
            else{
                terminalSpannable.append(" Failed! ${extra}\n\n$strStart")
//                terminalStr += " Failed! ${extra}\n\n"+strStart
            }

        }
        else if(type == "save"){
            if(success){
                terminalSpannable.append(" Save success.\n\n$strStart")
//                terminalStr = spannedString.toString()+strStart
            }
            else{
                terminalSpannable.append(" Failed! ${extra}\n\n$strStart")
//                terminalStr += " Failed! ${extra}\n\n"+strStart
            }

        }
        else{

        }
        terminalTxt.text = terminalSpannable
        terminalScroll.post {
            terminalScroll.fullScroll(View.FOCUS_DOWN)
        }

    }

}



