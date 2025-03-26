package com.example.humanreactor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.humanreactor.R
import com.example.humanreactor.custom.ColorAdapter
import com.example.humanreactor.custom.ConnectionDrawData
import com.example.humanreactor.custom.KeypointDrawData
import com.example.humanreactor.custom.KeypointType
import com.example.humanreactor.custom.KeypointView
import com.example.humanreactor.custom.Motion
import com.example.humanreactor.custom.MotionAnalyzer
import com.example.humanreactor.custom.Pose
import com.example.humanreactor.custom.Position
import com.example.humanreactor.custom.StaticPoseClassifier

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.acos
import kotlin.math.sqrt

class CustomMotionActivity : AppCompatActivity() {

    // UI 元素
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var statusTextView: TextView
    private lateinit var colorIndicatorView: View
    private lateinit var btnAdd: Button
    private lateinit var btnTrain: Button
    private lateinit var btnTest: Button

    // 相機相關
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService

    // 動作儲存
    private val motions = mutableListOf<Motion>()
    private val usedColors = mutableSetOf<Int>()

    // 目前選定的動作
    private var selectedMotion: Motion? = null

    private lateinit var keypointView: KeypointView

    //    private val classifier = MotionSVMClassifier()
    private val classifier = StaticPoseClassifier()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_motion)

        // 初始化 UI 元素
        cameraPreviewView = findViewById(R.id.cameraPreviewView)
        statusTextView = findViewById(R.id.statusTextView)
        colorIndicatorView = findViewById(R.id.colorIndicatorView)
        btnAdd = findViewById(R.id.btnAdd)
        btnTrain = findViewById(R.id.btnTrain)
        btnTest = findViewById(R.id.btnTest)
        keypointView = findViewById(R.id.keypointView)

        // 初始化相機執行器
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 請求相機權限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 設置按鈕點擊事件
        setupButtons()


    }

    private fun setupButtons() {
        btnAdd.setOnClickListener {
            showAddMotionDialog()
        }

        btnTrain.setOnClickListener {
            showTrainMotionDialog()
        }

        btnTest.setOnClickListener {
            showTestMotionDialog()
        }
    }

    private fun showAddMotionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_motion, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.editTextMotionName)
        val colorPicker = dialogView.findViewById<View>(R.id.colorPickerView)

        // 這裡應該有一個顏色選擇器的實現，為了簡化，我使用預設顏色
        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.BLACK
        ).filter { it !in usedColors }

        val colorAdapter = ColorAdapter(this, colors)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.colorSpinner)
        colorSpinner.adapter = colorAdapter

        var selectedColor = colors.firstOrNull() ?: Color.WHITE

        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedColor = colors[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("新增動作")
            .setView(dialogView)
            .setPositiveButton("新增") { _, _ ->
                val motionName = nameEditText.text.toString()
                if (motionName.isNotBlank() && selectedColor != Color.WHITE) {
                    addMotion(motionName, selectedColor)
                } else {
                    Toast.makeText(this, "請輸入動作名稱並選擇顏色", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTrainMotionDialog() {
        if (motions.isEmpty()) {
            Toast.makeText(this, "請先新增動作", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("訓練所有動作")
            .setMessage("確定要訓練所有動作嗎？")
            .setPositiveButton("開始訓練") { _, _ ->
                trainAllMotions(5,1000)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun trainAllMotions(durationSeconds: Int, sampleCount: Int) {
        if (motions.isEmpty()) {
            statusTextView.text = "請先新增動作"
            return
        }

        Thread {
            for (motion in motions) {
                // 顯示準備訓練的訊息
                runOnUiThread {
                    statusTextView.text = "準備訓練: ${motion.name}"
                    colorIndicatorView.setBackgroundColor(motion.color)
                    colorIndicatorView.visibility = View.VISIBLE
                }

                // 倒數3秒，讓用戶準備
                for (i in 3 downTo 1) {
                    runOnUiThread {
                        statusTextView.text = "準備捕捉動作: ${motion.name}，$i 秒後開始..."
                    }
                    Thread.sleep(1000) // 等待1秒
                }

                // 設置訓練時間與採樣頻率
                val samples = mutableListOf<Pose>()
                val startTime = System.nanoTime()
                val totalDurationNanos = durationSeconds * 1_000_000_000L
                val sampleInterval = totalDurationNanos / sampleCount

                var lastSampleTime = startTime

                while (System.nanoTime() - startTime < totalDurationNanos && samples.size < sampleCount) {
                    val currentTime = System.nanoTime()
                    if (currentTime - lastSampleTime >= sampleInterval) {
                        val currentPose = getCurrentPose()
                        if (currentPose != null) {
                            samples.add(currentPose)
                        }
                        lastSampleTime = currentTime
                    }

                    // 降低 UI 更新頻率，避免 UI 卡頓
                    if (samples.size % (sampleCount / 10) == 0) {
                        runOnUiThread {
                            val progress = (samples.size.toFloat() / sampleCount) * 100
                            statusTextView.text = "姿勢名稱:${motion.name}\n收集樣本: ${samples.size} (${progress.toInt()}%)"
                        }
                    }
                }

                motion.isTrained = true
                motion.samples = samples
                // 訓練完一個動作後，稍作停頓
                Thread.sleep(500)
            }

            // 全部動作訓練完成
            runOnUiThread {
                colorIndicatorView.visibility = View.INVISIBLE
                statusTextView.text = "正在準備數據集..."
                Log.d("SVMClassifier", "Num classes: ${motions.size}")
                val dataset = prepareDataset(motions)
                Thread.sleep(500)
                statusTextView.text = "正在訓練模型..."
//                classifier.train(dataset)
                classifier.loadFromDataset(dataset)
                classifier.setSimilarityThreshold(0.75)
                Log.d("SVMClassifier", "Num classes A4: ${motions.size}")
                Thread.sleep(500)
                statusTextView.text = "所有動作訓練完成！"

            }
        }.start()
    }

    private fun prepareDataset(motions: List<Motion>): List<Pair<List<List<Double>>, String>> {
        val dataset = mutableListOf<Pair<List<List<Double>>, String>>()

        for (motion in motions) {
            val validSamples = motion.samples.filter { sample ->
                hasRequiredKeypoints(sample)
            }

            if (validSamples.isEmpty()) {
                Log.w("prepareDataset", "動作 ${motion.name} 沒有足夠的關鍵點樣本，跳過")
                continue
            }

            val allAngles = mutableListOf<List<Double>>() // 存放所有樣本的角度
            for (sample in validSamples) {
                val angles = calculateAngles(sample)
                allAngles.add(angles)
            }

//            // 計算平均角度（用於數據增強和穩定）
//            val avgAngles = calculateAverageAngles(allAngles)
//
//            // 將 (特徵值, 動作名稱) 加入 dataset
//            dataset.add(Pair(avgAngles, motion.name))
//
//            Log.d("prepareDataset", "動作 ${motion.name} - 样本數: ${validSamples.size}, 平均特徵: $avgAngles")
            dataset.add(Pair(allAngles, motion.name))

            Log.d("prepareDataset", "動作 ${motion.name} - 样本數: ${validSamples.size}, 所有特徵: $allAngles")
        }

        return dataset
    }

    private fun hasRequiredKeypoints(sample: Pose): Boolean {
        val requiredKeypoints = setOf(
            KeypointType.NOSE, KeypointType.LEFT_SHOULDER, KeypointType.RIGHT_SHOULDER,
            KeypointType.LEFT_ELBOW, KeypointType.RIGHT_ELBOW, KeypointType.LEFT_WRIST,
            KeypointType.RIGHT_WRIST, KeypointType.LEFT_HIP, KeypointType.RIGHT_HIP
        )
        val sampleKeypoints = sample.keypoints.map { it.type }.toSet()
        return requiredKeypoints.all { it in sampleKeypoints }
    }

    private fun calculateAngles(sample: Pose): List<Double> {
        val keypoints = sample.keypoints
        val nose = keypoints.find { it.type == KeypointType.NOSE }?.position
        val leftShoulder = keypoints.find { it.type == KeypointType.LEFT_SHOULDER }?.position
        val rightShoulder = keypoints.find { it.type == KeypointType.RIGHT_SHOULDER }?.position
        val leftElbow = keypoints.find { it.type == KeypointType.LEFT_ELBOW }?.position
        val rightElbow = keypoints.find { it.type == KeypointType.RIGHT_ELBOW }?.position
        val leftWrist = keypoints.find { it.type == KeypointType.LEFT_WRIST }?.position
        val rightWrist = keypoints.find { it.type == KeypointType.RIGHT_WRIST }?.position
        val leftHip = keypoints.find { it.type == KeypointType.LEFT_HIP }?.position
        val rightHip = keypoints.find { it.type == KeypointType.RIGHT_HIP }?.position

        if (nose == null || leftShoulder == null || rightShoulder == null || leftElbow == null || rightElbow == null || leftWrist == null || rightWrist == null || leftHip == null || rightHip == null) {
            return listOf() // 数据不完整，跳过
        }

        return listOf(
            calculateAngle(rightShoulder, nose, leftShoulder), // 肩膀角度
            calculateAngle(leftWrist, leftElbow, leftShoulder), // 左手肘角度
            calculateAngle(rightWrist, rightElbow, rightShoulder), // 右手肘角度
            calculateAngle(leftElbow, leftShoulder, leftHip), // 左上臂角度
            calculateAngle(rightElbow, rightShoulder, rightHip), // 右上臂角度
            calculateAngle(leftShoulder, leftHip, rightHip) // 身體傾斜角度
        )
    }

    private fun calculateAngle(a: Position, b: Position, c: Position): Double {
        val ab = Pair(a.x - b.x, a.y - b.y)
        val cb = Pair(c.x - b.x, c.y - b.y)

        val dotProduct = ab.first * cb.first + ab.second * cb.second
        val magnitudeAB = sqrt((ab.first * ab.first + ab.second * ab.second).toDouble())
        val magnitudeCB = sqrt((cb.first * cb.first + cb.second * cb.second).toDouble())

        return Math.toDegrees(acos(dotProduct / (magnitudeAB * magnitudeCB)))
    }

    private fun calculateAverageAngles(allAngles: List<List<Double>>): List<Double> {
        val numSamples = allAngles.size
        val numAngles = allAngles[0].size

        return List(numAngles) { i ->
            allAngles.sumOf { it[i] } / numSamples // 計算每個角度的平均值
        }
    }


    private fun showTestMotionDialog() {
        Log.d("showTestMotionDialog","motions size: ${motions.size}")
        Log.d("showTestMotionDialog","model created: ${classifier.modelTrained()}")
//        if (motions.size == 0) {
//            Toast.makeText(this, "請先新增並訓練動作", Toast.LENGTH_SHORT).show()
//            return
//        }
//
        val trainedMotions = motions.filter { it.isTrained }.map { it.name }.toTypedArray()

        if (!classifier.modelTrained()) {
            Toast.makeText(this, "請先訓練至少一個動作", Toast.LENGTH_SHORT).show()
            return
        }

        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("選擇要測試的動作")
            .setSingleChoiceItems(trainedMotions, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("測試") { _, _ ->
                val trainedMotionNames = motions.filter { it.isTrained }.map { it.name }
                val motion = motions.first { it.name == trainedMotionNames[selectedIndex] }
                selectedMotion = motion
                startTesting(motion)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addMotion(name: String, color: Int) {
        // 檢查顏色是否已使用
        if (color in usedColors) {
            Toast.makeText(this, "該顏色已被使用", Toast.LENGTH_SHORT).show()
            return
        }

        val motion = Motion(name, color)
        motions.add(motion)
        usedColors.add(color)

        Toast.makeText(this, "動作 '$name' 新增成功", Toast.LENGTH_SHORT).show()
    }


    // 檢查姿勢是否有效（包含足夠的關鍵點且具有足夠的置信度）
    private fun isPoseValid(pose: Pose): Boolean {
        val minKeypoints = 13 // 要求更多關鍵點
        val minConfidence = 0.75f // 提高最小置信度要求

        // 檢查關鍵點數量
        if (pose.keypoints.size < minKeypoints) {
            Log.d("PoseValidation", "關鍵點數量不足: ${pose.keypoints.size} < $minKeypoints")
            return false
        }

        // 檢查上半身關鍵點是否存在且置信度足夠
        val requiredKeypoints = listOf(
            KeypointType.LEFT_SHOULDER, KeypointType.RIGHT_SHOULDER,
            KeypointType.LEFT_ELBOW, KeypointType.RIGHT_ELBOW,
            KeypointType.LEFT_WRIST, KeypointType.RIGHT_WRIST,
            KeypointType.NOSE
        )

        // 確保所有必需的關鍵點都存在且置信度足夠
        for (keyType in requiredKeypoints) {
            val keypoint = pose.keypoints.find { it.type == keyType }
            if (keypoint == null) {
                Log.d("PoseValidation", "缺少關鍵點: $keyType")
                return false
            }
            if (keypoint.score < minConfidence) {
                Log.d("PoseValidation", "關鍵點 $keyType 置信度不足: ${keypoint.score} < $minConfidence")
                return false
            }
        }

//        // 檢查關鍵點之間的相對位置是否合理
//        val leftShoulder = pose.keypoints.find { it.type == KeypointType.LEFT_SHOULDER }?.position
//        val rightShoulder = pose.keypoints.find { it.type == KeypointType.RIGHT_SHOULDER }?.position
//
//        if (leftShoulder != null && rightShoulder != null) {
//            // 計算肩膀寬度
//            val shoulderWidth = Math.sqrt(
//                Math.pow((leftShoulder.x - rightShoulder.x).toDouble(), 2.0) +
//                        Math.pow((leftShoulder.y - rightShoulder.y).toDouble(), 2.0)
//            )
//
//            // 如果肩膀寬度太小，可能是檢測不准確
//            if (shoulderWidth < 50) {
//                Log.d("PoseValidation", "肩膀寬度太小: $shoulderWidth < 50")
//                return false
//            }
//        }

        return true
    }

    // 新增一個處理器和方法來獲取當前姿勢
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var currentPoseData: Pose? = null

    private fun getCurrentPose(): Pose? {
        return currentPoseData
    }

    private fun startTesting(motion: Motion) {
        statusTextView.text = "測試動作: ${motion.name}"
        colorIndicatorView.setBackgroundColor(motion.color)
        colorIndicatorView.visibility = View.VISIBLE

        testMotion(motion)
    }

    private fun testMotion(motion: Motion) {
        // 確保動作有訓練數據
        if (!motion.isTrained) {
            Toast.makeText(this, "此動作沒有完成訓練，請先訓練", Toast.LENGTH_SHORT).show()
            selectedMotion = null
            return
        }

        // 先做一次倒計時，讓用戶準備好做動作
        statusTextView.text = "請準備好做出 ${motion.name} 動作"
        colorIndicatorView.setBackgroundColor(motion.color)
        colorIndicatorView.visibility = View.VISIBLE

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000 + 1
                statusTextView.text = "準備測試，$seconds 秒後開始..."
            }

            override fun onFinish() {
                // 倒計時結束，開始實際測試
                startActualTesting2(motion)
            }
        }.start()
    }

    private fun startActualTesting(motion: Motion) {
        // 實際測試邏輯
        statusTextView.text = "測試動作: ${motion.name}，請做出相應姿勢"

        // 設置一個標誌以跟踪是否已經檢測到動作
        var motionDetected = false

        // 為了更準確地識別動作，需要連續多次檢測到相同姿勢才算成功
        val requiredSuccessiveDetections = 5 // 增加到需要連續檢測到5次
        var successiveDetectionCount = 0

        // 保存之前的檢測結果，用於計算穩定性
        val detectionHistory = mutableListOf<Boolean>()
        val historyMaxSize = 10 // 保存最近10次的結果

        // 設置倒計時，如果在規定時間內沒有檢測到動作，則顯示失敗
        object : CountDownTimer(2000, 10) { // 增加到20秒
            override fun onTick(millisUntilFinished: Long) {
                // 在每個tick檢查當前姿勢
                val currentPose = getCurrentPose()
                if (currentPose != null && !motionDetected) {
                    val isDetected = detectMotion(currentPose, motion)

                    // 添加到歷史記錄
                    detectionHistory.add(isDetected)
                    if (detectionHistory.size > historyMaxSize) {
                        detectionHistory.removeAt(0)
                    }

                    // 計算最近的檢測穩定性
                    val stability = if (detectionHistory.size >= 6) {
                        detectionHistory.takeLast(3).count { it } / 3.0
                    } else {
                        0.0
                    }

                    if (isDetected) {
                        successiveDetectionCount++

                        // 如果連續多次檢測到且檢測穩定性高，則認為成功
                        if (successiveDetectionCount >= requiredSuccessiveDetections && stability >= 0.8) {
                            motionDetected = true
                            statusTextView.text = "成功識別動作: ${motion.name}!"
                            Toast.makeText(this@CustomMotionActivity, "測試成功!", Toast.LENGTH_SHORT).show()
                            cancel() // 停止計時器

                            // 延遲一段時間後重置UI
                            handler.postDelayed({
                                selectedMotion = null
                                colorIndicatorView.visibility = View.GONE
                            }, 2000)
                        }
                    } else {
                        // 如果這次沒檢測到，重置連續計數
                        successiveDetectionCount = 0
                    }
                }

                // 更新剩餘時間
                if (!motionDetected) {
                    val secondsLeft = millisUntilFinished / 1000
                    statusTextView.text = "請做出 ${motion.name} 動作... (${secondsLeft}秒)"
                    if (successiveDetectionCount > 0) {
                        statusTextView.text = "${statusTextView.text} (檢測中: $successiveDetectionCount/$requiredSuccessiveDetections)"
                    }
                }
            }

            override fun onFinish() {
                if (!motionDetected) {
                    statusTextView.text = "未能識別動作，請重試"
                    Toast.makeText(this@CustomMotionActivity, "無法識別此動作，請確保正確做出姿勢", Toast.LENGTH_SHORT).show()

                    // 重置UI
                    selectedMotion = null
                    colorIndicatorView.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun startActualTesting2(motion: Motion) {
        // 實際測試邏輯
        statusTextView.text = "測試動作: ${motion.name}，請做出相應姿勢"

        // 設置一個標誌以跟踪是否已經檢測到動作
        var motionDetected = false

        // 為了更準確地識別動作，需要連續多次檢測到相同姿勢才算成功
        val requiredSuccessiveDetections = 5 // 增加到需要連續檢測到5次
        var successiveDetectionCount = 0

        // 保存之前的檢測結果，用於計算穩定性
        val detectionHistory = mutableListOf<Boolean>()
        val historyMaxSize = 10 // 保存最近10次的結果

        // 設置倒計時，如果在規定時間內沒有檢測到動作，則顯示失敗
        object : CountDownTimer(20000, 100) { // 增加到20秒
            override fun onTick(millisUntilFinished: Long) {
                // 在每個tick檢查當前姿勢
                val currentPose = getCurrentPose()
                if (currentPose != null && !motionDetected) {
//                    val isDetected = detectMotion(currentPose, motion)
                    val isDetected = detectMotion2(currentPose, motion)

                    // 添加到歷史記錄
                    detectionHistory.add(isDetected)
                    if (detectionHistory.size > historyMaxSize) {
                        detectionHistory.removeAt(0)
                    }

                    // 計算最近的檢測穩定性
                    val stability = if (detectionHistory.size >= 3) {
                        detectionHistory.takeLast(3).count { it } / 3.0
                    } else {
                        0.0
                    }

                    if (isDetected) {
                        successiveDetectionCount++

                        // 如果連續多次檢測到且檢測穩定性高，則認為成功
                        if (successiveDetectionCount >= requiredSuccessiveDetections && stability >= 0.8) {
                            motionDetected = true
                            statusTextView.text = "成功識別動作: ${motion.name}!"
                            Toast.makeText(this@CustomMotionActivity, "測試成功!", Toast.LENGTH_SHORT).show()
                            cancel() // 停止計時器

                            // 延遲一段時間後重置UI
                            handler.postDelayed({
                                selectedMotion = null
                                colorIndicatorView.visibility = View.GONE
                            }, 2000)
                        }
                    } else {
                        // 如果這次沒檢測到，重置連續計數
                        successiveDetectionCount = 0
                    }
                }

                // 更新剩餘時間
                if (!motionDetected) {
                    val secondsLeft = millisUntilFinished / 1000
                    statusTextView.text = "請做出 ${motion.name} 動作... (${secondsLeft}秒)"
                    if (successiveDetectionCount > 0) {
                        statusTextView.text = "${statusTextView.text} (檢測中: $successiveDetectionCount/$requiredSuccessiveDetections)"
                    }
                }
            }

            override fun onFinish() {
                if (!motionDetected) {
                    statusTextView.text = "未能識別動作，請重試"
                    Toast.makeText(this@CustomMotionActivity, "無法識別此動作，請確保正確做出姿勢", Toast.LENGTH_SHORT).show()

                    // 重置UI
                    selectedMotion = null
                    colorIndicatorView.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(cameraPreviewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, MotionAnalyzer { poses ->
                // 在這裡處理姿勢檢測結果
                processPoses(poses)
            })

            try {
                cameraProvider.unbindAll()

                // 使用前置攝像頭
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

            } catch (e: Exception) {
                Toast.makeText(this, "相機啟動失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processPoses(poses: List<Pose>) {
        if (poses.isEmpty()) return

        // 更新當前姿勢數據，用於訓練和測試
        currentPoseData = poses.first()

        val currentMotion = selectedMotion ?: return
        val pose = poses.first()
        drawKeypoints(pose)

        runOnUiThread {
            when {
                currentMotion.isTrained && statusTextView.text.toString().startsWith("正在測試") ||
                        statusTextView.text.toString().startsWith("測試動作") -> {
                    // 測試模式下檢測姿勢
                    val detected = detectMotion(poses.first(), currentMotion)
                    if (detected) {
                        statusTextView.text = "檢測到動作: ${currentMotion.name}"
                    }
                }
            }
        }
    }

    // 添加關鍵點繪製方法
    private fun drawKeypoints(pose: Pose) {
        val keypoints = pose.keypoints.map { keypoint ->
            KeypointDrawData(
                x = keypoint.position.x,
                y = keypoint.position.y,
                score = keypoint.score,
                type = keypoint.type
            )
        }

        // 創建連接線數據
        val connections = createConnections(pose)

        runOnUiThread {
            keypointView.setKeypoints(keypoints, connections)
        }
    }

    // 創建人體骨架連接線
    private fun createConnections(pose: Pose): List<ConnectionDrawData> {
        val connections = mutableListOf<ConnectionDrawData>()
        val keypoints = pose.keypoints.associateBy { it.type }

        // 定義需要連接的關鍵點對
        val pairs = listOf(
            Pair(KeypointType.LEFT_SHOULDER, KeypointType.RIGHT_SHOULDER),
            Pair(KeypointType.LEFT_SHOULDER, KeypointType.LEFT_ELBOW),
            Pair(KeypointType.RIGHT_SHOULDER, KeypointType.RIGHT_ELBOW),
            Pair(KeypointType.LEFT_ELBOW, KeypointType.LEFT_WRIST),
            Pair(KeypointType.RIGHT_ELBOW, KeypointType.RIGHT_WRIST),
            Pair(KeypointType.LEFT_SHOULDER, KeypointType.LEFT_HIP),
            Pair(KeypointType.RIGHT_SHOULDER, KeypointType.RIGHT_HIP),
            Pair(KeypointType.LEFT_HIP, KeypointType.RIGHT_HIP),
            Pair(KeypointType.NOSE, KeypointType.LEFT_SHOULDER),
            Pair(KeypointType.NOSE, KeypointType.RIGHT_SHOULDER)
        )

        // 為每一對關鍵點創建連接線
        for ((type1, type2) in pairs) {
            val kp1 = keypoints[type1]
            val kp2 = keypoints[type2]

            if (kp1 != null && kp2 != null && kp1.score > 0.5 && kp2.score > 0.5) {
                connections.add(
                    ConnectionDrawData(
                        startX = kp1.position.x,
                        startY = kp1.position.y,
                        endX = kp2.position.x,
                        endY = kp2.position.y
                    )
                )
            }
        }

        return connections
    }

    private fun detectMotion(pose: Pose, motion: Motion): Boolean {
        // 實際的姿勢檢測邏輯，使用上半身關節角度進行比較

        // 首先檢查姿勢是否有效
        if (!isPoseValid(pose)) {
            Log.d("MotionDetection", "姿勢無效，無法進行檢測")
            return false
        }

        val keypoints = pose.keypoints

        // 獲取左右肩、肘、腕關鍵點
        val nose = keypoints.find{it.type == KeypointType.NOSE}?.position?: return false
        val leftShoulder = keypoints.find { it.type == KeypointType.LEFT_SHOULDER }?.position ?: return false
        val rightShoulder = keypoints.find { it.type == KeypointType.RIGHT_SHOULDER }?.position ?: return false
        val leftElbow = keypoints.find { it.type == KeypointType.LEFT_ELBOW }?.position ?: return false
        val rightElbow = keypoints.find { it.type == KeypointType.RIGHT_ELBOW }?.position ?: return false
        val leftWrist = keypoints.find { it.type == KeypointType.LEFT_WRIST }?.position ?: return false
        val rightWrist = keypoints.find { it.type == KeypointType.RIGHT_WRIST }?.position ?: return false

        // 計算肩膀角度 (肩膀連線與水平線的角度)
        val shoulderAngle = calculateAngle(
            rightShoulder.x, rightShoulder.y,
            nose.x, nose.y,
            leftShoulder.x, leftShoulder.y
        )

        // 計算左右手肘角度
        val leftElbowAngle = calculateAngle(
            leftShoulder.x, leftShoulder.y,
            leftElbow.x, leftElbow.y,
            leftWrist.x, leftWrist.y
        )

        val rightElbowAngle = calculateAngle(
            rightShoulder.x, rightShoulder.y,
            rightElbow.x, rightElbow.y,
            rightWrist.x, rightWrist.y
        )

        // 計算左右腕相對於肘部的角度
        val leftWristAngle = calculateAngle(
            leftShoulder.x, leftShoulder.y,
            leftWrist.x, leftWrist.y,
            leftElbow.x, leftElbow.y,
        )

        val rightWristAngle = calculateAngle(
            rightShoulder.x, rightShoulder.y,
            rightWrist.x, rightWrist.y,
            rightElbow.x, rightElbow.y,
        )

        val rightArmAngle = calculateAngle(
            rightShoulder.x, rightShoulder.y,
            rightElbow.x, rightElbow.y,
            leftShoulder.x, leftShoulder.y)

        val leftArmAngle = calculateAngle(
            leftShoulder.x, leftShoulder.y,
            leftElbow.x, leftElbow.y,
            rightShoulder.x, rightShoulder.y)

        // 如果這是訓練階段且動作還未設定角度，保存這些角度到動作中
        if (!motion.hasAngles()) {
            motion.shoulderAngle = shoulderAngle
            motion.leftElbowAngle = leftElbowAngle
            motion.rightElbowAngle = rightElbowAngle
            motion.leftWristAngle = leftWristAngle
            motion.rightWristAngle = rightWristAngle
            motion.rightArmAngle = rightArmAngle
            motion.leftArmAngle = leftArmAngle
            return true
        }

        // 在測試階段，比較當前角度與已保存的角度
        // 降低容差，使檢測更嚴格
        val shoulderTolerance = 10.0 // 非常嚴格的角度容差
        val elbowTolerance = 25.0
        val wristTolerance = 20.0
        val armTolerance = 15.0

        val shoulderMatch = Math.abs(shoulderAngle - motion.shoulderAngle) <= shoulderTolerance
        val leftElbowMatch = Math.abs(leftElbowAngle - motion.leftElbowAngle) <= elbowTolerance
        val rightElbowMatch = Math.abs(rightElbowAngle - motion.rightElbowAngle) <= elbowTolerance
        val leftWristMatch = Math.abs(leftWristAngle - motion.leftWristAngle) <= wristTolerance
        val rightWristMatch = Math.abs(rightWristAngle - motion.rightWristAngle) <= wristTolerance
        val rightArmAngleMatch = Math.abs(rightArmAngle - motion.rightArmAngle) <= armTolerance
        val leftArmAngleMatch = Math.abs(leftArmAngle - motion.leftArmAngle) <= armTolerance

        // 記錄所有角度差異，用於調試
        Log.d("MotionDetection", "============================================")
        Log.d("MotionDetection", "肩部差異: ${Math.abs(shoulderAngle - motion.shoulderAngle)}")
        Log.d("MotionDetection", "左肘差異: ${Math.abs(leftElbowAngle - motion.leftElbowAngle)}")
        Log.d("MotionDetection", "右肘差異: ${Math.abs(rightElbowAngle - motion.rightElbowAngle)}")
        Log.d("MotionDetection", "左腕差異: ${Math.abs(leftWristAngle - motion.leftWristAngle)}")
        Log.d("MotionDetection", "右腕差異: ${Math.abs(rightWristAngle - motion.rightWristAngle)}")
        Log.d("MotionDetection", "左上臂差異: ${Math.abs(leftArmAngle - motion.leftArmAngle)}")
        Log.d("MotionDetection", "右上臂差異: ${Math.abs(rightArmAngle - motion.rightArmAngle)}")
        Log.d("MotionDetection", "============================================")
        // 更嚴格的匹配邏輯：需要肩膀和至少兩組其他關節角度匹配
        val jointMatches = listOf(leftElbowMatch, rightElbowMatch, leftWristMatch, rightWristMatch,rightArmAngleMatch,leftArmAngleMatch)
        val matchCount = jointMatches.count { it }

        val matchResult = shoulderMatch && matchCount >= 6

        // 記錄詳細的匹配信息，有助於調試
        if (matchResult) {
            Log.d("MotionDetection", "檢測到匹配動作: ${motion.name}")
            Log.d("MotionDetection", "匹配關節數量: $matchCount")
        }

        return matchResult
    }

    private fun detectMotion2(pose: Pose, motion: Motion): Boolean {
        // 實際的姿勢檢測邏輯，使用上半身關節角度進行比較

        // 首先檢查姿勢是否有效
        if (!hasRequiredKeypoints(pose)) {
            Log.d("MotionDetection", "姿勢無效，無法進行檢測")
            return false
        }

        val features = calculateAngles(pose)
        val predictClass = classifier.predict(features)

        return predictClass.first  == motion.name
    }

    // 計算三點形成的角度（以度為單位）
    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Double {
        // 計算兩個向量
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2

        // 計算點積
        val dotProduct = v1x * v2x + v1y * v2y

        // 計算向量長度
        val v1Length = Math.sqrt((v1x * v1x + v1y * v1y).toDouble())
        val v2Length = Math.sqrt((v2x * v2x + v2y * v2y).toDouble())

        // 計算角度（弧度）
        val cosAngle = dotProduct / (v1Length * v2Length)

        // 轉換為度
        return Math.toDegrees(Math.acos(cosAngle.coerceIn(-1.0, 1.0)))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "未獲取必要權限", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}