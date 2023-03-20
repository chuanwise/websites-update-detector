package cn.chuanwise.wud.detector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
     * @return 网站内容
     */
     Object detect(URL url) throws Exception;
}
