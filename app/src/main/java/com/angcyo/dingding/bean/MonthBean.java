package com.angcyo.dingding.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/11/19
 */
public class MonthBean {

    /**
     * reason : Success
     * result : {"data":{"year":"2018","year-month":"2018-8","holiday":"[{\"name\":\"中秋节\",\"festival\":\"2018-9-24\",\"desc\":\"9月24日放假，与周末连休。\",\"rest\":\"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假\",\"list\":[{\"date\":\"2018-9-22\",\"status\":\"1\"},{\"date\":\"2018-9-23\",\"status\":\"1\"},{\"date\":\"2018-9-24\",\"status\":\"1\"}],\"list#num#\":3}]","holiday_array":[{"name":"中秋节","festival":"2018-9-24","desc":"9月24日放假，与周末连休。","rest":"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假","list":[{"date":"2018-9-22","status":"1"},{"date":"2018-9-23","status":"1"},{"date":"2018-9-24","status":"1"}],"list#num#":3,"list_num":3}]}}
     * error_code : 0
     */

    private String reason;
    private ResultBean result;
    private int error_code = -1;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ResultBean getResult() {
        return result;
    }

    public void setResult(ResultBean result) {
        this.result = result;
    }

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public static class ResultBean {
        /**
         * data : {"year":"2018","year-month":"2018-8","holiday":"[{\"name\":\"中秋节\",\"festival\":\"2018-9-24\",\"desc\":\"9月24日放假，与周末连休。\",\"rest\":\"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假\",\"list\":[{\"date\":\"2018-9-22\",\"status\":\"1\"},{\"date\":\"2018-9-23\",\"status\":\"1\"},{\"date\":\"2018-9-24\",\"status\":\"1\"}],\"list#num#\":3}]","holiday_array":[{"name":"中秋节","festival":"2018-9-24","desc":"9月24日放假，与周末连休。","rest":"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假","list":[{"date":"2018-9-22","status":"1"},{"date":"2018-9-23","status":"1"},{"date":"2018-9-24","status":"1"}],"list#num#":3,"list_num":3}]}
         */

        private DataBean data;

        public DataBean getData() {
            return data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public static class DataBean {
            /**
             * year : 2018
             * year-month : 2018-8
             * holiday : [{"name":"中秋节","festival":"2018-9-24","desc":"9月24日放假，与周末连休。","rest":"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假","list":[{"date":"2018-9-22","status":"1"},{"date":"2018-9-23","status":"1"},{"date":"2018-9-24","status":"1"}],"list#num#":3}]
             * holiday_array : [{"name":"中秋节","festival":"2018-9-24","desc":"9月24日放假，与周末连休。","rest":"拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假","list":[{"date":"2018-9-22","status":"1"},{"date":"2018-9-23","status":"1"},{"date":"2018-9-24","status":"1"}],"list#num#":3,"list_num":3}]
             */

            private String year;
            @SerializedName("year-month")
            private String yearmonth;
            private String holiday;
            private List<HolidayArrayBean> holiday_array;

            public String getYear() {
                return year;
            }

            public void setYear(String year) {
                this.year = year;
            }

            public String getYearmonth() {
                return yearmonth;
            }

            public void setYearmonth(String yearmonth) {
                this.yearmonth = yearmonth;
            }

            public String getHoliday() {
                return holiday;
            }

            public void setHoliday(String holiday) {
                this.holiday = holiday;
            }

            public List<HolidayArrayBean> getHoliday_array() {
                return holiday_array;
            }

            public void setHoliday_array(List<HolidayArrayBean> holiday_array) {
                this.holiday_array = holiday_array;
            }

            public static class HolidayArrayBean {
                /**
                 * name : 中秋节
                 * festival : 2018-9-24
                 * desc : 9月24日放假，与周末连休。
                 * rest : 拼假建议：2018年9月25日（周二）~2018年9月30日（周日）请假6天，与国庆节衔接，拼16天小长假
                 * list : [{"date":"2018-9-22","status":"1"},{"date":"2018-9-23","status":"1"},{"date":"2018-9-24","status":"1"}]
                 * list#num# : 3
                 * list_num : 3
                 */

                private String name;
                private String festival;
                private String desc;
                private String rest;
                @SerializedName("list#num#")
                private int _$ListNum7; // FIXME check this code
                private int list_num;
                private List<ListBean> list;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getFestival() {
                    return festival;
                }

                public void setFestival(String festival) {
                    this.festival = festival;
                }

                public String getDesc() {
                    return desc;
                }

                public void setDesc(String desc) {
                    this.desc = desc;
                }

                public String getRest() {
                    return rest;
                }

                public void setRest(String rest) {
                    this.rest = rest;
                }

                public int get_$ListNum7() {
                    return _$ListNum7;
                }

                public void set_$ListNum7(int _$ListNum7) {
                    this._$ListNum7 = _$ListNum7;
                }

                public int getList_num() {
                    return list_num;
                }

                public void setList_num(int list_num) {
                    this.list_num = list_num;
                }

                public List<ListBean> getList() {
                    return list;
                }

                public void setList(List<ListBean> list) {
                    this.list = list;
                }

                public static class ListBean {
                    /**
                     * date : 2018-9-22
                     * status : 1
                     */

                    private String date;
                    private String status;

                    public String getDate() {
                        return date;
                    }

                    public void setDate(String date) {
                        this.date = date;
                    }

                    public String getStatus() {
                        return status;
                    }

                    public void setStatus(String status) {
                        this.status = status;
                    }
                }
            }
        }
    }
}
