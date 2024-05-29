package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
    private Camera mCamera;
    private Preview mPreview;
    private SurfaceView mPreSurfaceView;
    private SurfaceHolder mPreSurfaceHolder;
    private SurfaceView mPostSurfaceView;
    private SurfaceHolder mPostSurfaceHolder;
    private ImageAnalysis mImageAnalysis;
    private ExecutorService mCameraExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "[-->] onCreate");
        setContentView(R.layout.activity_main);

        mPreviewView = findViewById(R.id.previewView);
        mPreSurfaceView = findViewById(R.id.preSurfaceView);
        mPreSurfaceHolder = mPreSurfaceView.getHolder();
        mPostSurfaceView = findViewById(R.id.postSurfaceView);
        mPostSurfaceHolder = mPostSurfaceView.getHolder();
        HandlerThread ht = new HandlerThread("HandlerThread");
        ht.start();
        mHandler = new Handler(ht.getLooper());
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
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // TODO Action for every frame
            int format = image.getFormat();
            Log.i(TAG, "frame action called : getFormat()=" + format);
            Canvas canvas = mPreSurfaceHolder.lockCanvas();
            canvas.drawBitmap(image.toBitmap(), new Matrix(), null);
            mPreSurfaceHolder.unlockCanvasAndPost(canvas);
            image.close();
            Bitmap dest = Bitmap.createBitmap(mPreSurfaceView.getWidth(), mPreSurfaceView.getHeight(), Bitmap.Config.ARGB_8888);
            PixelCopy.request(mPreSurfaceView, dest, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int i) {
                    Log.i(TAG, "result code : " + i);
                    Canvas c = mPostSurfaceHolder.lockCanvas();
                    c.drawBitmap(dest, new Matrix(), null);
                    mPostSurfaceHolder.unlockCanvasAndPost(c);
                }
            }, mHandler);
        }
    }
}