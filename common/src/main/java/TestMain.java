import cn.wildfirechat.proto.WFCMessage;

/**
 * Created by echov on 2020/6/23.
 */
public class TestMain
{

    public static void main(String[] args) {
        print(
            "\n\032wfcmessage_community.proto\"6\n\020AddFrien" +
                "dRequest\022\022\n\ntarget_uid\030\001 \002(\t\022\016\n\006reason\030\002" +
                " \002(\t\":\n\014Conversation\022\014\n\004type\030\001 \002(\005\022\016\n\006ta" +
                "rget\030\002 \002(\t\022\014\n\004line\030\003 \002(\005\"\370\001\n\tGroupInfo\022\021" +
                "\n\ttarget_id\030\001 \001(\t\022\014\n\004name\030\002 \002(\t\022\020\n\010portr" +
                "ait\030\003 \001(\t\022\r\n\005owner\030\004 \001(\t\022\014\n\004type\030\005 \002(\005\022\024" +
                "\n\014member_count\030\006 \001(\005\022\r\n\005extra\030\007 \001(\t\022\021\n\tu" +
                "pdate_dt\030\010 \001(\003\022\030\n\020member_update_dt\030\t \001(\003" +
                "\022\014\n\004mute\030\n \001(\005\022\021\n\tjoin_type\030\013 \001(\005\022\024\n\014pri" +
                "vate_chat\030\014 \001(\005\022\022\n\nsearchable\030\r \001(\005\"P\n\013G",
            "roupMember\022\021\n\tmember_id\030\001 \002(\t\022\r\n\005alias\030\002" +
                " \001(\t\022\014\n\004type\030\003 \002(\005\022\021\n\tupdate_dt\030\004 \001(\003\"F\n" +
                "\005Group\022\036\n\ngroup_info\030\001 \002(\0132\n.GroupInfo\022\035" +
                "\n\007members\030\002 \003(\0132\014.GroupMember\"\304\001\n\013Channe" +
                "lInfo\022\021\n\ttarget_id\030\001 \001(\t\022\014\n\004name\030\002 \002(\t\022\020" +
                "\n\010portrait\030\003 \001(\t\022\r\n\005owner\030\004 \001(\t\022\016\n\006statu" +
                "s\030\005 \001(\005\022\014\n\004desc\030\006 \001(\t\022\r\n\005extra\030\007 \001(\t\022\021\n\t" +
                "update_dt\030\010 \001(\003\022\016\n\006secret\030\t \001(\t\022\020\n\010callb" +
                "ack\030\n \001(\t\022\021\n\tautomatic\030\013 \001(\005\"D\n\021ModifyCh" +
                "annelInfo\022\022\n\nchannel_id\030\001 \002(\t\022\014\n\004type\030\002 ",
            "\002(\005\022\r\n\005value\030\003 \002(\t\"8\n\017TransferChannel\022\022\n" +
                "\nchannel_id\030\001 \002(\t\022\021\n\tnew_owner\030\002 \002(\t\"3\n\017" +
                "PullChannelInfo\022\022\n\nchannel_id\030\001 \002(\t\022\014\n\004h" +
                "ead\030\002 \002(\003\"H\n\023PullChannelListener\022\022\n\nchan" +
                "nel_id\030\001 \002(\t\022\016\n\006offset\030\002 \002(\005\022\r\n\005count\030\003 " +
                "\002(\005\"R\n\031PullChannelListenerResult\022\023\n\013tota" +
                "l_count\030\001 \002(\005\022\016\n\006offset\030\002 \002(\005\022\020\n\010listene" +
                "r\030\003 \003(\t\"3\n\rListenChannel\022\022\n\nchannel_id\030\001" +
                " \002(\t\022\016\n\006listen\030\002 \002(\005\"E\n\023SearchChannelRes" +
                "ult\022\035\n\007channel\030\001 \003(\0132\014.ChannelInfo\022\017\n\007ke",
            "yword\030\002 \002(\t\"\212\002\n\016MessageContent\022\014\n\004type\030\001" +
                " \002(\005\022\032\n\022searchable_content\030\002 \001(\t\022\024\n\014push" +
                "_content\030\003 \001(\t\022\017\n\007content\030\004 \001(\t\022\014\n\004data\030" +
                "\005 \001(\014\022\021\n\tmediaType\030\006 \001(\005\022\026\n\016remoteMediaU" +
                "rl\030\007 \001(\t\022\024\n\014persist_flag\030\010 \001(\005\022\027\n\017expire" +
                "_duration\030\t \001(\005\022\026\n\016mentioned_type\030\n \001(\005\022" +
                "\030\n\020mentioned_target\030\013 \003(\t\022\r\n\005extra\030\014 \001(\t" +
                "\"\207\001\n\025AddGroupMemberRequest\022\020\n\010group_id\030\001" +
                " \002(\t\022\"\n\014added_member\030\002 \003(\0132\014.GroupMember" +
                "\022\017\n\007to_line\030\003 \003(\005\022\'\n\016notify_content\030\004 \001(",
            "\0132\017.MessageContent\"e\n\022CreateGroupRequest" +
                "\022\025\n\005group\030\001 \002(\0132\006.Group\022\017\n\007to_line\030\002 \003(\005" +
                "\022\'\n\016notify_content\030\003 \001(\0132\017.MessageConten" +
                "t\"a\n\023DismissGroupRequest\022\020\n\010group_id\030\001 \002" +
                "(\t\022\017\n\007to_line\030\002 \003(\005\022\'\n\016notify_content\030\003 " +
                "\001(\0132\017.MessageContent\"\226\001\n\rFriendRequest\022\020" +
                "\n\010from_uid\030\001 \001(\t\022\016\n\006to_uid\030\002 \002(\t\022\016\n\006reas" +
                "on\030\003 \002(\t\022\016\n\006status\030\004 \001(\005\022\021\n\tupdate_dt\030\005 " +
                "\001(\003\022\030\n\020from_read_status\030\006 \001(\010\022\026\n\016to_read" +
                "_status\030\007 \001(\010\"#\n\rGeneralResult\022\022\n\nerror_",
            "code\030\001 \002(\005\"?\n\025GetUploadTokenRequest\022\022\n\nm" +
                "edia_type\030\001 \002(\005\022\022\n\nmedia_path\030\002 \002(\t\"S\n\024G" +
                "etUploadTokenResult\022\016\n\006domain\030\001 \002(\t\022\r\n\005t" +
                "oken\030\002 \002(\t\022\016\n\006server\030\003 \002(\t\022\014\n\004port\030\004 \001(\005" +
                "\"H\n\023HandleFriendRequest\022\022\n\ntarget_uid\030\001 " +
                "\002(\t\022\016\n\006status\030\002 \002(\005\022\r\n\005extra\030\003 \001(\t\"\023\n\005ID" +
                "Buf\022\n\n\002id\030\001 \002(\t\"\027\n\tIDListBuf\022\n\n\002id\030\001 \003(\t" +
                "\"\256\001\n\007Message\022#\n\014conversation\030\001 \002(\0132\r.Con" +
                "versation\022\021\n\tfrom_user\030\002 \002(\t\022 \n\007content\030" +
                "\003 \002(\0132\017.MessageContent\022\022\n\nmessage_id\030\004 \001",
            "(\003\022\030\n\020server_timestamp\030\005 \001(\003\022\017\n\007to_user\030" +
                "\006 \001(\t\022\n\n\002to\030\007 \003(\t\"\353\001\n\004User\022\013\n\003uid\030\001 \002(\t\022" +
                "\014\n\004name\030\002 \001(\t\022\024\n\014display_name\030\003 \001(\t\022\020\n\010p" +
                "ortrait\030\004 \001(\t\022\016\n\006mobile\030\005 \001(\t\022\r\n\005email\030\006" +
                " \001(\t\022\017\n\007address\030\007 \001(\t\022\017\n\007company\030\010 \001(\t\022\r" +
                "\n\005extra\030\t \001(\t\022\021\n\tupdate_dt\030\n \001(\003\022\016\n\006gend" +
                "er\030\013 \001(\005\022\016\n\006social\030\014 \001(\t\022\014\n\004type\030\r \001(\005\022\017" +
                "\n\007deleted\030\016 \001(\005\"c\n\005Robot\022\013\n\003uid\030\001 \002(\t\022\r\n" +
                "\005state\030\002 \002(\005\022\r\n\005owner\030\003 \001(\t\022\016\n\006secret\030\004 " +
                "\001(\t\022\020\n\010callback\030\005 \001(\t\022\r\n\005extra\030\006 \001(\t\"(\n\017",
            "GetRobotsResult\022\025\n\005entry\030\001 \003(\0132\006.Robot\"P" +
                "\n\005Thing\022\013\n\003uid\030\001 \002(\t\022\r\n\005state\030\002 \002(\005\022\r\n\005t" +
                "oken\030\003 \002(\t\022\r\n\005owner\030\004 \001(\t\022\r\n\005extra\030\005 \001(\t" +
                "\"(\n\017GetThingsResult\022\025\n\005entry\030\001 \003(\0132\006.Thi" +
                "ng\"g\n\030UploadDeviceTokenRequest\022\020\n\010platfo" +
                "rm\030\001 \002(\005\022\020\n\010app_name\030\002 \002(\t\022\024\n\014device_tok" +
                "en\030\003 \002(\t\022\021\n\tpush_type\030\004 \002(\005\"\201\001\n\026ModifyGr" +
                "oupInfoRequest\022\020\n\010group_id\030\001 \002(\t\022\014\n\004type" +
                "\030\002 \002(\005\022\r\n\005value\030\003 \002(\t\022\017\n\007to_line\030\004 \003(\005\022\'" +
                "\n\016notify_content\030\005 \001(\0132\017.MessageContent\"",
            "\203\001\n\026SetGroupManagerRequest\022\020\n\010group_id\030\001" +
                " \002(\t\022\014\n\004type\030\002 \002(\005\022\017\n\007user_id\030\003 \003(\t\022\017\n\007t" +
                "o_line\030\004 \003(\005\022\'\n\016notify_content\030\005 \001(\0132\017.M" +
                "essageContent\"(\n\tInfoEntry\022\014\n\004type\030\001 \002(\005" +
                "\022\r\n\005value\030\002 \002(\t\"0\n\023ModifyMyInfoRequest\022\031" +
                "\n\005entry\030\001 \003(\0132\n.InfoEntry\";\n\rNotifyMessa" +
                "ge\022\014\n\004type\030\001 \002(\005\022\014\n\004head\030\002 \002(\003\022\016\n\006target" +
                "\030\003 \001(\t\"=\n\022PullMessageRequest\022\n\n\002id\030\001 \002(\003" +
                "\022\014\n\004type\030\002 \002(\005\022\r\n\005delay\030\003 \001(\003\"M\n\021PullMes" +
                "sageResult\022\031\n\007message\030\001 \003(\0132\010.Message\022\017\n",
            "\007current\030\002 \002(\003\022\014\n\004head\030\003 \002(\003\"/\n\023PullGrou" +
                "pInfoResult\022\030\n\004info\030\001 \003(\0132\n.GroupInfo\"6\n" +
                "\026PullGroupMemberRequest\022\016\n\006target\030\001 \002(\t\022" +
                "\014\n\004head\030\002 \002(\003\"5\n\025PullGroupMemberResult\022\034" +
                "\n\006member\030\001 \003(\0132\014.GroupMember\"-\n\013UserRequ" +
                "est\022\013\n\003uid\030\001 \002(\t\022\021\n\tupdate_dt\030\002 \001(\003\"0\n\017P" +
                "ullUserRequest\022\035\n\007request\030\001 \003(\0132\014.UserRe" +
                "quest\"/\n\nUserResult\022\023\n\004user\030\001 \002(\0132\005.User" +
                "\022\014\n\004code\030\002 \002(\005\"-\n\016PullUserResult\022\033\n\006resu" +
                "lt\030\001 \003(\0132\013.UserResult\"^\n\020QuitGroupReques",
            "t\022\020\n\010group_id\030\001 \002(\t\022\017\n\007to_line\030\002 \003(\005\022\'\n\016" +
                "notify_content\030\003 \001(\0132\017.MessageContent\"~\n" +
                "\030RemoveGroupMemberRequest\022\020\n\010group_id\030\001 " +
                "\002(\t\022\026\n\016removed_member\030\002 \003(\t\022\017\n\007to_line\030\003" +
                " \003(\005\022\'\n\016notify_content\030\004 \001(\0132\017.MessageCo" +
                "ntent\"u\n\024TransferGroupRequest\022\020\n\010group_i" +
                "d\030\001 \002(\t\022\021\n\tnew_owner\030\002 \002(\t\022\017\n\007to_line\030\003 " +
                "\003(\005\022\'\n\016notify_content\030\004 \001(\0132\017.MessageCon" +
                "tent\"s\n\026ModifyGroupMemberAlias\022\020\n\010group_" +
                "id\030\001 \002(\t\022\r\n\005alias\030\002 \002(\t\022\017\n\007to_line\030\003 \003(\005",
            "\022\'\n\016notify_content\030\004 \001(\0132\017.MessageConten" +
                "t\"P\n\020UserSettingEntry\022\r\n\005scope\030\001 \002(\005\022\013\n\003" +
                "key\030\002 \002(\t\022\r\n\005value\030\003 \002(\t\022\021\n\tupdate_dt\030\004 " +
                "\002(\003\"A\n\024ModifyUserSettingReq\022\r\n\005scope\030\001 \002" +
                "(\005\022\013\n\003key\030\002 \002(\t\022\r\n\005value\030\003 \002(\t\"\032\n\007Versio" +
                "n\022\017\n\007version\030\001 \002(\003\"8\n\024GetUserSettingResu" +
                "lt\022 \n\005entry\030\001 \003(\0132\021.UserSettingEntry\"f\n\006" +
                "Friend\022\013\n\003uid\030\001 \002(\t\022\r\n\005state\030\002 \002(\005\022\021\n\tup" +
                "date_dt\030\003 \002(\003\022\r\n\005alias\030\004 \001(\t\022\017\n\007blacked\030" +
                "\005 \001(\005\022\r\n\005extra\030\006 \001(\t\"*\n\020GetFriendsResult",
            "\022\026\n\005entry\030\001 \003(\0132\007.Friend\"7\n\026GetFriendReq" +
                "uestResult\022\035\n\005entry\030\001 \003(\0132\016.FriendReques" +
                "t\"\243\001\n\021ConnectAckPayload\022\020\n\010msg_head\030\001 \001(" +
                "\003\022\023\n\013friend_head\030\002 \001(\003\022\026\n\016friend_rq_head" +
                "\030\003 \001(\003\022\024\n\014setting_head\030\004 \001(\003\022\021\n\tnode_add" +
                "r\030\005 \001(\t\022\021\n\tnode_port\030\006 \001(\005\022\023\n\013server_tim" +
                "e\030\007 \001(\003\"P\n\rIMHttpWrapper\022\r\n\005token\030\001 \002(\t\022" +
                "\021\n\tclient_id\030\002 \002(\t\022\017\n\007request\030\003 \002(\t\022\014\n\004d" +
                "ata\030\004 \001(\014\"A\n\021SearchUserRequest\022\017\n\007keywor" +
                "d\030\001 \002(\t\022\r\n\005fuzzy\030\002 \001(\005\022\014\n\004page\030\003 \001(\005\"(\n\020",
            "SearchUserResult\022\024\n\005entry\030\001 \003(\0132\005.User\"@" +
                "\n\026GetChatroomInfoRequest\022\023\n\013chatroom_id\030" +
                "\001 \002(\t\022\021\n\tupdate_dt\030\002 \001(\003\"\227\001\n\014ChatroomInf" +
                "o\022\r\n\005title\030\001 \002(\t\022\014\n\004desc\030\002 \001(\t\022\020\n\010portra" +
                "it\030\003 \001(\t\022\024\n\014member_count\030\004 \001(\005\022\021\n\tcreate" +
                "_dt\030\005 \001(\003\022\021\n\tupdate_dt\030\006 \001(\003\022\r\n\005extra\030\007 " +
                "\001(\t\022\r\n\005state\030\010 \001(\005\"F\n\034GetChatroomMemberI" +
                "nfoRequest\022\023\n\013chatroom_id\030\001 \002(\t\022\021\n\tmax_c" +
                "ount\030\002 \001(\005\";\n\022ChatroomMemberInfo\022\024\n\014memb" +
                "er_count\030\001 \001(\005\022\017\n\007members\030\002 \003(\t\"\026\n\010INT64",
            "Buf\022\n\n\002id\030\001 \002(\003\"4\n\023NotifyRecallMessage\022\n" +
                "\n\002id\030\001 \002(\003\022\021\n\tfrom_user\030\002 \002(\t\"/\n\020BlackUs" +
                "erRequest\022\013\n\003uid\030\001 \002(\t\022\016\n\006status\030\002 \002(\005\"\323" +
                "\001\n\014RouteRequest\022\013\n\003app\030\001 \001(\t\022\020\n\010platform" +
                "\030\002 \001(\005\022\021\n\tpush_type\030\003 \001(\005\022\023\n\013device_name" +
                "\030\004 \001(\t\022\026\n\016device_version\030\005 \001(\t\022\022\n\nphone_" +
                "name\030\006 \001(\t\022\020\n\010language\030\007 \001(\t\022\024\n\014carrier_" +
                "name\030\010 \001(\t\022\023\n\013app_version\030\t \001(\t\022\023\n\013sdk_v" +
                "ersion\030\n \001(\t\"D\n\rRouteResponse\022\014\n\004host\030\001 " +
                "\002(\t\022\021\n\tlong_port\030\002 \002(\005\022\022\n\nshort_port\030\003 \002",
            "(\005\"G\n\017GetTokenRequest\022\017\n\007user_id\030\001 \002(\t\022\021" +
                "\n\tclient_id\030\002 \002(\t\022\020\n\010platform\030\003 \001(\005\"\\\n\022L" +
                "oadRemoteMessages\022#\n\014conversation\030\001 \002(\0132" +
                "\r.Conversation\022\022\n\nbefore_uid\030\002 \002(\003\022\r\n\005co" +
                "unt\030\003 \002(\005\"a\n\020MultiCastMessage\022\021\n\tfrom_us" +
                "er\030\001 \002(\t\022 \n\007content\030\002 \002(\0132\017.MessageConte" +
        "nt\022\n\n\002to\030\003 \003(\t\022\014\n\004line\030\004 \002(\005B#\n\025cn.wildf" +
            "irechat.protoB\nWFCMessage"
        );
    }

    private static void print(String... s){
//        for (String _s: s) {
//            System.out.print(_s);
//        }

        System.out.println(WFCMessage.getDescriptor().toProto());
    }
}




