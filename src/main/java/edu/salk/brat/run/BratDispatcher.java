package edu.salk.brat.run;

import edu.salk.brat.analysis.SegmentationReader;
import edu.salk.brat.gui.fx.log.BratFxLogHandler;
import edu.salk.brat.gui.fx.log.LogQueue;
import edu.salk.brat.gui.fx.log.DebugLogFormatter;
import edu.salk.brat.gui.fx.log.SingleLineFormatter;
import edu.salk.brat.parameters.ParamLocation;
import edu.salk.brat.parameters.Parameters;
import edu.salk.brat.utility.DirectoryReader;
import edu.salk.brat.utility.ExceptionLog;
import edu.salk.brat.utility.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BratDispatcher {
//	private final static ClassLoader classloader = BratDispatcher.class.getClassLoader();
	private final static Logger log=Logger.getLogger("edu.salk.brat");
	private Thread thread;


	public BratDispatcher(){
	}

	void runHeadless(){
//		log.config("starting headless run.");
//		String strBaseDir=System.getenv("BRAT_BASEDIR");
//		log.config(String.format("ENV: BRAT_BASEDIR=%s",strBaseDir));
//		prefs_basic.put("baseDirectory",strBaseDir);
//
//		String strFlipHorizontal=System.getenv("BRAT_FLIPHORIZONTAL");
//		log.config(String.format("ENV: BRAT_FLIPHORIZONTAL=%s",strFlipHorizontal));
//		prefs_basic.put("flipHorizontal",strFlipHorizontal);
//
//		String strFilesetNr=System.getenv("BRAT_FILESETNR");
//		log.config(String.format("ENV: BRAT_FILESETNR=%s",strFilesetNr));
//		int filesetNr = Integer.parseInt(System.getenv("BRAT_FILESETNR"));
//
//		String strHaveDayZero=System.getenv("BRAT_HAVEDAYZERO");
//		log.config(String.format("ENV: BRAT_HAVEDAYZERO=%s",strHaveDayZero));
//		prefs_basic.put("haveDayZeroImage",strHaveDayZero);
//
//		readBaseDirectory();
//		String filesetKey = new ArrayList<>(filesets.keySet()).get(filesetNr);
//		Map<Integer, String> workSet = filesets.get(filesetKey);
//		for(String f:workSet.values()){
//			log.config(String.format("prepared for working on: %s",f));
//		}
//		PlateSet plateSet=new PlateSet(workSet);
//		plateSet.run();
//
	}

	public void runFromGUI(){
		Handler[] curHandlers = log.getHandlers();
		for (Handler h : curHandlers) {
			if (h instanceof ConsoleHandler) {
				h.setLevel(Level.parse(Parameters.logLevel.getValue()));
			}
		}

		thread = new Thread(() -> {
			log.info("starting gui run.");
			try {
				logConfiguration();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}

			Map<String,Map<Integer,String>> filesets = DirectoryReader.read(Parameters.baseDirectory.getValue());
			Map<String,Map<Integer,String>> origsets = DirectoryReader.read(Parameters.origDirectory.getValue());

			if(filesets.size()==0){
				log.warning("No images found. Terminating.");
				return;
			}
			int numThreads = Integer.parseInt(Parameters.numThreads.getValue());
			log.info(String.format("Starting thread pool with %d threads.", numThreads));
			ThreadPoolExecutor executorPool = new ThreadPoolExecutor(numThreads,numThreads,Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>());

			for(Map.Entry<String, Map<Integer, String>> entry:filesets.entrySet()){
				Map<Integer, String> origSet = origsets.get(entry.getKey());
				SegmentationReader segReader = new SegmentationReader(entry.getKey(), entry.getValue(), origSet);
				executorPool.execute(segReader);
			}

			while(executorPool.getCompletedTaskCount()<executorPool.getTaskCount()){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					log.severe("thread shutdown requested.");
					executorPool.shutdownNow();
					break;
				}
				catch (Exception e) {
					log.severe(String.format("Unhandled Exception: %s", e.getMessage()));
					log.severe(ExceptionLog.StackTraceToString(e));
				}
			}
			log.info("All threads terminated. Shutting down.");
			executorPool.shutdown();
		});
		thread.start();
	}

	public void shutdown(){
		thread.interrupt();
	}

	public void initLogger(LogQueue logQueue, final String version) {
		Logger rootLog = LogManager.getLogManager().getLogger("");
		rootLog.setLevel(Level.ALL);

		Handler[] curHandlers = rootLog.getHandlers();
		for (Handler h : curHandlers) {
			rootLog.removeHandler(h);
		}

		Logger log = Logger.getLogger("edu.salk.brat");

		Level logLevel = Level.parse(Parameters.logLevel.getValue());
		log.setLevel(Level.ALL);

		curHandlers = log.getHandlers();
		for (Handler h : curHandlers) {
			if (h instanceof ConsoleHandler || h instanceof BratFxLogHandler) {
				log.removeHandler(h);
			}
		}

		Handler h = new ConsoleHandler();
		h.setLevel(logLevel);
		if (logLevel.intValue() >= Level.FINE.intValue()) {
			h.setFormatter(new SingleLineFormatter());
		} else {
			h.setFormatter(new DebugLogFormatter());
		}
		h.setLevel(logLevel);
		log.addHandler(h);

		if (logQueue != null) {
			BratFxLogHandler guiLogHandler = new BratFxLogHandler(logQueue);
			guiLogHandler.setLevel(Level.ALL);
			log.addHandler(guiLogHandler);
		}
		log.info("Brat Logging started.");
		log.info(String.format("Brat version %s", version));
	}

	private void logConfiguration() throws BackingStoreException {
		String lsep = System.getProperty("line.separator");
		StringBuilder sb =new StringBuilder();
		sb.append(String.format("Configuration%s", lsep));
		for (Parameters p:Parameters.values()){
			sb.append(String.format("\t%s: %s%s", p.name(), p.getValue(), lsep));
		}
		Preferences lPrefs = ParamLocation.layout.getPrefs().node("Layout_"+Parameters.selectedLayout.getValue());
		sb.append(lsep);
		sb.append(String.format("\tLayout%s", lsep));
		for (String key:lPrefs.keys()){
			sb.append(String.format("\t\t%s: %s%s", key, lPrefs.get(key, null), lsep));
		}
		log.config(sb.toString());
	}
