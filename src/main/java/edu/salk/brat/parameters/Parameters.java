package edu.salk.brat.parameters;

import edu.salk.brat.layout.IJRoiImporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public enum Parameters {
    baseDirectory(System.getProperty("user.home"), ParamType.strng, ParamLocation.basic),
    origDirectory(System.getProperty("user.home"), ParamType.strng, ParamLocation.basic),
    fileExtension("tif", ParamType.strng, ParamLocation.basic),
    flipHorizontal("true", ParamType.bool, ParamLocation.basic),
//    useSets("true", ParamType.bool, ParamLocation.basic),
//    haveDayZeroImage("false", ParamType.bool, ParamLocation.basic),
//    haveStartPoints("false", ParamType.bool, ParamLocation.basic),
    numThreads("1", ParamType.inum, ParamLocation.basic),

    experimentIdentifier("^(.*?)_", ParamType.strng, ParamLocation.advanced),
    setIdentifier("set\\d+", ParamType.strng, ParamLocation.advanced),
    timePtIdentifier("day\\d+", ParamType.strng, ParamLocation.advanced),
    plateIdentifier("_\\d{3}[\\._]", ParamType.strng, ParamLocation.advanced),
    resolution("1200", ParamType.inum, ParamLocation.advanced),

    segLblBackground("1", ParamType.inum, ParamLocation.advanced),
    segLblRoot("2", ParamType.inum, ParamLocation.advanced),
    segLblShoot("3", ParamType.inum, ParamLocation.advanced),
    segLblTransition("4", ParamType.inum, ParamLocation.advanced),
    segLblCenterline("5", ParamType.inum, ParamLocation.advanced),

    minShootPix("20", ParamType.inum, ParamLocation.advanced),
    minRootPix("20", ParamType.inum, ParamLocation.advanced),
    minTransitionPix("1", ParamType.inum, ParamLocation.advanced),

    cropRegion("624, 1168, 5280, 3960", ParamType.strng, ParamLocation.advanced),
    plantTreeMaxLevel("10", ParamType.inum, ParamLocation.advanced),
    selectedLayout("Default (2 x 12)", ParamType.strng, ParamLocation.advanced),
    availableLayouts(updateAvailableLayouts(), ParamType.strng, ParamLocation.advanced),

    circleDiameter("10", ParamType.inum, ParamLocation.advanced),
    labelSize("30", ParamType.inum, ParamLocation.advanced),
    labelFont("sansserif-BOLD-30", ParamType.strng, ParamLocation.advanced),
    numberSize("150", ParamType.inum, ParamLocation.advanced),
    numberFont("sansserif-BOLD-150", ParamType.strng, ParamLocation.advanced),
    shootRoiColor("#00FF00", ParamType.strng, ParamLocation.advanced),
    rootRoiColor("#1E90FF", ParamType.strng, ParamLocation.advanced),
    skeletonColor("#808080", ParamType.strng, ParamLocation.advanced),
    mainRootColor("#FF00FF", ParamType.strng, ParamLocation.advanced),
    startPtColor("#FFB366", ParamType.strng, ParamLocation.advanced),
    endPtColor("#1E90FF", ParamType.strng, ParamLocation.advanced),
    shootCMColor("#FF0000", ParamType.strng, ParamLocation.advanced),
    generalColor("#FF0000", ParamType.strng, ParamLocation.advanced),
    logLevel("OFF", ParamType.strng, ParamLocation.advanced);

//    private final static Preferences prefs_basic = Preferences.userRoot().node("edu/salk/bratanalysis");
//    private final static Preferences prefs_advanced = prefs_basic.node("advanced");
    private final static Logger log=Logger.getLogger("edu.salk.brat");
    private final ParamType type;
    private final ParamLocation loc;

    private final String dflt;
    private String current;

    Parameters(String dflt, ParamType type, ParamLocation loc) {
        this.dflt = dflt;
        this.type = type;
        this.loc = loc;
    }

    public String getValue() {
        if (this.current != null) {
            return this.current;
        }
        else {
            return this.dflt;
        }
    }

    public String getDefaultValue() {
        return this.dflt;
    }

    public void setValue(String value) {
        this.current = value;
    }

    public ParamType getType() {
        return this.type;
    }

    public ParamLocation getLoc() {
        return this.loc;
    }

    public static void write(ParamLocation location) throws BackingStoreException {
        Preferences prefs = location.getPrefs();
//        if (location == ParamLocation.basic) {
//            prefs = prefs_basic;
//        }
        for (Parameters param : Parameters.values()) {
            if (param.getLoc() == location) {
                switch (param.getType()) {
                    case strng:
                        prefs.put(param.name(), param.getValue());
                        break;
                    case bool:
                        prefs.putBoolean(param.name(), Boolean.parseBoolean(param.getValue()));
                        break;
                    case inum:
                        prefs.putInt(param.name(), Integer.parseInt(param.getValue()));
                        break;
                    case dnum:
                        prefs.putDouble(param.name(), Double.parseDouble(param.getValue()));
                        break;
                }
            }
            prefs.flush();
        }
    }

    public static void read(ParamLocation location) throws IllegalStateException{
        Preferences prefs = location.getPrefs();
//        Preferences prefs = prefs_advanced;
//        if (location == ParamLocation.basic) {
//            prefs = prefs_basic;
//        }
        List<String> failures = new ArrayList<>();
        for (Parameters param : Parameters.values()) {
            if (param.getLoc() == location) {
                String value = prefs.get(param.name(), null);
                if (value == null) {
                    failures.add(param.name());
                }
                param.setValue(value);
            }
        }
        if (failures.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error reading parameters. Will use default values for: \n");
            sb.append(String.join(", ", failures));

            throw new IllegalStateException(sb.toString());
        }
    }

    public static void reset(ParamLocation location) {
        for (Parameters p: Parameters.values()) {
            if (p.getLoc() == location) {
                p.setValue(null);
            }
        }
    }

    public static void createDefaultLayout () throws BackingStoreException {
        String name = Parameters.selectedLayout.getDefaultValue();
        String nodeName = "Layout_" + name;

        ParamLocation location = ParamLocation.layout;
        if (location.getPrefs().nodeExists(nodeName)) {
            location.getPrefs().node(nodeName).removeNode();
        }
        Preferences prefs = location.getPrefs().node(nodeName);

        prefs.putInt("rows", 2);
        prefs.putInt("cols", 12);

        // flipped layout
        prefs.put("plant01", "855,1728");
        prefs.put("plant02", "1209,1758");
        prefs.put("plant03", "1633,1722");
        prefs.put("plant04", "2207,1706");
        prefs.put("plant05", "2621,1698");
        prefs.put("plant06", "2977,1698");
        prefs.put("plant07", "3565,1684");
        prefs.put("plant08", "4021,1738");
        prefs.put("plant09", "4419,1690");
        prefs.put("plant10", "4973,1650");
        prefs.put("plant11", "5369,1702");
        prefs.put("plant12", "5703,1700");
        prefs.put("plant13", "878,4028");
        prefs.put("plant14", "1254,4024");
        prefs.put("plant15", "1600,3992");
        prefs.put("plant16", "2206,4086");
        prefs.put("plant17", "2612,4042");
        prefs.put("plant18", "2968,4080");
        prefs.put("plant19", "3554,4016");
        prefs.put("plant20", "3998,4072");
        prefs.put("plant21", "4412,4058");
        prefs.put("plant22", "4920,4078");
        prefs.put("plant23", "5354,4060");
        prefs.put("plant24", "5644,4110");


        // not flipped layout
//        prefs.put("plant01", "906,1694");
//        prefs.put("plant02", "1233,1709");
//        prefs.put("plant03", "1633,1650");
//        prefs.put("plant04", "2199,1713");
//        prefs.put("plant05", "2587,1739");
//        prefs.put("plant06", "3038,1691");
//        prefs.put("plant07", "3634,1702");
//        prefs.put("plant08", "3985,1702");
//        prefs.put("plant09", "4402,1700");
//        prefs.put("plant10", "4970,1726");
//        prefs.put("plant11", "5400,1760");
//        prefs.put("plant12", "5757,1732");
//        prefs.put("plant13", "948,4122");
//        prefs.put("plant14", "1245,4100");
//        prefs.put("plant15", "1680,4094");
//        prefs.put("plant16", "2205,4061");
//        prefs.put("plant17", "2612,4074");
//        prefs.put("plant18", "3050,4017");
//        prefs.put("plant19", "3632,4086");
//        prefs.put("plant20", "4002,4045");
//        prefs.put("plant21", "4408,4085");
//        prefs.put("plant22", "5007,3998");
//        prefs.put("plant23", "5353,4026");
//        prefs.put("plant24", "5729,4024");

        Parameters.availableLayouts.setValue(updateAvailableLayouts());
        Parameters.selectedLayout.setValue(name);
        prefs.flush();
        log.info(String.format("Default layout \"%s\" created.", name));
    }

    public static boolean checkDefaultLayout() {
        Preferences prefs = ParamLocation.layout.getPrefs(); //.node("Layout_"+Parameters.selectedLayout.getDefaultValue());
        boolean layoutExists = false;

        try {
            if (prefs.nodeExists("Layout_"+Parameters.selectedLayout.getDefaultValue())) {
                String[] ch = prefs.node("Layout_"+Parameters.selectedLayout.getDefaultValue()).keys();
                if (ch.length > 0){
                    layoutExists = true;
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        return layoutExists;
    }

    public static void importIJRoiLayout(String name, String filepath, int rows, int cols) throws IOException, BackingStoreException {
        String nodeName = "Layout_"+name;

        log.info(String.format("importing layout from %s", filepath));
        IJRoiImporter ijri = new IJRoiImporter();
        ijri.openZip(filepath);
        List<int[]> positions = ijri.getRoiPositions();

        int expectedPositions = rows*cols;
        if (positions.size() != expectedPositions) {
            log.severe(String.format("expected %d positions but got %d. Not importing.", expectedPositions, positions.size()));
            throw new IOException("could not retrieve valid number of positions.");
        }

        ParamLocation location = ParamLocation.layout;
        if (location.getPrefs().nodeExists(nodeName)) {
            location.getPrefs().node(nodeName).removeNode();
        }
        Preferences prefs = location.getPrefs().node(nodeName);
        prefs.putInt("rows", rows);
        prefs.putInt("cols", cols);
        for (int i=0; i<positions.size(); ++i) {
            int[] pos = positions.get(i);
            prefs.put(String.format("plant%02d", i+1), String.format("%d,%d", pos[0], pos[1]));
            log.info(String.format("imported Position (%d, %d)", pos[0], pos[1]));
//            System.out.println(String.format("prefs.put(\"plant%02d\", \"%d,%d\");", i+1, pos[0], pos[1]));
        }

        Parameters.availableLayouts.setValue(updateAvailableLayouts());
        Parameters.selectedLayout.setValue(name);
        prefs.flush();
    }

    public static void removeLayout(String layout) throws BackingStoreException {
        Preferences prefs = ParamLocation.layout.getPrefs();

        if (prefs.nodeExists(layout)) {
            prefs.removeNode();
            updateAvailableLayouts();
            if (Parameters.selectedLayout.getValue() == layout){
                Parameters.selectedLayout.setValue(null);
            }
        }
    }

    private static String updateAvailableLayouts() {
        ParamLocation location = ParamLocation.layout;
        List<String> availableLayouts = new ArrayList<>();
        try {
            for (String nodeName: location.getPrefs().childrenNames()) {
                if (nodeName.startsWith("Layout_")) {
                    availableLayouts.add(nodeName.replace("Layout_", ""));
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        return String.join(";", availableLayouts);
    }

    private static void printLayout(String name) throws BackingStoreException {
        Preferences prefs = ParamLocation.layout.getPrefs().node("Layout_"+name);

        for (String key:prefs.keys()){
            if (key.startsWith("plant")) {
                System.out.println(String.format("prefs.put(\"%s\", \"%s\")", key, prefs.get(key, null)));
            }
        }
    }
    public static void main(String[] args) {
//        Parameters.read(ParamLocation.basic);
//        for (Parameters p: Parameters.values()) {
//            System.out.println(String.format("%s : %s", p.name(), p.getValue()));
//        }

//        try {
//            Parameters.importIJRoiLayout("Default (2 x 12)", "/home/crisoo/RoiSet.zip", 2, 12);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (BackingStoreException e) {
//            e.printStackTrace();
//        }
        try {
            Parameters.printLayout("flipped (2 x 12");
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

//        for (Parameters p: Parameters.values()) {
//            System.out.println(String.format("%s : %s", p.name(), p.getValue()));
//        }
//        System.out.println();


    }
}

