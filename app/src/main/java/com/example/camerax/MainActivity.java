package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CameraX DEMO";
    private static final int REQUEST_CODE_FOR_PERMISSIONS = 1234;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    "android.permission.CAMERA",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            };

    private PreviewView mPreviewView;
    private TextView mTextView;
    private Camera mCamera;
    private Preview mPreview;
    private ImageAnalysis mImageAnalysis;
    private ExecutorService mCameraExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "[-->] onCreate");
        setContentView(R.layout.activity_main);

        mPreviewView = findViewById(R.id.previewView);
        mTextView = findViewById(R.id.textView);
        if (checkPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }
        Log.i(TAG, "[<--] onCreate");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startCamera();
    }

    private void startCamera() {
        Log.i(TAG, "[-->] startCamera");
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context context = this;
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    mPreview = new Preview.Builder().build();
                    mImageAnalysis = new ImageAnalysis.Builder().build();
                    mImageAnalysis.setAnalyzer(mCameraExecutor, new MyImageAnalyzer());
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

                    cameraProvider.unbindAll();
                    mCamera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, mPreview, mImageAnalysis);
                    mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
                } catch (Exception e) {
                    Log.e(TAG, "[startCamera] Use case binding failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
        Log.i(TAG, "[<--] startCamera");
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // TODO Action for every frame
            int format = image.getFormat();
            Log.i(TAG, "frame action called : getFormat()=" + format);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String tmp = System.currentTimeMillis() + "getFormat()=" + format;
                    mTextView.setText(tmp);
                }
            });
            image.close();
        }
    }
}