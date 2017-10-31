package com.fengzi.takepicure;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.fengzi.picurelibrary.ImageConfig;
import com.fengzi.picurelibrary.ImageSelector;
import com.fengzi.picurelibrary.ImageSelectorActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 1000;
    EditText et_num;
    CheckBox cb_camer, cb_crop;
    boolean isSingle;
    private Adapter adapter;
    private ArrayList<String> path = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onlcik();
        RecyclerView recycler = findViewById(R.id.recycler);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recycler.setLayoutManager(gridLayoutManager);
        adapter = new Adapter(this, path);
        recycler.setAdapter(adapter);

    }

    void onlcik() {
        et_num = findViewById(R.id.editText);
        cb_camer = findViewById(R.id.checkBox);
        cb_crop = findViewById(R.id.checkBox2);
        findViewById(R.id.but).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isSingle = true;
                initPhoto();
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isSingle = false;
                initPhoto();
            }
        });
    }

    void initPhoto() {
//        if (ContextCompat.checkSelfPermission(MainActivity.this,
//                Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
//        } else {
            openPhoto();
//        }
    }

    void openPhoto() {
        int num = 9;
        if (!TextUtils.isEmpty(et_num.getText().toString())) {
            num = Integer.parseInt(et_num.getText().toString());
            num = num > 0 ? num : 1;
        }

        ImageConfig.Builder builder = new ImageConfig.Builder(
                new GlideLoader())
                // 如果在 4.4 以上，则修改状态栏颜色 （默认黑色）
                .steepToolBarColor(getResources().getColor(R.color.blue))
                // 标题的背景颜色 （默认黑色）
                .titleBgColor(getResources().getColor(R.color.blue))
                // 提交按钮字体的颜色  （默认白色）
                .titleSubmitTextColor(getResources().getColor(R.color.white))
                // 标题颜色 （默认白色）
                .titleTextColor(getResources().getColor(R.color.white))
                // 多选时的最大数量   （默认 9 张）
                .mutiSelectMaxSize(num)
                // 已选择的图片路径
                .pathList(path)
                // 拍照后存放的图片路径（默认 /temp/picture）
                .filePath("/ImageSelector/Pictures")
                // 开启拍照功能 （默认开启）
                .requestCode(REQUEST_CODE);

        if (isSingle) {
            builder.singleSelect();
        } else {
            builder.mutiSelect();
        }

        if (cb_camer.isChecked()) {
            builder.showCamera();
        }
        if (cb_crop.isChecked()) {
            builder.crop();
        }

        ImageSelector.open(MainActivity.this, builder.build());   // 开启图片选择器
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openPhoto();
                } else {
                    boolean b = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (!b) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("存储权限不可用")
                                .setMessage("请在-应用设置-权限-中，允许支付宝使用存储权限来保存用户数据")
                                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 123);
                                    }
                                }).setNegativeButton("取消", null).setCancelable(false).show();
                    } else {
                        toast("权限拒绝将影响应用正常使用");
                    }
                }
                return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> pathList = data.getStringArrayListExtra(ImageSelectorActivity.EXTRA_RESULT);

            for (String path : pathList) {
                Log.i("ImagePathList", path);
            }

            path.clear();
            path.addAll(pathList);
            adapter.notifyDataSetChanged();

        }
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
