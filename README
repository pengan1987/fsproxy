           FSPv2 -> HTTP/1.1 protocol proxy
                     Radim Kolar
                       2003-9
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

1. Get FSP protocol stack for Java - fsp javalib

This program needs correctly installed (in CLASSPATH) jfsplib,
you can get it from main FSP protocol suite site:
http://fsp.sourceforge.net/javalib.html
You need at least 1.0rc7 version of jfsplib

2. Setup WWW browser's proxy
Setup Gopher proxy in web browser to port 9090 on host which is running
fsproxy.

3. Run fsp proxy daemon

distributed fsproxy.jar is autostartable, but there seems to be problem
that java launcher (tested on 1.6.0_16) is unable to merge classpath with
autostartable jar content.

Running:
Usage: java -cp fsproxy-0.X.jar;jfsplib-1.0rc8.jar fspproxy [portnumber] [mime type table]

Note: Unix systems are using : instead of ; as classpath separator

FSP proxy can load mime.types style file for mapping file extensions
to mime types. You can get this file from apache httpd distribution.

Examples:
Unix
   java -cp jfsplib-1.0rc8.jar:fsproxy-0.8.jar 9090 /usr/local/etc/mime.types

Windows
   C:\target>java -cp jfsplib-1.0rc8.jar;fsproxy-0.8.jar fspproxy
   Fri Oct 30 13:48:08 CET 2009 FSP Proxy 0.8 ready on 9090/*

Operation:

FSP Proxy runs on port by default 9090 and accepts fsp://*, http://*, ftp://*
and gopher://* URLs on input side. These requests are transformed info FSP v2
requests and sent to the target FSP server.  Most browser do not recognize
fsp:// urls, so you have to type gopher: instead of fsp:

Simply type in your browser URL bar 'gopher://your.favourite.fsp.site/' without
quotes and begin to browse content of particular FSP site.
In most cases you must use port number as part of URL:
   gopher://your.favourite.fsp.site:2000/

Directory listing is pretty fast, isn't it?

     **********************************************
         This is a free software.  Be creative.
        Let me know of any bugs and suggestions.
               *************************

Radim Kolar
hsn@users.sourceforge.net
Main developer of FSP project