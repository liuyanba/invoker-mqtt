package io.moquette.liuyan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Liu_Yan-admin
 * @create 2020/11/18 15:31
 * @description 抽取日常习惯的工具类
 * @Version: 1.0
 */
public class LaoLiuUtils {

    private LaoLiuUtils(){
        throw new RuntimeException("No io.moquette.liuyan.utils.LaoLiuUtils instances for you!");
    }

    private static final Logger log = LoggerFactory.getLogger(LaoLiuUtils.class);

    public static boolean isEmpty(Collection<?> coll) {
        return Objects.isNull(coll) || coll.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }


    /**
     * 错误日志打印
     *
     * @param e 异常
     */
    public static void errorLog(Exception e) {
        // 异常类型
        log.error(e.toString());
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement track : e.getStackTrace()) {
            builder.append(track.toString()).append("\n");
        }
        // 异常具体内容
        log.error(builder.toString());
    }

    /**
     * 错误日志打印
     *
     * @param e 异常
     */
    public static void errorLog(String exceptionStr, String jsonObj, Exception e) {
        // 异常类型
        log.error(exceptionStr, jsonObj);
        log.error(e.toString());
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement track : e.getStackTrace()) {
            builder.append(track.toString()).append("\n");
        }
        // 异常具体内容
        log.error(builder.toString());
    }

    /**
     * 错误日志打印
     *
     * @param e 异常
     */
    public static void errorLog(String exceptionStr, Exception e) {
        // 异常类型
        log.error(exceptionStr);
        log.error(e.toString());
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement track : e.getStackTrace()) {
            builder.append(track.toString()).append("\n");
        }
        // 异常具体内容
        log.error(builder.toString());
    }
}