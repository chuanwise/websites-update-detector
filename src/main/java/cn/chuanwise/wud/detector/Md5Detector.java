package cn.chuanwise.wud.detector;

import org.slf4j.Logger;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;

/**
 * <h1>Hash 码探测器</h1>
 *
 * @author Chuanwise
 */
public class Md5Detector
    implements Detector {
    
    private static final MessageDigest MD5;
    static {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchElementException("当前平台缺少 MD5 算法");
        }
        MD5 = digest;
    }
    
    private static final Md5Detector INSTANCE = new Md5Detector();
    
    public static Md5Detector getInstance() {
        return INSTANCE;
    }
    
    private Md5Detector() {
    }
    
    private volatile byte[] cache;
    
    @Override
    public String detect(URL url, Logger logger) throws Exception {
        final byte[] prev = this.cache;
        final byte[] curr = MD5.digest(url.openConnection().getInputStream().readAllBytes());
        
        if (prev == null) {
            cache = curr;
            logger.info("初始化完成，初始 MD5：" + Base64.getEncoder().encodeToString(curr));
            return null;
        }
        if (Arrays.equals(prev, curr)) {
            logger.trace("没有发现变化");
            return null;
        }
        cache = curr;
        return "<ul>\n" +
            "<li><b>变化前</b>：" + Base64.getEncoder().encodeToString(prev) + "</li>\n" +
            "<li><b>变化后</b>：" + Base64.getEncoder().encodeToString(curr) + "</li>\n" +
            "</ul>";
    }
}
