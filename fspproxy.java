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
	new fsploop(9090,null).run();
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
	   ou.writeBytes("HTTP/1.0 "+rc+" FSPPROXY\r\nContent-Type: text/html\r\n\r\n");
       }
       ou.writeBytes(bd.toString());
       ou.close();

       throw new EOFException("Request processing canceled");
    }
}
