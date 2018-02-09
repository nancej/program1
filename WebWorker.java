/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection.
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring
* the fact that the entirety of the webserver execution might be handling
* other clients, too.
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format).
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String path = readHTTPRequest(is);

      //Path includes addition /, remove this dash
  	  path = path.substring(1);

  	  //At this point we need to distinguish between GIF, JPEG PNG image files
  	  String contentType = "";
  	  //Check GIF
  	  if(path.toLowerCase().contains((".GIF").toLowerCase()) == true){
  		contentType = "image/gif";
  	  }
  	  //Check JPEG
  	  else if(path.toLowerCase().contains((".JPEG").toLowerCase()) == true){
  		contentType = "image/jpeg";
  	  }
  	  //Check PNG
  	  else if(path.toLowerCase().contains((".PNG").toLowerCase()) == true){
  		contentType = "image/png";
  	  }
  	  //At this point, file can be assumed to be text/html
  	  else{
  		contentType = "text/html";
  	  }
  	  //Write HTTP header based on file extension
  	  writeHTTPHeader(os,contentType,path);
      writeContent(os,path,contentType);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line;
   String holdGetLine = "";
   String holdPath = "";

   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();

         //Print every line to the terminal
         System.err.println("Request line: ("+line+")");

         //Check if line is = 'Get'
         if(line.length() > 0){
        	 holdGetLine = line.substring(0,3);
         }

         if(holdGetLine.equals("GET")){

        	 //Get rest of string excluding 'GET'
           //os.write("Date: ".getBytes());
        	 //Note path beings after GET_ with a space thus the get string after four character
        	 holdPath = line.substring(4);
        	 holdPath = holdPath.substring(0, holdPath.indexOf(" "));
        	 System.err.println("Requested pwd is: " +holdPath);
         }

         //If done reading lines, break
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return holdPath;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String path) throws Exception
{

   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));

   //Checking if file exists write request as okay
 	File f = new File(path);
 	if(f.exists() && !f.isDirectory()){
 		os.write("HTTP/1.1 200 OK\n".getBytes());
   		os.write("Date: ".getBytes());
   		os.write((df.format(d)).getBytes());
   		os.write("\n".getBytes());
   		os.write("Server: Jon's very own server\n".getBytes());
   		//os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   		//os.write("Content-Length: 438\n".getBytes());
   		os.write("Connection: close\n".getBytes());
   		os.write("Content-Type: ".getBytes());
   		os.write(contentType.getBytes());
   		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   }
 	//Put status of 404
 	else{
 		os.write("HTTP/1.1 404 Not Found \n".getBytes());
   		os.write((df.format(d)).getBytes());
   		os.write("\n".getBytes());
   		os.write("Server: Jon's very own server\n".getBytes());
	    os.write("Connection: close\n".getBytes());
  		os.write("Content-Type: ".getBytes());
  		os.write(contentType.getBytes());
	    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
 	}

   return;
}



/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String path, String contentType)throws Exception{
	//Check if file exists
	File f = new File(path);

	if(contentType.equals("text/html") == true){

		if(f.exists() && !f.isDirectory()){
			// Open the file
			FileInputStream fstream = new FileInputStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			//Create string to hold lines to read to os.write
			String strLine;

			//Read File Line By Line
			while ((strLine = br.readLine()) != null){

				//Check for date
				if(strLine.toLowerCase().contains(("<cs371date>").toLowerCase()) == true){
					Date dateobj = new Date();

					String finalString = strLine.replaceAll("<cs371date>", dateobj.toString());
					os.write(finalString.getBytes());
					os.write("<br>".getBytes());
				}
				//Check server
				else if(strLine.toLowerCase().contains(("<cs371server>").toLowerCase()) == true){
			        os.write("Server's identification string : This is James's server".getBytes());
				}
				//Write what is written in line
				else{
					//Print the using os.write
					os.write(strLine.getBytes());
				}
			}

			//Close the input stream
			br.close();
		}else{
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>Error: 404 Not Found</h3>".getBytes());
			os.write("</body></html>\n".getBytes());
		}
	}//End if "text/html"

	//At this point the file path contains a string
	else if(contentType.toLowerCase().contains(("image").toLowerCase()) == true){
		FileInputStream fileInputStream = new FileInputStream(f);
		byte [] data = new byte[(int)f.length()];
		fileInputStream.read(data);
		fileInputStream.close();

		DataOutputStream fileOutputStream = new DataOutputStream(os);
		fileOutputStream.write(data);
		fileOutputStream.close();
	}
}

} // end class
