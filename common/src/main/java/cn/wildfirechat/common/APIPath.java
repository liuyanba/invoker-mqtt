package cn.wildfirechat.common;

public interface APIPath {
    String Create_Chatroom = "/admin/chatroom/create";
    String Chatroom_Destroy = "/admin/chatroom/del";
    String Chatroom_Info = "/admin/chatroom/info";
    String Chatroom_GetMembers = "/admin/chatroom/members";
    String Chatroom_SetBlacklist = "/admin/chatroom/set_black_status";
    String Chatroom_GetBlacklist = "/admin/chatroom/get_black_status";
    String Chatroom_SetManager = "/admin/chatroom/set_manager";
    String Chatroom_GetManagerList = "/admin/chatroom/get_manager_list";
    String Chatroom_MuteAll = "/admin/chatroom/mute_all";


    String Sensitive_Add = "/admin/sensitive/add";
    String Sensitive_Del = "/admin/sensitive/del";
    String Sensitive_Query = "/admin/sensitive/query";

    String Create_User = "/admin/user/create";
    String Destroy_User = "/admin/user/destroy";
    String Create_Robot = "/admin/robot/create";
    String CreateOrUpdate_Device = "/admin/device/create";
    String Get_Device = "/admin/device/get";
    String Get_User_Devices = "/admin/device/user_devices";

    /**
     * 获取用户token
     * <table>
     * <thead>
     * <tr>
     * <th>参数</th>
     * <th>类型</th>
     * <th>必需</th>
     * <th>描述</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>userId</td>
     * <td>string</td>
     * <td>是</td>
     * <td>用户ID</td>
     * </tr>
     * <tr>
     * <td>clientId</td>
     * <td>string</td>
     * <td>是</td>
     * <td>客户端ID</td>
     * </tr>
     * <tr>
     * <td>platform</td>
     * <td>int</td>
     * <td>否</td>
     * <td>平台类型iOS 1, Android 2, Windows 3, OSX 4, WEB = 5</td>
     * </tr>
     * </tbody>
     * </table>
     * <hr>
     * <table>
     * <thead>
     * <tr>
     * <th>参数</th>
     * <th>类型</th>
     * <th>必需</th>
     * <th>描述</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>userId</td>
     * <td>string</td>
     * <td>是</td>
     * <td>用户ID</td>
     * </tr>
     * <tr>
     * <td>token</td>
     * <td>string</td>
     * <td>是</td>
     * <td>用户token</td>
     * </tr>
     * </tbody>
     * </table>
     */
    String User_Get_Token = "/admin/user/get_token";
    /**
     * 设置用户状态 请求参数 status userId
     * 0.正常
     * 1.禁言
     * 2.禁止
     */
    String User_Update_Block_Status = "/admin/user/update_block_status";
    String User_Get_Info = "/admin/user/get_info";
    String User_Get_Blocked_List = "/admin/user/get_blocked_list";
    String User_Check_Block_Status = "/admin/user/check_block_status";
    String User_Get_Online_Status = "/admin/user/onlinestatus";


    String Friend_Update_Status = "/admin/friend/status";
    String Friend_Get_List = "/admin/friend/list";
    String Blacklist_Update_Status = "/admin/blacklist/status";
    String Blacklist_Get_List = "/admin/blacklist/list";
    String Friend_Get_Alias = "/admin/friend/get_alias";
    String Friend_Set_Alias = "/admin/friend/set_alias";

    String Msg_Send = "/admin/message/send";
    String Msg_Recall = "/admin/message/recall";
    String Msg_Delete = "/admin/message/delete";
    String Msg_Broadcast = "/admin/message/broadcast";
    String Msg_Multicast = "/admin/message/multicast";
    String Msg_LRM = "/admin/message/lrm";

    String Create_Group = "/admin/group/create";
    String Group_Dismiss = "/admin/group/del";
    String Group_Transfer = "/admin/group/transfer";
    String Group_Get_Info = "/admin/group/get_info";
    String Group_Modify_Info = "/admin/group/modify";
    String Group_Member_List = "/admin/group/member/list";
    String Group_Member_Add = "/admin/group/member/add";
    String Group_Member_Kickoff = "/admin/group/member/del";
    String Group_Member_Quit = "/admin/group/member/quit";
    String Group_Set_Manager = "/admin/group/manager/set";
    String Group_Mute_Member = "/admin/group/manager/mute";
    String Get_User_Groups = "/admin/group/of_user";


    String Create_Channel = "/admin/channel/create";
    String Get_System_Setting = "/admin/system/get_setting";
    String Put_System_Setting = "/admin/system/put_setting";

    String Channel_User_Info = "/channel/user_info";
    String Channel_Update_Profile = "/channel/update_profile";
    String Channel_Get_Profile = "/channel/get_profile";
    String Channel_Message_Send = "/channel/message/send";
    String Channel_Subscribe = "/channel/subscribe";
    String Channel_Subscriber_List = "/channel/subscriber_list";

    String Robot_User_Info = "/robot/user_info";
    String Robot_Message_Send = "/robot/message/send";

    String Set_User_Info = "/admin/set_user_info";
}
