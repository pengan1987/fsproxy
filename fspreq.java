import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

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
	    ou=new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

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
          }
	String req2=null;

	if(!req.startsWith("GET ")) 
	{
	     fspproxy.send_error(http10?10:9,501,"Only GET access method is possible for UI",ou);
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
	/* process request */
        // send_reply(ui.process(req2));
    }
    catch (IOException err)
    {
    }
}

}
