package com.fengzi.picurelibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentContainer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;


import com.fengzi.picurelibrary.adapter.FolderAdapter;
import com.fengzi.picurelibrary.adapter.ImageAdapter;
import com.fengzi.picurelibrary.bean.Folder;
import com.fengzi.picurelibrary.bean.Image;
import com.fengzi.picurelibrary.utils.FileUtils;
import com.fengzi.picurelibrary.utils.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageSelectorFragment
 * Created by Yancy on 2015/12/2.
 */
public class ImageSelectorFragment extends Fragment {

    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;
    private static final int REQUEST_CAMERA = 100;

    final int REQUEST_PERMISS_PHOTO = 0x14;
    final int REQUEST_PERMISS_CAMER = 0x15;

    private ArrayList<String> resultList = new ArrayList<>();
    private List<Folder> folderList = new ArrayList<>();

    private List<Image> imageList = new ArrayList<>();

    private Callback callback;

    private ImageAdapter imageAdapter;
    private FolderAdapter folderAdapter;

    private ListPopupWindow folderPopupWindow;

    private TextView time_text;
    private TextView category_button;
    private View popupAnchorView;
    private GridView grid_image;


    private int gridWidth, gridHeight;

    private boolean hasFolderGened = false;

    private File tempFile;

    private Context context;

