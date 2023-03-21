package cn.chuanwise.wud;

import cn.chuanwise.wud.data.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.text.DateFormat;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebsitesUpdateDetectorMain {
    public static void main(String[] args) {
    
        final Logger logger = LoggerFactory.getLogger("main");
    
        final File workingDirectory;
        if (args.length == 0) {
            workingDirectory = new File(System.getProperty("user.dir"));
        } else if (args.length == 1) {
            workingDirectory = new File(args[0]);
        } else {
            logger.error("Params: [working-directory = ${user.dir}]");
            return;
        }
        
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        final File configurationFile = new File(workingDirectory, "configuration.yml");
        
        // save default configuration
        if (!configurationFile.isFile()) {
            final File parentFile = configurationFile.getParentFile();
            if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                throw new IllegalStateException("Can not create parent file: " + parentFile.getAbsolutePath());
            }
    
            try {
                if (!configurationFile.createNewFile()) {
                    throw new IOException("Create new file returns false!");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Can not create default configuration file", e);
            }
    
            try (final InputStream inputStream = WebsitesUpdateDetectorMain.class.getClassLoader().getResourceAsStream("configuration.yml");
                 final OutputStream outputStream = new FileOutputStream(configurationFile)) {
                
                if (inputStream == null) {
                    throw new IOException("Resource file configuration.yml doesn't exists!");
                }
                outputStream.write(inputStream.readAllBytes());
                
            } catch (IOException e) {
                throw new IllegalStateException("Can not save default configuration file", e);
            }
            
    
            logger.info("Default configuration file generated at " + configurationFile.getAbsolutePath() + ", " +
                "change it and restart program, please.");
            return;
        }
    
        final Configuration configuration;
        try {
            configuration = mapper.readValue(configurationFile, Configuration.class);
        } catch (IOException e) {
            throw new IllegalStateException("Can not load configuration file", e);
        }
    
        // check if settings is wrong
        if (configuration.getSmtp() == null) {
            logger.error("Smtp settings is empty, change configuration and restart program, please");
            return;
        }
        if (configuration.getWebsites().isEmpty()) {
            logger.error("Website settings is empty, change configuration and restart program, please");
            return;
        }
        if (configuration.getRandomMillisecondsScale() < 0) {
            logger.error("Random milliseconds scale is smaller than 0, change configuration and restart program, please");
        }
    
        // ready to start server
        final Random random = new Random();
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        
        class DetectorTask
            implements Runnable {
        
            private final String name;
            private final Configuration.Website website;
            private final Logger logger;
    
            private volatile int failCount = 0;
    
            public DetectorTask(String name, Configuration.Website website) {
                this.name = name;
                this.website = website;
                this.logger = LoggerFactory.getLogger(name);
            }
        
            @Override
            public void run() {
                final long period = website.getPeriod() + (random.nextLong() % configuration.getRandomMillisecondsScale());
    
                try {
                    final String message = website.getDetector().detect(website.getUrl(), logger);
    
                    if (message != null) {
                        logger.warn(website.getName() + " updated: ");
                        logger.warn(message);
    
                        final Configuration.Smtp smtp = configuration.getSmtp();
                        final Transport transport = smtp.getTransport();
                        try {
                            MimeMessage mimeMessage = new MimeMessage(smtp.getSession());
                            mimeMessage.setFrom(new InternetAddress(smtp.getEmail()));
        
                            // send message to all receipts
                            final String subject = website.getName() + "更新！";
                            final String content = "<h1>" + website.getName() + "出现更新！</h1>\n" +
                                "<h2>详细信息</h2>\n" +
                                "<p>" + message + "</p>\n" +
                                "<h2>查询状态</h2>\n" +
                                "<ul>\n" +
                                "<li><b>时间</b>：" + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + "</p></li>\n" +
                                "<li><b>网址</b>：<a href = \"" + website.getUrl() + "\">" + website.getName() + "</a></li>\n" +
                                "</ul>";
                            
                            for (String email : website.getEmails()) {
                                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
                                mimeMessage.setSubject(subject);
                                mimeMessage.setContent(content,"text/html;charset=UTF-8");
                                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                            }
                        } finally {
                            transport.close();
                        }
                    }
                } catch (Exception e) {
                    logger.error("探测" + website.getName() + "时出现异常", e);
                    synchronized (this) {
                        failCount++;
                    }
                }
    
                final int failCount;
                synchronized (this) {
                    failCount = this.failCount;
                }
                if (configuration.getMaxFailCount() != -1
                    && failCount >= configuration.getMaxFailCount()) {
                    
                    logger.error(name + "的探测失败次数 " + failCount + " 已达上限 " + configuration.getMaxFailCount() + "，停止对其探测");
    
                    final Configuration.Smtp smtp = configuration.getSmtp();
                    final Transport transport;
                    try {
                        transport = smtp.getTransport();
                    } catch (MessagingException e) {
                        logger.error("无法连接到 SMTP 服务器：" + smtp.getEmail(), e);
                        return;
                    }
                    
                    try {
                        final MimeMessage mimeMessage = new MimeMessage(smtp.getSession());
                        mimeMessage.setFrom(new InternetAddress(smtp.getEmail()));
        
                        // send message to all receipts
                        final String subject = "程序自动取消了对" + website.getName() + "的探测";
                        final String content = "<h1>" + website.getName() + " 探测失败次数已达上限</h1>\n" +
                            "<h2>查询状态</h2>\n" +
                            "<ul>\n" +
                            "<li><b>查询时间</b>：" + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + "</p></li>\n" +
                            "<li><b>网址</b>：<a href = \"" + website.getUrl() + "\">" + website.getName() + "</a></li>\n" +
                            "<li><b>失败次数</b>：" + failCount + "</li>\n" +
                            "</ul>";
        
                        for (String email : configuration.getEmails()) {
                            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
                            mimeMessage.setSubject(subject);
                            mimeMessage.setContent(content, "text/html;charset=UTF-8");
                            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                        }
                    } catch (Exception e) {
                        logger.error("发送提示消息失败", e);
                    } finally {
                        try {
                            transport.close();
                        } catch (MessagingException e) {
                            logger.error("关闭连接失败", e);
                        }
                    }
                } else {
                    logger.trace("next query time is " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis() + period) +
                        " ( after " + period + " milliseconds )");
                    service.schedule(this, period, TimeUnit.MILLISECONDS);
                }
            }
        }
        
        for (Map.Entry<String, Configuration.Website> entry : configuration.getWebsites().entrySet()) {
            final String name = entry.getKey();
            final Configuration.Website website = entry.getValue();
            
            service.submit(new DetectorTask(name, website));
        }
    }
}
