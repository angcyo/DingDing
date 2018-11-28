package com.angcyo.dingding.bean;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/11/28
 */
public class UpdateBean {

    /**
     * versionCode : 39
     * des : 修复部分BUG
     * 优化体验
     * force : 0
     * url : https://raw.githubusercontent.com/angcyo/app/master/DingDing-3.2.2_2018-11-28_apk_release_angcyo.apk
     */

    private int versionCode;
    private String des;
    private String versionName = "";
    private int force;
    private String url;

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
}
