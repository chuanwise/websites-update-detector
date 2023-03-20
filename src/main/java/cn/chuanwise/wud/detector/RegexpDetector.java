package cn.chuanwise.wud.detector;

import org.slf4j.Logger;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>正则表达式探测器</h1>
 *
 * @author Chuanwise
 */
public class RegexpDetector
    extends AbstractCollectionDetector<String> {
    
    private final Charset charset;
    private final String group;
    private final Pattern pattern;
    
    public RegexpDetector(Pattern pattern, String group, Charset charset) {
        Objects.requireNonNull(pattern, "Pattern is null!");
        
        this.pattern = pattern;
        this.group = group;
        this.charset = charset == null ? Charset.defaultCharset() : charset;
    }
    
    @Override
    protected Collection<String> detect0(URL url, Logger logger) throws Exception {
        final List<String> list = new ArrayList<>();
    
        final String content = new String(url.openConnection().getInputStream().readAllBytes(), charset);
        final Matcher matcher = pattern.matcher(content);
        if (group == null) {
            while (matcher.find()) {
                list.add(content.substring(matcher.start(), matcher.end()));
            }
        } else {
            while (matcher.find()) {
                list.add(matcher.group(group));
            }
        }
    
        return list;
    }
}
