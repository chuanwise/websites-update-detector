package cn.chuanwise.wud.detector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;

import java.net.URL;

/**
 * <h1>探测器</h1>
 *
 * <p>一旦发现和缓存内容不匹配，立刻发送消息</p>
 *
 * @author Chuanwise
 */
@JsonDeserialize(using = DetectorDeserializer.class)
public interface Detector {
    
    /**
     * 探测网站内容
     *
     * @param url    网址
     * @param logger 日志
     * @return 报告信息，若为 null 表示无信息报告
     */
    String detect(URL url, Logger logger) throws Exception;
}