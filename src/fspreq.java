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
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import net.fsp.FSPsession;
import net.fsp.FSPstat;
import net.fsp.FSPutil;

public class fspreq implements Runnable
{
    private Socket s;
    private boolean http10;
    private DataInputStream in;
    private DataOutputStream ou;
    private long rangestart=0;     /* where to start download */
    private long rangelength=-1;   /* how many bytes to download */
    private long rangefilesize=-1; /* expected filesize */

    private static DateFormat formatter= new SimpleDateFormat("dd-MMM-yyyy hh:mm", Locale.US);
    public final static int NAMELEN=30;
    public final static int DATELEN=20;

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
              if(s1.equals("range"))
	      {
                  /* Range: bytes = 2 - 5 / 23 */
		  try
		  {
		      String r;
		      long l;
		      int i;
		      byte state;
		      s2=s2.toLowerCase();
		      i=s2.indexOf("bytes");
		      if(i==-1)
			  throw new Exception();
		      for(i=i+5;;i++)
			  if ( s2.charAt(i)=='=')
			  {
			      s2=s2.substring(i+1).trim();
			      break;
			  }

		      //System.out.println("parse start "+s2);
		      if(s2.charAt(0)=='-')
	              {
			  rangestart = -1;
			  state = 1;
		      }
		      else
			  state = 0;
		      StringTokenizer st=new StringTokenizer(s2," \t\r\n-/");
		      while(st.hasMoreTokens())
		      {
		          r=st.nextToken();
		          //System.out.println("r2 "+r);
                          l=Long.valueOf(r).longValue();
			  switch(state)
			  {
			       case 0:
			           rangestart=l;
			           break;
			       case 1:
			           if ( rangestart == -1 )
				       rangelength = l;
				   else
			               rangelength=l-rangestart+1;
			           break;
			       case 2:
			           rangefilesize=l;
				   break;
			       default:
			           throw new Exception();
			   }
			   state++;
		      }
                  }
		  catch (Exception e)
		  {
		    rangestart=0;
		    rangelength=-1;
		    rangefilesize=-1;
	     	fspproxy.send_error(http10?10:9,416,"Unable to parse Range header in request.",ou);
		  }
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
		 !parsed[4].equalsIgnoreCase("gopher") &&
		 !parsed[4].equalsIgnoreCase("ftp") &&
		 !parsed[4].equalsIgnoreCase("http")
		)
	  )
	{
	    fspproxy.send_error(http10?10:9,501,"This proxy speaks only FSP v2 protocol. A fsp://, gopher://, ftp://, http:// URLs are accepted on input.",ou);
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

        /*
	System.out.println("p2="+parsed[2]);
	System.out.println("p3="+parsed[3]);
	*/

	/* try to avoid stat */
	if(parsed[2].endsWith("/") && parsed[3].length()==0)
	{
	    /* fake stat info */
	    stat=new FSPstat();
	    stat.name=parsed[2];
	    stat.type=FSPstat.RDTYPE_DIR;
	}
	else
	{
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
	       fspproxy.send_error(http10?10:9,502,"Can't get directory listing of "+stat.name,ou);
	    }

	    ou.writeBytes("HTTP/1.1 200 Listing\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nContent-Type: text/html\r\nConnection: Close\r\nAccept-Ranges: bytes\r\n\r\n");
	    /* write title */
	    ou.writeBytes("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n<HTML>\n<HEAD>\n<TITLE>Index of ");
	    ou.writeBytes(stat.name);
	    ou.writeBytes("</TITLE>\n</HEAD>\n<BODY>\n<H1>Index of ");
	    ou.writeBytes(stat.name);
	    ou.writeBytes("</H1>\n<PRE>      ");
            ou.writeBytes(text_align("Name",NAMELEN));
	    ou.writeBytes(" ");
	    ou.writeBytes(text_align("Last modified",DATELEN));
	    ou.writeBytes("Size\n<HR>\n");
	    for(int i=0;i<list.length;i++)
	    {
		if(list[i].name.equals(".")) continue;
		if(list[i].name.equals(".."))
		{
		    if(stat.name.equals("/"))
			list[i].name="";
		    else
			list[i].name=stat.name.substring(0,stat.name.lastIndexOf('/',stat.name.length()-2));
		    ou.writeBytes("[DIR] <a href=\""+list[i].name+"/\">Parent directory</a>\n");
		    continue;
		    /*
		    ou.writeBytes("[parent]");
		    */
		}
		if(list[i].type==FSPstat.RDTYPE_DIR)
		{
		    ou.writeBytes("[DIR] ");
		    list[i].name+="/";
		} else
		    ou.writeBytes("[TXT] ");
		int nlen;
		nlen=list[i].name.length();
		ou.writeBytes("<a href=\""+list[i].name+"\">"+list[i].name.substring(0,Math.min(NAMELEN,nlen))+"</a> ");
		for(int q=NAMELEN-nlen;q>0;q--)
		    ou.write(' ');

                ou.writeBytes(text_align(formatter.format(new Date(list[i].lastmod)),DATELEN));
		ou.writeBytes(""+list[i].length/1024);
		ou.writeBytes("k\n");
	    }
	    ou.writeBytes("</PRE>\n<HR>\n");

	    ou.writeBytes("Listing generated by <a href=\"http://fsp.sourceforge.net/fsproxy.html\">"+fsploop.NAME+" "+fsploop.VERSION+"</a>.\n");
	    ou.close();
	    return;
	}

	/* transfer file */

	/* do we want range transfer? */
	if(rangestart>0 || rangelength > -1)
	{
	    /* yes, check user suplied valuez */
	    if ( rangestart == -1 )
		rangestart = stat.length - rangelength;
	    if (rangelength == -1 )
		rangelength = stat.length - rangestart;
	    if (
		rangestart + rangelength > stat.length ||
		(rangefilesize != stat.length && rangefilesize>=0 )
	       )
	     	    fspproxy.send_error(http10?10:9,416,"Specified Range is incorrect for this file.",ou);
            /* Do partial file transfer */
	    ou.writeBytes("HTTP/1.1 206 Partial\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nAccept-Ranges: bytes\r\nContent-Type: "+fspproxy.guessContentType(stat.name)+"\r\nContent-length: "+rangelength+"\r\nContent-Range: bytes "+rangestart+"-"+(rangestart+rangelength-1)+"/"+stat.length+"\r\nConnection: Close\r\nLast-Modified: "+new Date(stat.lastmod).toGMTString()+"\r\n\r\n");
	    FSPutil.download(ses,stat.name,ou,rangestart,rangelength);
	    ou.close();
	    return;
	}

	ou.writeBytes("HTTP/1.1 200 Transfering\r\nConnection: Close\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\nAccept-Ranges: bytes\r\nContent-Type: "+fspproxy.guessContentType(stat.name)+"\r\nContent-length: "+stat.length+"\r\nLast-Modified: "+new Date(stat.lastmod).toGMTString()+"\r\n\r\n");
	FSPutil.download(ses,stat.name,ou,0,-1);
	ou.close();
	return;
    }
    catch (IOException err)
    {
	try
	{
	    // linger close
	    //System.out.println(err);
	    s.setSoLinger(true,0);
	    s.close();
	}
	catch (IOException ignore) {}
    }
}

final static public String text_align(String text,int data)
{
    if(text.length()>=data) return text.substring(0,data);
    StringBuffer sb=new StringBuffer(data);
    sb.append(text);
    for(int i=data-text.length();i>0;i--)
	sb.append(' ');
    return sb.toString();
}

}
