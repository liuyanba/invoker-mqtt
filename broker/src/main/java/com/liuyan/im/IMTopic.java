/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.liuyan.im;

public interface IMTopic {
    String SendMessageTopic = "MS";               //发消息
    String SendMessageToClusterTopic = "MSCLUSTER";               //集群转发消息
    String MultiCastMessageTopic = "MMC";
    String RecallMessageTopic = "MR";
    String PullMessageTopic = "MP";               //拉指定消息
    String NotifyMessageTopic = "MN";
    String NotifyRecallMessageTopic = "RMN";
    String BroadcastMessageTopic = "MBC";   //保存消息并发送

    String GetUserSettingTopic = "UG";            //获取用户信息
    String PutUserSettingTopic = "UP";             //修改用户信息
    String NotifyUserSettingTopic = "UN";

    String CreateGroupTopic = "GC";                    //创建群聊
    String AddGroupMemberTopic = "GAM";                //添加群聊成员
    String KickoffGroupMemberTopic = "GKM";            //踢出群成员
    String QuitGroupTopic = "GQ";                      //主动退出群
    String DismissGroupTopic = "GD";
    String ModifyGroupInfoTopic = "GMI";
    String ModifyGroupAliasTopic = "GMA";
    String GetGroupInfoTopic = "GPGI";                  //获取群聊详情         群聊第四步
    String GetGroupMemberTopic = "GPGM";                //获取群聊成员         群聊第五步
    String TransferGroupTopic = "GTG";
    String SetGroupManagerTopic = "GSM";                  //添加管理员

    String GetUserInfoTopic = "UPUI";                   //添加好友时 点击用户详情
    String ModifyMyInfoTopic = "MMI";

    String GetQiniuUploadTokenTopic = "GQNUT";

    String AddFriendRequestTopic = "FAR";               //添加好友
    String HandleFriendRequestTopic = "FHR";            //同意申请好友


    String FriendRequestPullTopic = "FRP";               //获取申请好友列表
    String NotifyFriendRequestTopic = "FRN";             //通知发送好友请求
    String RriendRequestUnreadSyncTopic = "FRUS";        //获取未读好友申请列表
    String DeleteFriendTopic = "FDL";                    //删除好友
    String NewDleteFriendTopic = "NFDL";                    //删除好友 / 2021年4月15日15点25分 2.3.0新增
    String FriendPullTopic = "FP";                       //好友列表
    String NotifyFriendTopic = "FN";
    String BlackListUserTopic = "BLU";                   //屏蔽用户
    String SetFriendAliasTopic = "FALS";
    String UploadDeviceTokenTopic = "UDT";
    String UserSearchTopic = "US";                        //查询好友
    String JoinChatroomTopic = "CRJ";
    String QuitChatroomTopic = "CRQ";
    String GetChatroomInfoTopic = "CRI";
    String GetChatroomMemberTopic = "CRMI";
    String RouteTopic = "ROUTE";

    String CreateChannelTopic = "CHC";
    String ModifyChannelInfoTopic = "CHMI";
    String TransferChannelInfoTopic = "CHT";
    String DestoryChannelInfoTopic = "CHD";
    String ChannelSearchTopic = "CHS";
    String ChannelListenTopic = "CHL";
    String ChannelPullTopic = "CHP";

    String GetTokenTopic = "GETTOKEN";
    String DestroyUserTopic = "DESTROYUSER";


    String LoadRemoteMessagesTopic = "LRM";  //拉取历史消息
    String KickoffPCClientTopic = "KPCC";
    String NewAddFriendTopic = "AFNL"; //新版添加好友  调用后直接添加成功,但是属于单向添加
    String NewAddFriendToClusterTopic = "AFNLTOCLUSTER"; //新版添加好友  调用后直接添加成功,但是属于集群推送
}