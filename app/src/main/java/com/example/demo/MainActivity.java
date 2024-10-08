package com.example.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cameraxlib.CameraXHelper;
import com.example.temilib.PatrolHelper;

public class MainActivity extends AppCompatActivity {
    private CameraXHelper cameraXHelper;
    private PatrolHelper patrolHelper;

    private Button startButton, stopButton, closeButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private Runnable photoCaptureRunnable;
    private static final int CAMERA_REQUEST_CODE = 100;
    private boolean isCapturing = false; // 防止重複拍照

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 CameraXHelper
        cameraXHelper = new CameraXHelper(this);
        // 初始化 patrolHelper
        patrolHelper = new PatrolHelper();
        patrolHelper.initPatrol();

        // 設定按鈕
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        closeButton = findViewById(R.id.close_button);

        // 檢查相機權限
        checkCameraPermission();

        // 初始化拍照任務
        photoCaptureRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && !isCapturing) {
                    isCapturing = true;
                    cameraXHelper.capturePhoto();
                    handler.postDelayed(() -> isCapturing = false, 5000); // 5秒間隔
                }
                handler.postDelayed(this, 500); // 確保任務持續執行，短間隔檢查狀態
            }
        };

        // 開始事件
        startButton.setOnClickListener(v -> {
            patrolHelper.startPatrolling(); // 開始巡邏
            isRunning = true;
            handler.post(photoCaptureRunnable);
            Toast.makeText(this, "拍照已開始", Toast.LENGTH_SHORT).show();
        });

        // 暫停拍照
        stopButton.setOnClickListener(v -> {
            pauseCapture();
            patrolHelper.stopPatrolling();
            Log.d("MainActivity", "巡邏已暫停。");
        });

        // 關閉應用程式
        closeButton.setOnClickListener(v -> {
            patrolHelper.stopPatrolling(); // 結束巡邏
            Log.d("MainActivity", "巡邏已結束。");
            closeApp();
        });
    }

    // 檢查和請求相機權限
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            cameraXHelper.startCamera();  // 如果已授權，則啟動相機
        }
    }

    // 處理權限請求的結果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraXHelper.startCamera();  // 獲得權限後啟動相機
        } else {
            Toast.makeText(this, "相機權限被拒絕", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 在 onResume 中啟動相機並開始拍照
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraXHelper.startCamera();  // 確保相機在 Activity 恢復時啟動
            if (isRunning) {
                handler.post(photoCaptureRunnable);  // 如果拍照正在進行，則恢復拍照任務
            }
        }
    }

    // 在 onPause 中暫停拍照以釋放資源
    @Override
    protected void onPause() {
        super.onPause();
        pauseCapture();  // 暫停拍照
        cameraXHelper.stopCamera();  // 暫停相機操作以釋放資源
    }

    // 在 onDestroy 中確保釋放所有資源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        patrolHelper.destroyPatrol();
        cameraXHelper.releaseResources();  // 釋放相機等所有資源
        handler.removeCallbacksAndMessages(null);  // 移除所有未執行的任務
    }

    // 暫停拍照
    private void pauseCapture() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(photoCaptureRunnable);
            Toast.makeText(this, "拍照已暫停", Toast.LENGTH_SHORT).show();
        }
    }

    // 關閉應用程式
    private void closeApp() {
        finishAffinity();  // 關閉所有的 Activity 並結束應用程式
        System.exit(0);    // 完全退出應用程式
    }
}