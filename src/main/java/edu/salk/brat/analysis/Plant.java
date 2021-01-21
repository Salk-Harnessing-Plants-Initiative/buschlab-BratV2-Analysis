package edu.salk.brat.analysis;

import edu.salk.brat.analysis.graph.SkeletonForrest;
import edu.salk.brat.analysis.graph.SkeletonNode;
import edu.salk.brat.analysis.graph.SkeletonGraph;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class Plant {
	private final static Logger log=Logger.getLogger("edu.salk.brat");
	String plantID;
	
	Roi seedRoi;
	Point2D seedCenter;
	
//	List<Roi> shootRoi;
//	List<Roi> plantRoi;
	Map<Integer, Phenotype> phenotype;
	Point rootTrackPt;
	Point shootCoM;
	
	public Plant(int plantNr){
		this.plantID=String.format("%02d",plantNr+1);
		this.phenotype=new TreeMap<Integer, Phenotype>();
//		this.shootRoi=new ArrayList<Roi>();
//		this.plantRoi=new ArrayList<Roi>();
	}
	
	public void setSeedRoi(Roi seedRoi){
		this.seedRoi=seedRoi;
	}
	public Point2D getSeedCenter() {
		return seedCenter;
	}
	public void setSeedCenter(Point2D seedCenter) {
		this.seedCenter = seedCenter;
	}

	
	public String getPlantID(){
		return plantID;
	}
	public void setPlantNr(int plantNr) { this.plantID=String.format("%02d",plantNr+1);}

	public void removeTimePt(int timePt) {
		if (phenotype.containsKey(timePt)){
			phenotype.remove(timePt);
		}
	}

	public  Collection<Integer> getTimePts() {
		return phenotype.keySet();
	}

	public double getTotalSize(int time) {
		if (phenotype.containsKey(time)) {
			return phenotype.get(time).topology.getTotalSize();
		}
		return -1;
	}

	public void setPhenotype(int time, Phenotype phenotype) {
		this.phenotype.put(time, phenotype);
	}

	public Phenotype getPhenotype(int time) {
		if (this.phenotype.containsKey(time)) {
			return this.phenotype.get(time);
		}
		return null;
	}

	public Roi getShootRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.shootRoi;
		}
		return null;
	}
	public void setShootRoi(Integer time,Roi shootRoi) {
		if(!phenotype.containsKey(time))
			phenotype.put(time,new Phenotype());
		phenotype.get(time).topology.shootRoi=shootRoi;
	}

	public void setShootCoM(Point shootCoM){
		this.shootCoM=shootCoM;
	}
	public Point getShootCoM(){
		return shootCoM;
	}
	
	public Point getRootTrackPt() {
		return rootTrackPt;
	}
	public void setRootTrackPt(Point rootTrackPt) {
		this.rootTrackPt = rootTrackPt;
	}

	
	public Roi getRootRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootRoi;
		}
		return null;
	}
	public void setRootRoi(Integer time,Roi rootRoi) {
		if(phenotype.containsKey(time)){
			phenotype.get(time).topology.rootRoi=rootRoi;
		}
	}

	public Roi getTransitionRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.transitionRoi;
		}
		return null;
	}

	public void setTransitionRoi(Integer time, Roi transitionRoi) {
		if (phenotype.containsKey(time)) {
			phenotype.get(time).topology.transitionRoi = transitionRoi;
		}
	}

	public Roi getCenterlineRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.centerlineRoi;
		}
		return null;
	}

	public void setCenterlineRoi(Integer time, Roi centerlineRoi) {
		if (phenotype.containsKey(time)) {
			phenotype.get(time).topology.centerlineRoi = centerlineRoi;
		}
	}


	public void createTopology(Integer time) throws TopologyException {
		if(phenotype.containsKey(time)){
			phenotype.get(time).topology.determineTopology();
		}
	}
	public void createTopologies() throws TopologyException {
		for(Integer time:phenotype.keySet()){
			phenotype.get(time).topology.determineTopology();
		}
	}
	
	public List<SkeletonNode> getRootMainPath(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootMainPath; 
		}
		return null;
	}

	public List<SkeletonNode> getSkeleton(int time) {
		if(phenotype.containsKey(time)){
			return new ArrayList<>(phenotype.get(time).topology.graph.getNodes());
		}
		return null;
	}

	public List<Point> getRootMainPathPoints(int time){
		List<Point> pathPts=null;
		if(phenotype.containsKey(time)){
			List<SkeletonNode> mainPath=phenotype.get(time).topology.rootMainPath;
			if(mainPath!=null){
				pathPts=new ArrayList<Point>();
				for(SkeletonNode node:mainPath){
					pathPts.add(new Point(node.getX(),node.getY()));
				}
			}
		}
		return pathPts;
	}
	
	public SkeletonNode getStartNode(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootStartNode; 
		}
		return null;
	}
	
	public SkeletonNode getEndNode(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootEndNode; 
		}
		return null;
		
	}
	public void calcTraits(Integer time){
		if(phenotype.containsKey(time)){
			phenotype.get(time).calcTraits();
		}
	}
	
//	public Topology getTopology(int time){
//		if(phenotype.containsKey(time)){
//			return phenotype.get(time).topology; 
//		}
//		return null;
//	}
	
	public List<Object> getTraitsAsList(Integer time){
		if(phenotype.containsKey(time)){
			List<Object> tl=new ArrayList<Object>();
			tl.add(phenotype.get(time).traits.rootEuclidianLength); 
			tl.add(phenotype.get(time).traits.rootTotalLength);
			tl.add(phenotype.get(time).traits.rootDirectionalEquivalent);
			for(int i=0;i<phenotype.get(time).traits.rootStdDevs.length;++i){
				tl.add(phenotype.get(time).traits.rootStdDevs[i]);
			}
			for(int i=0;i<phenotype.get(time).traits.rootAverageWidths.length;++i){
				tl.add(phenotype.get(time).traits.rootAverageWidths[i]);
			}
			tl.add(phenotype.get(time).traits.shootArea);
			tl.add(phenotype.get(time).traits.rootAngle);
			tl.add("unused");
			tl.add("unused");
			tl.add(phenotype.get(time).traits.rootGravitropicScore);

			return tl;
		}
		return null;
	}
	
	public Double getRootEuclidianLength(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootEuclidianLength;
		}
		return null;
	}
	public Double getRootTotalLength(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootTotalLength;
		}
		return null;
	}
	public Double getRootDirectionalEquivalent(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootDirectionalEquivalent;
		}
		return null;
	}
	public Double[] getRootStdDevs(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootStdDevs;
		}
		return null;
	}
	public Double getRootAngle(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootAngle;
		}
		return null;
	}
	public Double getRootGravitropicScore(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootGravitropicScore;
		}
		return null;
	}
	public Double[] getRootAverageWidths(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootAverageWidths;
		}
		return null;
	}
	
//	public Collection<SkeletonNode> getRootSkeleton(Integer time){
//		return phenotype.get(time).topology.graph.getNodes();
//	}
}


