package com.example.humanreactor.custom

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class MotionAnalyzer(private val listener: (List<Pose>) -> Unit) : ImageAnalysis.Analyzer {

    private val poseDetector: PoseDetector

    init {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        poseDetector.process(image)
            .addOnSuccessListener { poseDetection ->
                // 將 ML Kit 的姿勢轉換為我們的數據結構
                val poses = poseDetection?.let { convertPoses(it) } ?: emptyList()
                listener(poses)
            }
            .addOnFailureListener {
                // 處理錯誤
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun convertPoses(mlKitPose: com.google.mlkit.vision.pose.Pose): List<Pose> {
        val keypoints = mlKitPose.allPoseLandmarks.map { landmark ->
            Keypoint(
                type = mapLandmarkToKeypoint(landmark.landmarkType),
                position = Position(landmark.position.x, landmark.position.y),
                score = landmark.inFrameLikelihood
            )
        }

        return listOf(Pose(keypoints))
    }

    private fun mapLandmarkToKeypoint(landmarkType: Int): KeypointType {
        return when (landmarkType) {
            0 -> KeypointType.NOSE
            1 -> KeypointType.LEFT_EYE
            2 -> KeypointType.RIGHT_EYE
            3 -> KeypointType.LEFT_EAR
            4 -> KeypointType.RIGHT_EAR
            5 -> KeypointType.LEFT_SHOULDER
            6 -> KeypointType.RIGHT_SHOULDER
            7 -> KeypointType.LEFT_ELBOW
            8 -> KeypointType.RIGHT_ELBOW
            9 -> KeypointType.LEFT_WRIST
            10 -> KeypointType.RIGHT_WRIST
            11 -> KeypointType.LEFT_HIP
            12 -> KeypointType.RIGHT_HIP
            13 -> KeypointType.LEFT_KNEE
            14 -> KeypointType.RIGHT_KNEE
            15 -> KeypointType.LEFT_ANKLE
            16 -> KeypointType.RIGHT_ANKLE
            else -> KeypointType.NOSE
        }
    }
}