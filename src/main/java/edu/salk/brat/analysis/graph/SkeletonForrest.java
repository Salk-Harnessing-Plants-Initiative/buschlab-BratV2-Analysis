package edu.salk.brat.analysis.graph;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.EDM;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class SkeletonForrest {
    private final static Logger log = Logger.getLogger("edu.salk.brat");
    private final double SQRT2 = Math.sqrt(2.0);
    private final List<SkeletonTree> forrest;

    public SkeletonForrest() {
        this.forrest = new ArrayList<>();
    }

    public void create(ImageProcessor skelIp, int bgValue, ImageProcessor edmIp, ImageProcessor maskIp, Rectangle bounds) {
        Stack<SkeletonNode> stack = new Stack<>();
        for (int y = 0; y < skelIp.getHeight(); ++y) {
            for (int x = 0; x < skelIp.getWidth(); ++x) {
                if (skelIp.get(x, y) != bgValue) {
                    int edmVal = edmIp.get(x, y);
                    int typeVal = maskIp.get(x, y);
                    SkeletonNode curNode = new SkeletonNode(x + bounds.x, y + bounds.y, edmVal, typeVal);
                    stack.push(curNode);
                    skelIp.set(x, y, bgValue);

                    SkeletonTree tree = new SkeletonTree();
                    while (!stack.empty()) {
                        curNode = stack.pop();
                        int xx = curNode.getY() - bounds.x;
                        int yy = curNode.getY() - bounds.y;
                        for (int ny = yy - 1; ny <= yy + 1; ++ny) {
                            if (ny < 0 || ny > skelIp.getHeight() - 1) {
                                continue;
                            }
                            for (int nx = xx - 1; nx <= xx + 1; ++nx) {
                                if (nx < 0 || nx > skelIp.getWidth() - 1 || (nx == x && ny == y)) {
                                    continue;
                                }
                                if (skelIp.get(nx, ny) != bgValue) {
                                    edmVal = edmIp.get(nx, ny);
                                    typeVal = maskIp.get(nx, ny);
                                    SkeletonNode nbNode = new SkeletonNode(nx + bounds.x, ny + bounds.y, edmVal, typeVal);
                                    skelIp.set(nx, ny, bgValue);

                                    double linkLength = Math.abs(x - nx) + Math.abs(y - ny) == 2 ? SQRT2 : 1.0;
                                    tree.addLink(new SkeletonLink(linkLength), curNode, nbNode);
                                    stack.push(nbNode);
                                }
                            }
                        }
                    }
                    forrest.add(tree);
                }
            }
        }
    }

    public void create(Roi shootRoi, Roi rootRoi, Roi transitionRoi, Roi centerlineRoi) {
        ShapeRoi sRoiCombined = new ShapeRoi(shootRoi);
        sRoiCombined.or(new ShapeRoi(rootRoi)).or(new ShapeRoi(transitionRoi)).or(new ShapeRoi(centerlineRoi));
        Rectangle bounds = sRoiCombined.getBounds();

        ImageProcessor maskIp = sRoiCombined.getMask();
        BinaryProcessor skelIp = new BinaryProcessor((ByteProcessor) maskIp.duplicate());
        skelIp.invert();
        skelIp.skeletonize();

//        ShapeRoi sRoiCombined2 = new ShapeRoi(shootRoi);
//        sRoiCombined2.or(new ShapeRoi(transitionRoi)).or(new ShapeRoi(centerlineRoi));
//        Rectangle bounds2 = sRoiCombined2.getBounds();
//
//        ImageProcessor maskIp2 = sRoiCombined2.getMask();
//        BinaryProcessor skelIp2 = new BinaryProcessor((ByteProcessor) maskIp2.duplicate());
//        skelIp2.invert();
//        skelIp2.skeletonize();
//
//        ImageProcessor dbgIp = new ColorProcessor(Math.max(skelIp.getWidth(), skelIp2.getWidth()), Math.max(skelIp.getHeight(), skelIp2.getHeight()));
//        ImageProcessor shootMask = shootRoi.getMask();
//        dbgIp.setColor(Color.green);
//        for (int y=0; y<shootMask.getHeight(); ++y) {
//            for (int x=0; x<shootMask.getWidth(); ++x) {
//                if (shootMask.get(x,y)==0){
//                    dbgIp.drawPixel(x, y);
//                }
//            }
//        }
//
//        dbgIp.setColor(Color.blue);
//        for (int y=0; y<skelIp.getHeight(); ++y) {
//            for (int x=0; x<skelIp.getWidth(); ++x) {
//                if (skelIp.get(x,y)==0){
//                    dbgIp.drawPixel(x, y);
//                }
//            }
//        }
//        dbgIp.setColor(Color.red);
//        for (int y=0; y<skelIp2.getHeight(); ++y) {
//            for (int x=0; x<skelIp2.getWidth(); ++x) {
//                if (skelIp2.get(x,y)==0){
//                    dbgIp.drawPixel(x, y);
//                }
//            }
//        }
//        new ImagePlus("skeleton", dbgIp).show();

        ImageProcessor edmIp = maskIp.duplicate();
        EDM edm =new EDM();
        edm.toEDM(edmIp);

        Roi rRoi = new ShapeRoi(rootRoi);
        rRoi.setLocation(rootRoi.getBounds().x - sRoiCombined.getBounds().x, rootRoi.getBounds().y - sRoiCombined.getBounds().y);
        Roi sRoi = new ShapeRoi(shootRoi);
        sRoi.setLocation(shootRoi.getBounds().x - sRoiCombined.getBounds().x, shootRoi.getBounds().y - sRoiCombined.getBounds().y);
        Roi tRoi = new ShapeRoi(transitionRoi);
        tRoi.setLocation(transitionRoi.getBounds().x - sRoiCombined.getBounds().x, transitionRoi.getBounds().y - sRoiCombined.getBounds().y);
//		ImagePlus img = new ImagePlus("maskdbg", maskIp);
//		img.show();
        maskIp.setColor(1);
        maskIp.fill(rRoi);
//		img.updateAndDraw();
        maskIp.setColor(2);
        maskIp.fill(sRoi);
//		img.updateAndDraw();
        maskIp.setColor(3);
        maskIp.fill(tRoi);
//		img.updateAndDraw();
//		img.close();

        create(skelIp, 255, edmIp, maskIp, bounds);
    }
}


class SkeletonTree {
    final private Graph<SkeletonNode, SkeletonLink> graph;
    private boolean updated = true;
    private Set<SkeletonNode> endNodes;

    SkeletonTree() {
        this.graph = new UndirectedSparseGraph<>();
    }

    void addLink(SkeletonLink link, SkeletonNode n1, SkeletonNode n2) {
        this.graph.addEdge(link, n1, n2);
        updated = true;
    }

    Collection<SkeletonNode> getEndNodes() {
        if (updated) {
            endNodes = new HashSet<>();
            for (SkeletonNode n:graph.getVertices()) {
                if (graph.getIncidentEdges(n).size() == 1) {
                    endNodes.add(n);
                }
            }
        }
        return endNodes;
    }
}