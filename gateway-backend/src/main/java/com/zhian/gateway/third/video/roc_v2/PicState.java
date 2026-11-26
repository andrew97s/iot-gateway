package com.zhian.gateway.third.video.roc_v2;

/**
 * 照片下发状态
 *
 * @author tongwenjin
 * +@since 2025-4-10
 */
public class PicState {
    private int sequence;

    private boolean success;

    private String msg;

    public PicState(int sequence, boolean success, String msg) {
        this.sequence = sequence;
        this.success = success;
        this.msg = msg;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
