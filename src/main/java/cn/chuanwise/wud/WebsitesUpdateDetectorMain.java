package cn.chuanwise.wud;

import cn.chuanwise.wud.data.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.text.DateFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebsitesUpdateDetectorMain {
    public static void main(String[] args) {
        final File workingDirectory;
        if (args.length == 0) {
            workingDirectory = new File(System.getProperty("user.dir"));
        } else if (args.length == 1) {
            workingDirectory = new File(args[0]);
        } else {
            System.err.println("Params: [working-directory]");
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
            
    
            System.out.println("Default configuration file generated at " + configurationFile.getAbsolutePath() + ", " +
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
        if (configuration.getSmtp().isEmpty()) {
            System.err.println("Smtp settings is empty, change configuration and restart program, please");
            return;
        }
        if (configuration.getWebsites().isEmpty()) {
            System.err.println("Website settings is empty, change configuration and restart program, please");
            return;
        }
        if (configuration.getRandomMillisecondsScale() < 0) {
            System.err.println("Random milliseconds scale is smaller than 0, change configuration and restart program, please");
        }
    
        // ready to start server
        final Random random = new Random();
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        
        class DetectorTask
            implements Runnable {
        
            private final String name;
            private final Configuration.Website website;
            private volatile int times = 0;
            
            private volatile Object cache;
    
            public DetectorTask(String name, Configuration.Website website) {
                this.name = name;
                this.website = website;
            }
        
            @Override
            public void run() {
                final int times;
                synchronized (this) {
                    times = this.times;
                    this.times++;
                }
    
                final long period = website.getPeriod() + (random.nextLong() % configuration.getRandomMillisecondsScale());
    
                if (times == 0) {
                    try {
                        info("Initialization...");
                        
                        final Object initCache = website.getDetector().detect(website.getUrl());
                        synchronized (this) {
                            cache = initCache;
                        }
                        
                        info("Initial value is " + initCache + ", " +
                            "next query time is " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis() + period) +
                            " ( after " + period + " milliseconds ), listening...");
                    } catch (Exception e) {
                        error("Error occurred when initial detecting " + name);
                        e.printStackTrace();
                        info("next query time is " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis() + period) +
                            " ( after " + period + " milliseconds ), listening...");
                        
                    }
                } else {
                    final Object currCache;
                    final Object prevCache;
                    synchronized (this) {
                        prevCache = this.cache;
                    }
                    
                    try {
                        info("Query...");
                        currCache = website.getDetector().detect(website.getUrl());
    
                        if (Objects.equals(currCache, prevCache)) {
                            info("No differences. next query time is " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis() + period) +
                                " ( after " + period + " milliseconds ), listening...");
                        } else {
                            warn("Difference occurred! Previous value is " + prevCache + ", current value is " + currCache + ", post message!");
                            for (Map.Entry<String, Configuration.Smtp> entry : configuration.getSmtp().entrySet()) {
                                final String name = entry.getKey();
                                final Configuration.Smtp smtp = entry.getValue();
    
                                final Transport transport = smtp.getTransport();
                                try {
                                    MimeMessage mimeMessage = new MimeMessage(smtp.getSession());
                                    mimeMessage.setFrom(new InternetAddress(smtp.getEmail()));
                                    
                                    // send message to all receipts
                                    final String subject = "[重要] " + name + " 网站出现更新！";
                                    final String content = "<h1>" + name + " 网站出现更新！</h1>\n" +
                                        "<h2>查询状态</h2>\n" +
                                        "<ul>\n" +
                                        "<li><b>查询时间</b>：" + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + "</p></li>\n" +
                                        "<li><b>网址</b>：" + website.getUrl() + "</li>\n" +
                                        "</ul>\n" +
                                        "<h2>前后变化</h2>\n" +
                                        "<p>更新前：" + prevCache + "</p>\n" +
                                        "<p>更新后：" + currCache + "</p>";
                                    
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
                        }
                    } catch (Exception e) {
                        error("Error occurred when detecting " + name);
                        e.printStackTrace();
                        info("next query time is " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis() + period) +
                            " ( after " + period + " milliseconds ), listening...");
                    }
                }
                
                service.schedule(this, period, TimeUnit.MILLISECONDS);
            }
            
            private void info(String content) {
                System.out.println(DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + " : " + name + " : INFO : " + content);
            }
            
            private void warn(String content) {
                System.out.println(DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + " : " + name + " : WARN : " + content);
            }
            
            private void error(String content) {
                System.err.println(DateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + " : " + name + " : ERROR : " + content);
            }
        }
        
        for (Map.Entry<String, Configuration.Website> entry : configuration.getWebsites().entrySet()) {
            final String name = entry.getKey();
            final Configuration.Website website = entry.getValue();
            
            service.submit(new DetectorTask(name, website));
        }
    }
}
