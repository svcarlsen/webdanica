package dk.kb.webdanica.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import dk.kb.webdanica.datamodel.IngestLog;
import dk.kb.webdanica.datamodel.IngestLogDAO;
import dk.kb.webdanica.datamodel.Seed;
import dk.kb.webdanica.datamodel.SeedDAO;
import dk.kb.webdanica.datamodel.URL_REJECT_REASON;
import dk.kb.webdanica.utils.UrlUtils;

/**
 * Program to load seeds into the webdanica database.
 * Policy: 
 *  - Seeds already present in the database is ignored
 *  - seeds that are not proper URLs are not loaded into the database
 *  - seeds that have extensions matching any of the suffixes in our ignored suffixes are also skipped
 *  
 *  Remember to call program with -Dwebdanica.settings.file=$LOADSEEDS_HOME/webdanica_settings_file
 *  and -Ddk.netarkivet.settings.file=$LOADSEEDS_HOME/settings_NAS_Webdanica.xml
 *  and -Dlogback.configurationFile=$LOADSEEDS_HOME/silent_logback.xml 
 *  
 *  TESTED with webdanica-core/src/main/resources/outlink-reportfile-final-1460549754730.txt
 *  TESTED with webdanica-core/src/main/resources/outlinksWithAnnotations.txt
 *  TESTED with webdanica-core/src/main/resources/webdanica-seeds.table
 */
public class LoadSeeds {

	private File seedsfile;
    private boolean writeAcceptLog = false;
    private boolean writeRejectLog = false;
	private File rejectLog = null;
	private File acceptLog = null;
	private List<String> acceptedList = new ArrayList<String>();
	
	public LoadSeeds(File seedsfile) {
	   this.seedsfile = seedsfile;
    }

	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println("Need seedsfile as argument");
			System.exit(1);
		}
		File seedsfile = new File(args[0]);
		if (!seedsfile.isFile()){
			System.err.println("The seedsfile located '" + seedsfile.getAbsolutePath() + "' does not exist or is not a proper file");
			System.exit(1);
		}
		
		System.out.println("Processing seeds from file '" + seedsfile.getAbsolutePath() + "'");
		
		System.out.println();
		LoadSeeds loadseeds = new LoadSeeds(seedsfile);
		loadseeds.writeAcceptLog = true;
		loadseeds.writeRejectLog = true;
		
		IngestLog res = loadseeds.processSeeds();
		System.out.println(res.getStatistics());
		System.out.println("Acceptlog in file: " + loadseeds.getAcceptLog());
		System.out.println("Rejectlog in file: " + loadseeds.getRejectLog());
		
	}
	/**
	 * @return the ingestLog for the file just processed
	 */
	public IngestLog processSeeds() {
		SeedDAO dao = SeedDAO.getInstance();
		String line;
        long linecount=0L;
        long insertedcount=0L;
        long rejectedcount=0L;
        long duplicatecount=0L;
        String trimmedLine = null;
        BufferedReader fr = null;
        List<String> logentries = new ArrayList<String>();
        try {
        	fr = new BufferedReader(new FileReader(seedsfile));
	        while ((line = fr.readLine()) != null) {
	            trimmedLine = line.trim();
	            
	            trimmedLine = removeAnnotationsIfNecessary(trimmedLine);
	         
	            linecount++;
	            URL_REJECT_REASON rejectreason = UrlUtils.isRejectableURL(trimmedLine);
	            if (rejectreason == URL_REJECT_REASON.NONE) {
	            	Seed singleSeed = new Seed(trimmedLine);
	            	boolean inserted = dao.insertSeed(singleSeed);
	            	if (!inserted) { // assume duplicate url 
	            		rejectreason = URL_REJECT_REASON.DUPLICATE;
	            		duplicatecount++;
	            	} else {
	            		insertedcount++;
	            		acceptedList.add(trimmedLine);
	            	}
	            }
	            if (rejectreason != URL_REJECT_REASON.NONE) {
	            	logentries.add(rejectreason + ": " + trimmedLine);
	            	rejectedcount++;
	            }
	            
	        }
	        
	        // write accept-log
	        if (writeAcceptLog) {
	        	acceptLog = new File(seedsfile.getParentFile(), seedsfile.getName() + ".accepted.txt");
	        	int count=0;
	        	while (acceptLog.exists()) {
	        		acceptLog = new File(seedsfile.getParentFile(), seedsfile.getName() + ".accepted.txt" + "." + count);
	        		count++;
	        	} 
	        	PrintWriter acceptWriter = new PrintWriter(new BufferedWriter(new FileWriter(acceptLog)));
	        	String acceptHeader = "Acceptlog for file '" + seedsfile.getAbsolutePath() + "' ingested at '" 
	        			+ new Date() + "'";
	        	String stats = "total lines: " + linecount + ", accepted = " + insertedcount + ", rejected=" + rejectedcount 
	        			+ " (of which " + duplicatecount + " duplicates";
	        	acceptWriter.println(acceptHeader);
	        	acceptWriter.println(stats);
	        	if (!acceptedList.isEmpty()) {
	        		acceptWriter.println("The " + insertedcount + " accepted :");
	        		for (String acc: acceptedList) {
	        			acceptWriter.println(acc);
	        		} 
	        	} else {
	        		acceptWriter.println("None were accepted!");
	        	}
	        	
	        	acceptWriter.close();
	        }
	        
	        // write reject-log
	        if (writeRejectLog) {
	        	rejectLog = new File(seedsfile.getParentFile(), seedsfile.getName() + ".rejected.txt");
	        	int count=0;
	        	while (rejectLog.exists()) {
	        		rejectLog = new File(seedsfile.getParentFile(), seedsfile.getName() + ".rejected.txt" + "." + count);
	        		count++;
	        	}
	        	PrintWriter rejectWriter = new PrintWriter(new BufferedWriter(new FileWriter(rejectLog)));
	        	String rejectHeader = "Rejectlog for file '" + seedsfile.getAbsolutePath() + "' ingested at '" 
	        			+ new Date() + "'";
	        	String stats = "total lines: " + linecount + ", accepted = " + insertedcount + ", rejected=" + rejectedcount 
	        			+ " (of which " + duplicatecount + " duplicates";
	        	rejectWriter.println(rejectHeader);
	        	rejectWriter.println(stats);
	        	rejectWriter.println("Rejected seeds:");
	        	for (String rej: logentries) {
	        		rejectWriter.println(rej);
	        	}
	        	rejectWriter.close();
	        }
	        
        } catch (Throwable e) {
	        e.printStackTrace();
        } finally {
        	IOUtils.closeQuietly(fr);
        	dao.close();
        }
        
	    IngestLog logresult = logIngestStats(logentries, linecount, insertedcount, rejectedcount, duplicatecount); 
	    return logresult;
	}

	private String removeAnnotationsIfNecessary(String trimmedLine) {
	    String[] trimmedParts = trimmedLine.split(" ");
	    if (trimmedParts.length > 1) {
	    	return trimmedParts[0];
	    } else {
	    	return trimmedLine;
	    }
    }

	private IngestLog logIngestStats(List<String> logentries, long linecount, long insertedcount, long rejectedcount, long duplicatecount) {
		IngestLogDAO dao = null;
		try {
			dao = IngestLogDAO.getInstance();
			IngestLog log = new IngestLog(logentries, seedsfile.getName(), linecount, insertedcount, rejectedcount, duplicatecount);
			dao.insertLog(log);
			return log;
		} finally {
			dao.close();
		}
	}
	
	File getRejectLog() {
		return this.rejectLog;
	}
	
	File getAcceptLog() {
		return this.acceptLog;
	}
}