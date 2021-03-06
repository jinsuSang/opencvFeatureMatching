package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.BRISK;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.FlannBasedMatcher;
import org.opencv.features2d.KAZE;
import org.opencv.features2d.ORB;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.spec.DESKeySpec;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "OcvTest1";
    private final int REQ_CODE_SELECT_IMAGE = 100;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV is not loaded!");
        } else {
            Log.d(TAG, "OpenCV is loaded!");
        }
    }

    private ImageView imageView1;
    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap bitmap3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // permission 확인
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(!hasPermissions(PERMISSIONS)){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        imageView1 = (ImageView)findViewById(R.id.imageView);
    }

    // destory할 때 bitmap recycle
    @Override
    protected void onDestroy() {
        bitmap1.recycle();
        bitmap1 = null;

        bitmap2.recycle();
        bitmap2 = null;

        bitmap3.recycle();
        bitmap3 = null;
        super.onDestroy();
    }

    public void onButton1Clicked(View view){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    imageView1.setImageResource(0);

                    ClipData clipData = data.getClipData();

                    String path1 = getImagePathFromURI(clipData.getItemAt(0).getUri());
                    String path2 = getImagePathFromURI(clipData.getItemAt(1).getUri());
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bitmap1 = BitmapFactory.decodeFile(path1, options);
                    bitmap2 = BitmapFactory.decodeFile(path2, options);

                    // detectEdge가 피처매칭 부분
                    if (bitmap1 != null) {
                        detectEdge();
                        imageView1.setImageBitmap(bitmap3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getImagePathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor == null){
            return contentUri.getPath();
        } else{
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String imgPath = cursor.getString(idx);
            cursor.close();
            return imgPath;
        }
    }

    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE"};

    private boolean hasPermissions(String[] permissions){
        int result;

        for (String perms : permissions){
            result = ContextCompat.checkSelfPermission(this, perms);

            if(result == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }

        return true;
    }

    public void detectEdge(){
        Mat src1 = new Mat();
        Mat src2 = new Mat();
        Utils.bitmapToMat(bitmap1, src1);
        Utils.bitmapToMat(bitmap2, src2);
        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();

        // detector 모음
////         ORB detector
         ORB detector1 = ORB.create();
         ORB detector2 = ORB.create();

        // KAZE detector
//        KAZE detector1 = KAZE.create();
//        KAZE detector2 = KAZE.create();

        // AKAZE detector

//        AKAZE detector1 = AKAZE.create();
//        AKAZE detector2 = AKAZE.create();
        // BRISK detector
//         BRISK detector1 = BRISK.create();
//         BRISK detector2 = BRISK.create();

        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        // keypoint와 description 생성
        detector1.detectAndCompute(src1, new Mat(), keyPoint1, descriptor1);
        detector2.detectAndCompute(src2, new Mat(), keyPoint2, descriptor2);

        // Matcher 종류 선택

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//         BFMatcher matcher = BFMatcher.create();
//         FlannBasedMatcher matcher = FlannBasedMatcher.create();

        MatOfDMatch matches = new MatOfDMatch();
        MatOfDMatch filteredMatches = new MatOfDMatch();
        matcher.match(descriptor1, descriptor2, matches);

        List<DMatch> matchesList = matches.toList();
        Double max_dist = 0.0;
        Double min_dist = 100.0;


        // 최소 최대 거리 확인
        for(int i = 0;i < matchesList.size(); i++){
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if ( dist > max_dist)
                max_dist = dist;
        }

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for(int i = 0;i < matchesList.size(); i++){
            // 중요!!
            // 얼마나 유사한 피처를 매칭시킬건지 설정
            // 숫자가 높을수록 피처간 정확도 상승 매칭 개수 감소, 낮을수록 피처간 정확도 감소 매칭 개수 상승
            if (matchesList.get(i).distance <= (3 * min_dist))
                good_matches.addLast(matchesList.get(i));
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);


        // 피처는 레드, 선은 그린
        Scalar RED = new Scalar(255,0,0);
        Scalar GREEN = new Scalar(0,255,0);

        Mat edge = new Mat();
        MatOfByte drawnMatches = new MatOfByte();

        // 두 이미지간 피처 매칭 그리기
        // Features2d.drawMatches(src1, keyPoint1, src2, keyPoint2, goodMatches, edge, GREEN, RED, drawnMatches, Features2d.DrawMatchesFlags_DRAW_RICH_KEYPOINTS);
        Features2d.drawMatches(src1, keyPoint1, src2, keyPoint2, goodMatches, edge, GREEN, RED, drawnMatches, Features2d.DrawMatchesFlags_DEFAULT);

        // 이미지별 키 포인트 개수와 매칭된 포인트 개수
        Log.d("RESULT", "keypoint1: " + keyPoint1.size() +", keypoint2: " + keyPoint2.size());
        Log.d("RESULT", "matches: " + good_matches.size());

        Utils.matToBitmap(src1, bitmap1);
        Utils.matToBitmap(src2, bitmap2);

        bitmap3 = Bitmap.createBitmap(edge.cols(), edge.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edge, bitmap3);

        src2.release();
        src1.release();
        edge.release();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
