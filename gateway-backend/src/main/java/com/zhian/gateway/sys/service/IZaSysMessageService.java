package com.zhian.gateway.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysMessageLog;
import com.zhian.gateway.third.common.bo.ProcessInfo;

import java.util.List;

/**
 * 接入消息Service接口
 *
 * @author yepanpan
 * @date 2024 -07-10
 */
public interface IZaSysMessageService  extends IService<ZaSysMessage>
{

    /**
     * 快速记录消息
     *
     * @param device  设备
     * @param type    类别
     * @param content 数据内容
     * @param results 结果
     * @return the za sys message
     */
    public ZaSysMessage log(ZaSysDevice device, String type, String content, String results);

    /**
     * Log za sys message.
     *
     * @param info the info
     * @return the za sys message
     */
    ZaSysMessage log(ProcessInfo info);

    /**
     * 查询接入消息
     *
     * @param id 接入消息主键
     * @return 接入消息 za sys message
     */
    public ZaSysMessage selectZaSysMessageById(Long id);

    /**
     * 查询接入消息列表
     *
     * @param zaSysMessage 接入消息
     * @return 接入消息集合 list
     */
    public List<ZaSysMessage> selectZaSysMessageList(ZaSysMessage zaSysMessage);

    /**
     * 新增接入消息
     *
     * @param zaSysMessage 接入消息
     * @return 结果 int
     */
    public int insertZaSysMessage(ZaSysMessage zaSysMessage);

    /**
     * 修改接入消息
     *
     * @param zaSysMessage 接入消息
     * @return 结果 int
     */
    public int updateZaSysMessage(ZaSysMessage zaSysMessage);

    /**
     * 批量删除接入消息
     *
     * @param ids 需要删除的接入消息主键集合
     * @return 结果 int
     */
    public int deleteZaSysMessageByIds(Long[] ids);

    /**
     * 删除接入消息信息
     *
     * @param id 接入消息主键
     * @return 结果 int
     */
    public int deleteZaSysMessageById(Long id);

    /**
     * Purge.
     */
    void purge();

    /**
     * 查询某条消息的推送记录（按推送时间倒序）
     *
     * @param messageId the message id
     * @return the list
     */
    List<ZaSysMessageLog> selectPushLogsByMessageId(Long messageId);

    /**
     * 重试发送指定消息（重新调用 MessageSyncHandler 推送 content）
     *
     * @param id 消息主键
     * @return true=成功, false=失败
     */
    boolean retryMessage(Long id);

    /**
     * 按平台统计消息数量
     *
     * @return list of {pfCode, total, sentCount, failedCount}
     */
    java.util.List<java.util.Map<String, Object>> countByPlatform();
}
