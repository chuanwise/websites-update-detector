package cn.chuanwise.wud.detector;

import org.slf4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h1>集合探测器</h1>
 *
 * <p>每次探测得到集合，比对集合元素是否变化的探测器</p>
 *
 * @author Chuanwise
 */
public abstract class AbstractCollectionDetector<T>
    implements Detector {
    
    private volatile Collection<T> cache;
    
    @Override
    public final String detect(URL url, Logger logger) throws Exception {
        final Collection<T> prev = cache;
        final Collection<T> curr = detect0(url, logger);
    
        this.cache = curr;
        if (prev == null) {
            cache = curr;
    
            if (curr.isEmpty()) {
                logger.info("初始化完成，初值为空");
            } else {
                logger.info("初始化完成，初值：" + cache.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining("\n + ")));
            }
            return null;
        }
        if (Objects.equals(curr, prev)) {
            logger.debug("没有发现变化");
            return null;
        }
        
        final List<String> changes = new ArrayList<>();
    
        // removed elements
        final List<T> remove = new ArrayList<>(prev);
        remove.removeAll(curr);
        if (!remove.isEmpty()) {
            changes.add("删除（" + remove.size() + "）项：\n" + remove.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("\n - ")));
        }
        
        // add elements
        final List<T> add = new ArrayList<>(curr);
        add.removeAll(prev);
        if (!add.isEmpty()) {
            changes.add("新增（" + add.size() + "）项：\n" + add.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("\n + ")));
        }
    
        return "<ul>\n" + changes.stream()
            .map(s -> "<li>" + s + "</li>")
            .collect(Collectors.joining("\n"))
            + "</ul>";
    }
    
    protected abstract Collection<T> detect0(URL url, Logger logger) throws Exception;
}
