package cn.chuanwise.wud.detector;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <h1>XPath 探测器</h1>
 *
 * <p>根据 XPath 提取元素，并比对提取到的元素列表。</p>
 *
 * @author Chuanwise
 */
public class XPathDetector
    extends AbstractCollectionDetector<Node> {
    
    private final XPathExpression xpath;
    
    public XPathDetector(XPathExpression xpath) {
        Objects.requireNonNull(xpath, "XPath is null!");
        this.xpath = xpath;
    }
    
    @Override
    protected Collection<Node> detect0(URL url, Logger logger) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(url.openConnection().getInputStream());
    
        final NodeList list = (NodeList) xpath.evaluate(document, XPathConstants.NODESET);
        
        final int length = list.getLength();
        final List<Node> nodes = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            nodes.add(list.item(i));
        }
    
        return nodes;
    }
}
