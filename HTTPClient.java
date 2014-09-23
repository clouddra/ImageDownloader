// Name: Chong Yun Long         Matriculation No: A0072292H

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;

class HTTPClient {

    public static Pattern p = Pattern.compile("(?i)<img.+?src\\s*?=\\s*?[\'\"](.*?)[\'\"]");
    private File fileDir;
    private String url;
    
    public HTTPClient(File f, String u){
        fileDir = f;
        url = u ;
    }

    public int downloadFiles() throws UnknownHostException, NullPointerException  {

        Vector<String> links = new Vector<String>();
        Vector<String> hosts = new Vector<String>();
        Vector<String> filename = new Vector<String>();
        Vector<String> path = new Vector<String>();

        Socket s = null;
        OutputStream os = null;
        InetAddress host = null;
        InputStreamReader isrServer = null;
        BufferedReader serverReader = null;
        byte buf[] = new byte[1024];
        int length = 0;
        String message;
        String html = "";
        String temp = "";
        FileOutputStream fos = null;

        links.add(0, url);
        
        // extract host name
        extractHost(links, url, hosts, filename, path, null);

        File currentPath = new File(path.get(0)) ;
        host = InetAddress.getByName(hosts.get(0));

        // tries to connect to remote host
        try {
            s = new Socket(host, 80);
            os = s.getOutputStream();
        } catch (IOException ex) {
            System.out.println("I/O exception");
            System.exit(1);
        }

        
        // constructs the http request message
        DataOutputStream serverWriter = new DataOutputStream(os);
        message = "GET " + path.get(0) + " HTTP/1.0\r\nHost: " + hosts.get(0) + "\r\n\r\n";
        try {
            serverWriter.write(message.getBytes());
            isrServer = new InputStreamReader(s.getInputStream());
        } catch (IOException ex) {
            System.out.println("I/O exception");
            System.exit(1);
        }
        
        
        serverReader = new BufferedReader(isrServer);

        // separates the html from the header
        try {
            // read header
            do {
                temp = serverReader.readLine();
                //System.out.println(temp) ;
            } while (!temp.equals(""));

            // read html and concatenate into 1 html string with all whitespace removed 
            while ((temp = serverReader.readLine()) != null) {
                temp = temp.trim();
                html += temp;
            }
        } catch (IOException ex) {
            System.out.println("I/O exception");
            System.exit(1);
        }
        
        // clear all arrays (the initial hostname entered by user)     
        links = new Vector<String>();
        hosts = new Vector<String>();
        filename = new Vector<String>();
        path = new Vector<String>();
        
        // process the html and extract the relevant image links
        extractImgLinks(html, links);
        extractHost(links, host.getHostName(), hosts, filename, path, currentPath);

        // do for all images
        for (int i = 0; i < links.size(); i++) {
            
            
            try {
                fos = new FileOutputStream(fileDir.getPath() + "\\" + filename.get(i), false);
            } catch (FileNotFoundException ex) {
                System.out.print(filename.get(i));
                System.out.println("File cannot be created! Filename may contain special characters");
                continue;
            }
            
            // new connection for each image
            try {
                s = new Socket(InetAddress.getByName(hosts.get(i)), 80);
                os = s.getOutputStream();
            } catch (IOException ex) {
                System.out.println("I/O exception");
                System.exit(1);
            }
            
            // constructs request message
            serverWriter = new DataOutputStream(os);
            message = "GET " + path.get(i) + " HTTP/1.0\r\nHost: " + hosts.get(i) + "\r\n\r\n";
            System.out.print(message);
            try {
                serverWriter.write(message.getBytes());
                DataInputStream imgReader = new DataInputStream(s.getInputStream());

                // read header
                while (!imgReader.readLine().equals("")) {
                    //we ignore all header lines
                }

                // read image data then write to file
                while ((length = imgReader.read(buf)) > 0) {
                    fos.write(buf, 0, length);
                }
            } catch (IOException ex) {
                System.out.println("I/O exception");
                System.exit(1);
            }
            
            try {
                fos.close();
            } catch (IOException ex) {
                System.out.print(filename.get(i));
                System.out.println("File cannot be created! Filename may contain special characters");
                continue;
            }
        }
        System.out.println("Finished downloading all images!");
        return 1;
    }

    // parses a link and splits it into hostname, pathname, filename 
    public static void extractHost(Vector<String> imgLinks, String defaultHost, Vector<String> h, Vector<String> f, Vector<String> p, File cPath) {

        int start = 0, end = 0;
        for (int i = 0; i < imgLinks.size(); i++) {
            
            String temp = imgLinks.get(i);
            
            // external URL
            if (temp.startsWith("//") || temp.startsWith("http://") || temp.startsWith("https://")) {   // for external links.
                start = temp.indexOf("//");
                end = temp.indexOf(("/"), start + 2);
               
                h.add(temp.substring(start + 2, end));   // host starts with "//" and ends with "/"
                p.add(temp.substring(end));     // path is after hostname
                f.add(temp.substring(temp.lastIndexOf("/") + 1));
            
                
            } 
            // current host 
            else {

                // if the current path name is a file in a folder, we have to get the full path 
                if (cPath.getParent()!=null)
                    temp = (cPath.getParent().replace("\\","/") + "/" + temp);
                
                h.add(defaultHost); // download from the default host if only pathname is supplied
                p.add("/"+temp);
                f.add(temp.substring(temp.lastIndexOf("/") + 1));
            }

        }
    }

    // get src attributes in <img>
    public static void extractImgLinks(String input, Vector<String> imgLinks) {
        Matcher findImgLinks = p.matcher(input);
        while (findImgLinks.find()) {
            String temp = findImgLinks.group(1) ;
            if (!imgLinks.contains(temp))   // prevent duplicate entries
                imgLinks.add(temp);
        }

    }
}