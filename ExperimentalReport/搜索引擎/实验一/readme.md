# 实验目的
+ 熟悉常用的搜索引擎
+ 熟练使用搜索引擎检索信息
+ 掌握爬虫实现的基本原理
+ 掌握主题爬虫的实现技术
+ 掌握动态页面的采集技术
+ 掌握深度页面的采集技术
# 实验内容

## 一、网络爬虫的基本原理

### 1. Web 服务器连接器

#### DNS 缓存

使用python 第三方库 ``dnspython`` 可以实现对任意域名的DNS解析，在对网页的爬取过程中，为了减小每次爬取时对域名DNS的解析这一过程的网络消耗，可以预处理出所有的域名的实际ip地址，完成一个对域名解析的DNS缓存，可以使用列表的另一个类似的容器：字典实现（当然也可以自己用列表实现字典），具体的代码如下：

```python
# -*- coding: UTF-8 -*-
# 实现对域名和ip的维护，即DNS缓存
# 实现思路：用一个字典来维护即可

import dns.resolver

class DNSCache:
    'DNS缓存类'
    __domain_ip_table = {}

    def __init__(self):
        self.__domain_ip_table = {}

    def dns_parse(self, domain):
        try:
            ret = dns.resolver.query(domain, "A")
            return ret.response.answer[0].items[0].address
        except Exception as e:
            print("dns parse errror!!!" + str(e))
            # self.dns_parse(domain)
            return None

    def dns_push(self, domain):
        try:
            # if(self.__domain_ip_table.has_key(domain) == True):
            if((domain in self.__domain_ip_table) == False):
                ip = self.dns_parse(domain)
                if(ip != None):
                    self.__domain_ip_table[domain] = ip
                    print(str(domain) + " has push in dns cache...")
                else:
                    print(str(domain) + " has not push in dns cache...")
                return
        except Exception as e:
            print("dns push error!!! " + str(e))
            print(23333)
    
    def dns_get(self, domain):
        try:
            # if(self.__domain_ip_table.has_key(domain) == True):
            if(domain in self.__domain_ip_table):
                return self.__domain_ip_table[domain]
        except Exception as identifier:
            print("no this " + str(domain) + " cache..")
            self.dns_push(domain)
            self.dns_get(domain)


# test
dnscache = DNSCache()
domains = ["baidu.com", "bilibili.com", "google.com"]
for domain in domains:
    dnscache.dns_push(domain)
print("")
for domain in domains:
    print(domain + "'s ip is " + dnscache.dns_get(domain))

```

