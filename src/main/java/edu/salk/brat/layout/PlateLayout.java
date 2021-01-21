package edu.salk.brat.layout;

import edu.salk.brat.analysis.Plant;
import edu.salk.brat.analysis.graph.SkeletonNode;
import edu.salk.brat.parameters.ParamLocation;
import edu.salk.brat.parameters.Parameters;
import edu.salk.brat.utility.StringConverter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlateLayout {
    private final static Logger log=Logger.getLogger("edu.salk.brat");
    private String name;
    private int rows;
    private int cols;
    private Point[] positions;
    private Rectangle cropRegion;

    public PlateLayout() {
        this.name = Parameters.selectedLayout.getValue();
        this.cropRegion = StringConverter.toRectangle(Parameters.cropRegion.getValue());

        try {
            importFromPrefs();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

    }

    public PlateLayout(String name) {
        this.name = name;
        try {
            importFromPrefs();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public Point[] getPositions() {
        return positions;
    }

    public void setPositions(Point[] positions) {
        this.positions = positions;
    }

    private void importFromPrefs() throws BackingStoreException {
        Preferences prefs = ParamLocation.layout.getPrefs().node("Layout_" + name);
        this.rows = prefs.getInt("rows", 0);
        this.cols = prefs.getInt("cols", 0);

        int offsetX = 0;
        int offsetY = 0;
        if (cropRegion != null) {
            offsetX = -cropRegion.x;
            offsetY = -cropRegion.y;
        }

        this.positions = new Point[rows * cols];
        Pattern plantKeyPattern = Pattern.compile("(plant)(\\d{2})");
        for (String key : prefs.keys()) {
            Matcher m = plantKeyPattern.matcher(key);
            if (m.find()) {
                int pNr = Integer.parseInt(m.group(2));
                String[] posStr = prefs.get(key, null).split(",");
                positions[pNr-1] = new Point(Integer.parseInt(posStr[0]) + offsetX, Integer.parseInt(posStr[1]) + offsetY);
            }
        }
    }

    public void assignPlantNumbers(List<Plant> plants, int timePt) {
        Set<Integer> assignedNumbers = new HashSet<>();
        Set<Integer> duplicates = new HashSet<>();
        for (Plant plant:plants) {
            SkeletonNode startPt = plant.getStartNode(timePt);

            double minDist = Double.MAX_VALUE;
            int assignedIndex = -1;
            for (int i=0; i<positions.length; ++i){
                Point pos = positions[i];
                double dist = pos.distanceSq(startPt.getX(), startPt.getY());
                if (dist < minDist) {
                    minDist = dist;
                    assignedIndex = i;
                }
            }

            plant.setPlantNr(assignedIndex);
            if(!assignedNumbers.add(assignedIndex)) {
                duplicates.add(assignedIndex);
            }
            log.fine(String.format("assigned position %d to Plant with start point at (%d, %d)",
                    assignedIndex+1, startPt.getX(), startPt.getY()));
        }

        for (int nr:duplicates){
            log.fine(String.format("There were duplicate plant IDs for %d. Only keeping largest detection.", nr));
            NavigableMap<Double, Plant> sizeIndexMap = new TreeMap<>();
            for (Plant p : plants) {
                if (Integer.parseInt(p.getPlantID()) == nr + 1) {
                    sizeIndexMap.put(p.getTotalSize(timePt), p);
                }
            }
            sizeIndexMap.pollLastEntry();
            plants.removeAll(sizeIndexMap.values());
        }

        if (duplicates.size() > 0) {
            log.fine("There were duplicate assigned plant numbers: " +
                    duplicates.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(", ")));
        }
    }
}
