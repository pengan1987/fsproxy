/*
Copyright (c) 2003-2004 by Radim HSN Kolar (hsn@netmag.cz)

You may copy or modify this file in any manner you wish, provided
that this notice is always included, and that you hold the author
harmless for any loss or damage resulting from the installation or
use of this software.

		     This is a free software.  Be creative. 
		    Let me know of any bugs and suggestions.
*/
import java.io.*;
import java.util.StringTokenizer;
import java.util.Date;

/* stand-alone version of fsp -> http proxy server */
public class fspproxy
{

    public static int client_timeout;
    public static boolean trace_url;
    public static String defaultype="application/octet-stream";
   
    public static void print_usage()
    {
	System.out.println("java fspproxy [port number] [mime types file]");
	System.exit(1);
    }
    	
    public static void main(String argv[])
    {
	client_timeout=500000;
	trace_url=true;
	int port=9090;
	if(argv.length>0)
	{   
	    try
	    { 
	       port=Integer.valueOf(argv[0]).intValue();
	    }
	    catch (Exception z)
	    {
		print_usage();
            }
	    if(argv.length>1)
	    {
                System.out.println(new Date()+" Using user defined MIME types table from "+argv[1]);
                loadMimeTypes(argv[1]);
	    }
	}    
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
       bd.append("Generated by <a href=\"http://fsp.sourceforge.net/fsproxy.html\">"+fsploop.NAME+" "+fsploop.VERSION+"</a>.\n");
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

    /* code from Smart Cache mgr.java */
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

 public final static void loadMimeTypes(String fname)
 {
  if(fname==null) return;
  File f=new File(fname);
  if(!f.isFile())
  {
      System.err.println("[ERROR] Can't load mime.types from file '"+fname+"'");
      return;
  }
  try
  {
    BufferedReader in=new BufferedReader(new LineNumberReader(new FileReader(fname)));
    String line;
    StringTokenizer st;
    String mimetype,ext;
    // init GT
    guesstable=new String[0];
    while(true)
    {
	    ext=mimetype=null;
	    line=in.readLine();
	    if(line==null) break;
	    st=new StringTokenizer(line);
	    if(!st.hasMoreTokens()) continue;
	    mimetype=st.nextToken();
	    if(mimetype.startsWith("#")) continue;
	    while(true)
	    {
		
	    	if(!st.hasMoreTokens()) break;
	    	ext=st.nextToken();
		updateGuessTable(mimetype,ext);
	    }
    }
    in.close();
  }
  catch (IOException grrrrrrrrrrrrr)
   {
    System.err.println("[ERROR] Reading mime.types from "+fname);
   }
 }

 public final static String guessContentType(String fname)
 {
  fname=fname.toLowerCase();
  for(int i=0;i<guesstable.length;i+=2)
   {
    if(fname.endsWith(guesstable[i])) return guesstable[i+1];
   }
  // System.out.println("[WARNING] Can not determine MIME type for "+fname+", defaulting to "+defaultype);
  return defaultype;
 }

 /* pozdejsi koncovka prepise predchozi hodnotu */
 private final static void updateGuessTable(String mimetype,String ext)
 {
 	 if(mimetype==null || ext==null) return;
	 if(mimetype.length()==0 || ext.length()==0) return;
	 ext=("."+ext).toLowerCase();
	 for(int i=0;i<guesstable.length;i+=2)
	 {
		 if(ext.equals(guesstable[i]))
		   {
		     guesstable[i+1]=mimetype;
		     return;
		   }
	 }
	 String tmp[];
	 tmp=new String[guesstable.length+2];
	 System.arraycopy(guesstable,0,tmp,0,guesstable.length);
	 tmp[guesstable.length]=ext;
	 tmp[guesstable.length+1]=mimetype;
	 guesstable=tmp;
}

 static String guesstable[]=
           {
             // java stuff
             // ".java","application/java",
             ".java","text/plain",
             ".class","application/java-vm",
             ".jar","application/java-archive",
	
	     // images
             ".gif","image/gif",
	     ".ief","image/ief",
	     ".tiff","image/tiff",
	     ".tif","image/tiff",
             ".jpeg","image/jpeg",
             ".jpe","image/jpeg",
             ".jpg","image/jpeg",
	     ".png","image/png",
	     ".ras","image/x-cmu-raster",
	     ".bmp","image/x-ms-bmp",
	     ".pnm","image/x-portable-anymap",
	     ".pbm","image/x-portable-bitmap",
	     ".pgm","image/x-portable-graymap",
	     ".ppm","image/x-portable-pixmap",
	     ".rgb","image/x-rgb",
	     ".xbm","image/x-xbitmap",
	     ".xpm","image/x-xpixmap",
	     ".xwd","image/x-xwindowdump",
	
	     // plain text
             ".txt","text/plain",
             ".text","text/plain",
             ".doc","text/plain",
             ".log","text/plain",
	     ".csv","text/comma-separated-values",
	     ".tsv","text/tab-separated-values",

	     // hypertext
             "welcome",  "text/html", // cache-generated index
	     ".css","text/css",
	     ".js","text/javascript",
	     ".pl",  "text/html",
	     ".cgi",  "text/html",
	     ".asp",  "text/html",
	     ".jsp",  "text/html",
             ".htm",  "text/html",
             ".html",  "text/html",
	     ".shtml", "text/html",
             ".htmli",  "text/html",
             ".dchtml",  "text/html",
	     ".pht",   "text/html",
	     ".phtml", "text/html",
	     ".php",   "text/html",
	     ".php3",  "text/html",
	     ".php4",  "text/html",
	     ".php3p", "text/html",
	     ".php4p", "text/html",
	
	     ".texi","application/x-texinfo",
	     ".texinfo","application/x-texinfo",

	     // VRML
	     ".vrm","x-world/x-vrml",
	     ".vrml","x-world/x-vrml",
	     ".wrl" ,"x-world/x-vrml",
	
	     // formated text
	     ".rtx","text/richtext",
             ".pdf","application/pdf",
	     ".rtf","application/rtf",
	     ".ai","application/postscript",
	     ".ps","application/postscript",
	     ".eps","application/postscript",
	     ".wp5","application/wordperfect5.1",
	     ".wk","application/x-123",
	     ".dvi","application/x-dvi",
	     ".frm","application/x-maker",
	     ".maker","application/x-maker",
	     ".frame","application/x-maker",
	     ".fm"   ,"application/x-maker",
	     ".fb"   ,"application/x-maker",
	     ".book", "application/x-maker",
	     ".fbdoc","application/x-maker",

	     // fonts
	     ".pfa","application/x-font",
	     ".pfb","application/x-font",
	     ".gsf","application/x-font",
	     ".pcf","application/x-font",
	     ".pcf.z","application/x-font",
	     ".gf","application/x-tex-gf",
	     ".pk","application/x-tex-pk",

	     // archives
             ".zip","application/zip",
             ".tar","application/x-tar",
	     ".hqx","application/mac-binhex40",
	     ".bcpio","application/x-bcpio",
	     ".cpio","application/x-cpio",
	     ".deb","application/x-debian-package",
             ".gtar","application/x-gtar",
	     ".tgz" ,"application/x-gtar",
	     ".tar.gz","application/x-gtar",
	     ".shar","application/x-shar",
	     ".sit","application/x-stuffit",
	     ".sv4cpio","application/x-sv4cpio",
	     ".sv4crc","application/x-sv4crc",
	
	     //audio
	     ".au","audio/basic",
	     ".snd","audio/basic",
             ".mid","audio/midi",
             ".midi","audio/midi",
             ".mp2","audio/mpeg",
             ".mpega","audio/mpeg",
             ".mpga","audio/mpeg",
             ".mp3","audio/mpeg",
	     ".m3u","audio/mpegurl",
             ".aif","audio/x-aiff",
             ".aiff","audio/x-aiff",
             ".aifc","audio/x-aiff",
	     ".wav","audio/x-wav",
	     ".ra" ,"audio/x-pn-realaudio",
	     ".rm" ,"audio/x-pn-realaudio",
	     ".ram","audio/x-pn-realaudio",

	     //video
	     ".mpeg","video/mpeg",
	     ".mpg","video/mpeg",
             ".mpe","video/mpeg",
             ".qt","video/quicktime",
             ".mov","video/quicktime",
             ".avi","video/x-msvideo",
             ".movie","video/x-sgi-movie",
	     ".dl","video/dl",
	     ".fli","video/fli",
	     ".gl","video/gl",
	     ".asf","video/x-ms-asf",
	     ".asx","video/x-ms-asf",

	     //sources
	     ".tex","text/x-tex",
	     ".c","text/plain",
	     ".cc","text/plain",
	     ".cpp","text/plain",
	     ".h","text/plain",
	     ".hpp","text/plain",
	     ".py","text/plain",
	     ".ltx","text/x-tex",
	     ".sty","text/x-tex",
	     ".cls","text/x-tex",
	     ".latex","application/x-latex",
	     ".oda","application/oda",
	     ".t"  ,"application/x-troff",
	     ".tr" ,"application/x-troff",
	     ".roff","application/x-troff",
	     ".man", "application/x-troff-man",
	     ".me",  "application/x-troff-me",
	     ".ms",  "application/x-troff-ms",
	     ".vcs", "text/x-vCalendar",
	     ".vcf", "text/x-vCard",

	     // misc apps
	     ".csm","application/cu-seeme",
	     ".cu", "application/cu-seeme",
	     ".tsp","application/dsptype",
	     ".spl","application/futuresplash",
	     ".pgp","application/pgp-signature",
             ".wz", "application/x-Wingz",
	     ".dcr","application/x-director",
	     ".dir","application/x-director",
	     ".dxr","application/x-director",
             ".hdf","application/x-hdf",
             ".mif","application/x-mif",
	     ".nc" ,"application/x-netcdf",
	     ".cdf","application/x-netcdf",
	     ".pac","application/x-ns-proxy-autoconfig",
	     ".swf","application/x-shockwave-flash",
	     ".swfl","application/x-shockwave-flash",	
	     ".ustar","application/x-ustar",
	     ".src","application/x-wais-source",
	     ".torrent","application/x-bittorrent",

	     // msdos
	     ".com","application/x-msdos-program",
	     ".exe","application/x-msdos-program",
	     ".bat","application/x-msdos-program",

	     //microsoft apps
	     ".xls","application/excel",
	     ".dot","application/msword",
             ".ppt","application/powerpoint",
	
	     //binary files
	     ".bin","application/octet-stream",

           };
}