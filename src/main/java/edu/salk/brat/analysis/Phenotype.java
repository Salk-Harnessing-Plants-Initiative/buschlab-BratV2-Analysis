package edu.salk.brat.analysis;

import edu.salk.brat.analysis.graph.SkeletonForrest;
import edu.salk.brat.analysis.graph.SkeletonGraph;
import edu.salk.brat.analysis.graph.SkeletonNode;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

class Phenotype{
    Topology topology;
    Traits traits;
    int[] gravityVec={0,1};

    protected Phenotype(){
        this.topology=new Topology();
        this.traits=new Traits();
    }

    protected void calcTraits(){
        calcRootLengths();
        calcRootWidths();
        calcRootAngle();
        calcRootStdDevs();
        calcRootDirectionalEquivalent();
        calcShootArea();
    }


    private void calcRootLengths(){
//		List<SkeletonNode> mainPath=topology.rootMainPath;
//		if(mainPath!=null){
//			SkeletonNode startNode=mainPath.get(0);
//			SkeletonNode endNode=mainPath.get(mainPath.size()-1);
        SkeletonNode startNode = topology.rootStartNode;
        SkeletonNode endNode = topology.rootEndNode;
        if (startNode != null && endNode != null) {
            traits.rootEuclidianLength=startNode.distance(endNode);
            traits.rootTotalLength=topology.graph.getPathLength(startNode,endNode);
        }
    }

    private void calcRootWidths(){
        List<SkeletonNode> mainPath=topology.rootMainPath;
        if(mainPath!=null){
            double[] avgWidth = new double[traits.percentiles.length+1];
            int[] pixCnts = new int[avgWidth.length];
            int numPix=mainPath.size();
            int[] percPix=new int[traits.percentiles.length];

            for(int i=0;i<traits.percentiles.length;++i){
                percPix[i]=(int)Math.round(numPix/100.0*traits.percentiles[i]);
            }

            int avgId=0;
            for(int pixId=0;pixId<numPix;++pixId){
                avgWidth[0]+=2.0*mainPath.get(pixId).getDMapValue();
                pixCnts[0]++;
                for(int i=0;i<percPix.length;++i)
                    if(pixId==percPix[i]){
                        avgId++;
                    }
                if(avgId>0){
                    avgWidth[avgId]+=2.0*mainPath.get(pixId).getDMapValue();
                    pixCnts[avgId]++;
                }
            }
            for(int i=0;i<avgWidth.length;++i){
                avgWidth[i]/=pixCnts[i];
            }
            for(int i=0;i<avgWidth.length;++i){
                traits.rootAverageWidths[i]=(Double)avgWidth[i];
            }
        }
    }

    private void calcRootDirectionalEquivalent(){
        List<SkeletonNode> mainRoot=topology.rootMainPath;
        if(mainRoot==null) return;
        int sumVecEqui=0;
        for(int pixNr=0;pixNr<mainRoot.size()-1;++pixNr){
            SkeletonNode pix1=mainRoot.get(pixNr);
            SkeletonNode pix2=mainRoot.get(pixNr+1);


            int[] pixVec={pix2.getX()-pix1.getX(),pix2.getY()-pix1.getY()};//pix1.vectorTo(pix2);
            int[] deltaVec=new int[2];
            deltaVec[0]=pixVec[0]-gravityVec[0];
            deltaVec[1]=pixVec[1]-gravityVec[1];
            if(Math.abs(deltaVec[0])>1 || Math.abs(deltaVec[1])>1){
                continue;
            }

            int vecEquiv=Math.abs(deltaVec[0])+Math.abs(deltaVec[1]);
            if(vecEquiv==2 && deltaVec[1]==-2)
                vecEquiv=4;

            sumVecEqui+=vecEquiv;
        }

        traits.rootDirectionalEquivalent=new Double((double)sumVecEqui/(double)(mainRoot.size()));
    }

