# DNSResolver

This is a low level implementation of a :
* **hostname** to **IP address**, and 
* **IP address** to **hostname**

resolver in Java similarly to [InetAddress](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html) 
class' methods [getHostName()](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html#getHostName()) 
and [getHostAddress()](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html#getHostAddress()).

It implements "manual" UDP packets construction and response parsing according to [RFC 1035](https://www.ietf.org/rfc/rfc1035.txt)

It is a 1st project of a series leading to the implementation of a **Web Robot**
 1. [DNSResolver](https://github.com/Iulian-Stan/DNSResolver) 
 2. [HTTPCrawler](https://github.com/Iulian-Stan/HTTPCrawler) 
 3. [HTMLParser](https://github.com/Iulian-Stan/HTMLParser)
 4. [WebRobot](https://github.com/Iulian-Stan/WebRobot) - work in progress
