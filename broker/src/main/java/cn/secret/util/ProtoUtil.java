package cn.secret.util;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.moquette.spi.impl.Utils.readBytesAndRewind;

/**
 * Created by echov on 2020/6/30.
 */
public class ProtoUtil {


    public static void main1(String[] args) {

//        WFCMessage.MessageContent mc = WFCMessage.MessageContent.newBuilder().setType(0).build();
//        WFCMessage.Conversation con = WFCMessage.Conversation.newBuilder().setType(0).setLine(0).setTarget("m1").build();
//        WFCMessage.Message message  = WFCMessage.Message.newBuilder().setContent(mc).setFromUser("RUR-R-tt").setConversation(con).build();
//        System.out.println(new Gson().toJson(message));
        //System.out.println(new Gson().fromJson(json, WFCMessage.Message.class));
//        System.out.println(JSON.parseObject(json,WFCMessage.Message.class));
    }


    public static String toJson(Class clazz, GeneratedMessage message) {
        return getGson(clazz).toJson(message);
    }

    public static <T extends GeneratedMessage> T toProto(Class<T> klass, String json) {
        //   System.out.println(" klass : " + klass + "==============json:" + json);
        return getGson(klass).fromJson(json, klass);
    }

    /**
     * 如果这个方法要设置为public方法，那么需要确定gson是否是一个不可变对象，否则就不应该开放出去
     *
     * @param messageClass
     * @param <E>
     * @return
     */
    private static <E extends GeneratedMessage> Gson getGson(Class<E> messageClass) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(messageClass, new MessageAdapter(messageClass)).create();