    private ImageConfig imageConfig;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("The Activity must implement ImageSelectorFragment.Callback interface...");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.imageselector_main_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestPermission(REQUEST_PERMISS_PHOTO);
    }

    boolean isHavePermiision(String permiss){
        if (Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(getContext(),permiss) == PackageManager.PERMISSION_GRANTED)
            return false;
        }
        return true;
    }

    void requestPermission(int requestCode) {
        String pess = requestCode == REQUEST_PERMISS_PHOTO ? Manifest.permission.READ_EXTERNAL_STORAGE : Manifest.permission.CAMERA;
        if (isHavePermiision(pess)) {
            requestPermissions( new String[]{pess}, requestCode);
        } else {
            if (requestCode == REQUEST_PERMISS_PHOTO) {
                getImages();
            } else {
                openCamer();
            }
        }
    }

    void getImages(){
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getActivity();

        time_text = (TextView) view.findViewById(R.id.time_text);
        category_button = (TextView) view.findViewById(R.id.category_button);
        grid_image = (GridView) view.findViewById(R.id.grid_image);
        popupAnchorView = view.findViewById(R.id.footer_layout);

        time_text.setVisibility(View.GONE);

        init();
    }

    private void init() {
        imageConfig = ImageSelector.getImageConfig();

        folderAdapter = new FolderAdapter(context, imageConfig);

        imageAdapter = new ImageAdapter(context, imageList, imageConfig);

        imageAdapter.setShowCamera(imageConfig.isShowCamera());
        imageAdapter.setShowSelectIndicator(imageConfig.isMutiSelect());
        grid_image.setAdapter(imageAdapter);

        resultList = imageConfig.getPathList();

        category_button.setText(R.string.all_folder);
        category_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (folderPopupWindow == null) {
                    createPopupFolderList(gridWidth, gridHeight);
                }

                if (folderPopupWindow.isShowing()) {
                    folderPopupWindow.dismiss();
                } else {
                    folderPopupWindow.show();
                    int index = folderAdapter.getSelectIndex();
                    index = index == 0 ? index : index - 1;
                    folderPopupWindow.getListView().setSelection(index);
                }
            }
        });

        grid_image.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    time_text.setVisibility(View.GONE);
                } else if (scrollState == SCROLL_STATE_FLING) {
                    time_text.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (time_text.getVisibility() == View.VISIBLE) {
                    int index = firstVisibleItem + 1 == view.getAdapter().getCount() ? view.getAdapter().getCount() - 1 : firstVisibleItem + 1;
                    Image image = (Image) view.getAdapter().getItem(index);
                    if (image != null) {
                        time_text.setText(TimeUtils.formatPhotoDate(image.path));
                    }
                }
            }
        });

        grid_image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int width = grid_image.getWidth();
                final int height = grid_image.getHeight();

                gridWidth = width;
                gridHeight = height;

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                final int numCount = width / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (width - columnSpace * (numCount - 1)) / numCount;
                imageAdapter.setItemSize(columnWidth);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    grid_image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    grid_image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        grid_image.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (imageAdapter.isShowCamera()) {
                    if (i == 0) {
                        showCameraAction();
                    } else {
                        Image image = (Image) adapterView.getAdapter().getItem(i);
                        selectImageFromGrid(image, imageConfig.isMutiSelect());
                    }
                } else {
                    // 正常操作
                    Image image = (Image) adapterView.getAdapter().getItem(i);
                    selectImageFromGrid(image, imageConfig.isMutiSelect());
                }
            }
        });
    }

    /**
     * 创建弹出的ListView
     */
    private void createPopupFolderList(int width, int height) {
        folderPopupWindow = new ListPopupWindow(getActivity());
        folderPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        folderPopupWindow.setAdapter(folderAdapter);
        folderPopupWindow.setContentWidth(width);
        folderPopupWindow.setWidth(width);
        folderPopupWindow.setHeight(height * 5 / 8);
        folderPopupWindow.setAnchorView(popupAnchorView);
        folderPopupWindow.setModal(true);
        folderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                folderAdapter.setSelectIndex(i);

                final int index = i;
                final AdapterView v = adapterView;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        folderPopupWindow.dismiss();

                        if (index == 0) {
                            getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
                            category_button.setText(R.string.all_folder);
                            if (imageConfig.isShowCamera()) {
                                imageAdapter.setShowCamera(true);
                            } else {
                                imageAdapter.setShowCamera(false);
                            }
                        } else {
                            Folder folder = (Folder) v.getAdapter().getItem(index);
                            if (null != folder) {
                                imageList.clear();
                                imageList.addAll(folder.images);
                                imageAdapter.notifyDataSetChanged();

                                category_button.setText(folder.name);
                                // 设定默认选择
                                if (resultList != null && resultList.size() > 0) {
                                    imageAdapter.setDefaultSelected(resultList);
                                }
                            }
                            imageAdapter.setShowCamera(false);
                        }

                        // 滑动到最初始位置
                        grid_image.smoothScrollToPosition(0);
                    }
                }, 100);

            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (folderPopupWindow != null) {
            if (folderPopupWindow.isShowing()) {
                folderPopupWindow.dismiss();
            }
        }

        grid_image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int height = grid_image.getHeight();

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                final int numCount = grid_image.getWidth() / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (grid_image.getWidth() - columnSpace * (numCount - 1)) / numCount;
                imageAdapter.setItemSize(columnWidth);

                if (folderPopupWindow != null) {
                    folderPopupWindow.setHeight(height * 5 / 8);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    grid_image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    grid_image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 选择相机
     */
    private void showCameraAction() {

       requestPermission(REQUEST_PERMISS_CAMER);
    }

    void openCamer(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            tempFile = FileUtils.createTmpFile(getContext(), imageConfig.getFilePath());
            Uri imageUri = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                imageUri = Uri.fromFile(tempFile);
            } else {
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                imageUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", tempFile);
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
             startActivityForResult(cameraIntent, REQUEST_CAMERA);

        } else {
            Toast.makeText(context, R.string.msg_no_camera, Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImageFromGrid(Image image, boolean isMulti) {
        if (image != null) {
            if (isMulti) {
                if (resultList.contains(image.path)) {
                    resultList.remove(image.path);
                    if (callback != null) {
                        callback.onImageUnselected(image.path);
                    }
                } else {
                    if (imageConfig.getMaxSize() == resultList.size()) {
                        Toast.makeText(context, R.string.msg_amount_limit, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    resultList.add(image.path);
                    if (callback != null) {
                        callback.onImageSelected(image.path);
                    }
                }
                imageAdapter.select(image);
            } else {
                if (callback != null) {
                    callback.onSingleImageSelected(image.path);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                if (tempFile != null) {
                    if (callback != null) {
                        callback.onCameraShot(tempFile);
                    }
                }
            } else {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISS_PHOTO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImages();
                } else {
                    boolean b = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (!b) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.storage_permission_no_use)
                                .setMessage(R.string.go_system_open_storage_permission)
                                .setPositiveButton(R.string.now_open, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 123);
                                    }
                                }).setNegativeButton(R.string.cancle, null).setCancelable(false).show();
                    } else {
                        toast(getString(R.string.refuse_permisson_affect_use));
                        getActivity().finish();
                    }
                }
                break;

            case REQUEST_PERMISS_CAMER:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamer();
                } else {
                    boolean b = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
                    if (!b) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.camer_permission_no_use)
                                .setMessage(R.string.go_system_open_camer_permission)
                                .setPositiveButton(R.string.now_open, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 123);
                                    }
                                }).setNegativeButton(R.string.cancle, null).setCancelable(false).show();
                    } else {
                        toast(getString(R.string.refuse_permisson_affect_use));
                    }
                }

                break;
        }

    }

    void toast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID};

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ALL) {
                CursorLoader cursorLoader =
                        new CursorLoader(getActivity(),
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                                null, null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            } else if (id == LOADER_CATEGORY) {
                CursorLoader cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[0] + " like '%" + args.getString("path") + "%'", null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                int count = data.getCount();
                if (count > 0) {
                    List<Image> tempImageList = new ArrayList<>();
                    data.moveToFirst();
                    do {
                        String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        String name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        long dateTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                        Image image = new Image(path, name, dateTime);
                        tempImageList.add(image);
                        if (!hasFolderGened) {
                            File imageFile = new File(path);
                            File folderFile = imageFile.getParentFile();
                            Folder folder = new Folder();
                            folder.name = folderFile.getName();
                            folder.path = folderFile.getAbsolutePath();
                            folder.cover = image;
                            if (!folderList.contains(folder)) {
                                List<Image> imageList = new ArrayList<>();
                                imageList.add(image);
                                folder.images = imageList;
                                folderList.add(folder);
                            } else {
                                Folder f = folderList.get(folderList.indexOf(folder));
                                f.images.add(image);
                            }
                        }

                    } while (data.moveToNext());

                    imageList.clear();
                    imageList.addAll(tempImageList);
                    imageAdapter.notifyDataSetChanged();

                    if (resultList != null && resultList.size() > 0) {
                        imageAdapter.setDefaultSelected(resultList);
                    }

                    folderAdapter.setData(folderList);

                    hasFolderGened = true;

                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };


    public interface Callback {
        void onSingleImageSelected(String path);

        void onImageSelected(String path);

        void onImageUnselected(String path);

        void onCameraShot(File imageFile);
    }

}