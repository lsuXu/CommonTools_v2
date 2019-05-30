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

    public Group<DownloadTask> buildTask(){
        return buildTask("");
    }

    public Group<DownloadTask> buildTask(String groupName){
        return new Group<DownloadTask>(groupId++, new ArrayList<DownloadTask>(),groupName){

        };
    }

    public Group<Group> buildGroup(){
        return buildGroup("");
    }

    public Group<Group> buildGroup(String groupName){
        return new Group<Group>(groupId ++ , new ArrayList<Group>() , groupName) {

        };
    }

    interface ListType {
        int success = 0 ;
        int wait = 1;
        int error = 2 ;
    }

    public interface GroupListener{

        void onStart(int totalSize);

        //队列剩余大小
        void waitTaskSize(int size);

        void onFinish();
    }

}
