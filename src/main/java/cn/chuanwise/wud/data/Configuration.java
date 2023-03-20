package cn.chuanwise.wud.data;

import cn.chuanwise.wud.detector.Detector;
import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <h1>配置信息</h1>
 *
 * @author Chuanwise
 */
public class Configuration {
    
    /**
     * 网站设置
     */
    public static class Website {
        private final String name;
        private final URL url;
        private final Detector detector;
        private final long period;
        private final Set<String> emails;
    
        // for deserializer
        private Website() {
            this.name = null;
            this.url = null;
            this.detector = null;
            this.period = 0;
            this.emails = null;
        }
    
        public Website(String name, URL url, Detector detector, long period, Set<String> emails) {
            Objects.requireNonNull(name, "Name is null!");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name is empty!");
            }
            Objects.requireNonNull(url, "URL is null!");
            Objects.requireNonNull(detector, "Detector is null!");
            Objects.requireNonNull(emails, "Emails is null!");
            if (period <= 0) {
                throw new IllegalArgumentException("Period must be greater than 0!");
            }
            
            this.name = name;
            this.url = url;
            this.detector = detector;
            this.period = period;
            this.emails = emails;
        }
    
        public Set<String> getEmails() {
            return emails;
        }
    
        public String getName() {
            return name;
        }
    
        public URL getUrl() {
            return url;
        }
    
        public Detector getDetector() {
            return detector;
        }
    
        public long getPeriod() {
            return period;
        }
    }
    
    private final Map<String, Website> websites = new HashMap<>();
    
    /**
     * SMTP 服务器设置
     */
    public static class Smtp {
        private final String host;
        private final int port;
        private final String email;
        private final String authCode;
        private final boolean debug;
        
        private volatile Session session;
        
        // for deserializer
        private Smtp() {
            this.host = null;
            this.port = 0;
            this.email = null;
            this.authCode = null;
            this.debug = false;
        }
    
        public Smtp(String host, int port, String email, String authCode, boolean debug) {
            Objects.requireNonNull(host, "Host is null!");
            Objects.requireNonNull(email, "Email is null!");
            if (host.isEmpty()) {
                throw new IllegalArgumentException("Host is empty!");
            }
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Protocol is empty!");
            }
            if (port <= 0) {
                throw new IllegalArgumentException("Unexpected port: " + port);
            }
            
            this.host = host;
            this.port = port;
            this.email = email;
            this.authCode = authCode;
            this.debug = debug;
        }
    
        public String getHost() {
            return host;
        }
    
        public int getPort() {
            return port;
        }
    
        public String getEmail() {
            return email;
        }
    
        public String getAuthCode() {
            return authCode;
        }
    
        public boolean isDebug() {
            return debug;
        }
    
        public boolean isAuth() {
            return authCode != null;
        }
    
        public Session getSession() {
            if (session == null) {
                synchronized (this) {
                    if (session == null) {
                        final Properties properties = new Properties();
                        properties.put("mail.transport.protocol", "smtp");
                        properties.put("mail.smtp.host", host);
                        properties.put("mail.smtp.port", 465);
                        properties.put("mail.smtp.auth", isAuth());
                        properties.put("mail.smtp.ssl.enable", true);
                        properties.put("mail.debug", debug);
    
                        final MailSSLSocketFactory sf;
                        try {
                            sf = new MailSSLSocketFactory();
                        } catch (GeneralSecurityException e) {
                            throw new IllegalStateException("Can not create mail ssl socket factory");
                        }
                        sf.setTrustAllHosts(true);
                        properties.put("mail.smtp.ssl.enable", "true");
                        properties.put("mail.smtp.ssl.socketFactory", sf);
                        
                        final Authenticator authenticator = new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(email, authCode);
                            }
                        };
    
                        session = Session.getInstance(properties, authenticator);
                    }
                }
            }
            return session;
        }
    
        public Transport getTransport() throws MessagingException {
            final Transport transport = getSession().getTransport();
            transport.connect(host, email, authCode);
            return transport;
        }
    }
    
    /**
     * 随机周期波动范围
     */
    private final long randomMillisecondsScale = TimeUnit.MINUTES.toMicros(3);
    
    private final int maxFailCount = 1;
    
    private final Set<String> emails = new HashSet<>();
    
    private final Map<String, Smtp> smtp = new HashMap<>();
    
    public int getMaxFailCount() {
        return maxFailCount;
    }
    
    public Set<String> getEmails() {
        return emails;
    }
    
    public long getRandomMillisecondsScale() {
        return randomMillisecondsScale;
    }
    
    public Map<String, Website> getWebsites() {
        return websites;
    }
    
    public Map<String, Smtp> getSmtp() {
        return smtp;
    }
}
