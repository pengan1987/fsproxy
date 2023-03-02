# FSP Proxy Mod
This is a modified version of the original [FSP proxy server](https://fsp.sourceforge.net/fsproxy.html) to support access to FSP([File Service Protocol](https://fsp.sourceforge.net/index.html)) servers from modern WWW browser.

Since Chromium-based browsers no longer have support for FTP and Gopher proxy, to use the original FSProxy might be tricky, this modified version provide an extra parameter to setup a default remote host address and make FSProxy as an FSP to HTTP reverse proxy.

The new usage is:
```bash
java -jar fspproxy.jar -p [port number] -m [mime types file] -r [default remote host:port]
```
For example, if you want setup a HTTP to FSP reverse proxy on your machine with port 8000 and connect to remote FSP server 192.168.33.44:2121, the command is like
```bash
java -jar fsproxy.jar -p 8000 -r 192.168.33.44:2121
```

Then open the `http://127.0.0.1:8000` to browse the remote FSP server directory.