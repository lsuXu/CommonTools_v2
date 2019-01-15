package com.wxtoplink.base.camera;

import android.content.Context;

import com.wxtoplink.base.camera.impl.NoViewPreviewCamera;
import com.wxtoplink.base.camera.impl.CameraTakePhotoImpl;
import com.wxtoplink.base.camera.impl.ViewPreviewCamera;
import com.wxtoplink.base.camera.interfaces.CameraTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * 相机工具类的入口，同步不同功能相机的使用，以及正确的资源释放
 * Created by 12852 on 2018/8/29.
 */

public class CameraComponent {

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
        safeRelease();
        CameraTemplate cameraTemplate = null;
        if(cameraTemplateMap.containsKey(cameraType.getTypeName())){
            cameraTemplate =  cameraTemplateMap.get(cameraType.getTypeName());
            currentCameraType = cameraType ;//标记当前使用的相机类型
        }else{
            try {
                Constructor constructor = cameraType.getTargetClass().getConstructor(Context.class);
                if(constructor != null) {
                    cameraTemplate = (CameraTemplate) constructor.newInstance(context);
                    if (cameraTemplate != null) {
                        cameraTemplateMap.put(cameraType.getTypeName(), cameraTemplate);
                        currentCameraType = cameraType ;//标记当前使用的相机类型
                    }
                }
            }catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return cameraTemplate;
    }

    //释放相机资源
    public synchronized void safeRelease(){
        for(CameraTemplate cameraTemplate : cameraTemplateMap.values()){
            cameraTemplate.stopPreview();
        }
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
        private CameraType(Class c,String typeName){
            this.targetClass = c ;
            this.typeName = typeName ;
        }

        public String getTypeName() {
            return typeName;
        }
    }
}
