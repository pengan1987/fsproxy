           FSPv2 -> HTTP/1.1 protocol proxy
                     Radim Kolar
                       2003-2020
        http://fsp.sourceforge.net/fsproxy.html
          This software is part of FSP project

Copyright:
You may copy or modify this file in any manner you wish, provided
that this notice is always included, and that you hold the author
harmless for any loss or damage resulting from the installation or
use of this software.

Summary:
This proxy supports HTTP/1.1 on output side and FSP v2 on input side.
FSP v2 over HTTP/1.1. This proxy do not caches anything.

In detail:
Using this proxy, you can browse FSP sites by any web browser
which support gopher protocol.
This proxy DOES NOT tunnels HTTP over FSP. This is a different project.

Installation:

1. Get java
This program needs Java virtual machine to run. Get one from
https://adoptopenjdk.net/releases.html

2. Setup WWW browser's proxy
Setup Gopher proxy in web browser to port 9090 on host which is running
fsproxy, usually localhost 127.0.0.1. If your browser doesn't have
gopher proxy configuration, you can use ftp as well. If everything fails,
this proxy can handle http input as well, but use some plugin like Proxy
Switcher to restrict its use for http protocol only to known fsp sites
otherwise will your web pages stop working.

3. Run fsp proxy daemon
Distributed fsproxy.jar is auto startable, just click on it or you can
run it on command line by hand:

Usage: java -jar fsproxy.jar [portnumber] [mime type table]

FSP proxy can load mime.types style file for mapping file extensions
to mime types. You can get this file from apache httpd distribution.

Example run proxy on port 9090 with provided mime.types:
   java -jar fsproxy.jar 9090 /usr/local/etc/mime.types

Operation:

FSP Proxy runs on port by default 9090 and accepts fsp://*, http://*, ftp://*
and gopher://* URLs on input side. These requests are transformed info FSP v2
requests and sent to the target FSP server.  Most browser do not recognize
fsp:// urls, so you have to type gopher: instead of fsp:

Simply type in your browser URL bar 'gopher://your.favourite.fsp.site/' without
quotes and begin to browse content of particular FSP site. If you configured
fspproxy in browser as proxy for FTP protocol then use
'ftp://your.favourite.fsp.site/' instead. In most cases you must use port number
as part of URL:
   gopher://your.favourite.fsp.site:2000/

Directory listing is pretty fast, isn't it?

     **********************************************
         This is a free software.  Be creative.
        Let me know of any bugs and suggestions.
               *************************

Radim Kolar
hsn@users.sourceforge.net
Main developer of FSP project
