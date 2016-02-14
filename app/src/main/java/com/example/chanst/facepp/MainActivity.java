package com.example.chanst.facepp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_CODE = 1;
    private ImageView mPhoto;
    private Button mGetImage,mDetect;
    private TextView mTip;
    private View mWaitting;
    private Bitmap mPhotoImage;
    private String mCurrentPhotoStr;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvent();
        mPaint = new Paint();
    }

    private void initEvent() {
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);

    }

    private void initViews() {
        mPhoto = (ImageView) findViewById(R.id.id_photo);
        mGetImage = (Button) findViewById(R.id.id_getImage);
        mDetect = (Button) findViewById(R.id.id_detect);
        mTip = (TextView) findViewById(R.id.id_tip);
        mWaitting = findViewById(R.id.id_waiting);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.id_getImage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PICK_CODE);
                break;
            case R.id.id_detect:
                mWaitting.setVisibility(View.VISIBLE);
                FaceppDetect.detect(mPhotoImage, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getErrorMessage();
                        mHandler.sendMessage(msg);
                    }
                });
                break;

        }
    }
    private static final int  MSG_SUCCESS = 0x111;
    private static final int  MSG_ERROR = 0x112;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errorMessage = (String) msg.obj;
                    if(TextUtils.isEmpty(errorMessage)){
                        mTip.setText("Error");
                    }else{
                        mTip.setText(errorMessage);
                    }
                    break;
                case MSG_SUCCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject rs= (JSONObject) msg.obj;
                    prepareRsBitmap(rs);//绘制脸框图
                    mPhoto.setImageBitmap(mPhotoImage);
                    break;
            }
        }
    };

    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImage.getWidth(),mPhotoImage.getHeight(),mPhotoImage.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImage,0,0,null);
        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("Find "+faceCount);
            for(int i = 0 ; i < faceCount; i++){
                JSONObject face = faces.getJSONObject(i);
                JSONObject position = face.getJSONObject("position");

                float x = (float) position.getJSONObject("center").getDouble("x");
                float y = (float) position.getJSONObject("center").getDouble("y");

                float w  = (float) position.getDouble("width");
                float h  = (float) position.getDouble("height");
                //换算脸在图片的具体位置
                x = x/100*bitmap.getWidth();
                y = y/100*bitmap.getHeight();
                w = w/100*bitmap.getWidth();
                h = h/100*bitmap.getHeight();
                //画框
                mPaint.setColor(Color.BLUE);
                mPaint.setStrokeWidth(3);
                canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);
                mPhotoImage = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_CODE){
            if(data!=null){
                Uri uri = data.getData();
                Cursor cur =getContentResolver().query(uri, null, null, null, null);
                cur.moveToFirst();
                int idx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cur.getString(idx);
                cur.close();
                resizePhoto();
                mPhoto.setImageBitmap(mPhotoImage);
                mTip.setText("Click Detect ==>");
            }
        }
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=true;//只获取尺寸

        BitmapFactory.decodeFile(mCurrentPhotoStr,options);//options 里面存放图片高度宽度
        double ratio = Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);
        options.inSampleSize= (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhotoImage = BitmapFactory.decodeFile(mCurrentPhotoStr, options);//压缩完图片
    }
}
