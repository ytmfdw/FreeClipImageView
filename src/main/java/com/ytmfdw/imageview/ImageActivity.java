package com.ytmfdw.imageview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ImageActivity extends AppCompatActivity {

    MyImageView miv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        miv = (MyImageView) findViewById(R.id.miv);
        miv.setDoubleClickListener(new MyImageView.OnDoubleClickListener() {
            @Override
            public void getBitmap(Bitmap bitmap) {
                Intent intent = getIntent();
                //注意：bitmap的大小不能超过40kb，否则传递失败，请用Application来保存
//                intent.putExtra("bitmap", bitmap);
                App.getApp().setBitmap(bitmap);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
