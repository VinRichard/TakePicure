该项目是通过ImageSelector-master修改过来的项目.使用方法还是还以前的一样还么来及修改,在里面加了Android6.0权限判断.
    当前编译版本是:    
        compileSdkVersion 26
         minSdkVersion 14
        targetSdkVersion 26
        
     gradle版本:
        classpath 'com.android.tools.build:gradle:3.0.0'
        gradle-4.1-all.zip

使用方法如下:

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
                .mutiSelectMaxSize(9)
                // 已选择的图片路径
                .pathList(path)
                // 拍照后存放的图片路径（默认 /temp/picture）
                .filePath("/ImageSelector/Pictures")
                // 开启拍照功能 （默认开启）
                .requestCode(REQUEST_CODE);

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
