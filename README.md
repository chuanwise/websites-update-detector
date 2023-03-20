# 网站更新探测器

因为需要关注多家院校的调剂信息和复试信息，手动查询效率低且麻烦，所以用 `Java` 编写了一个自动化查询工具，参照了 [zhongy1026/clawer](https://github.com/zhongy1026/clawer) 的创意。

## 功能

* 爬取网站公告并检查有无更新。若有更新则发送邮件。可以指定多个接收方和发送方。
* 爬取网站的周期可随机波动。

## 场景

* 校园公告，如调剂、复试等信息。
* 单位公告，如外出、放假等信息。

## 配置

### 配置文件

```yaml
# 网站设置
websites:
  # 可以检测多个网站，名字只要不重复即可
  xidian:
    # 在邮箱标题和正文中出现的网站名，可以重复
    name: website
    # 网站
    url: url
    # 检测器，用于获取网站信息
    detector:
      # 通过正则表达式检测，下面是一个例子
      type: regexp
      exp: target="_blank">(?<title>.+?)</a><span>
      group: title
    # 检测周期。
    # 实际检测周期为本周期加位于 [0, randomMillisecondsScale] 的一个随机值，单位毫秒
    period: 60000
    # 接收方邮箱列表。当网站出现更新，则这些人会收到邮件
    emails:
      # 你的邮箱
      - you@domain
      
# smtp 设置，用于发送服务器
smtp:
  # smtp 服务器地址和端口，如 QQ smtp 服务器（SSL）
  host: smtp.qq.com
  port: 465
  # smtp 邮箱，用于发送邮件
  email: you@domain
  # 授权码，为 null 时表示无需登录
  auth: auth
  # 是否输出调试信息
  debug: true

# 查询周期随机扰动值
randomMillisecondsScale: 180000

# 查询失败多少次后取消查询任务
maxFailCount: 1

emails:
  - you@domain
```

### 探测器

#### 正则提取探测器

此探测器需要给定正则表达式，探测器将网站内容中所有符合正则表达式的部分提取出来形成集合，并比对集合差异。若出现变动，则判定为出现更新。此探测器适合用于检测网站公告等信息的变动。

例如，这是检测网站所有正整数的探测器，若出现变化则认为网站更新：

```yaml
detector:
  type: regexp
  exp: \d+
```

正则表达式中可以出现 `(?<name>exp)` 定义变量，并用 `group` 属性说明需要注意的变量名。例如：

```yaml
detector:
  type: regexp
  exp: target="_blank">(?<title>.+?)</a><span>
  group: title
```

这样每次探测便会提取变量 `title` 并检查它的集合。

#### `XPath` 提取探测器

此探测器和正则提取探测器很类似，通过 `XPath` 表达式提取节点形成集合，并比对集合差异。若出现变动，则判定为出现更新。此探测器适合用于检测网站公告等信息的变动。

```yaml
detector:
  type: xpath
  exp: /root
```

上述例子将检查根部的 `root` 节点并检查差异。

#### `MD5` 探测器

此探测器将会在每次获取网站内容并计算 `MD5`，若前后两次计算结果不一致即判定为出现更新。请慎用此探测器，因为部分网站可能存在时间、访问次数等统计，可能导致每次网站的 `MD5` 值都不同。

探测器设置：

```yaml
detector:
  type: md5
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