package com.mahmoudjoe3.Hchat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageCompressor {
    public static String encode_Image_To_String(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //compress bitmap to stream
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        //convert stream to byte array
        byte[] bytes = stream.toByteArray();
        //decode byte array to Base64
        return Base64.encodeToString(bytes, Base64.DEFAULT);

    }

    public static Bitmap decode_String_To_Image(String code) {
        //convert Base64 to byte array
        byte[] bytes = Base64.decode(code, Base64.DEFAULT);
        //convert byte array to bitmap

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    //                new Handler().postDelayed(new Runnable() {
//                    public void run() {
//                        // do something...
//                    }
//                }, 1000);

}
