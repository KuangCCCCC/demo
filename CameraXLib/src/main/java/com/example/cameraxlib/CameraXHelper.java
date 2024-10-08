package com.example.cameraxlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXHelper {
    private Context context;
    private ProcessCameraProvider cameraProvider; // 相機處理提供者
    private ImageCapture imageCapture; // 圖像捕捉功能

    public CameraXHelper(Context context) {
        this.context = context;
    }

    // 啟動相機
    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get(); // 獲取相機提供者
                bindCameraUseCases(); // 綁定相機使用案例
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace(); // 錯誤處理
            }
        }, ContextCompat.getMainExecutor(context)); // 使用主執行緒
    }

    // 綁定相機使用案例
    private void bindCameraUseCases() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK) // 使用後鏡頭
                .build();

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1920, 1080)) // 設定捕捉圖像的解析度
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 最小化延遲
                .build();

        cameraProvider.unbindAll(); // 解除綁定所有相機使用案例
        cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageCapture); // 綁定到生命週期
        Log.d("CameraXHelper", "相機已啟動並綁定");
    }

    // 停止相機
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll(); // 停止相機，釋放資源
            Log.d("CameraXHelper", "相機已停止");
        }
    }

    // 釋放相機和其他資源
    public void releaseResources() {
        stopCamera(); // 停止相機
    }

    // 拍照並上傳到 Firebase
    public void capturePhoto() {
        if (imageCapture != null) {
            Log.d("CameraXHelper", "正在進行拍照...");
            imageCapture.takePicture(ContextCompat.getMainExecutor(context), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    Log.d("CameraXHelper", "拍照成功，開始處理圖片");
                    Bitmap bitmap = imageProxyToBitmap(image); // 將 ImageProxy 轉為 Bitmap
                    String fileName = generateImageName();  // 使用自訂命名方法
                    uploadImageToFirebase(bitmap, fileName);  // 上傳圖片
                    image.close(); // 關閉 ImageProxy 以釋放資源
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("CameraXHelper", "拍照失敗: " + exception.getMessage()); // 錯誤處理
                }
            });
        }
    }

    // 自訂命名方法
    public String generateImageName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "IMG_" + sdf.format(new Date()) + ".jpg";  // 生成類似 "IMG_20241004_112030.jpg" 這樣的名稱
    }

    // 將 ImageProxy 轉為 Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer(); // 獲取圖像數據
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes); // 將數據讀入 byte 陣列
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length); // 解碼為 Bitmap
    }

    // 上傳圖片到 Firebase
    private void uploadImageToFirebase(Bitmap bitmap, String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance(); // 獲取 Firebase 存儲實例
        StorageReference storageRef = storage.getReference().child(fileName); // 獲取儲存參考

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // 將 Bitmap 壓縮為 JPEG 格式
        byte[] data = baos.toByteArray(); // 轉換為 byte 陣列

        UploadTask uploadTask = storageRef.putBytes(data); // 上傳數據
        uploadTask.addOnSuccessListener(taskSnapshot -> Log.d("CameraXHelper", "圖片上傳成功")) // 成功回調
                .addOnFailureListener(e -> Log.e("CameraXHelper", "圖片上傳失敗: " + e.getMessage())); // 失敗回調
    }
}


