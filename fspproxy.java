/*
Copyright (c) 2003 by Radim HSN Kolar (hsn@cybermail.net)

You may copy or modify this file in any manner you wish, provided
that this notice is always included, and that you hold the author
harmless for any loss or damage resulting from the installation or
use of this software.

		     This is a free software.  Be creative. 
		    Let me know of any bugs and suggestions.
*/
import java.io.*;

/* stand-alone version of fsp -> http proxy server */
public class fspproxy
{

    public static int client_timeout;
    public static boolean trace_url;
    
    public static void main(String argv[])
    {
	client_timeout=30000;
	trace_url=true;
	int port=9090;
	if(argv.length>0)
	    port=Integer.valueOf(argv[0]).intValue();
	new fsploop(port,null).run();
    }

    /** generates error message and closes the connection */
    public final static void send_error(int httpversion,int rc,String err,
	   DataOutputStream ou) throws IOException
    {
       StringBuffer bd=new StringBuffer(1024); // body

       bd.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n<HTML><HEAD><TITLE>Error</TITLE></HEAD>\n<BODY><h2>");
       bd.append(rc);
       bd.append(" ");
       bd.append(err);
       bd.append("</h2>\r\n");
       bd.append("</BODY></HTML>\r\n");

       if(httpversion>9)
       {
	   /* write HTTP header */
	   ou.writeBytes("HTTP/1.0 "+rc+" FSPPROXY\r\nContent-Type: text/html\r\nServer: "+fsploop.NAME+" "+fsploop.VERSION+"\r\n\r\n");
       }
       ou.writeBytes(bd.toString());
       ou.close();

       throw new EOFException("Request processing canceled");
    }

    /*  prechrousta URL a vrati
    0 - hostname
    1 - port (if any) jinak null
    2 - directory
    3 - file including query string ("" if empty)
    4 - protocol (null if http)
    */

    /* code from Smart Cache */
    final public static String[] parseURL(String url,String proto)
    {
      String[] res=new String[5];
      res[3]=""; /* HashTable do not likes NULL */
      int i,j,seven;

      if(proto!=null)
      {
      /* we allready have protocol parsed */
      res[4]=proto;
      seven=proto.length()+3; /* '://' */

      }
      else 
      {
	  seven=url.indexOf("://");
	  res[4]=url.substring(0,seven);
	  seven+=3;
      }
      /* http -> null */
      if(seven==7)
       {
	char c;
	c=res[4].charAt(0);
	if(c=='h' || c=='H') res[4]=null;
       }

      i=url.indexOf('/',seven);
      if(i==-1) { url+='/'; i=url.length()-1;}

      j=url.indexOf(':',seven);
      /* http://zero.dsd:434/ */
      if(j!=-1 && j<i)  /* mame tu portname */
			{
			  res[0]=url.substring(seven,j).toLowerCase();
			  if(j+1<i)
			  {
			    res[1]=url.substring(j+1,i);
			    if(res[1].equals("80")) res[1]=null;
			  }
			}
	       else
		 {
		  res[0]=url.substring(seven,i).toLowerCase();
		 }

      /* parse adr */

      /* najdeme nejvice vlevo z moznych problemovych znaku */

       j=url.length()-1;
       byte v[];
       v=new byte[j+1];
       url.getBytes(i,j+1,v,i);
       // getChars(i,j,v,0);
       loop1:for(int zz=i;zz<=j;zz++)
	{
	switch(v[zz])
	{
	case 0x3b: // ;
	case 0x3a: // :
	case 0x3d: // =
	case 0x3f: // ?
		  j=zz;break loop1;
	case 0x23: // #
	 url=url.substring(0,zz);break loop1;
	}
	}


       j=url.lastIndexOf('/',j);

      String adr=url.substring(i,j+1); // adresar
      /* Normalize URL - remove /../ */
      /* NOTE: /./ should be also removed, but it do not hurt anything? */
      while( (i=adr.indexOf("/../")) >= 0 )
      {
       int l;
       if( (l=adr.lastIndexOf('/',i-1)) >=0 )
	  adr= adr.substring(0,l)+ adr.substring(i+3);
       else
	 adr=adr.substring(i+3);
      }
      res[2]=adr;
      if(j+1!=url.length()) res[3]=url.substring(j+1);
      return res;
    }

}
