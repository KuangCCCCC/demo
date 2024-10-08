package com.example.temilib;

import android.util.Log;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.List;

public class PatrolHelper implements
        OnRobotReadyListener,
        OnGoToLocationStatusChangedListener,
        OnCurrentPositionChangedListener {

    private static final String TAG = "PatrolHelper";
    private Robot robot;
    private List<String> patrolPoints;
    private int currentPointIndex = 0;
    private boolean isPatrolling = false;

    public PatrolHelper() {
        this.robot = Robot.getInstance();
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
            robot.goTo(destination);
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
    public void onGoToLocationStatusChanged(String location, String status, int id, String desc) {
        Log.d(TAG, "地點：" + location + ", 狀態：" + status);
        if (status.equalsIgnoreCase("complete")) {
            currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
            goToNextPoint();
        } else if (status.equalsIgnoreCase("abort")) {
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
}
