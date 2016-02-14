package com.example.chanst.facepp;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by chanst on 16-2-14.
 */
public class FaceppDetect {
    public interface  CallBack{
        void success(JSONObject result);
        void error(FaceppParseException exception);
    }
    public static  void detect(final Bitmap bm,final CallBack callback){
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //request
                    HttpRequests request = new HttpRequests(Constant.KEY,Constant.SECRET,true,true) ;
                    Bitmap bmSmall = Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    //图片二进制数组
                    byte[] arrays = stream.toByteArray();

                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject jsonObject = request.detectionDetect(parameters);
                    Log.e("TAG",jsonObject.toString());
                    if(callback!=null ){
                        callback.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if(callback!=null){
                        callback.equals(e);
                    }
                }
            }
        }).start();
    }
}
