/*
Copyright (c) 2003 by Radim HSN Kolar (hsn@cybermail.net)

You may copy or modify this file in any manner you wish, provided
that this notice is always included, and that you hold the author
harmless for any loss or damage resulting from the installation or
use of this software.

		     This is a free software.  Be creative. 
		    Let me know of any bugs and suggestions.
*/
import java.net.*;
import java.io.IOException;
import java.util.Date;

public class fsploop implements Runnable
{
    public static final String  VERSION="0.3";
    public static final String  NAME="FSP Proxy";

    public static int MAXCLIENTS=15;

    /* port for running proxy server */
    public int serverport;
    public InetAddress serveradr;

    private ServerSocket server;
   
    fsploop(int port, InetAddress adr)
    {
	serverport=port;
	serveradr=adr;

	if(serverport==0) return;
	/* create server socket */
	if(serveradr!=null)
	    try {
		 server=new ServerSocket(serverport,10,serveradr);
	    }
	    catch(IOException e) 
	    {
		  System.err.println("[FSPPROXY] ERROR: Cannot bind to port "+serverport+"/"+serveradr.getHostAddress()+"  Reason: "+e);
		  return;
	    }
	 else
	     try{
		     server=new ServerSocket(serverport);
		 }
	     catch(IOException e) 
	     {
		       System.err.println("[FSPPROXY] ERROR: Cannot bind to my port "+serverport+"/*  Reason: "+e);
		       return;
	     }
    }

    public final void run()
    {
      ThreadGroup clients;
      fspreq client;
      Socket clientSocket;

      if(server==null) return;

      clients=new ThreadGroup("FSPPROXY-http-clients");
      System.out.println(new Date()+" "+NAME+" "+VERSION+" ready on "+serverport+"/"+(serveradr==null?"*":serveradr.getHostAddress()));
       while (true) 
       {
		clientSocket = null;
		client=null;
		try 
		{
		    clientSocket = server.accept();
		} 
		catch (IOException e) 
		{
		    System.err.println("Warning: Accept failed: "+e);
		    continue;
		}
		if(clients.activeCount()>MAXCLIENTS)
		  try 
		  {
		      System.err.println(new Date()+" [FSPPROXY] Warning: Active connections limit ("+MAXCLIENTS+") reached, HTTP request rejected.");
		      clientSocket.setSoLinger(true,0);
		      clientSocket.close();
		  }
		  catch(IOException e) {}
		  finally { continue;}
		client=new fspreq(clientSocket);
		new Thread(clients,client).start();
       }  /* listen */
    }
}
