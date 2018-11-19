package com.angcyo.dingding.bean;

import android.text.TextUtils;
import com.angcyo.dingding.DingDingService;
import com.angcyo.uiview.less.RApplication;
import com.angcyo.uiview.less.utils.Root;
import com.orhanobut.hawk.Hawk;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/10/17
 */
public class ConfigBean {

    public static String lastTaskId = Hawk.get("lastTaskId", "");

    /**
     * register : 1
     */

    public int enable = 1;

    /**
     * 相同任务id, 只执行一次, 防止多次触发
     */
    public String taskId = "";

    /**
     * 需要执行的设备uuid, all 表示所有
     */
    public String device = "";

    /**
     * 需要忽略的设备, 用,号隔开
     */
    public String deviceIgnore = "";

    /**
     * 需要指定的任务
     */
    public int task = -1;

    /**
     * 是否需要忽略此次执行
     */
    public boolean isIgnore() {
        return deviceIgnore.toLowerCase().contains(Root.initImei().toLowerCase());
    }

    /**
     * 是否需要执行任务
     */
    public boolean needExe() {
        if (!TextUtils.equals(taskId, lastTaskId)) {
            if (isIgnore()) {
            } else {
                if (TextUtils.equals(device.toLowerCase(), "all") ||
                        device.toLowerCase().contains(Root.initImei().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void save() {
        Hawk.put("lastTaskId", taskId);
        lastTaskId = taskId;
    }

    public void runTask() {
        if (task < 0) {
            return;
        }

        if (!needExe()) {
            return;
        }

        if (task == DingDingService.TASK_SHARE_SHOT ||
                task == DingDingService.TASK_JUST_DING) {
            sendTaskToService();
        }
    }

    private void sendTaskToService() {
        save();
        DingDingService.start(RApplication.getApp(), DingDingService.class, task);
    }
}
