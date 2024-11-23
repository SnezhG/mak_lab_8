package com.example.mak_lab_8;

import android.Manifest;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;
    Button captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        sv = findViewById(R.id.surfaceView);
        captureButton = findViewById(R.id.captureButton);

        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);

        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        captureButton.setOnClickListener(v -> {
            if (camera != null) {
                camera.takePicture(null, null, (data, cam) -> {
                    savePicture(data);
                    camera.startPreview();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            camera = Camera.open(CAMERA_ID);
        }
        setPreviewSize();
        setCameraDisplayOrientation();
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка инициализации камеры", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera.stopPreview();
            setCameraDisplayOrientation();
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        }
    }

    void setPreviewSize() {
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        Camera.Size size = camera.getParameters().getPreviewSize();
        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());
        if (widthIsMax) {
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();

        if (!FULL_SCREEN) {
            matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START);
        } else {
            matrix.setRectToRect(rectDisplay, rectPreview, Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        matrix.mapRect(rectPreview);

        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation() {
        camera.setDisplayOrientation(getCameraRotation());
    }

    void savePicture(byte[] data) {
        String fileName = "IMG_" + new Date().getTime() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            try {
                try (OutputStream outputStream = getContentResolver().openOutputStream(imageUri)) {
                    if (outputStream != null) {

                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                        int rotation = getCameraRotation();

                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        rotatedBitmap.recycle();

                        Toast.makeText(this, "Изображение сохранено!", Toast.LENGTH_SHORT).show();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка сохранения снимка", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ошибка создания снимка", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCameraRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation) % 360;
        } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = ((360 - degrees) - info.orientation + 360) % 360;
        }

        return result;
    }


}
