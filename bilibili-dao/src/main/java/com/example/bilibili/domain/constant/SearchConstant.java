package com.example.bilibili.domain.constant;

public interface SearchConstant {

    //视频查询-综合排序
    // Video lookup - comprehensive
    public static final String DEFAULT = "0";

    // Video lookup - most played
    public static final String VIEW_COUNT = "1";

    // Video lookup - recents
    public static final String CREATE_TIME = "2";

    // Video lookup - most danmus
    public static final String DANMU_COUNT = "3";

    // User lookup - default
    public static final String USER_DEFAULT = "4";

    // User lookup - rank by subscription
    public static final String USER_FAN_COUNT_DESC = "5";

    // User lookup - rank by subscription reverse
    public static final String USER_FAN_COUNT_ASC = "6";

}
