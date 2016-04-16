# DNSResolver

This is a low level implementation of a :
* **hostname** to **IP address**, and 
* **IP address** to **hostname**

resolver in Java similarly to [InetAddress](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html) 
class' methods [getHostName()](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html#getHostName()) 
and [getHostAddress()](http://download.java.net/jdk7/archive/b123/docs/api/java/net/InetAddress.html#getHostAddress()).

It implements "manual" UDP packets construction and response parsing according to [RFC 1035](https://www.ietf.org/rfc/rfc1035.txt)
