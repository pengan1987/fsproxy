/*
Copyright (c) 2003-2004 by Radim HSN Kolar (hsn@netmag.cz)

You may copy or modify this file in any manner you wish, provided
that this notice is always included, and that you hold the author
harmless for any loss or damage resulting from the installation or
use of this software.

		     This is a free software.  Be creative. 
		    Let me know of any bugs and suggestions.
*/
import java.net.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Date;
import net.fsp.FSPsession;
import net.fsp.FSPstat;
import net.fsp.FSPutil;

public class fspreq implements Runnable
{
    private Socket s;
    private boolean http10;
    private DataInputStream in;
    private DataOutputStream ou;
   
    fspreq(Socket ns)
    {
	s=ns;
    }

    public void run()
    {
	http10=true;

	try 
	{ 
	    in=new DataInputStream (new BufferedInputStream(s.getInputStream()));
	    ou=new DataOutputStream(new BufferedOutputStream(s.getOutputStream(),4096));

	/* precteme radku GET / */

	s.setSoTimeout(fspproxy.client_timeout);

	String req=in.readLine();
	if(req==null) 
	{ 
	    s.close();ou.close();s=null;ou=null;
	    return;
	}

        if(fspproxy.trace_url==true)
        {
            System.out.println("[FSPPROXY TRACE "+Thread.currentThread().getName()+"] > "+req);
        }

	StringTokenizer st=new StringTokenizer(req);
        int req_method;
	long ims=0;
	req_method=req.indexOf(" HTTP/1.",0);
	if (req_method==-1) http10=false;
	else
          while(true)
          {
	      String line;
	      /* read rest of HTTP headers */
	      line=in.readLine();
              if(line==null) break;
              if(line.length()==0) break;
	      String s1,s2;
	      int j;
	      j=line.indexOf(':',0);
	      if(j==-1) continue;
	      s1=line.substring(0,j).toLowerCase();
	      s2=line.substring(j+1);

              if(s1.equals("if-modified-since")) 
	      {
                /* cut of file size */
               	j=s2.indexOf(';',0);
		if(j>-1) s2=s2.substring(0,j);
		try
		{
		   ims=new Date(s2).getTime();
	        }
	        catch (IllegalArgumentException baddate)                                        {}
		continue;
	      }
          }
	String req2=null;

	if(!req.startsWith("GET ")) 
	{
	     fspproxy.send_error(http10?10:9,501,"This FSP proxy server supports only GET access method.",ou);
	}
	/* access check 
	if(!mgr.checkInetAdr(s.getInetAddress().getAddress())) {
	      httpreq.server_error(http10?10:9,403,"Cache access denied.",ou);
	   }
	*/

	/* extract URL as req2 */
	int space;
	space=req.indexOf(' ');
        if(space==-1)
	    fspproxy.send_error(http10?10:9,400,"Can not find method and URI in request",ou);
	if(req_method==-1)
	    req2=req.substring(space+1);
	else
	    req2=req.substring(space+1,req_method);
	
        req2=req2.trim();
	if(req2.length()==0)
	    fspproxy.send_error(http10?10:9,400,"Can not find URI in request",ou);

	if(req2.charAt(0)=='/')
	{
	    fspproxy.send_error(http10?10:9,400,"Only proxy requests are supported.",ou);
	}
	
	/* parse request */
	String parsed[]=fspproxy.parseURL(req2,null);
	if(parsed[4]==null || 
		(!parsed[4].equalsIgnoreCase("fsp") && 
		 !parsed[4].equalsIgnoreCase("gopher") 
		)
	  )
	{
	    fspproxy.send_error(http10?10:9,400,"This proxy supports only FSP protocol. Both fsp:// and gopher:// URLs are accepted.",ou);
	}
	
	/* create FSP session to host */
	FSPsession ses;
	FSPstat stat;
	if(parsed[1]==null) parsed[1]="0";
	try
	{
	    ses=new FSPsession(parsed[0],Integer.valueOf(parsed[1]).intValue());
	}
	catch (Exception en)
	{
	    fspproxy.send_error(http10?10:9,500,"Can not open session to FSP site "+parsed[0]+":"+parsed[1]+" Reason: "+en,ou);
	    return;
	}
	/* stat the URL */
	try
	{
	    stat=FSPutil.stat(ses,parsed[2]+parsed[3]);
	}
	catch (IOException io)
	{
	    fspproxy.send_error(http10?10:9,500,"Error getting file info. Reason: "+io,ou);
	    return;
	}

	if(stat==null)
	{
	    /* file not found! */
	    fspproxy.send_error(http10?10:9,404,"File not found",ou);
	}
	/* Check IMS */
	if(ims>0 && stat.lastmod<=ims)
	{
	    /* not modified! */
	    ou.writeBytes("HTTP/1.0 304 YahOO\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\n\r\n");
	    ou.close();
	    return;
	}
	/* handle file request */
	if(stat.type==FSPstat.RDTYPE_DIR)
	{
	    if(!stat.name.endsWith("/"))
	    {
		/* send redirect */
		ou.writeBytes("HTTP/1.0 301 Moved\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nLocation: "+stat.name+"/\r\n\r\n");
		ou.close();
		return;
	    }
		
	    /* generate a directory listing */
	    FSPstat list[];

	    list=FSPutil.statlist(ses,stat.name);
	    if(list==null)
	    {
	       fspproxy.send_error(http10?10:9,401,"Can't list directory",ou);
	    }
	    
	    ou.writeBytes("HTTP/1.0 200 Listing\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nContent-Type: text/html\r\n\r\n");
	    ou.writeBytes("<h2>Directory "+stat.name+"</h2>\n<pre>\n");
	    /*
	    if(stat.name.endsWith("/"))
		stat.name=stat.name.substring(0,stat.name.length()-1);
		*/
	    for(int i=0;i<list.length;i++)
	    {
		if(list[i].name.equals(".")) continue;
		if(list[i].name.equals(".."))
		{
		    if(stat.name.equals("/"))
			list[i].name="";
		    else
			list[i].name=stat.name.substring(0,stat.name.lastIndexOf('/',stat.name.length()-2));
		    ou.writeBytes("[parent]");
		}
		if(list[i].type==FSPstat.RDTYPE_DIR)
		{
		    ou.writeBytes("[directory] ");
		    list[i].name+="/";
		} else
		    ou.writeBytes("[file] ");
		ou.writeBytes("<a href=\""+list[i].name+"\">"+list[i].name+"</a>\n");
	    }
	    ou.writeBytes("</pre>\n");
	    ou.writeBytes("Listing generated by "+fsploop.NAME+" "+fsploop.VERSION+".\n");
	    ou.close();
	    return;
	}

	/* transfer file */
	ou.writeBytes("HTTP/1.0 200 Transfering\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nContent-Type: "+fspproxy.guessContentType(stat.name)+"\r\nContent-length: "+stat.length+"\r\nLast-Modified: "+new Date(stat.lastmod).toGMTString()+"\r\n\r\n");
	FSPutil.download(ses,stat.name,ou,0);
	ou.close();
	return;
    }
    catch (IOException err)
    {
	try
	{
	    // linger close
	    System.out.println(err);
	    s.setSoLinger(true,0);
	    s.close();
	}
	catch (IOException ignore) {}
    }
}

}