    private void calcRootStdDevs(){
        List<SkeletonNode> mainRoot=topology.rootMainPath;
        if(mainRoot==null) return;
        double[] mean={0.0,0.0};
        int n=mainRoot.size();
        for(int pixNr=0;pixNr<n;++pixNr){
            mean[0]+=(double)mainRoot.get(pixNr).getX();
            mean[1]+=(double)mainRoot.get(pixNr).getY();
        }
        mean[0]/=(double)n;
        mean[1]/=(double)n;

        double s2[]={0.0,0.0,0.0};
        for(int pixNr=0;pixNr<n;++pixNr)
        {
            double tx,ty;
            tx=(double)mainRoot.get(pixNr).getX()-mean[0];
            ty=(double)mainRoot.get(pixNr).getY()-mean[1];
            s2[0]+=tx*tx;
            s2[1]+=ty*ty;
            s2[2]+=tx*ty;
        }
        s2[0]/=n; //Sxx
        s2[1]/=n; //Syy
        s2[2]/=n; //Sxy


        double[] s={0.0,0.0,0.0};
        s[0]=Math.sqrt(s2[0]); //Sx (stdDev_x)
        s[1]=Math.sqrt(s2[1]); //Sy

        double rxy=s2[2]/(s[0]*s[1]);

        s[2]=rxy*rxy;//syy/s2[1]; //R^2 value
        for(int i=0;i<3;++i)
            traits.rootStdDevs[i]=s[i];
    }

    private void calcRootAngle(){

        if(topology.rootStartNode==null || topology.rootEndNode==null) return;

        double dx=(double)(topology.rootEndNode.getX()-topology.rootStartNode.getX());
        double dy=(double)(topology.rootEndNode.getY()-topology.rootStartNode.getY());

        double phi=Math.atan2(dy,dx);
        //gravitational direction is 0 deg

        double grav_angle=Math.atan2(gravityVec[1],gravityVec[0]);
        double phiDeg=Math.toDegrees(grav_angle)-Math.toDegrees(phi);

        traits.rootAngle=new Double(phiDeg);

        double gravitropicScore;
        if(Math.abs(phiDeg)<15)
            gravitropicScore=0;
        else if(Math.abs(phiDeg)<45)
            gravitropicScore=0.5;
        else
            gravitropicScore=1.0;

        traits.rootGravitropicScore=new Double(gravitropicScore);
    }

    private void calcShootArea(){
        if(topology.shootRoi==null) return;
        ImageProcessor roiMask=topology.shootRoi.getMask();
        Roi tmpRoi=(Roi)topology.shootRoi.clone();
        tmpRoi.setLocation(0,0);
        roiMask.setRoi(tmpRoi);
        traits.shootArea=roiMask.getStatistics().area;
    }
}

class Topology{
    private final static Logger log=Logger.getLogger("edu.salk.brat");
    Roi shootRoi; // node type = 2
    Roi rootRoi; // node type = 1
    Roi transitionRoi; // node type = 3
    Roi centerlineRoi;
    Point shootCenterOfMass;

    Rectangle combinedBounds;

    SkeletonNode rootStartNode;
    SkeletonNode rootEndNode;
    List<SkeletonNode> rootMainPath;

    SkeletonGraph graph;
    SkeletonForrest forrest;

    protected void determineTopology() throws TopologyException {
        calcShootCenterOfMass();
        createGraph();
        detectRootStartEnd();
//		detectRootStartPoint();
        detectRootMainPath();
//		setRootEndPoint();
    }

    private void calcShootCenterOfMass() throws TopologyException {
        Rectangle b = shootRoi.getBounds();
        if (b.width == 0 || b.height == 0) {
            throw new TopologyException("Unexpected shoot dimension. Probably not a plant.");
        }
        ImageProcessor shootMask = shootRoi.getMask();
        shootCenterOfMass = new Point ((int)shootMask.getStatistics().xCenterOfMass+b.x,
                (int)shootMask.getStatistics().yCenterOfMass+b.y);

    }
    private void createGraph(){
        graph = new SkeletonGraph();
        graph.create(shootRoi, rootRoi, transitionRoi, centerlineRoi);
//		forrest = new SkeletonForrest();
//		forrest.create(shootRoi, rootRoi, transitionRoi, centerlineRoi);
    }