//	private void readBaseDirectory(){
//		log.info("Analyzing base directory.");
//		filesets= new TreeMap<>();
//
//		File dir=new File(Parameters.baseDirectory.getValue());
//		String[] filenames=dir.list(new ImageFilter());
//
//		Pattern expPattern=Pattern.compile(Parameters.experimentIdentifier.getValue());
//		Pattern setPattern=Pattern.compile(Parameters.setIdentifier.getValue());
//		Pattern platePattern=Pattern.compile(Parameters.plateIdentifier.getValue());
//		Pattern timePtPattern = Pattern.compile(Parameters.timePtIdentifier.getValue());
//		Pattern intPattern = Pattern.compile("\\d+");
//
//		assert filenames != null;
//		int minFilesetSize=Integer.MAX_VALUE;
//		int maxFilesetSize=Integer.MIN_VALUE;
//		for(String filename:filenames){
//			String trimmedName=FileUtils.removeExtension(filename);
//			String setID="";
//			Matcher matcher=expPattern.matcher(trimmedName);
//			if(matcher.find()){
//				setID+=matcher.group().replaceAll("_","");
//			}
//			else {
//				continue;
//			}
//			setID+="_";
//			matcher=setPattern.matcher(trimmedName);
//			if(matcher.find()){
//				setID+=matcher.group();
//			}
//			else {
//				continue;
//			}
//			setID+="_";
//			matcher=platePattern.matcher(trimmedName);
//			if(matcher.find()){
//				String m = matcher.group();
//				matcher = intPattern.matcher(m);
//				if (matcher.find()) {
//					setID += matcher.group();
//				}
//				else {
//					continue;
//				}
//			}
//			else {
//				continue;
//			}
//
//			String timePtID;
//			matcher = timePtPattern.matcher(trimmedName);
//			if(matcher.find()){
//				timePtID = matcher.group();
//				matcher = intPattern.matcher(timePtID);
//				if (matcher.find()) {
//					timePtID = matcher.group();
//				}
//				else {
//					continue;
//				}
//			}
//			else {
//				continue;
//			}
//
//			if(!filesets.containsKey(setID)){
//				filesets.put(setID, new TreeMap<>());
//			}
//			filesets.get(setID).put(Integer.parseInt(timePtID), filename);
//		}
//		for(Map<Integer,String> set:filesets.values()){
//			int curSetSize=set.size();
//			if(minFilesetSize>curSetSize){
//				minFilesetSize=curSetSize;
//			}
//			if(maxFilesetSize<curSetSize){
//				maxFilesetSize=curSetSize;
//			}
//		}
//		log.info(String.format("Found %d image sets. (min/max set size = %d/%d)", filesets.size(),
//				minFilesetSize!=Integer.MAX_VALUE ? minFilesetSize : 0,
//				maxFilesetSize!=Integer.MIN_VALUE ? maxFilesetSize : 0));
//	}


//	private class ImageFilter implements FilenameFilter{
//		String fileExt;
//
//		ImageFilter(){
//			fileExt = Parameters.fileExtension.getValue();
//			if(!fileExt.startsWith(".")){
//				fileExt="."+fileExt;
//			}
//		}
//
//		@Override
//		public boolean accept(File dir, String name) {
//			return name.endsWith(fileExt);
//		}
//	}

//	private class DayComparator implements Comparator<String>{
//		Pattern dayPattern=Pattern.compile(prefs_advanced.get("dayIdentifier",null));
//		Pattern intPattern=Pattern.compile("\\d+");
//
//		@Override
//		public int compare(String o1, String o2) {
//			Matcher m=dayPattern.matcher(o1);
//			Integer day1=null;
//			if(m.find()){
//				Matcher m2=intPattern.matcher(m.group());
//				if(m2.find()){
//					day1=Integer.parseInt(m2.group());
//				}
//
//			}
//			if(day1==null){
//				return 1;
//			}
//			m=dayPattern.matcher(o2);
//			Integer day2=null;
//			if(m.find()){
//				Matcher m2=intPattern.matcher(m.group());
//				if(m2.find()){
//					day2=Integer.parseInt(m2.group());
//				}
//
//			}
//			if(day2==null){
//				return -1;
//			}
//
//			return day1.compareTo(day2);
//		}
//
//	}
}

