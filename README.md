#该项目是多图选择,带有拍照和图片裁剪功能

    当前编译版本是:
        compileSdkVersion 26
         minSdkVersion 14
        targetSdkVersion 26
        
    gradle版本:
        classpath 'com.android.tools.build:gradle:3.0.0'
        gradle-4.1-all.zip

#使用方法如下:  
         ImageConfig.Builder builder = new ImageConfig.Builder(
                new GlideLoader())
                .steepToolBarColor(getResources().getColor(R.color.blue))  // 如果在 4.4 以上，则修改状态栏颜色 （默认黑色）
                .titleBgColor(getResources().getColor(R.color.blue))      // 标题的背景颜色 （默认黑色）
                .titleSubmitTextColor(getResources().getColor(R.color.white))      // 提交按钮字体的颜色  （默认白色）
                .titleTextColor(getResources().getColor(R.color.white))                // 标题颜色 （默认白色）
                .mutiSelectMaxSize(9)                // 多选时的最大数量   （默认 9 张）
                .pathList(path)           // 已选择的图片路径
                .filePath("/ImageSelector/Pictures") // 拍照后存放的图片路径（默认 /temp/picture）
                .requestCode(REQUEST_CODE);            // 开启拍照功能 （默认开启）
                
        if (isSingle) {//单选
            builder.singleSelect();
        } else {//多选
            builder.mutiSelect();
        }
        
        if (cb_camer.isChecked()) {//显示相机
            builder.showCamera();
        }
        if (cb_crop.isChecked()) {//裁剪
            builder.crop();
        }

        ImageSelector.open(MainActivity.this, builder.build());   // 开启图片选择器
