package com.zhian.gateway.third.video.roc_v2;

import java.util.List;

/**
 * 全量用户信息
 *
 * @author tongwenjin
 * @since 2025-3-31
 */


public class TotalUser {
    private int totalCount;

    private List<String> userIds;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}
