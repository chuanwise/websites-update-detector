# 网站更新探测器

因为需要关注多家院校的调剂信息和复试信息，手动查询效率低且麻烦，所以用 `Java` 编写了一个自动化查询工具，参照了 [zhongy1026/clawer](https://github.com/zhongy1026/clawer) 的创意。

## 功能

* 爬取网站公告并检查有无更新。若有更新则发送邮件。可以指定多个接收方和发送方。
* 爬取网站的周期可随机波动。

## 场景

* 校园公告，如调剂、复试等信息。
* 单位公告，如外出、放假等信息。

## 配置

```yaml
# smtp 设置，用于发送服务器
smtp:
  # name 只要不重复即可，无实际作用
  name:
    # smtp 服务器地址和端口，如 QQ smtp 服务器（SSL）
    host: smtp.qq.com
    port: 465
    # smtp 邮箱，用于发送邮件
    email: your-qq@qq.com
    # 授权码，为 null 时表示无需登录
    authCode: your-qq-email-smtp-code
    # 是否输出调试信息
    debug: true

# 查询周期随机扰动值
randomMillisecondsScale: 180000

# 查询失败多少次后取消查询任务
maxFailCount: 1

# 网站设置
websites:
  # 可以检测多个网站，名字只要不重复即可
  xidian:
    # 在邮箱标题和正文中出现的网站名，可以重复
    name: website-name
    # 网站
    url: <url>
    # 检测器，用于获取网站信息
    detector:
      # 通过正则表达式检测，下面是一个例子
      type: regexp
      regexp: target="_blank">(?<title>.+?)</a><span>
      group: title
    # 检测周期。
    # 实际检测周期为本周期加位于 [0, randomMillisecondsScale] 的一个随机值，单位毫秒
    period: 60000
    # 接收方邮箱列表。当网站出现更新，则这些人会收到邮件
    emails:
      # 你的邮箱
      - <your email>
```

## 快速开始

### 直接部署

下载最新的 [RELEASE](https://github.com/Chuanwise/websites-update-detector/releases/) ，在服务器创建一个文件夹作为工作目录，将 `Jar` 文件放入其中。

创建文件 `java.security`，内容填写：

```
jdk.tls.disabledAlgorithms=SSLv3, TLSv1.1, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, include jdk.disabled.namedCurves
```

创建启动脚本，内容为：

```shell
java -Djava.security.properties=${path}/java.security -jar ${jar}
```

（其中 `=` 后面填写绝对路径）。

随后启动程序即可。根据程序的提示，修改 `configuration.yml` 里有关邮箱、网站的设置后重启程序即可。