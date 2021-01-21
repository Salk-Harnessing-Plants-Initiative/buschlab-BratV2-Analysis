package edu.salk.brat.utility;

import edu.salk.brat.parameters.Parameters;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryReader {
    private final static Logger log=Logger.getLogger("edu.salk.brat");

    public static Map<String, Map<Integer, String>> read(String dirPath){
        log.info(String.format("Analyzing directory \"%s\"", dirPath));
        Map<String, Map<Integer, String>> filesets= new TreeMap<>();

        File dir=new File(dirPath);
        String[] filenames=dir.list(new ImageFilter());

        Pattern expPattern=Pattern.compile(Parameters.experimentIdentifier.getValue());
        Pattern setPattern=Pattern.compile(Parameters.setIdentifier.getValue());
        Pattern platePattern=Pattern.compile(Parameters.plateIdentifier.getValue());
        Pattern timePtPattern = Pattern.compile(Parameters.timePtIdentifier.getValue());
        Pattern intPattern = Pattern.compile("\\d+");

        assert filenames != null;
        int minFilesetSize=Integer.MAX_VALUE;
        int maxFilesetSize=Integer.MIN_VALUE;
        for(String filename:filenames){
            String trimmedName= filename; //FileUtils.removeExtension(filename);
            String setID="";
            Matcher matcher=expPattern.matcher(trimmedName);
            if(matcher.find()){
                setID+=matcher.group().replaceAll("_","");
            }
            else {
                continue;
            }
            setID+="_";
            matcher=setPattern.matcher(trimmedName);
            if(matcher.find()){
                setID+=matcher.group();
            }
            else {
                continue;
            }
            setID+="_";
            matcher=platePattern.matcher(trimmedName);
            if(matcher.find()){
                String m = matcher.group();
                matcher = intPattern.matcher(m);
                if (matcher.find()) {
                    setID += matcher.group();
                }
                else {
                    continue;
                }
            }
            else {
                continue;
            }

            String timePtID;
            matcher = timePtPattern.matcher(trimmedName);
            if(matcher.find()){
                timePtID = matcher.group();
                matcher = intPattern.matcher(timePtID);
                if (matcher.find()) {
                    timePtID = matcher.group();
                }
                else {
                    continue;
                }
            }
            else {
                continue;
            }

            if(!filesets.containsKey(setID)){
                filesets.put(setID, new TreeMap<>());
            }
            filesets.get(setID).put(Integer.parseInt(timePtID), filename);
        }
        for(Map<Integer,String> set:filesets.values()){
            int curSetSize=set.size();
            if(minFilesetSize>curSetSize){
                minFilesetSize=curSetSize;
            }
            if(maxFilesetSize<curSetSize){
                maxFilesetSize=curSetSize;
            }
        }
        log.info(String.format("Found %d image sets. (min/max set size = %d/%d)", filesets.size(),
                minFilesetSize!=Integer.MAX_VALUE ? minFilesetSize : 0,
                maxFilesetSize!=Integer.MIN_VALUE ? maxFilesetSize : 0));

        return filesets;
    }

    private static class ImageFilter implements FilenameFilter {
        String fileExt;

        ImageFilter(){
            fileExt = Parameters.fileExtension.getValue();
            if(!fileExt.startsWith(".")){
                fileExt="."+fileExt;
            }
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(fileExt);
        }
    }

}

