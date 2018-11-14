package com.angcyo.dingding.bean;

import android.graphics.Rect;
import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/11/13
 */
public class WordBean {

    /**
     * log_id : 3613905262969373581
     * words_result_num : 12
     * words_result : [{"location":{"width":85,"top":1,"left":27,"height":29},"words":"P:0/1"},{"location":{"width":827,"top":2,"left":173,"height":27},"words":"dX780dY4270Xv1023Y10281Prs:1.0Sze10"},{"location":{"width":295,"top":231,"left":75,"height":39},"words":"11月13日周二下午"},{"location":{"width":429,"top":232,"left":570,"height":36},"words":"0.00Ks令100%z"},{"location":{"width":58,"top":387,"left":509,"height":52},"words":"A"},{"location":{"width":350,"top":557,"left":115,"height":29},"words":"◎今日:0B本月:75.6MB"},{"location":{"width":136,"top":556,"left":840,"height":31},"words":"设置套餐>"},{"location":{"width":302,"top":877,"left":208,"height":36},"words":"正在通过USB充电"},{"location":{"width":303,"top":926,"left":208,"height":35},"words":"点按即可查看更多选项"},{"location":{"width":301,"top":1049,"left":210,"height":36},"words":"已连接到∪SB调试"},{"location":{"width":391,"top":1098,"left":212,"height":35},"words":"点按即可停用∪SB调试功能。"},{"location":{"width":194,"top":1243,"left":88,"height":43},"words":"不重要通知"}]
     */

    private long log_id;
    private int words_result_num;
    private List<WordsResultBean> words_result;

    public long getLog_id() {
        return log_id;
    }

    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }

    public int getWords_result_num() {
        return words_result_num;
    }

    public void setWords_result_num(int words_result_num) {
        this.words_result_num = words_result_num;
    }

    public List<WordsResultBean> getWords_result() {
        if (words_result == null) {
            return Collections.emptyList();
        }
        return words_result;
    }

    public void setWords_result(List<WordsResultBean> words_result) {
        this.words_result = words_result;
    }

    /**
     * 返回指定关键字, 在图片中的矩形坐标
     */
    public Rect getRectByWord(String word) {
        Rect rect = new Rect();

        boolean isHaveFull = false;
        //完整匹配
        for (WordsResultBean bean : getWords_result()) {
            if (TextUtils.equals(bean.words, word)) {
                isHaveFull = true;
                rect.set(bean.location.left, bean.location.top,
                        bean.location.left + bean.location.width,
                        bean.location.top + bean.location.height);
                break;
            }
        }

        if (!isHaveFull) {
            for (WordsResultBean bean : getWords_result()) {
                if (bean.words.contains(word)) {
                    rect.set(bean.location.left, bean.location.top,
                            bean.location.left + bean.location.width,
                            bean.location.top + bean.location.height);
                    break;
                }
            }
        }
        return rect;
    }

    public static class WordsResultBean {
        /**
         * location : {"width":85,"top":1,"left":27,"height":29}
         * words : P:0/1
         */

        private LocationBean location;
        private String words = "";

        public LocationBean getLocation() {
            return location;
        }

        public void setLocation(LocationBean location) {
            this.location = location;
        }

        public String getWords() {
            return words;
        }

        public void setWords(String words) {
            this.words = words;
        }

        public static class LocationBean {
            /**
             * width : 85
             * top : 1
             * left : 27
             * height : 29
             */

            private int width;
            private int top;
            private int left;
            private int height;

            public int getWidth() {
                return width;
            }

            public void setWidth(int width) {
                this.width = width;
            }

            public int getTop() {
                return top;
            }

            public void setTop(int top) {
                this.top = top;
            }

            public int getLeft() {
                return left;
            }

            public void setLeft(int left) {
                this.left = left;
            }

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
            }
        }
    }
}
