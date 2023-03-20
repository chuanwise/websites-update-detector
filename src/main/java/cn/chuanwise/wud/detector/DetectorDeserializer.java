package cn.chuanwise.wud.detector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public class DetectorDeserializer
    extends JsonDeserializer<Detector> {
    
    private static final DetectorDeserializer INSTANCE = new DetectorDeserializer();
    
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
                final String regexp = ((TextNode) treeNode.get("regexp")).asText();
                final Pattern pattern = Pattern.compile(regexp);
                final String group = ((TextNode) treeNode.get("group")).asText();
                final TreeNode charsetTreeNode = treeNode.get("charset");
                
                return new RegexpDetector(pattern, group,
                    charsetTreeNode == null ? null : Charset.forName(((TextNode) charsetTreeNode).asText()));
            default:
                throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }
}
