package cn.chuanwise.wud.detector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * <h1>探测器反序列化器</h1>
 *
 * <p>用于反序列化 {@link Detector}</p>
 *
 * @author Chuanwise
 */
public class DetectorDeserializer
    extends JsonDeserializer<Detector> {
    
    private static final DetectorDeserializer INSTANCE = new DetectorDeserializer();
    private volatile XPath xPath;
    
    public static DetectorDeserializer getInstance() {
        return INSTANCE;
    }
    
    private DetectorDeserializer() {
    }
    
    @Override
    public Detector deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        final TreeNode treeNode = parser.readValueAsTree();
        final String type = ((TextNode) treeNode.get("type")).asText();
        if (type == null) {
            throw new IllegalArgumentException("Type is null!");
        }
        switch (type) {
            case "regexp":
                final String regexp = ((TextNode) treeNode.get("exp")).asText();
                final Pattern pattern;
                try {
                    pattern = Pattern.compile(regexp);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unexpected regexp: " + regexp, e);
                }
                final TreeNode groupTreeNode = treeNode.get("group");
                final TreeNode charsetTreeNode = treeNode.get("charset");
                
                return new RegexpDetector(pattern,
                    groupTreeNode == null ? null : ((TextNode) groupTreeNode).asText(),
                    charsetTreeNode == null ? null : Charset.forName(((TextNode) charsetTreeNode).asText()));
            case "md5":
                return Md5Detector.getInstance();
            case "xpath":
                if (xPath == null) {
                    synchronized (this) {
                        if (xPath == null) {
                            xPath = XPathFactory.newInstance().newXPath();
                        }
                    }
                }
                final String exp = ((TextNode) treeNode.get("exp")).asText();
                final XPathExpression expression;
                try {
                    expression = xPath.compile(exp);
                } catch (XPathExpressionException e) {
                    throw new IllegalArgumentException("Unexpected xpath exp: " + exp, e);
                }
                return new XPathDetector(expression);
            default:
                throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }
}
