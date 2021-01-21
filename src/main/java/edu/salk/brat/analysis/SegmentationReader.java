package edu.salk.brat.analysis;

import edu.salk.brat.layout.PlateLayout;
import edu.salk.brat.output.DataOutput;
import edu.salk.brat.parameters.Parameters;
import edu.salk.brat.utility.ExceptionLog;
import edu.salk.brat.utility.FileUtils;
import edu.salk.brat.utility.StringConverter;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.Opener;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class SegmentationReader implements Runnable {
    private final static Logger log=Logger.getLogger("edu.salk.brat");
//    final private static Logger log=Logger.getLogger(SegmentationReader.class.getName());

    private String workSetID;
    private Map<Integer, String> fileSet;
    private Map<Integer, String> origSet;
    private Map<String, Integer> segmentationLabels;
    private Rectangle cropRegion;
    private Map<Integer, Plant> plants;
//    private List<Roi> plantRois;

    public SegmentationReader (final String workSetID, final Map<Integer, String> fileSet, final Map<Integer, String> origSet) {
        this.workSetID = workSetID;
        this.fileSet = fileSet;
        this.origSet = origSet;
    }

    @Override
    public void run() {
        try {
            Opener opener = new Opener();

            segmentationLabels = new HashMap<>();
            segmentationLabels.put("background", Integer.parseInt(Parameters.segLblBackground.getValue()));
            segmentationLabels.put("root", Integer.parseInt(Parameters.segLblRoot.getValue()));
            segmentationLabels.put("shoot", Integer.parseInt(Parameters.segLblShoot.getValue()));
            segmentationLabels.put("transition", Integer.parseInt(Parameters.segLblTransition.getValue()));
            segmentationLabels.put("centerline", Integer.parseInt(Parameters.segLblCenterline.getValue()));

            this.plants = new TreeMap<>();
            for (Map.Entry<Integer, String> fileEntry : fileSet.entrySet()) {
                int timePt = fileEntry.getKey();
                String filename = fileEntry.getValue();
                log.info(String.format("reading file %s -> time point %d", filename, timePt));

                ImageProcessor segmentedIp = opener.openImage(Parameters.baseDirectory.getValue(), filename).getProcessor();
                if (!(segmentedIp instanceof ByteProcessor)) {
                    segmentedIp = segmentedIp.convertToByte(false);
                }
                log.info(String.format("%s -> image dimensions: (%d, %d)", filename, segmentedIp.getWidth(), segmentedIp.getHeight()));

                if (Boolean.parseBoolean(Parameters.flipHorizontal.getValue())) {
                    log.info(String.format("%s -> flipping horizontal", filename));
                    segmentedIp.flipHorizontal();
                }
//            showLabels(segmentedIp);

                List<Integer> unlabeledPixels = new ArrayList<>();
                preprocessLabels(segmentedIp, unlabeledPixels);
                if (unlabeledPixels.size() == 0) {
                    log.fine(String.format("%s --> %d unlabeled pixels.", filename, unlabeledPixels.size()));
                } else {
                    log.warning(String.format("%s --> %d unlabeled pixels.", filename, unlabeledPixels.size()));
                }

//            ImageProcessor fgMask = createFGMask(segmentedIp);
                List<Roi> plantRois = detectPlants(segmentedIp);
                log.info(String.format("%s -> identified %d plant ROIs.", filename, plantRois.size()));
                // fgMask = null; //TODO: free fgmask

                log.info(String.format("%s -> registering plants.", filename));
                List<Plant> tmpPlants = registerPlants(timePt, segmentedIp, plantRois);
                log.info(String.format("%s -> creating plant topologies.", filename));
                createTopologies(timePt, tmpPlants);

                PlateLayout layout = new PlateLayout();
                layout.assignPlantNumbers(tmpPlants, timePt);
                for (Plant plant:tmpPlants) {
                    Integer plantIdx = Integer.valueOf(plant.getPlantID())-1;
                    if (! this.plants.containsKey(plantIdx)) {
                        this.plants.put(plantIdx, plant);
                    }
                    else {
                        this.plants.get(plantIdx).setPhenotype(timePt, plant.getPhenotype(timePt));
                    }
                }

                log.info(String.format("%s -> calculating plant traits.", filename));
                calcTraits(timePt);

                ImageProcessor diagIp = null;
                diagIp = getOrigIp(timePt);

                Rectangle diagOffset = null;
                if (diagIp == null) {
                    log.warning(String.format("%s -> no original image could be assigned. Creating artificial image for diagnostics.", filename));
                    diagIp = new ColorProcessor(segmentedIp.getWidth(), segmentedIp.getHeight());
                    diagIp.setColor(Color.darkGray);
                    for (Roi roi : plantRois) {
                        diagIp.fill(roi);
                    }
                } else {
                    diagOffset = StringConverter.toRectangle(Parameters.cropRegion.getValue());
                }

                String extStrippedFilename = FileUtils.removeExtension(filename);
                log.info(String.format("%s -> writing traits.", filename));
                DataOutput.writeTraits(plants.values(), timePt, extStrippedFilename);


                log.info(String.format("%s -> creating per-plate diagnostics.", filename));
                DataOutput.writePlateDiags(diagIp, plants.values(), timePt, extStrippedFilename, diagOffset);
                log.info(String.format("%s -> creating per-plant diagnostics.", filename));
                DataOutput.writeSinglePlantDiagnostics(diagIp, plants.values(), timePt, extStrippedFilename, diagOffset);

                FileUtils.moveFile(Parameters.baseDirectory.getValue() + System.getProperty("file.separator") + filename,
                        Parameters.baseDirectory.getValue() + System.getProperty("file.separator") +
                                "tifs-processed-success" + System.getProperty("file.separator") + filename);

                log.info(String.format("%s -> analysis terminated successful.\nMoved source file.\n", filename));
//            if (plantRois.size() == 23){
//                ImageProcessor dbgIp = createDbgIP(segmentedIp, fgMask, plantRois);
//                new ImagePlus("dbg", dbgIp).show();
//            }
            }
        }
        catch (Exception e) {
            log.severe(String.format("Unhandled Exception: %s", e.getMessage()));
            log.severe(ExceptionLog.StackTraceToString(e));

        }
    }

    private void preprocessLabels(final ImageProcessor segmentedIp, final List<Integer> unlabeledPixels) {
        int[] allLabels = new int[] {segmentationLabels.get("background"), segmentationLabels.get("root"),
                                     segmentationLabels.get("shoot"), segmentationLabels.get("transition"),
                                     segmentationLabels.get("centerline")};

        for (int i=0; i<segmentedIp.getPixelCount(); ++i) {
            int v = segmentedIp.get(i);
            boolean pixLabeled = false;
            for (int label : allLabels) {
                if (v == label) {
                    pixLabeled = true;
//                    if (v == segmentationLabels.get("centerline")) {
//                        segmentedIp.set(i, segmentationLabels.get("root"));
//                    }
                    break;
                }
            }
            if (!pixLabeled) {
                unlabeledPixels.add(i);
            }
        }
    }

    private ImageProcessor createFGMask(ImageProcessor segmentedIp) {
        int bgVal = segmentationLabels.get("background");
        ImageProcessor fgMask = new ByteProcessor(segmentedIp.getWidth(), segmentedIp.getHeight());
        for (int i=0; i< fgMask.getPixelCount(); ++i){
            if (segmentedIp.get(i) != bgVal){
                fgMask.set(i, 255);
            }
        }

        return fgMask;
    }

    private List<Roi> detectPlants(ImageProcessor segmentedIp){
        ImageProcessor fgMask = createFGMask(segmentedIp);
        fgMask.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        ThresholdToSelection ts = new ThresholdToSelection();
        ShapeRoi fgRoi = new ShapeRoi(ts.convert(fgMask));
        Roi[] singleRois = fgRoi.getRois();

        int minRootSize = Integer.parseInt(Parameters.minRootPix.getValue());
        int minShootSize = Integer.parseInt(Parameters.minShootPix.getValue());
        int minTransitionSize = Integer.parseInt(Parameters.minTransitionPix.getValue());

        int rootVal = segmentationLabels.get("root");
        int shootVal = segmentationLabels.get("shoot");
        int transitionVal = segmentationLabels.get("transition");
        int centerline = segmentationLabels.get("centerline");

        List<Roi> plantRois = new ArrayList<>();
        for(Roi roi: singleRois) {
            int cntRoot = 0;
            int cntShoot = 0;
            int cntTransition = 0;
            int cntCenterline = 0;

            Rectangle roiBounds = roi.getBounds();
            ImageProcessor roiMask = roi.getMask();

            roiloop:
            for (int y=roiBounds.y, yy=0; y<roiBounds.y+roiBounds.height; ++y, ++yy) {
                for (int x=roiBounds.x, xx=0; x<roiBounds.x+roiBounds.width; ++x, ++xx){
                    if (roiMask.get(xx, yy) > 0) {
                        int sVal = segmentedIp.get(x, y);
                        if ( sVal == shootVal){
                            ++cntShoot;
                        }
                        else if (sVal == rootVal) {
                            ++cntRoot;
                        }
                        else if (sVal == transitionVal) {
                            ++cntTransition;
                        }
                        else if (sVal == centerline) {
                            ++cntCenterline;
                        }
                    }

                    if ((cntRoot>=minRootSize) && (cntShoot>=minShootSize) && (cntTransition>=minTransitionSize)) {
                        plantRois.add(roi);
                        break roiloop;
                    }
                }
            }
        }
        return plantRois;
    }

    private List<Plant> registerPlants(final int timePt, final ImageProcessor segmentedIp, final List<Roi> plantRois) {
        List<Plant> plants = new ArrayList<>();

        ThresholdToSelection ts = new ThresholdToSelection();
        segmentedIp.setThreshold(segmentationLabels.get("shoot"), segmentationLabels.get("shoot"), ImageProcessor.NO_LUT_UPDATE);
        ShapeRoi allShootsRoi = new ShapeRoi(ts.convert(segmentedIp));
        segmentedIp.setThreshold(segmentationLabels.get("root"), segmentationLabels.get("root"), ImageProcessor.NO_LUT_UPDATE);
        ShapeRoi allRootsRoi = new ShapeRoi(ts.convert(segmentedIp));
        segmentedIp.setThreshold(segmentationLabels.get("transition"), segmentationLabels.get("transition"), ImageProcessor.NO_LUT_UPDATE);
        ShapeRoi allTransitionsRoi = new ShapeRoi(ts.convert(segmentedIp));
        segmentedIp.setThreshold(segmentationLabels.get("centerline"), segmentationLabels.get("centerline"), ImageProcessor.NO_LUT_UPDATE);
        ShapeRoi allCenterlineRoi = new ShapeRoi(ts.convert(segmentedIp));
        allRootsRoi.or(allCenterlineRoi);
//        ImageProcessor dbgIp = new ColorProcessor(segmentedIp.getWidth(), segmentedIp.getHeight());
//        dbgIp.setColor(Color.gray);
//        for (Roi roi: plantRois) {
//            dbgIp.fill(roi);
//        }
//        dbgIp.setColor(Color.red);
//        dbgIp.draw(allShootsRoi);

//        ImagePlus img = new ImagePlus("intersections", dbgIp);
//        img.show();

//        int plantCnt = 0;
        for (Roi plantRoi: plantRois) {
            Plant plant = new Plant(99);

            ShapeRoi shootRoi = new ShapeRoi(plantRoi);
            shootRoi.and(allShootsRoi);
            plant.setShootRoi(timePt, shootRoi);

            ShapeRoi rootRoi = new ShapeRoi(plantRoi);
            rootRoi.and(allRootsRoi);
            plant.setRootRoi(timePt, rootRoi);

            ShapeRoi transitionRoi = new ShapeRoi(plantRoi);
            transitionRoi.and(allTransitionsRoi);
            plant.setTransitionRoi(timePt, transitionRoi);

            ShapeRoi centerlineRoi = new ShapeRoi(plantRoi);
            centerlineRoi.and(allCenterlineRoi);

            ShapeRoi newTransitionRoi = null;
            for (Roi roi: transitionRoi.getRois()) {
                Rectangle rect = roi.getBounds();
                rect.x = rect.x-1 > 0 ? rect.x-1 : 0;
                rect.y = rect.y-1 > 0 ? rect.y-1 : 0;
                rect.width = rect.x + rect.width + 2 < segmentedIp.getWidth() ? rect.width + 2 : segmentedIp.getWidth() - rect.x - 1;
                rect.height = rect.y + rect.height + 2 < segmentedIp.getHeight() ? rect.height + 2 : segmentedIp.getHeight() - rect.y - 1;
                boolean hasShootBorder = false;
                boolean hasRootBorder = false;
                for (int x=rect.x; x<rect.x+rect.width; ++x){
                    if (segmentedIp.get(x, rect.y) == segmentationLabels.get("shoot") ||
                            segmentedIp.get(x, rect.y+rect.height) == segmentationLabels.get("shoot")) {
                        hasShootBorder = true;
                    }
                    else if (segmentedIp.get(x, rect.y) == segmentationLabels.get("root") ||
                            segmentedIp.get(x, rect.y+rect.height) == segmentationLabels.get("root")) {
                        hasRootBorder = true;
                    }
                }
                for (int y=rect.y; y<rect.y+rect.height; ++y){
                    if (segmentedIp.get(rect.x, y) == segmentationLabels.get("shoot") ||
                            segmentedIp.get(rect.x+rect.width, y) == segmentationLabels.get("shoot")) {
                        hasShootBorder = true;
                    }
                    else if (segmentedIp.get(rect.x, y) == segmentationLabels.get("root") ||
                            segmentedIp.get(rect.x+rect.width, y) == segmentationLabels.get("root")) {
                        hasRootBorder = true;
                    }
                }
                if (hasRootBorder && hasShootBorder){
                    if (newTransitionRoi == null){
                        newTransitionRoi = new ShapeRoi(roi);
                    }
                    else {
                        newTransitionRoi.or(new ShapeRoi(roi));
                    }
                }
            }
            if (newTransitionRoi != null) {
                plant.setCenterlineRoi(timePt, newTransitionRoi);
            }
            else {
                plant.setCenterlineRoi(timePt, transitionRoi);
            }

            plants.add(plant);
//
//            dbgIp.setColor(Color.green);
//            dbgIp.draw(shootRoi);
//            img.updateAndDraw();
//
//            dbgIp.setColor(Color.blue);
//            dbgIp.draw(rootRoi);
//            img.updateAndDraw();
//
//            dbgIp.setColor(Color.red);
//            dbgIp.draw(transitionRoi);
//            img.updateAndDraw();
//
//            Rectangle r = transitionRoi.getBounds();
//            dbgIp.draw(new TextRoi(r.x-20, r.y, String.format("%02d", plantCnt)));
//            img.updateAndDraw();
//            ++plantCnt;
        }
//        new ImagePlus("intersections", dbgIp).show();
        return plants;
    }

    private void createTopologies(final int timePt, final List<Plant> plants) {
        for (Iterator<Plant> itr = plants.iterator(); itr.hasNext(); ) {
            Plant p = itr.next();
            try {
                p.createTopology(timePt);
            } catch (TopologyException e) {
                log.fine(String.format("TopologyException: %s", e.getMessage()));
                p.removeTimePt(timePt);
                if (p.getTimePts().size() == 0) {
                    itr.remove();
                }
            }
        }
    }

    private void calcTraits(final int timePt) {
        for (Plant plant:plants.values()) {
            plant.calcTraits(timePt);
        }
    }

    private ImageProcessor getOrigIp(int timePt) {
        if (origSet == null){
            return null;
        }
        String filename = origSet.get(timePt);
        if (filename == null) {
            return null;
        }

        Opener opener = new Opener();
        ImageProcessor origIp = opener.openImage(Parameters.origDirectory.getValue(), filename).getProcessor();
        if (!(origIp instanceof ColorProcessor)) {
            origIp = origIp.convertToColorProcessor();
        }
        if (Boolean.parseBoolean(Parameters.flipHorizontal.getValue())){
            origIp.flipHorizontal();
        }
        return origIp;
    }

    private void showLabels(final ImageProcessor segIp) {
        ImageProcessor dbgIp = new ColorProcessor(segIp.getWidth(), segIp.getHeight());

        int shootVal = segmentationLabels.get("shoot");
        int rootVal = segmentationLabels.get("root");
        int transitionVal = segmentationLabels.get("transition");
        int centerlineVal = segmentationLabels.get("centerline");

        for (int i=0; i<dbgIp.getPixelCount(); ++i) {
            int label = segIp.get(i);

            if (label == shootVal) {
                dbgIp.set(i, Integer.parseInt("00ff00", 16));
            }
            if(label == rootVal) {
                dbgIp.set(i, Integer.parseInt("0000ff", 16));
            }
            if(label == transitionVal) {
                dbgIp.set(i, Integer.parseInt("ff0000", 16));
            }
            if(label == centerlineVal) {
                dbgIp.set(i, Integer.parseInt("ffff00", 16));
            }
        }

        new ImagePlus("segmentation Labels", dbgIp).show();

    }

    private ImageProcessor createDbgIP(final ImageProcessor segmentedIp, final ImageProcessor fgMask, final List<Roi> plantRois) {
        ImageProcessor dbgIp = new ColorProcessor(segmentedIp.getWidth(), segmentedIp.getHeight());
        for(int i=0; i<dbgIp.getPixelCount(); ++i) {
            if (fgMask.get(i) > 0) {
                dbgIp.set(i, 8355711);
            }
        }

        int shootVal = segmentationLabels.get("shoot");
        int rootVal = segmentationLabels.get("root");
        int transitionVal = segmentationLabels.get("transition");
        int centerlineVal = segmentationLabels.get("centerline");
        for (Roi roi:plantRois) {
            Rectangle roiBounds = roi.getBounds();
            ImageProcessor roiMask = roi.getMask();
            for (int y=roiBounds.y, yy=0; y<roiBounds.y+roiBounds.height; ++y, ++yy) {
                for (int x=roiBounds.x, xx=0; x<roiBounds.x+roiBounds.width; ++x, ++xx){
                    if (roiMask.get(xx, yy) > 0) {
                        int sVal = segmentedIp.get(x, y);
                        if ( sVal == shootVal){
                            dbgIp.set(x, y, 65280);
                        }
                        else if (sVal == rootVal) {
                            dbgIp.set(x, y, 65535);
                        }
                        else if (sVal == transitionVal) {
                            dbgIp.set(x, y, 16711680);
                        }
                    }
                }
            }
        }

        return dbgIp;
    }

}

class OriginalImageFilter implements FilenameFilter {
    private String worksetID;

    OriginalImageFilter(String worksetID, String dayID){

    }

    @Override
    public boolean accept(File file, String s) {
        return false;
    }
}