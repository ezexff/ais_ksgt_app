package com.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;

public class StaticMethods {

    private final static String TAG = "T_StaticMethods";

    // Преобразовать число в boolean (для хранения логического типа в локальной БД SQLite используется число)
    public static boolean intToBoolean(int value){
        return (value > 0);
    }

    // Всплывающее сообщение (с выравниванием)
    public static void showToast(Context _context, String string){
        Toast toast = Toast.makeText(_context, string, Toast.LENGTH_SHORT);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if( v != null) v.setGravity(Gravity.CENTER);
        toast.show();
    }

    // Проверка разрешений на доступ к камере и хранилищу
    public static boolean verifyPermissions(Activity activity) {

        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int storage_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Разрешения предоставлены
        if (camera_permission == PackageManager.PERMISSION_GRANTED && storage_permission == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        String[] PERMISSIONS_CAMERA_AND_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};

        // Предложить пользователю принять разрешения
        ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA_AND_STORAGE, 1);

        return false;
    }

    // Перевод картинки в byte[]
    public static byte[] bitmapCompressAndToByte(Bitmap userImage, int quality) {

        Log.d(TAG, "Рамзер до сжатия: " + DebugGetBitmapLength(userImage));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        userImage.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] Result = baos.toByteArray();
        Log.d(TAG, "Рамзер после сжатия: " + Result.length);

        return Result;
    }


    // Уменьшение размера картинки
    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        Log.d(TAG, "Уменьшение изображения (width: " + width + "px | height: " + height + "px) по ширине до " + maxSize + "px с сохранением пропорций");

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    // Размер картинки
    public static int DebugGetBitmapLength(Bitmap bitmap){
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bao);
        byte[] ba = bao.toByteArray();
        int length = ba.length;
        return length;
    }
}