        return gson;
    }

    private static class MessageAdapter<E extends GeneratedMessage> extends TypeAdapter<E> {

        private Class<E> messageClass;

        public MessageAdapter(Class<E> messageClass) {
            this.messageClass = messageClass;
        }

        @Override
        public void write(JsonWriter jsonWriter, E value) throws IOException {
            jsonWriter.jsonValue(JsonFormat.printer().print(value));
        }

        @Override
        public E read(JsonReader jsonReader) throws IOException {
            try {
                // 这里必须用范型<E extends Message>，不能直接用 Message，否则将找不到 newBuilder 方法
                Method method = messageClass.getMethod("newBuilder");
                // 调用静态方法
                E.Builder builder = (E.Builder) method.invoke(null);

                JsonParser jsonParser = new JsonParser();
                JsonFormat.parser().merge(jsonParser.parse(jsonReader).toString(), builder);
                return (E) builder.build();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
//                throw new ProtoJsonConversionException(e);
            }
            return null;
        }
    }

    public static void main(String[] args) throws Descriptors.DescriptorValidationException {
/*

        String json1 = "\n" +
                "{\n" +
                "  \"group\": {\n" +
                "    \"groupInfo\": {\n" +
                "      \"name\": \"群聊名称\",\n" +
                "      \"portrait\": \"imgUrl\",\n" +
                "      \"type\": 2                      \n" +
                "    },\n" +
                "    \"members\": [{                \n" +
                "      \"memberId\": \"PbPNPNHH\",\n" +
                "      \"type\": 0,                       \n" +
                "      \"updateDt\": \"123\"\n" +
                "    }, {\n" +
                "      \"memberId\": \"d1d5d5nn\",\n" +
                "      \"type\": 0,\n" +
                "      \"updateDt\": \"123\"\n" +
                "    }]\n" +
                "  },\n" +
                "  \"notifyContent\": {\n" +
                "    \"type\": 0                      \n" +
                "  },\n" +
                "\"toLine\": [0]                  \n" +
                "}";
        WFCMessage.CreateGroupRequest createGroupRequest = toProto(WFCMessage.CreateGroupRequest.class, json1);
        System.out.println(createGroupRequest);


        WFCMessage.MessageContent mc = WFCMessage.MessageContent.newBuilder().setType(0).setPersistFlag(3).setContent("hello,you got it!").build();
        WFCMessage.Conversation con = WFCMessage.Conversation.newBuilder().setType(0).setLine(0).setTarget("m1").build();
        WFCMessage.MessageContent content = WFCMessage.MessageContent.newBuilder().setType(0).build();
        WFCMessage.Version message = WFCMessage.Version.newBuilder().setVersion(1).build();

        // String json = toJson(WFCMessage.MessageContent.class, mc);
        WFCMessage.ModifyGroupInfoRequest mi = WFCMessage.ModifyGroupInfoRequest.newBuilder().setGroupId("1").setNotifyContent(content).setType(1).setValue("2222").build();

        WFCMessage.QuitGroupRequest gq = WFCMessage.QuitGroupRequest.newBuilder().setGroupId("0").setNotifyContent(content).buildPartial();
        String json = toJson(WFCMessage.QuitGroupRequest.class, gq);

        System.out.println("json:" + json);
        System.out.println("########");
        WFCMessage.GroupInfo groupInfo = WFCMessage.GroupInfo.newBuilder().setName("群聊名称").setType(2).setPortrait("imgUrl").build();
        WFCMessage.GroupMember member1 = WFCMessage.GroupMember.newBuilder().setMemberId("123").setType(0).setUpdateDt(123).build();
        WFCMessage.GroupMember member2 = WFCMessage.GroupMember.newBuilder().setMemberId("123111").setType(0).setUpdateDt(123).build();
        WFCMessage.Group group = WFCMessage.Group.newBuilder().setGroupInfo(groupInfo).addMembers(member1).addMembers(member2).build();


        //分组详情

        WFCMessage.UserRequest UR = WFCMessage.UserRequest.newBuilder().setUid("xpxPxPNN").setUpdateDt(123).build();
        WFCMessage.PullUserRequest PR = WFCMessage.PullUserRequest.newBuilder().addRequest(UR).build();


        // WFCMessage.AddGroupMemberRequest AM = WFCMessage.AddGroupMemberRequest.newBuilder().addAddedMember(member1).setGroupId("xpxPxPNN").addToLine(0).build();

        WFCMessage.RemoveGroupMemberRequest RM = WFCMessage.RemoveGroupMemberRequest.newBuilder().addRemovedMember("RKRdRdBB").addRemovedMember("456").setGroupId("xpxPxPNN").addToLine(0).build();
*/
        cn.wildfirechat.proto.WFCMessage.UserRequest ur = cn.wildfirechat.proto.WFCMessage.UserRequest.newBuilder().setUid("123").setUpdateDt(123L).build();
        WFCMessage.PullUserRequest PU = WFCMessage.PullUserRequest.newBuilder().addRequest(ur).build();

        /*WFCMessage.ModifyUserSettingReq MU = WFCMessage.ModifyUserSettingReq.newBuilder().setScope(1).setKey("1-0-6p6D6Dzz").setValue("1").build();
        WFCMessage.SetGroupManagerRequest GM = WFCMessage.SetGroupManagerRequest.newBuilder().addUserId("123").setType(2).setGroupId("123").addToLine(0).build();
        String json2 = toJson(WFCMessage.SetGroupManagerRequest.class, GM);*/
        //  WFCMessage.PullUserRequest psua = WFCMessage.PullUserRequest.newBuilder().setField(null, null).build();
        String json = toJson(WFCMessage.PullUserRequest.class, PU);
        // System.out.println("json:" + PU);
        ByteBuf byteBufJson = Unpooled.wrappedBuffer(json.getBytes());
        byte[] payloadContent = readBytesAndRewind(byteBufJson);
        WFCMessage.PullUserRequest userRequest = ProtoUtil.toProto(WFCMessage.PullUserRequest.class, new String(payloadContent));
        json = toJson(WFCMessage.PullUserRequest.class, userRequest);
        System.out.println(json);
    }


}
