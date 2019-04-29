package com.wxtoplink.base.download;

import java.util.ArrayList;

/**
 * 任务组的构造器，主要用于序列化任务组
 * Created by 12852 on 2019/4/25.
 */

public class GroupBuilder {

    private static GroupBuilder instance ;

    private volatile int groupId ;

    private GroupBuilder(){
        groupId = 1;
    };

    public static GroupBuilder getInstance(){
        if(instance == null){
            synchronized (GroupBuilder.class){
                if(instance == null)
                    instance = new GroupBuilder();
            }
        }
        return instance ;
    }

    public Group build(Group.GroupListener groupListener){
        return new Group(groupId++, new ArrayList<DownloadTask>() , groupListener);
    }
}