运行的测试结果如下：
![](https://img-blog.csdnimg.cn/20200607171721420.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3BpMzE0MTU5MjY1MzV4,size_16,color_FFFFFF,t_70)

#### Robots文件解析

当把模拟 ``useragent`` 换成 ``Baiduspider`` 时将返回 **不允许抓取** 的信息，这是因为在想要爬取的网站的Robots.txt中显式的表明不允许以百度爬虫进行爬取：![](https://img-blog.csdnimg.cn/20200607172434337.png)

#### 错误和异常处理

+ 对于前一个网址的访问，程序返回的是 **404，页面不存在** ，因为该请求文件不存在
+ 对于后一个网址的访问，程序返回的是 **Error** 请求错误，url中没有指定访问的方式，添加 ``https://`` 既可以正常访问，返回200

### 2. 超链接（URL）提取和过滤

#### URL 提取方法

为了能够提取出所有的超链接，可以将正则表达式中的 ``"http://`` 去掉，即将匹配的规则扩大到所有 a标签中的 href 属性的值，对于那些相对链接，可以在已知当前页面地址的情况下，使用 ``urljoin`` 来实现相对地址向绝对地址的转换。


### 3. 爬行策略搜索

通过整合上面各个模块，分别使用dfs和bfs实现一个爬虫：

#### 公共模块部分
##### RobotsParse.py 对网站robots.txt的解析

```python
# -*- coding: UTF-8 -*-

import urllib
import urllib.robotparser
import requests

rp = urllib.robotparser.RobotFileParser()
# useragent = 'Baiduspider'
useragent='Googlebot'

def init(url):
    proto, rest = urllib.request.splittype(url)
    res, rest = urllib.request.splithost(rest)
    url = "https://" + res + "/robots.txt"
    print(url)
    rp.set_url(url)
    rp.read()

def IsCanFetch(url):
    return rp.can_fetch(useragent, url)


if __name__ == '__main__':
    init("https://www.runoob.com/robots.txt")
    if(IsCanFetch("https://www.runoob.com/python/python-lists.html")):
        print("ohhhhhhh")
    else:
        print("emmmm")
```


##### URLRequest.py 对指定url的抓取

对于这个项目，仅爬取所有的html文件，故对其他二进制文件将不会请求下载，可以通过 ``FileDownload `` 来控制对文件的下载。

```python
# -*- coding: UTF-8 -*-
# 对于给定的一个链接进行爬取操作，返回爬取到的页面信息

from urllib import request as re
import requests
from requests.exceptions import ReadTimeout, ConnectionError, RequestException

FileDownload = False    # 是否请求非html文件
header = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36'}
def URLRequest(urls):
    """
    urls: 要爬取的页面的链接
    当爬取成功时将返回爬取的页面
    """

    try:
        # 这里不能直接去get链接，有可能时文件下载链接，会下载完后才处理
        # request = requests.get(url=urls, headers=header, timeout=10)
        request = re.urlopen(urls, timeout=10)
        status_code = request.getcode()
    except ReadTimeout:
        # 超时异常
        print('Timeout')
    # 需要把当前的 url 放到任务中，过一段时间再尝试连接
    except ConnectionError:
        # 连接异常
        print('Connection error')
        return None
    except RequestException:
        # 请求异常
        print('Error')
        return None
    except Exception as e:
        print("URLRequest error!!! " + str(e))
    else:
        if(status_code==200):
            print('访问正常！')

            # 根据请求头判断请求的文件类型，对于web文件将返回utf-8编码的内容
            # 对于其他二进制文件，根据设定返回
            if(request.info()["Content-Type"] == "text/html"):
                return request.read().decode('utf-8')
            elif(FileDownload == True):
                return requests.get(url=urls, headers=header, timeout=10).content
            else:
                return None
        if(status_code==404):
            print('页面不存在！')
            return None
        if(status_code==403):
            print('页面禁止访问！')
            return None

# test
if __name__ == '__main__':
    URLRequest("https://www.baidu.com/")
```

##### HTMLParse.py 对给定的html页面进行分析，获得所有超链接
使用正则表达式： ``<[a|A] href="[a-zA-Z0-9/\.\-:_]+`` 提取出所有的a标签后的超链接，剔除不符合要求的链接，并对相对链接进行拼接得到绝对链接。

```python
# -*- coding: UTF-8 -*-
# 将给定的一个HTNL页面解析出所有的超链接

import re
from urllib.parse import urljoin

def HTMLParse(url, html):
    """
    url: 页面的链接，方便相对路径的解析
    html: 爬取的页面
    返
    回解析到的所有超链接列表，此处仅解析 a 标签中的超链接，包括相对链接
    """
    urls = re.findall('<[a|A] href="[a-zA-Z0-9/\.\-:_]+"', html)
    # print(urls)
    Len = len(urls)
    if(Len == 0):
        return None
    for i in range(Len):
        # print(urls[i])
        urls[i] = urls[i][urls[i].find("href=\"") + 6: -1]
        # print(urls[i])
        if(urls[i].find("javascript:void(0)") != -1):
            continue
        if(urls[i].find("http") == -1):
            # print(url + "-----" + urls[i])
            urls[i] = urljoin(url, urls[i])

        if(urls[i][len(urls[i]) - 1] == '/'):
            urls[i] = urls[i][0: -1]
        
        # if(urls[i].find("http://") != -1):
        #     urls[i] = "https://" + urls[i][7::]
        # print(urls[i])
        # print("")
    return urls

if __name__ == '__main__':
    import URLRequest
    # html = URLRequest.URLRequest("https://baidu.com")
    # print(html)
    # print("-----")
    # HTMLParse("https://baidu.com", html)

    # s='''<li><a href="http://news.sina.com.cn/o/2018-11-06/a75.shtml"target="_blank">进博会</a></li><li><a href="http://news.sina.com.cn/o/2018-11-06/a76.shtml"target="_blank">大数据</a></li><li><a href="/o/2018-11-06/a75.shtml" target="_blank">进博会</a></li>'''
    # HTMLParse("https://news.sina.com.cn/", s)
    html = URLRequest.URLRequest("https://www.runoob.com/w3cnote/ten-sorting-algorithm.html/")
    # print(html)
    print("")
    # HTMLParse("https://www.runoob.com/w3cnote/ten-sorting-algorithm.html/", html)
    for u in HTMLParse("https://www.runoob.com/w3cnote/ten-sorting-algorithm.html/", html):
        print(u)

```

#### DFSspider.py 深度优先搜索爬虫

使用深搜的思想，爬取页面的所有链接：

```python
# -*- coding: UTF-8 -*-
# 深度优先搜索DFS爬虫

import RobotsParse
import URLRequest
import HTMLParse


urls = []    # 最后爬过的链接，这里没有保存每次爬取到的页面信息，可以同步开一个保存页面信息的列表用于其他分析
pagenum = 0 # 要爬取的页面数量
ISCHECKROBOT = True     # 是否检查robots.txt


def init(_URL, _NUM, _ISCHECKROBOT):
    """
    _URL: 深搜的起始点
    _NUM: 要爬取的页面个数
    _ISCHECKROBOT: 是否要检查robots.txt
    """
    global pagenum
    global ISCHECKROBOT
    pagenum = _NUM
    ISCHECKROBOT = _ISCHECKROBOT
    urls = []
    urls.append(_URL)
    dfs(_URL)

def dfs(url):
    global pagenum
    print(str(pagenum) + ": " + url)
    if(pagenum == 0):
        return
    
    if(ISCHECKROBOT == True):
        if(RobotsParse.IsCanFetch(url) == False):
            return
    
    html = URLRequest.URLRequest(url)
    if(html == None):
        return
    temp_urls = HTMLParse.HTMLParse(url, html)
    pagenum = pagenum - 1
    # 保存爬取到的结果
    urls.append(url)

    if(temp_urls == None):
        return

    # 去重
    tmp = []
    for u in temp_urls:
        if(urls.count(u) == 0 and tmp.count(u) == 0):
            tmp.append(u)
    
    # 递归进行dfs
    for u in tmp:
        if(pagenum == 0):
            return
        if(urls.count(u) == 0):
            dfs(u)




# test
if __name__ == '__main__':
    init("https://www.runoob.com/python/python-lists.html", 20, False)
    for i in urls:
        print(i)
```

#### BFSspider.py 广度优先搜索爬虫

使用广搜的思想进行爬取网页：

```python
# -*- coding: UTF-8 -*-
# 广度优先搜索BFS

import RobotsParse
import URLRequest
import HTMLParse

urls = []   # 最后爬取到的链接，作为一个队列使用
pagenum = 0 # 要爬取到的页面数量
ISCHECKROBOT = True # 是否检查robots.txt

def init(_URL, _NUM, _ISCHECKROBOT):
    """
    _URL: 深搜的起始点
    _NUM: 要爬取的页面个数
    _ISCHECKROBOT: 是否要检查robots.txt
    """
    global pagenum
    global ISCHECKROBOT
    global urls
    pagenum = _NUM
    ISCHECKROBOT = _ISCHECKROBOT
    urls = []
    urls.append(_URL)
    bfs()

def bfs():
    global urls
    urls_i = 0  # 队列头指针
    while(urls_i < len(urls) and urls_i < pagenum):
        url = urls[urls_i]
        print(str(urls_i) + ": " + url)
        urls_i += 1
        
        if(ISCHECKROBOT == True):
            if(RobotsParse.IsCanFetch(url) == False):
                continue
        
        html = URLRequest.URLRequest(url)
        if(html == None):
            continue
        temp_urls = HTMLParse.HTMLParse(url, html)
        if(temp_urls == None):
            continue
        
        if(len(urls) > pagenum):
            continue
        # 去重
        tmp = []
        for u in temp_urls:
            if(urls.count(u) == 0 and tmp.count(u) == 0):
                tmp.append(u)
                
        # 入队
        urls.extend(tmp)
    urls = urls[0: pagenum]


# test
if __name__ == '__main__':
    init("https://www.runoob.com/python/python-lists.html", 20, False)
    for i in urls:
        print(i)
```

#### __mian__.py 项目入口 

初始化测试等必要的参数，分别调用深搜和广搜的爬虫对同一网站进行爬取页面：

```python
# -*- coding: UTF-8 -*-

import RobotsParse
import DFSspider
import BFSspider

PAGEURL = ""    # 要爬取的页面的网址
PAGENUM = 50    # 要爬取的最多的页面个数，默认是50个
ISCHECKROBOTS = True    # 是否在爬取时检查robots.txt，对于一些网站可能都没有这个文件所以可以选择不检查

if __name__ == '__main__':
    PAGEURL = input("要爬取的网址: ")
    PAGENUM = input("要爬取的页面个数: ")
    if((PAGEURL.find("http://") == -1) and (PAGEURL.find("https://") == -1)):
        PAGEURL = "https://" + PAGEURL
    ch = input("是否在爬取时检查robots.txt.[Y/n]: ")
    if(ch == 'Y'):
        ISCHECKROBOTS = True
        RobotsParse.init(PAGEURL)
    else:
        ISCHECKROBOTS = False

    print("======================= dfs ===========================")
    DFSspider.init(PAGEURL, int(PAGENUM), ISCHECKROBOTS)
    for i in range(len(DFSspider.urls)):
        print(str(i) + ": " + DFSspider.urls[i])
    print("")
    print("")
    print("")

    print("======================= bfs ===========================")
    BFSspider.init(PAGEURL, int(PAGENUM), ISCHECKROBOTS)
    for i in range(len(BFSspider.urls)):
        print(str(i) + ": " + BFSspider.urls[i])
    print("")
    print("")
    print("")
```

#### 测试结果

```text
PS G:\Backup\CollegeProjectBackup\ExperimentalReport\搜索引擎
\DFSandBFSspider> python -u "g:\Backup\CollegeProjectBackup\ExperimentalReport\搜索引擎\DFSandBFSspider\__mian__.py"      
要爬取的网址: www.hzau.edu.cn
要爬取的页面个数: 50
是否在爬取时检查robots.txt.[Y/n]: n
======================= dfs ===========================
50: https://www.hzau.edu.cn
访问正常！
49: http://211.69.143.162
URLRequest error!!! <urlopen error [WinError 10061] 由于目标
计算机积极拒绝，无法连接。>
49: http://mail.hzau.edu.cn
访问正常！
48: http://center.hzau.edu.cn/info/1072/1676.htm
访问正常！
47: http://center.hzau.edu.cn
访问正常！
46: http://center.hzau.edu.cn/index.htm
访问正常！
45: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
45: http://pdc.hzau.edu.cn
访问正常！
45: http://center.hzau.edu.cn/zxgk.htm
访问正常！
44: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
44: http://pdc.hzau.edu.cn
访问正常！
44: http://center.hzau.edu.cn/zxgk/zxjj.htm
访问正常！
43: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
43: http://pdc.hzau.edu.cn
访问正常！
43: http://center.hzau.edu.cn/zxgk/jgsz.htm
访问正常！
42: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
42: http://pdc.hzau.edu.cn
访问正常！
42: http://center.hzau.edu.cn/fwzn.htm
访问正常！
41: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
41: http://pdc.hzau.edu.cn
访问正常！
41: http://center.hzau.edu.cn/fwzn/xywfw.htm
访问正常！
40: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
40: http://pdc.hzau.edu.cn
访问正常！
40: http://center.hzau.edu.cn/fwzn/xykfw.htm
访问正常！
39: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
39: http://pdc.hzau.edu.cn
访问正常！
39: http://center.hzau.edu.cn/fwzn/jxfw.htm
访问正常！
38: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
38: http://pdc.hzau.edu.cn
访问正常！
38: http://center.hzau.edu.cn/fwzn/xxfw.htm
访问正常！
37: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
37: http://pdc.hzau.edu.cn
访问正常！
37: http://center.hzau.edu.cn/gzzd.htm
访问正常！
36: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
36: http://pdc.hzau.edu.cn
访问正常！
36: http://center.hzau.edu.cn/xzzq.htm
访问正常！
35: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
35: http://pdc.hzau.edu.cn
访问正常！
35: http://center.hzau.edu.cn/lxwm.htm
访问正常！
34: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
34: http://pdc.hzau.edu.cn
访问正常！
34: http://center.hzau.edu.cn/info/1052/1381.htm
访问正常！
33: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
33: http://pdc.hzau.edu.cn
访问正常！
33: http://center.hzau.edu.cn/ewm.htm
访问正常！
32: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
32: http://pdc.hzau.edu.cn
访问正常！
32: http://center.hzau.edu.cn/gzzd/1.htm
访问正常！
31: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
31: http://pdc.hzau.edu.cn
访问正常！
31: http://center.hzau.edu.cn/fwzn/1.htm
访问正常！
30: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
30: http://pdc.hzau.edu.cn
访问正常！
30: http://center.hzau.edu.cn/info/1018/1074.htm
访问正常！
29: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
29: http://pdc.hzau.edu.cn
访问正常！
29: http://zizhu.hzau.edu.cn
访问正常！
29: http://center.hzau.edu.cn/info/1018/1071.htm
访问正常！
28: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
28: http://pdc.hzau.edu.cn
访问正常！
28: http://zizhu.hzau.edu.cn
访问正常！
28: http://center.hzau.edu.cn/info/1072/1670.htm
URLRequest error!!! HTTP Error 404: Not Found
28: http://center.hzau.edu.cn/info/1072/1671.htm
访问正常！
27: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
27: http://pdc.hzau.edu.cn
访问正常！
27: http://i.hzau.edu.cn
URLRequest error!!! <urlopen error [Errno 11001] getaddrinfo 
failed>
27: http://center.hzau.edu.cn/info/1018/1638.htm
访问正常！
26: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
26: http://pdc.hzau.edu.cn
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-5.0.0-12606690.exe
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-AndroidOS-arm-5.0.0-12557366.apk
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-5.0.0-12557381.dmg
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-5.0.0-12557422.x64.bundle
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-5.0.0-12557422.x86.bundle
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-4.10.0-11021086.exe
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-x86_64-4.2.0-4336726.exe
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-Android-arm-4.10.0-11266069.apk
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonclient/VMware-Horizon-Client-4.10.0-11013656.dmg
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/win_install_client.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/macos_install_client.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/Android_install.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/ios.mp4    
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/horizon.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/win_install_oldclient.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/filemove.mp4
访问正常！
26: https://desktop.hzau.edu.cn:8080/horizonvideo/fileshare.mp4
访问正常！
26: http://center.hzau.edu.cn/info/1018/1067.htm
访问正常！
25: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
25: http://pdc.hzau.edu.cn
访问正常！
25: http://center.hzau.edu.cn/info/1018/1280.htm
访问正常！
24: http://www.hzau.edu.cn/2014/ch
URLRequest error!!! HTTP Error 404: Not Found
24: http://pdc.hzau.edu.cn
访问正常！
24: http://www.hzau.edu.cn
访问正常！
23: http://211.69.143.162
URLRequest error!!! <urlopen error [WinError 10061] 由于目标
计算机积极拒绝，无法连接。>
23: http://xwgk.hzau.edu.cn
访问正常！
22: http://xwgk.hzau.edu.cn/index.htm
访问正常！
21: http://xwgk.hzau.edu.cn/fgzd1.htm
访问正常！
20: http://xwgk.hzau.edu.cn/gkzn.htm
访问正常！
19: http://xwgk.hzau.edu.cn/gkml.htm
访问正常！
18: http://xwgk.hzau.edu.cn/ndbg.htm
访问正常！
17: http://xwgk.hzau.edu.cn/flcx.htm
访问正常！
16: http://xwgk.hzau.edu.cn/flcx/jbqk/xxgk.htm
访问正常！
15: http://xwgk.hzau.edu.cn/flcx/jbqk/gzzd.htm
访问正常！
14: http://xwgk.hzau.edu.cn/flcx/jbqk/jzgdbdh.htm
访问正常！
13: http://xwgk.hzau.edu.cn/flcx/jbqk/xswyh.htm
访问正常！
12: http://xwgk.hzau.edu.cn/flcx/jbqk/xxfzgh.htm
访问正常！
11: http://xwgk.hzau.edu.cn/flcx/jbqk/xxgkndbg.htm
访问正常！
10: http://xwgk.hzau.edu.cn/flcx/zsks/zszc.htm
访问正常！
9: http://xwgk.hzau.edu.cn/flcx/zsks/tslzs.htm
访问正常！
8: http://xwgk.hzau.edu.cn/flcx/zsks/lqcx.htm
访问正常！
7: http://xwgk.hzau.edu.cn/flcx/zsks/zszxss.htm
访问正常！
6: http://xwgk.hzau.edu.cn/flcx/zsks/yjszs.htm
访问正常！
5: http://xwgk.hzau.edu.cn/flcx/zsks/yjsfscj.htm
访问正常！
4: http://xwgk.hzau.edu.cn/flcx/zsks/nlqyjsmd.htm
访问正常！
3: http://xwgk.hzau.edu.cn/flcx/zsks/yjszszxss.htm
访问正常！
2: http://xwgk.hzau.edu.cn/flcx/cwzcjsf/cwzcglzd.htm
访问正常！
1: http://xwgk.hzau.edu.cn/flcx/cwzcjsf/sjzzcqk.htm
访问正常！
0: https://www.hzau.edu.cn
1: http://mail.hzau.edu.cn
2: http://center.hzau.edu.cn/info/1072/1676.htm
3: http://center.hzau.edu.cn
4: http://center.hzau.edu.cn/index.htm
5: http://center.hzau.edu.cn/zxgk.htm
6: http://center.hzau.edu.cn/zxgk/zxjj.htm
7: http://center.hzau.edu.cn/zxgk/jgsz.htm
8: http://center.hzau.edu.cn/fwzn.htm
9: http://center.hzau.edu.cn/fwzn/xywfw.htm
10: http://center.hzau.edu.cn/fwzn/xykfw.htm
11: http://center.hzau.edu.cn/fwzn/jxfw.htm
12: http://center.hzau.edu.cn/fwzn/xxfw.htm
13: http://center.hzau.edu.cn/gzzd.htm
14: http://center.hzau.edu.cn/xzzq.htm
15: http://center.hzau.edu.cn/lxwm.htm
16: http://center.hzau.edu.cn/info/1052/1381.htm
17: http://center.hzau.edu.cn/ewm.htm
18: http://center.hzau.edu.cn/gzzd/1.htm
19: http://center.hzau.edu.cn/fwzn/1.htm
20: http://center.hzau.edu.cn/info/1018/1074.htm
21: http://center.hzau.edu.cn/info/1018/1071.htm
22: http://center.hzau.edu.cn/info/1072/1671.htm
23: http://center.hzau.edu.cn/info/1018/1638.htm
24: http://center.hzau.edu.cn/info/1018/1067.htm
25: http://center.hzau.edu.cn/info/1018/1280.htm
26: http://www.hzau.edu.cn
27: http://xwgk.hzau.edu.cn
28: http://xwgk.hzau.edu.cn/index.htm
29: http://xwgk.hzau.edu.cn/fgzd1.htm
30: http://xwgk.hzau.edu.cn/gkzn.htm
31: http://xwgk.hzau.edu.cn/gkml.htm
32: http://xwgk.hzau.edu.cn/ndbg.htm
33: http://xwgk.hzau.edu.cn/flcx.htm
34: http://xwgk.hzau.edu.cn/flcx/jbqk/xxgk.htm
35: http://xwgk.hzau.edu.cn/flcx/jbqk/gzzd.htm
36: http://xwgk.hzau.edu.cn/flcx/jbqk/jzgdbdh.htm
37: http://xwgk.hzau.edu.cn/flcx/jbqk/xswyh.htm
38: http://xwgk.hzau.edu.cn/flcx/jbqk/xxfzgh.htm
39: http://xwgk.hzau.edu.cn/flcx/jbqk/xxgkndbg.htm
40: http://xwgk.hzau.edu.cn/flcx/zsks/zszc.htm
41: http://xwgk.hzau.edu.cn/flcx/zsks/tslzs.htm
42: http://xwgk.hzau.edu.cn/flcx/zsks/lqcx.htm
43: http://xwgk.hzau.edu.cn/flcx/zsks/zszxss.htm
44: http://xwgk.hzau.edu.cn/flcx/zsks/yjszs.htm
45: http://xwgk.hzau.edu.cn/flcx/zsks/yjsfscj.htm
46: http://xwgk.hzau.edu.cn/flcx/zsks/nlqyjsmd.htm
47: http://xwgk.hzau.edu.cn/flcx/zsks/yjszszxss.htm
48: http://xwgk.hzau.edu.cn/flcx/cwzcjsf/cwzcglzd.htm        
49: http://xwgk.hzau.edu.cn/flcx/cwzcjsf/sjzzcqk.htm



======================= bfs ===========================      
0: https://www.hzau.edu.cn
访问正常！
1: http://211.69.143.162
URLRequest error!!! <urlopen error [WinError 10061] 由于目标
计算机积极拒绝，无法连接。>
2: http://mail.hzau.edu.cn
访问正常！
3: http://xwgk.hzau.edu.cn
访问正常！
4: http://pdc.hzau.edu.cn
访问正常！
5: http://lib.hzau.edu.cn
访问正常！
6: http://bwg.hzau.edu.cn
访问正常！
7: https://www.hzau.edu.cn/yhtd/xs.htm
访问正常！
8: https://www.hzau.edu.cn/yhtd/jg.htm
访问正常！
9: https://www.hzau.edu.cn/yhtd/ks.htm
访问正常！
10: https://www.hzau.edu.cn/yhtd/xy.htm
访问正常！
11: https://www.hzau.edu.cn/yhtd/fk.htm
访问正常！
12: https://www.hzau.edu.cn/en/HOME.htm
访问正常！
13: https://www.hzau.edu.cn/index.htm
访问正常！
14: http://news.hzau.edu.cn
访问正常！
15: https://www.hzau.edu.cn/xxgk/xxjj.htm
访问正常！
16: https://www.hzau.edu.cn/xxgk/xxzc.htm
访问正常！
17: https://www.hzau.edu.cn/xxgk/lsyg.htm
访问正常！
18: https://www.hzau.edu.cn/xxgk/yxsz.htm
访问正常！
19: https://www.hzau.edu.cn/xxgk/jgsz.htm
访问正常！
20: https://www.hzau.edu.cn/xxgk/xrld.htm
访问正常！
21: https://www.hzau.edu.cn/xxgk/lrld.htm
访问正常！
22: https://www.hzau.edu.cn/jyjx/bksjy.htm
访问正常！
23: https://www.hzau.edu.cn/jyjx/yjsjy.htm
访问正常！
24: https://www.hzau.edu.cn/jyjx/gjsjy.htm
访问正常！
25: https://www.hzau.edu.cn/jyjx/jxjy.htm
访问正常！
26: https://www.hzau.edu.cn/zsjy/bkszs.htm
访问正常！
27: https://www.hzau.edu.cn/zsjy/yjszs.htm
访问正常！
28: https://www.hzau.edu.cn/zsjy/lxszs.htm
访问正常！
29: https://www.hzau.edu.cn/zsjy/jxjy.htm
访问正常！
30: https://www.hzau.edu.cn/zsjy/bksjy.htm
访问正常！
31: https://www.hzau.edu.cn/zsjy/yjsjy.htm
访问正常！
32: https://www.hzau.edu.cn/szdw1.htm
访问正常！
33: https://www.hzau.edu.cn/xkky/xkjs.htm
访问正常！
34: https://www.hzau.edu.cn/xkky/kxyj.htm
访问正常！
35: https://www.hzau.edu.cn/xkky/xsqk.htm
访问正常！
36: https://www.hzau.edu.cn/xkky/shfw.htm
访问正常！
37: http://fao.hzau.edu.cn
访问正常！
38: http://news.hzau.edu.cn/pic/fg
访问正常！
39: https://www.hzau.edu.cn/zjhn/xysh.htm
访问正常！
40: https://www.hzau.edu.cn/zjhn/syxx.htm
访问正常！
41: http://www.hzau.edu.cn/info/1062/10991.htm
访问正常！
42: http://special.hzau.edu.cn/200511/index.shtml
访问正常！
43: http://www.hzau.edu.cn/info/1062/10504.htm
访问正常！
44: https://www.hzau.edu.cn/info/1062/11081.htm
访问正常！
45: https://www.hzau.edu.cn/info/1062/11105.htm
访问正常！
46: https://www.hzau.edu.cn/info/1062/11104.htm
访问正常！
47: https://www.hzau.edu.cn/info/1062/11103.htm
访问正常！
48: https://www.hzau.edu.cn/info/1062/11093.htm
访问正常！
49: https://www.hzau.edu.cn/info/1062/11073.htm
访问正常！
0: https://www.hzau.edu.cn
1: http://211.69.143.162
2: http://mail.hzau.edu.cn
3: http://xwgk.hzau.edu.cn
4: http://pdc.hzau.edu.cn
5: http://lib.hzau.edu.cn
6: http://bwg.hzau.edu.cn
7: https://www.hzau.edu.cn/yhtd/xs.htm
8: https://www.hzau.edu.cn/yhtd/jg.htm
9: https://www.hzau.edu.cn/yhtd/ks.htm
10: https://www.hzau.edu.cn/yhtd/xy.htm
11: https://www.hzau.edu.cn/yhtd/fk.htm
12: https://www.hzau.edu.cn/en/HOME.htm
13: https://www.hzau.edu.cn/index.htm
14: http://news.hzau.edu.cn
15: https://www.hzau.edu.cn/xxgk/xxjj.htm
16: https://www.hzau.edu.cn/xxgk/xxzc.htm
17: https://www.hzau.edu.cn/xxgk/lsyg.htm
18: https://www.hzau.edu.cn/xxgk/yxsz.htm
19: https://www.hzau.edu.cn/xxgk/jgsz.htm
20: https://www.hzau.edu.cn/xxgk/xrld.htm
21: https://www.hzau.edu.cn/xxgk/lrld.htm
22: https://www.hzau.edu.cn/jyjx/bksjy.htm
23: https://www.hzau.edu.cn/jyjx/yjsjy.htm
24: https://www.hzau.edu.cn/jyjx/gjsjy.htm
25: https://www.hzau.edu.cn/jyjx/jxjy.htm
26: https://www.hzau.edu.cn/zsjy/bkszs.htm
27: https://www.hzau.edu.cn/zsjy/yjszs.htm
28: https://www.hzau.edu.cn/zsjy/lxszs.htm
29: https://www.hzau.edu.cn/zsjy/jxjy.htm
30: https://www.hzau.edu.cn/zsjy/bksjy.htm
31: https://www.hzau.edu.cn/zsjy/yjsjy.htm
32: https://www.hzau.edu.cn/szdw1.htm
33: https://www.hzau.edu.cn/xkky/xkjs.htm
34: https://www.hzau.edu.cn/xkky/kxyj.htm
35: https://www.hzau.edu.cn/xkky/xsqk.htm
36: https://www.hzau.edu.cn/xkky/shfw.htm
37: http://fao.hzau.edu.cn
38: http://news.hzau.edu.cn/pic/fg
39: https://www.hzau.edu.cn/zjhn/xysh.htm
40: https://www.hzau.edu.cn/zjhn/syxx.htm
41: http://www.hzau.edu.cn/info/1062/10991.htm
42: http://special.hzau.edu.cn/200511/index.shtml
43: http://www.hzau.edu.cn/info/1062/10504.htm
44: https://www.hzau.edu.cn/info/1062/11081.htm
45: https://www.hzau.edu.cn/info/1062/11105.htm
46: https://www.hzau.edu.cn/info/1062/11104.htm
47: https://www.hzau.edu.cn/info/1062/11103.htm
48: https://www.hzau.edu.cn/info/1062/11093.htm
49: https://www.hzau.edu.cn/info/1062/11073.htm



```

可以看出，深搜会不断的对网页的最开始的链接进行优先爬取，深度不断地递增，直到最深的页面所有链接都被访问后才会返回上一个指向它的页面进行爬取；对于广搜爬虫，会爬完一个页面的所有连接后，才会爬取第二个页面下的所有链接；深搜的过程可以看出从网站的某一根目录到其他页面的深度，而广搜的过程可以看出每一个页面所包含的页面。



### 4. 页面内容提取
#### html.parser 的使用
```python
from html.parser import HTMLParser

class MyHTMLParser(HTMLParser):  # 继承 HTMLParser 类

    ctag = False  # 当前解析的标签是否为内容所在的标签


    def handle_starttag(self, tag, attrs):
        print('begin a tag:'+tag)
        if tag == 'h1':
            for attr in attrs:
                print(attr[0])
                if attr[1] == 'center':
                    self.ctag = True
                    break
    def handle_data(self, data):
        print('handle a tag')
        if self.ctag == True:
            print("Extracted data :", data)
    def handle_endtag(self, tag):
        print('end a tag:'+tag)
        self.ctag = False


parser = MyHTMLParser()
parser.feed('<html><head><title>Test</title></head>'
            '<body><h1 align="center">Big data news</h1><h1 align="center">'
            'AI news</h1><h1 align="right">2018.8.1</h1></body></html>')
```

运行结果：

```text
begin a tag:html
begin a tag:head
begin a tag:title
handle a tag
end a tag:title
end a tag:head
begin a tag:body
begin a tag:h1
align
handle a tag
Extracted data : Big data news
end a tag:h1
begin a tag:h1
align
handle a tag
Extracted data : AI news
end a tag:h1
begin a tag:h1
align
handle a tag
end a tag:h1
end a tag:body
end a tag:html
```

可以看出三个函数分析出了每一个标签的开始、内容以及结束等信息

#### lxml 的使用

```python
from lxml import etree
html='''<html><head><title>Test</title></head><body><h1 align="center">Bigdata news</h1><h1 align="center">AI news</h1><h1 align="right">2018.8.1</h1></body></html>'''
content = etree.fromstring(html)
rows=content.xpath('/html/body/h1') #根据路径表达式获得所有符合条件的节点
for row in rows: #对每个节点进行处理
    t=row.xpath('./text()')[0]
    print(t)
# 对数据表格进行提取
html = '''<html><head><title>Test</title></head><body><tableid="table1"cellspacing="0px"><tr><th>学号</th><th>姓名</th><th>成绩</th></tr><tr><td>1001</td><td>曾平</td><td>90</td></tr><tr><td>1002</td><td>王一</td><td>92</td></tr><tr><td>1003</td><td>张三</td><td>88</td></tr></table></body></html>'''
content = etree.HTML(html)
rows = content.xpath('//table[@id="table1"]/tr')[1:]
for row in rows:
    id = row.xpath('./td[1]/text()')[0]
    name = row.xpath('./td[2]/text()')[0]
    score = row.xpath('./td[3]/text()')[0]
    print(id, name, score)

# 提取最后一个记录
content = etree.HTML(html)
rows = content.xpath('//table[@id="table1"]/tr[last()]')
for row in rows:
    id = row.xpath('./td[1]/text()')[0]
    name = row.xpath('./td[2]/text()')[0]
    score = row.xpath('./td[3]/text()')[0]
    print(id, name, score)
```

```text
Bigdata news
AI news
2018.8.1
1001 曾平 90
1002 王一 92
1003 张三 88
1003 张三 88
```

使用lxml提取出第一个html中所有的h1标签的值，提取出后一个html中所有的表格信息以及单独提取出最后一行

#### BeautifulSoup 的使用
```python
# -*- coding: utf-8 -*-
from bs4 import BeautifulSoup
html='''
<html><body><div id="second-title">访华前 这个国家的总理说“感谢中国体
谅”</div>
<div class="date-source">
<span class="date">2019 年 03 月 27 日 21:30</span></div>
<span class="publish source">参考消息</span>
<div class="article">
<p>原标题：锐参考 | 访华前，这个国家的总理说：“感谢中国体谅！”</p>
<p>“非常感谢中国的理解！”</p>
<p>在 25 日的新闻发布会上，新西兰总理杰辛达·阿德恩这样说道。</p>
</div>
</body></html>
'''
soup = BeautifulSoup(html, 'lxml')
#id 名前加#
title = soup.select('div#second-title')[0].text
#类名(class)前加点
date=soup.select('span.date')[0].text
#类名中的空格用点替换，即 publish.source
source=soup.select('span.publish.source')[0].text
#子标签通过 > 定义
content = soup.select('div.article > p')
contentstr = ''
for i in range(len(content)):
    contentstr += content[i].text+"\n"
print("标题：",title)
print("发布日期：",date)
print("消息来源：",source)
print("消息内容：", contentstr)
```

```text
标题： 访华前 这个国家的总理说“感谢中国体
谅”
发布日期： 2019 年 03 月 27 日 21:30
消息来源： 参考消息
消息内容： 原标题：锐参考 | 访华前，这个国家的总理说：“感谢中
国体谅！”
“非常感谢中国的理解！”
在 25 日的新闻发布会上，新西兰总理杰辛达·阿德恩这样说道。    

```

通过bs4来提取每一个标签内的值

## 二、 主题爬虫的实现 

对于计算余弦相似度，可以利用公式： $\frac{|A \cap B|}{\sqrt{|A|}*\sqrt{|B|}}$ 来计算

```python
try:
    # 余弦相似度计算
    commwords = topicwords.intersection(docwords)
    cossim = len(commwords) / (math.sqrt(float(len(topicwords)))
                                * math.sqrt(float(len(docwords))))
except Exception as e:
    print(e)
  ```

## 三、 动态页面爬虫的实现 

### （1） 构造带参数的 URL，利用参数传递动态请求；

一些网页的数据请求通过对url赋值来实现数据的传输，所以只需要观察构造出符合的url即可：

```python
url = 'https://search.jd.com/Search'
#以字典存储查询的关键词及属性
qrydata = {
 'keyword':'互联网大数据',
 'enc':'utf-8',
}
lt = []
for k,v in qrydata.items():
    lt.append(k+'='+str(v))
query_string = '&'.join(lt)

url = url + '?'+query_string
print(url) 
```

```text
https://search.jd.com/Search?keyword=互联网大数据&enc=utf-8  
```

对于参数间使用 ``&`` 来连接，url和参数间使用 ``?`` 来连接

### （2） 构造 Cookie 携带参数，利用 HTTP 头部传递动态请求的参数； 

```python
import requests
import re

#从浏览器的开发者模式复制 Cookie，保存到文本文件 taobao.txt
f=open(r'taobao.txt','r') #打开所保存的 cookies 内容文件
cookies={} #初始化 cookies 字典变量
for line in f.read().split(';'): #按照字符进行划分读取
    name,value=line.strip().split('=',1)
    cookies[name]=value #为字典 cookies 添加内容

r=requests.get("https://www.taobao.com/",cookies=cookies)
#print(r.text)
rs=re.findall(u'<title>.*</title>',r.text) #<title>淘宝网 -淘！我喜欢</title>
print(rs) 
```

```text
['<title>淘宝网 - 淘！我喜欢</title>']
```

将浏览器中的cookie保存到本地，使用cookie对网页请求，可以模拟登录爬取网页

### （3） Ajax 的动态请求技术； 

对于一些网站，其页面的数据是动态变化的，网页的部分数据是通过ajax等动态请求的：

```python
import requests
import json

url = 'https://hotels.ctrip.com/hotel/beijing1'
#以下 payload 数据来自浏览器看到的结果
payload = {"PlatformType":"pc","pageParameter":{"Refer":"","UA":"Mozilla%2F5.0%20(Windows%20NT%2010.0%3B%20WOW64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F55.0.2883.87%20Safari%2F537.36","PageID":102002,"VID":"1590400761906.17yfiq"},"marketParameter":{"AID":0,"SID":0},"terminalParameter":{"UserID":"","CityID":0},"pcAuthCodeParamet":{"IsGetAuthCode":"true","AppID":"","Length":4}}
payloadHeader = {'content-type':'application/json'}

# 以 POST 方法发送 URL 请求，同时指定所携带的参数给函数参数 data
res = requests.post(url, data=json.dumps(payload),
headers=payloadHeader)
res.encoding = 'utf-8'
print(res.text) 
```

```text
<!DOCTYPE html>
<html>
<head id="ctl00_Head1"><meta http-equiv="Content-Type" content="text/html; charset=gb2312" />

<meta content="all" name="robots" />
<meta name="robots" content="index,nofollow" />
<title>北京酒店,北京酒店预订查询,北京宾馆住宿【携程酒店】</title>
<meta name="keywords" content="北京酒店,北京酒店预订,北京酒店
查询,北京住宿,北京宾馆,携程酒店预订"/>
<meta name="description" content="携程酒店频道为您提供北京约0家酒店预订，您可以通过对比实时价格，查看酒店真实照片，浏览用 
户真实点评等多种手段选择舒适、优质的北京酒店。推荐今日热门酒 
店访问携程享受可订保障。"/>
<meta http-equiv="x-dns-prefetch-control" content="on" /><meta http-equiv="X-UA-Compatible" content="IE=edge" /><link rel="dns-prefetch" href="//webresource.c-ctrip.com" /><link rel="dns-prefetch" href="//pic.c-ctrip.com" /><link rel="dns-prefetch" href="//s.c-ctrip.com" /><link rel="apple-touch-startup-image" href="//pic.c-ctrip.com/h5/common/640.png" sizes="320x460" /><link rel="apple-touch-startup-image" href="//pic.c-ctrip.com/h5/common/940.png" sizes="640x920" /><link rel="apple-touch-startup-image" href="//pic.c-ctrip.com/h5/common/1004.png" sizes="768x1004" /><link rel="apple-touch-icon-precomposed" sizes="57x57" href="//pic.c-ctrip.com/h5/common/57.png" /><link rel="apple-touch-icon-precomposed" sizes="72x72" href="//pic.c-ctrip.com/h5/common/72.png" /><link rel="apple-touch-icon-precomposed" sizes="114x114" href="//pic.c-ctrip.com/h5/common/114.png" /><link rel="apple-touch-icon-precomposed" sizes="144x144" href="//pic.c-ctrip.com/h5/common/144.png" /> 
<meta name="location" content="province=北京;city=北京">     

<link rel="canonical" href="//hotels.ctrip.com/hotel/beijing1"/>
<script type="text/javascript">
    function imgError(obj) {
        obj.src = '//pic.c-ctrip.com/hotels110127/hotel_example.jpg';
        var next = obj.nextSibling;
        if (next) {
            obj.parentNode.removeChild(next);
        }
        obj.onerror = null;
    }
        </script>
</head>

<body id="mainbody"  >
<link href="//webresource.c-ctrip.com/ResHotelOnline/R8/search/css/un_searchresult.css?releaseno=2020-06-07-18-12-16" rel="stylesheet" type="text/css" />
<style>
.J_bp { margin-right: -1.5px; margin-left:1.5px; }
svg { margin: 0;}
vml { margin: 0;}
stylebold { font-weight:bold; }
</style>

...
```

### （4） 模拟浏览器技术

在爬取一些可能针对不懂访问者的页面，需要模拟真实登录者来爬取数据，此时要规定 ``useragent`` 以及 ``http_header`` 等参数

```python
import requests
useragent='Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N)
AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Mobile
Safari/537.36'
http_headers = {
 'User-Agent': useragent,
 'Accept': 'text/html'
 #其他头部属性
}
page=requests.get(url, headers=http_headers) #url 要请求的网址
```

## 四、 深度页面爬虫的实现 

因为可能时间比较久，实验指导书上的示例代码实际上因为目标网站的页面更新已经不能使用了，需要修改一些地方:
+ 原来的请求后的页面是分页的，所以有一步获取分页个数的操作，现在页面没有分页了，所以注释掉了该部分代码（也有可能是测试关键字返回的页面内容不够）
+ 新的页面的一些数据，如书名、作者、评论数等等在html中的class属性已经被更改，所以要重新观察，改变bs中的操作

```python
# -*- coding: utf-8 -*-
import requests
from bs4 import BeautifulSoup
import traceback
import os
import urllib

# 读取出版社列表
def read_list(txt_path):
    press_list = []
    f = open(txt_path, 'r')
    for line in f.readlines():
        press_list.append(line.strip('\n'))
    return press_list

# 定位input标签，拼接URL
def build_form(press_name):
    header = {'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko'}
    res = requests.get('http://search.dangdang.com/advsearch', headers=header)
    res.encoding = 'GB2312'
    soup = BeautifulSoup(res.text, 'html.parser')
    # 定位input标签
    input_tag_name = ''
    conditions = soup.select('.box2 > .detail_condition > label')
    print('共找到%d项基本条件,正在寻找input标签' % len(conditions))
    for item in conditions:
        text = item.select('span')[0].string
        if text == '出版社':
            input_tag_name = item.select('input')[0].get('name')
            print('已经找到input标签，name:', input_tag_name)
    # 拼接url
    keyword = {'medium': '01',
               input_tag_name: press_name.encode('gb2312'),
               'category_path': '01.00.00.00.00.00',
               'sort_type': 'sort_pubdate_desc'
               }
    url = 'http://search.dangdang.com/?'
    url += urllib.parse.urlencode(keyword)
    print('入口地址:%s' % url)
    return url

# 抓取信息
def get_info(entry_url):
    header = {'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko'}
    res = requests.get(entry_url, headers=header)
    res.encoding = 'GB2312'
    # 这里用lxml解析会出现内容缺失
    soup = BeautifulSoup(res.text, 'html.parser')
    # 获取页数
    # print(soup.select('.data > span'))
    # page_num = int(soup.select('.data > span')[1].text.strip('/'))
    # print('共 %d 页待抓取， 这里只测试采集1页' % page_num)
    page_num = 1    #这里只测试抓1页
    
    page_now = '&page_index='
    # 书名 价格 出版时间 评论数量 
    books_title = []
    books_price = []
    books_author = []
    books_comment = []
    for i in range(1, page_num+1):
        now_url = entry_url + page_now + str(i)
        print('正在获取第%d页,URL:%s' % (i, now_url))
        res = requests.get(now_url, headers=header)
        soup = BeautifulSoup(res.text, 'html.parser')
        # 获取书名
        tmp_books_title = soup.select('ul.products > li[ddt-pit] > a')
        for book in tmp_books_title:
            books_title.append(book.get('title'))
        # 获取价格
        tmp_books_price_num = soup.select('ul.products > li[ddt-pit] > p.price > span.rob > span.num')
        tmp_books_price_tail = soup.select('ul.products > li[ddt-pit] > p.price > span.rob > span.tail')
        for (num, tail) in zip(tmp_books_price_num, tmp_books_price_tail):
            books_price.append(num.text + tail.text)
        # 获取评论数量
        tmp_books_comment = soup.select('ul.products > li[ddt-pit] > p.link > a')
        for book in tmp_books_comment:
            books_comment.append(book.text)
        # 获取出版作者
        tmp_books_author = soup.select('ul.products > li[ddt-pit] > p.author')
        for book in tmp_books_author:
            books_author.append(book.text)
    books_dict = {'title': books_title, 'price': books_price, 'author': books_author, 'comment': books_comment}
    return books_dict

# 保存数据
def save_info(file_dir, press_name, books_dict):
    print(books_dict)
    res = ''
    try:
        for i in range(len(books_dict['title'])):
            res += (str(i+1) + '.' + '书名:' + books_dict['title'][i] + '\r\n' +
                    '价格:' + books_dict['price'][i] + '\r\n' +
                    '出版日期:' + books_dict['author'][i] + '\r\n' +
                    '评论数量:' + books_dict['comment'][i] + '\r\n' +
                    '\r\n'
                    )
    except Exception as e:
        print('保存出错')
        print(e)
        traceback.print_exc()
    finally:
        file_path = file_dir + os.sep + press_name + '.txt'
        f = open(file_path, "wb")
        f.write(res.encode("utf-8"))
        f.close()
        print(res.encode("utf-8"))
        return

# 入口
def start_spider(press_path, saved_file_dir):
    # 获取出版社列表
    press_list = read_list(press_path)
    for press_name in press_list:
        print('------ 开始抓取 %s ------' % press_name)
        press_page_url = build_form(press_name)
        books_dict = get_info(press_page_url)
        save_info(saved_file_dir, press_name, books_dict)
        print('------- 出版社: %s 抓取完毕 -------' % press_name)
    return

if __name__ == '__main__':
    # 出版社名列表所在文件路径
    press_txt_path = r'press.txt'
    # 抓取信息保存路径
    saved_file_dir = r'./'
    # 启动
    start_spider(press_txt_path, saved_file_dir)

```

最后查看同一目录下的结果文件：

```text
1.书名:2020新高考数学真题全刷：基础2000题
价格:73.90
出版日期:朱昊鲲 主编
评论数量:30919

2.书名:机器学习
价格:86.10
出版日期:周志华
评论数量:72858

3.书名:公文写作范例大全： 格式、要点、规范与技巧（第2版）
价格:87.60
出版日期:岳海翔    舒雪冬
评论数量:37854

4.书名:大问题：简明哲学导论（第10版）
价格:58.80
出版日期:[美]罗伯特・所罗门（Robert C. Solomon ）凯思林・希金斯（Kathleen M. Higgins）著 张卜天 译
评论数量:51323

5.书名:山海经
价格:122.60
出版日期:陈丝雨 绘 孙见坤 注
评论数量:77626

6.书名:红楼梦脂评汇校本（全套3册）
价格:189.60
出版日期:曹雪芹著，脂砚斋评，吴铭恩汇校
评论数量:55

7.书名:幸得诸君慰平生
价格:49.20
出版日期:故园风雨前
评论数量:27495
```

