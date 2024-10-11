package com.example.temilib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.List;

import com.example.cameraxlib.CameraXHelper;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;

public class PatrolHelper implements
        OnRobotReadyListener,
        OnGoToLocationStatusChangedListener,
        OnCurrentPositionChangedListener {

    private static final String TAG = "PatrolHelper";
    private Robot robot;
    private CameraXHelper cameraXHelper;
    private List<String> patrolPoints;
    private int currentPointIndex = 0;
    private boolean isPatrolling = false;


    public PatrolHelper(Context context) {

        this.robot = Robot.getInstance();
        this.cameraXHelper = new CameraXHelper(context); // 初始化 CameraXHelper
    }

    public void initPatrol() {
        robot.addOnRobotReadyListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnCurrentPositionChangedListener(this);
    }

    public void startPatrolling() {
        if (!isPatrolling) {
            patrolPoints = robot.getLocations();
            if (patrolPoints != null && !patrolPoints.isEmpty()) {
                isPatrolling = true;
                currentPointIndex = 0;
                goToNextPoint();
                Log.d(TAG, "開始巡邏。");
            } else {
                Log.d(TAG, "沒有已保存的地點。");
            }
        } else {
            Log.d(TAG, "已經在巡邏中。");
        }
    }



    public void stopPatrolling() {
        isPatrolling = false;
        Log.d(TAG, "停止巡邏。");
    }

    public void destroyPatrol() {
        robot.removeOnRobotReadyListener(this);
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeOnCurrentPositionChangedListener(this);
    }

    private void goToNextPoint() {
        if (patrolPoints != null && !patrolPoints.isEmpty()) {
            String destination = patrolPoints.get(currentPointIndex);
            Log.d(TAG, "前往地點：" + destination);
            //goTo(位置,是否倒著前往,是否繞過障礙物,設置速度等級)
            robot.goTo(destination, false, null, SpeedLevel.SLOW);

        }
    }

    // 設定 Temi 機器人頭部傾斜角度
    public void tiltHead(int degrees, float speed) {
        if (robot != null) {
            if (degrees >= -25 && degrees <= 55 && speed >= 0 && speed <= 1) {
                robot.tiltAngle(degrees, speed); // 使用 Temi SDK 的 tiltAngle 方法
            } else {
                // 如果參數無效，打印錯誤
                System.out.println("請設定有效的角度 (-25 ~ 55) 以及速度 (0 ~ 1)");
            }
        }
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            Log.d(TAG, "機器人已準備就緒。");
            robot.hideTopBar();
        }
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int id, @NotNull String desc) {
        Log.d(TAG, "地點：" + location + ", 狀態：" + status);
        if (status.equalsIgnoreCase("complete")) {
            // 如果到達的是充電桩，不執行定點動作，但繼續巡邏
            if ("home base".equalsIgnoreCase(location)) {
                Log.d(TAG, "機器人已到達充電桩，不執行動作，繼續巡邏至下一個地點。");
                // 前往下一個地點
                currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
                goToNextPoint();
                return;  // 跳過後面的動作執行
            }
            // 到達地點後暫停
            Log.d(TAG, "到達 " + location + "，執行定點動作...");

            // 暫停巡邏一段時間或等待執行動作
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 轉身並拍照的邏輯
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    int turnCount = 0;//設定轉動次數
                    @Override
                    public void run() {
                        if (turnCount < 8) {
                            turnAndCapture(45);  // 每次轉 45 度並拍照
                            turnCount++;
                            new Handler(Looper.getMainLooper()).postDelayed(this, 1000);  // 每次延遲2秒再繼續
                        } else {
                            Log.d(TAG, "動作執行完成，繼續巡邏");

                            if (!patrolPoints.isEmpty()) {
                                currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
                                goToNextPoint();
                            }
                        }
                    }
                });
            }, 10000); // 停留10秒，根據需要調整
        } else if (status.equalsIgnoreCase("abort")) {
            // 如果導航失敗，停止巡邏
            isPatrolling = false;
            Log.d(TAG, "導航中止，停止巡邏。");
        }
    }



    @Override
    public void onCurrentPositionChanged(Position position) {
        float x = position.getX();
        float y = position.getY();
        Log.d(TAG, "當前位置：x=" + x + ", y=" + y);
    }

    private void turnAndCapture(int degrees) {
        // 使用turnBy 方法
        robot.turnBy(degrees, 1.0f);
        cameraXHelper.capturePhoto();
    }
}
