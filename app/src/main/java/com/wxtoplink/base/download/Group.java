package com.wxtoplink.base.download;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 12852 on 2019/4/25.
 */

public abstract class Group<T> extends CollectionListener<T> {

    private int groupId ;

    private String groupName ;

    private Object tag ;

    private List<T> waitTasks , successTasks , errorTasks ;

    private CollectionListener<Group> parent ;

    private GroupBuilder.GroupListener groupListener ;

    public GroupBuilder.GroupListener getGroupListener() {
        return groupListener;
    }

    public int getGroupId() {
        return groupId;
    }

    public List<T> getWaitTasks() {
        return waitTasks;
    }

    public List<T> getSuccessTasks() {
        return successTasks;
    }

    public List<T> getErrorTasks() {
        return errorTasks;
    }

    public String getGroupName() {
        return groupName;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    Group(int groupId, List<T> waitTasks , String groupName) {
        this.groupId = groupId;
        this.waitTasks = waitTasks;
        this.successTasks = new ArrayList<T>();
        this.errorTasks = new ArrayList<T>();
        this.groupName = groupName ;
    }


    void notifyStart(){
        if(groupListener != null) {
            groupListener.onStart(countTotalTaskSize());
        }
    }

    void notifyFinish(){
        if(groupListener != null){
            groupListener.onFinish();
        }
    }

    void notifyParent(){
        if(this.parent != null && this.getWaitTasks().size() <= 0){
            if(this.getErrorTasks().size() == 0){
                this.parent.downloadSuccess(this);
            }else{
                this.parent.downloadError(this);
            }
        }
    }


    //添加下载任务到任务组
    public boolean add(T obj){
        if(getTClass().equals(Group.class)){
            ((Group)obj).parent = this;
        }
        return waitTasks.add(obj);
    }

    public void setGroupListener(GroupBuilder.GroupListener groupListener) {
        this.groupListener = groupListener;
    }

    public int countTotalTaskSize(){
        return waitTaskSize() + errorTaskSize() + successTaskSize() ;
    }

    public int waitTaskSize(){
        return countTaskSize(GroupBuilder.ListType.wait);
    }

    public int errorTaskSize(){
        return countTaskSize(GroupBuilder.ListType.error) ;
    }

    public int successTaskSize(){
        return countTaskSize(GroupBuilder.ListType.success) ;
    }

    //计算大小
    private int countTaskSize(int type){
        List<T> list = getList(type);
        int count = 0 ;
        if(list == null || list.isEmpty()){
            return 0 ;
        }else if(getTClass().equals(DownloadTask.class)){//列表中的子项为DownloadTask
            //返回列表大小
            return list.size();
        }else if(getTClass().equals(Group.class)){
            for(Group childGroup : (List<Group>)list){
                count += childGroup.countTaskSize(type);
            }
        }
        return count ;
    }

    /**
     * 根据类型获取对应的列表
     * @param type 列表类型(GroupBuilder.ListType)
     * @return 对应资源列表
     */
    private List<T> getList(int type){
        if(type == GroupBuilder.ListType.success){
            return successTasks;
        }else if(type == GroupBuilder.ListType.wait){
            return waitTasks;
        }else if(type == GroupBuilder.ListType.error){
            return errorTasks ;
        }else{
            return new ArrayList<>();
        }
    }

    @Override
    void downloadSuccess(T obj) {
        this.getWaitTasks().remove(obj);
        this.getSuccessTasks().add(obj);
        if(this.groupListener != null) {
            this.groupListener.waitTaskSize(this.waitTaskSize());
            if (this.getWaitTasks().size() <= 0 ) {
                this.groupListener.onFinish();
            }
        }
        notifyParent();
    }

    @Override
    void downloadError(T obj) {
        this.getWaitTasks().remove(obj);
        this.getErrorTasks().add(obj);
        if(this.groupListener != null) {
            this.groupListener.waitTaskSize(this.waitTaskSize());
            if (this.getWaitTasks().size() <= 0) {
                this.groupListener.onFinish();
            }
        }
        notifyParent();
    }
}
