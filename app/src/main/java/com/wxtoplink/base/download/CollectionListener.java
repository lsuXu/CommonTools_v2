package com.wxtoplink.base.download;

import java.lang.reflect.ParameterizedType;

/**
 * Created by 12852 on 2019/3/1.
 */

abstract class CollectionListener<T> {

    abstract void downloadSuccess(T obj);

    abstract void downloadError(T obj);

    //获取T 的类名称
    Class getTClass(){
        ParameterizedType type = (ParameterizedType)getClass().getGenericSuperclass();
        if(type.getActualTypeArguments().length > 0){
            return (Class) type.getActualTypeArguments()[0];
        }

        return Object.class;
    }

}
