package com.example.ffmpegvideorange2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PreviewFrameActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Bitmap> dataList = new ArrayList<>();
    private PreviewImageAdapter adapter;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_frame);

        recyclerView = findViewById(R.id.recycler_view);
        adapter = new PreviewImageAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        recyclerView.setAdapter(adapter);
        adapter.setData(dataList);

        decodecFrame();


    }



    private void decodecFrame(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                VideoUtils videoUtils = new VideoUtils();
                videoUtils.setOnGetFrameBitmapCallback(new VideoUtils.OnGetFrameBitmapCallback() {
                    @Override
                    public void onGetBitmap(final Bitmap bitmap) {
                        Log.e("kzg","**********************onGetBitmap:"+bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dataList.add(bitmap);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                });
                VideoUtils.getBitmapByUri(PreviewFrameActivity.this, Environment.getExternalStorageDirectory() + "/testVideo/101.mp4");
            }
        }).start();
    }

    private void decodecFrame2(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                VideoUtils2 videoUtils2 = new VideoUtils2(Environment.getExternalStorageDirectory() + "/testVideo/137.mp4",PreviewFrameActivity.this);
                videoUtils2.setOnGetFrameBitmapCallback(new VideoUtils2.OnGetFrameBitmapCallback() {
                    @Override
                    public void onGetBitmap(final Bitmap bitmap) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dataList.add(bitmap);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
                try {
                    videoUtils2.decodeFrame(1*1000*1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