    private void detectRootStartEnd() throws TopologyException {
        List<SkeletonNode> longestPath = graph.getLongestShortestPath();

        double minDist = Double.MAX_VALUE;
        SkeletonNode nearestNode = null;
        for (SkeletonNode node: longestPath) {
//			if (node.getType() != 2) {
//				continue;
//			}
            double dist = shootCenterOfMass.distanceSq(node.getX(), node.getY());
            if (dist < minDist){
                nearestNode = node;
                minDist = dist;
            }
        }
//		if(nearestNode == null) {
//			System.out.println();
//		}

        List<SkeletonNode> tmpPath = graph.getLongestPathFromNode(nearestNode);
        rootEndNode = tmpPath.get(tmpPath.size()-1);

//        tmpPath = graph.getLongestPathFromNode(rootEndNode);
        int t1=0;
        SkeletonNode curNode = tmpPath.get(0);
        while(curNode.getType() != 1) { //find first node of type root
            if (t1 == tmpPath.size()-1) {
                log.fine("start point detection: t1 reached end without finding root!");
                break;
            }
            curNode = tmpPath.get(++t1);
        }
        int t2 = t1;
        curNode = tmpPath.get(t2);
        while (curNode.getType() != 2) { // search backwards until shoot node found
            if (t2 == 0) {
                log.fine("start point detection: t2 reached start without finding shoot!");
                break;
            }
            curNode = tmpPath.get(--t2);
        }
        rootStartNode = tmpPath.get((t1 + t2)/2);
        if (rootStartNode.equals(rootEndNode)) {
            throw new TopologyException("Root start / end detection gave unexpected results. Probably not a plant.");
        }
        log.fine(String.format("Start point detection: t1 = %d, t2=%d, selected start node (%d, %d).", t1, t2,
                rootStartNode.getX(), rootStartNode.getY()));
    }

//	private void detectRootStartPoint(){
//		ShapeRoi sRoiShoot=new ShapeRoi(shootRoi);
//		ShapeRoi sRoiRoot=new ShapeRoi(rootRoi);
//		ShapeRoi combinedRoi=sRoiRoot.and(sRoiShoot);
//		if(combinedRoi.getBounds().width!=0 && combinedRoi.getBounds().height!=0){
//			ImageProcessor combinedMask=combinedRoi.getMask();
//			//		new ImagePlus("combined roi",combinedMask).show();
//			double stX=0;
//			double stY=0;
//			int stCnt=0;
//			for(int y=0;y<combinedMask.getHeight();++y){
//				for(int x=0;x<combinedMask.getWidth();++x){
//					if(combinedMask.get(x,y)>0){
//						stX+=x;
//						stY+=y;
//						stCnt++;
//					}
//				}
//			}
//			stX/=stCnt;
//			stY/=stCnt;
//
//			stX+=combinedRoi.getBounds().x;
//			stY+=combinedRoi.getBounds().y;
//			double minDist=Double.MAX_VALUE;
//			for(SkeletonNode node:graph.getNodes()){
//				double dist=node.distanceSq(stX,stY);
//				if(dist<minDist){
//					minDist=dist;
//					rootStartNode=node;
//				}
//			}
//		} //if
//		else{
//			Rectangle shootBounds=shootRoi.getBounds();
//			ImageProcessor shootMask=shootRoi.getMask();
//			List<SkeletonNode> shootNodes=new ArrayList<SkeletonNode>();
//			List<SkeletonNode> rootNodes=new ArrayList<SkeletonNode>();
//			for(SkeletonNode node:graph.getNodes()){
//				int x=node.getX()-shootBounds.x;
//				int y=node.getY()-shootBounds.y;
//				if(x>0 && x<shootBounds.width && y>0 && y<shootBounds.height){
//					if(shootMask.get(x,y)>0){
//						shootNodes.add(node);
//						continue;
//					}
//				}
//				rootNodes.add(node);
//			}
//
//			double minDist=Double.MAX_VALUE;
//			for(SkeletonNode rNode:rootNodes){
//				for(SkeletonNode sNode:shootNodes){
//					double dist=rNode.distanceSq(sNode);
//					if(dist<minDist){
//						minDist=dist;
//						rootStartNode=rNode;
//					}
//				}
//			}
//
//		}
//	}

    private void detectRootMainPath(){
        rootMainPath=graph.getShortestPath(rootStartNode, rootEndNode);
    }

    private void setRootEndPoint(){
        if(rootMainPath!=null){
            if(rootMainPath.size()>0){
                rootEndNode=rootMainPath.get(rootMainPath.size()-1);
            }
        }
    }

    public double getTotalSize() {
        ShapeRoi roi = new ShapeRoi(shootRoi);
        roi.or(new ShapeRoi(rootRoi)).or(new ShapeRoi(centerlineRoi)).or(new ShapeRoi(transitionRoi));
        return roi.getStatistics().area;
    }
}

class Traits{
    final double[] percentiles={0.0,20.0,40.0,60.0,80.0};
    Double rootEuclidianLength;
    Double rootTotalLength;
    Double rootDirectionalEquivalent;
    Double[] rootStdDevs=new Double[3];
    Double rootAngle;
    Double rootGravitropicScore;
    Double[] rootAverageWidths=new Double[percentiles.length+1];
    ;
    Double shootArea;
//	Double endPtScore;
//	Double[] avgShootColor;
//	Double[] medianShootColor;
}