package coinTraveler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.ContentFeed;
import com.google.gdata.data.sites.SitesLink;
import com.google.gdata.util.*;

// Script to download JSON from Overpass API file and upload to Google Site.
// Parameters:
//  1. Google account username (and Google Site domain name).
//  2. Google account password.
//  3. Local path for file.  The file's name will be what you want to update on Google Site.    

public class UpdateJsonOnSite {

    private static final String domain = "site";
    private static String sitename;
    public static final String APP_NAME = "CoinTraveler-v1.0";
    public static String username;
    public static String password;
    public static String filename;
    public static SitesService service;
    
    public static void main(String[] args) {
        
        username = args[0];
        sitename = args[0];
        password = args[1];
        filename = args[2];
        File jsonFileToUpload = new File(filename);
        int freshTime = 1000 * 60 * 60 * 12; //12 hours
        
        // If file already recently modified then don't do any further steps. 
        if(jsonFileToUpload.lastModified() > new java.util.Date().getTime() - freshTime){
           System.out.println("JSON data file recently modified.  Quiting");
           System.exit(100);
           return;
        }
        
        // Download the newest data from Overpass API.
        System.out.println("Downloading the newest data from Overpass API.");
        BufferedReader reader=null;
        String jsonData = null;
        try {
            URL url = new URL("http://overpass.osm.rambler.ru/cgi/interpreter?data=[out:json];(node[%22payment:bitcoin%22=yes];%3E;);out;");
            reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(jsonFileToUpload.getAbsolutePath(), "UTF-8");
            
            for (String line; (line = reader.readLine()) != null;) {
                jsonData = jsonData == null ? line: jsonData + "\n" + line; 
            }
            out.write(jsonData);
            out.flush();
            
        }catch(Exception e){
            System.out.println("Error downloading newest data from Overpass API.");
            e.printStackTrace();
            System.exit(1);
            return;
        }
        finally {
            if (reader != null) try { reader.close(); } catch (IOException ignore) {}
        }
        
        // Validate the downloaded JSON.
        try {
            JSONObject downloadedJson = new JSONObject(jsonData);
            JSONArray  mapNodes =  downloadedJson.getJSONArray("elements");
            System.out.println("Fetch JSON has " + mapNodes.length() +" map nodes."); 
        } catch(JSONException ex) { 
            System.out.println("Error validating JSON.  Quiting.");
            ex.printStackTrace();
            System.exit(1);
            return;
        }
        
        // Login using google username/password.
        service = new SitesService(APP_NAME);
        try{
                service.setUserCredentials(username, password);
        }catch(AuthenticationException e){
            System.out.println("Error logging in with username=" + username + "\nError=" + e.toString());
            System.exit(1);
            return;
        }
        System.out.println("Successfully logged in to Google Sites.");
        
        // Find the filename that matches our upload file's name
        HashMap<String,String> fileInfo = findFilename(jsonFileToUpload.getName());
        
        if(fileInfo.isEmpty()){
            System.out.println("ERROR: Unable to find existing file named '" + jsonFileToUpload.getName() + "'.  Quiting.");
            System.exit(1);
            return;
        }
        
        // Update the existing files on Google Site.
        try{
            uploadAttachment(jsonFileToUpload, fileInfo.get("parentID"), fileInfo.get("editID"), fileInfo.get("eTag"), jsonFileToUpload.getName());
        }catch(Exception e){ 
            System.out.println("Error trying to upload new file version");
            e.printStackTrace();
            System.exit(1);
            return;
        }
        
        System.out.println("Upload succeeded!  Done!");
        System.exit(0);
    }
    
    // Find the existing Google Site file to update.
    static HashMap<String,String> findFilename(String filename){
        ContentFeed contentFeed;
        HashMap<String,String> fileInfo = new HashMap<String,String>();
        try {
             contentFeed = service.getFeed(new URL("https://sites.google.com/feeds/content/" + domain + "/" + sitename + "/"), ContentFeed.class);
             for (AttachmentEntry entry : contentFeed.getEntries(AttachmentEntry.class)) {
                 if (!entry.getTitle().getPlainText().equals(filename)) { continue; }
                 if (entry.getParentLink() == null) { continue; }
                 
                 System.out.println("Found existing file named '" + filename + "'");
                 fileInfo.put("editID", getEntryId(entry));
                 fileInfo.put("parentID",getEntryId(entry.getParentLink().getHref()));
                 fileInfo.put("eTag", entry.getEtag());
             }
        } catch (Exception e) {
            System.out.println("Error trying to find existing file named '" + filename + "'");
            e.printStackTrace();
        } 
        return fileInfo;
        
    }
    
    // Upload the new JSON file into Google Site.
    public static AttachmentEntry uploadAttachment(File file, String parentId, String editId, String eTag, String title) throws IOException, ServiceException  {
        //String fileMimeType = mediaTypes.getContentType(file);
        String parentLink = "https://sites.google.com/feeds/content/" + domain + "/" + sitename + "/" + parentId;
        String editLink = "https://sites.google.com/feeds/content/" + domain + "/" + sitename + "/" + editId;
        
        AttachmentEntry newAttachment = new AttachmentEntry();
        newAttachment.setMediaSource(new MediaFileSource(file, "application/json"));
        newAttachment.setTitle(new PlainTextConstruct(title));
        newAttachment.setSummary(new PlainTextConstruct(""));
        newAttachment.addLink(SitesLink.Rel.PARENT, Link.Type.ATOM, parentLink);
        newAttachment.setEtag(eTag);
        
        URL editURL=new URL(editLink);
        
        return service.updateMedia(editURL, newAttachment);
      }
    
    private static String getEntryId(BaseContentEntry<?> entry) {
        String selfLink = entry.getSelfLink().getHref();
        return selfLink.substring(selfLink.lastIndexOf("/") + 1);
      }
    private static String getEntryId(String selfLink) {
        return selfLink.substring(selfLink.lastIndexOf("/") + 1);
      }
}
