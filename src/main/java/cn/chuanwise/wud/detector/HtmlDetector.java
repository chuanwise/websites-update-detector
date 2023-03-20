package cn.chuanwise.wud.detector;

import org.jsoup.nodes.Element;

import java.net.URL;

/**
 * <h1>Html 文档解析探测器</h1>
 *
 * @author Chuanwise
 */
public class HtmlDetector
    implements Detector {
    
    /**
     * 步骤
     */
    public interface Step {
        Object advance(Element element);
    }
    
    /**
     * 通过 ID 获取元素
     */
    private static class GetElementById
        implements Step {
    
        private final String id;
    
        public GetElementById(String id) {
            this.id = id;
        }
    
        @Override
        public Element advance(Element element) {
            return element.getElementById(id);
        }
    }
    
    @Override
    public Object detect(URL url) throws Exception {
        return null;
    }
}
