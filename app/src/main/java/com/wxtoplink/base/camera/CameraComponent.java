package com.wxtoplink.base.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;

import com.wxtoplink.base.camera.impl.NoViewPreviewCamera;
import com.wxtoplink.base.camera.impl.CameraTakePhotoImpl;
import com.wxtoplink.base.camera.impl.ViewPreviewCamera;
import com.wxtoplink.base.camera.interfaces.CameraTemplate;
import com.wxtoplink.base.camera.utils.CameraUtils;
import com.wxtoplink.base.log.CameraLog;
import com.wxtoplink.base.log.LogOperate;
import com.wxtoplink.base.log.LogOutput;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;


/**
 * 相机工具类的入口，同步不同功能相机的使用，以及正确的资源释放
 * Created by 12852 on 2018/8/29.
 */

public class CameraComponent implements LogOutput{

    private static final String TAG = CameraComponent.class.getSimpleName();

    private Map<String,CameraTemplate> cameraTemplateMap ;

    private CameraComponent(){
        cameraTemplateMap = new HashMap<>();
    }

    private CameraType currentCameraType ;

    public static CameraComponent getInstance(){
        return CameraHolder.instance ;
    }

    //设置相机用途，使用相机组件，需要第一个被调用来初始化相机
    public synchronized CameraTemplate getCamera(CameraType cameraType, Context context){
        CameraLog.getInstance().i(TAG,String.format("call getCamera('%s')",cameraType.getTypeName()));
        try {
            if(enableCamera(context)) {
                stopAllPreview();//停止所有预览
                CameraTemplate cameraTemplate = null;
                if (cameraTemplateMap.containsKey(cameraType.getTypeName())) {
                    cameraTemplate = cameraTemplateMap.get(cameraType.getTypeName());
                    currentCameraType = cameraType;//标记当前使用的相机类型
                } else {
                    //获取构造器
                    Constructor constructor = cameraType.getTargetClass().getConstructor(Context.class);
                    if (constructor != null) {
                        //创建实例
                        cameraTemplate = (CameraTemplate) constructor.newInstance(context);
                        if (cameraTemplate != null) {
                            //保存实例
                            cameraTemplateMap.put(cameraType.getTypeName(), cameraTemplate);
                            //标记当前使用的相机类型
                            currentCameraType = cameraType;
                        }
                    }
                }
                return cameraTemplate;
            }
        } catch (Exception e) {
            e.printStackTrace();
            CameraLog.getInstance().e(TAG,"getCamera() error",e);
        }

        return null ;
    }

    //检查相机是否可用
    public boolean enableCamera(Context context) throws CameraAccessException {

        //6.0版本，检查相机数量以及权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return CameraUtils.getCameraList(context).length >0
                    && context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ;
        }else {
            //小于6.0版本，检查是否大于5.0版本（camera2 API从5.0版本开始引入,当前未向下兼容），且相机可用数大于0
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && CameraUtils.getCameraList(context).length >0;
        }
    }

    //停止所有预览
    public synchronized void stopAllPreview(){
        for(CameraTemplate cameraTemplate : cameraTemplateMap.values()){
            cameraTemplate.stopPreview();
        }
    }

    //释放相机资源
    public synchronized void safeRelease(){
        stopAllPreview();//停止所有预览
        Camera2Holder.getInstance().release();
    }


    //获取当前正在使用的相机类型
    public CameraType getCurrentCameraType() {
        return currentCameraType;
    }

    //当前是否存在相机正在预览
    public boolean isPreview(){
        boolean isPreview = false ;
        for(CameraTemplate cameraTemplate : cameraTemplateMap.values()){
            isPreview = isPreview || cameraTemplate.isPreview();
        }
        return isPreview ;
    }

    //设置日志输出
    @Override
    public void setLogOutput(LogOperate operate, boolean printf) {
        CameraLog.getInstance().setPrintf(printf);
        CameraLog.getInstance().setLogOperate(operate);
    }


    private static final class CameraHolder{
        private static final CameraComponent instance = new CameraComponent();
    }

    public enum CameraType{

        NORMAL(CameraTakePhotoImpl.class,"normal"),//预览并且拍照
        VIEW_PREVIEW_DATA(ViewPreviewCamera.class,"viewPreviewData"),//界面预览，实时获取数据，提供最大分辨率拍照
        NO_VIEW_PREVIEW_DATA(NoViewPreviewCamera.class,"noViewPreviewData");//无界面预览，实时获取数据

        private Class targetClass ;//类型对应的实现类
        private String typeName ;

        public Class getTargetClass(){
            return targetClass ;
        }
        CameraType(Class c,String typeName){
            this.targetClass = c ;
            this.typeName = typeName ;
        }

        public String getTypeName() {
            return typeName;
        }
    }
}
