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
    # smtp 服务器地址和端口，如 QQ smtp 服务器
    host: smtp.qq.com
    port: 587
    # smtp 邮箱，用于发送邮件
    email: your-qq@qq.com
    # 授权码，为 null 时表示无需登录
    authCode: your-qq-email-smtp-code
    # 是否开启 SSL
    ssl: true
    # 是否输出调试信息
    debug: true

# 查询周期随机扰动值
randomMillisecondsScale: 180000

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

### 通过 Docker 部署

### 直接部署

下载最新的 RELEASE