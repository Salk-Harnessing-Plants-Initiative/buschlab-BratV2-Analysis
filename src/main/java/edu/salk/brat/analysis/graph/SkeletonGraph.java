package edu.salk.brat.analysis.graph;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.EDM;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.collections15.Transformer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class SkeletonGraph {
	private final static Logger log=Logger.getLogger(SkeletonGraph.class.getName());
	private final double SQRT2 = Math.sqrt(2.0);
	private Graph<SkeletonNode, SkeletonLink> graph;
	private Map<Integer, SkeletonNode> nodeMap;
	private List<SkeletonNode> endNodes;

//	List<SkeletonNode> path;



	public Collection<SkeletonNode> getEndNodes(){
		return endNodes;
	}

	public Collection<SkeletonNode> getNodes(){
		return nodeMap.values();
	}

//	public void testHash(Rectangle bounds){
//		Map<Integer,SkeletonNode> usedHashes=new HashMap<Integer,SkeletonNode>();
//		for(int y=bounds.y;y<bounds.y+bounds.height;++y){
//			for(int x=bounds.x;x<bounds.x+bounds.width;++x){
//				SkeletonNode node=new SkeletonNode(x,y);
//				if(!usedHashes.containsKey(node.hashCode())){
//					usedHashes.put(node.hashCode(),node);
//				}
//				else{
//					SkeletonNode prevNode=usedHashes.get(node.hashCode());
//					SkeletonNode newNode=new SkeletonNode(x,y);
//					IJ.log("duplicate hash: prev="+prevNode.toString()+"("+node.hashCode()+"), actual="+newNode.toString()+"("+newNode.hashCode()+")");
//				}
//			}
//		}
//	}

	public void create(Roi shootRoi, Roi rootRoi, Roi transitionRoi, Roi centerlineRoi) {
		ShapeRoi sRoiCombined = new ShapeRoi(shootRoi);
		sRoiCombined.or(new ShapeRoi(rootRoi)).or(new ShapeRoi(transitionRoi)).or(new ShapeRoi(centerlineRoi));
		Rectangle bounds = sRoiCombined.getBounds();

		ImageProcessor maskIp = sRoiCombined.getMask();
		BinaryProcessor skelIp = new BinaryProcessor((ByteProcessor) maskIp.duplicate());
		skelIp.invert();
		skelIp.skeletonize();
//		new ImagePlus("skeleton", skelIp).show();

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

		create(skelIp, 0, edmIp, maskIp, bounds);
	}

//	public void create(Roi roi){
//		Rectangle roiBounds=roi.getBounds();
//		log.fine(String.format("creating Roi. (roiBounds: %s)",roiBounds.toString()));
//
//		ImageProcessor maskIp=roi.getMask();
//
//		ImageProcessor edmIp=maskIp.duplicate();
//		EDM edm=new EDM();
//		edm.toEDM(edmIp);
//		BinaryProcessor skelIp=new BinaryProcessor((ByteProcessor) maskIp.duplicate());
//		skelIp.invert();
//		skelIp.skeletonize();
//
//		create(skelIp,0,edmIp,roiBounds);
//	}

	public void create(ImageProcessor skelIp,int fgValue,ImageProcessor edmIp,ImageProcessor maskIp, Rectangle bounds){
		graph=new UndirectedSparseGraph<SkeletonNode, SkeletonLink>();
//		new ImagePlus("edm",edmIp).show();
//		new ImagePlus("skeleton",skelIp).show();

//		double sqrt2=Math.sqrt(2.0);
		endNodes=new ArrayList<SkeletonNode>();
		nodeMap=new HashMap<Integer, SkeletonNode>();
		for(int y=0;y<skelIp.getHeight();++y){
			for(int x=0;x<skelIp.getWidth();++x){
				if(skelIp.get(x,y)==fgValue){
					int nbCnt=0;

					int edmVal = edmIp.get(x,y);
					int typeVal = maskIp.get(x,y);

					SkeletonNode node=new SkeletonNode(x+bounds.x,y+bounds.y,edmVal, typeVal);
					if(!nodeMap.containsKey(node.hashCode())){
						nodeMap.put(node.hashCode(),node);
					}
					else{
						node=nodeMap.get(node.hashCode());
					}

					for(int ny=y-1;ny<=y+1;++ny){
						if(ny<0 || ny>skelIp.getHeight()-1){
							continue;
						}
						for(int nx=x-1;nx<=x+1;++nx){
							if(nx<0 || nx>skelIp.getWidth()-1 || (nx==x && ny==y)){
								continue;
							}
							if(skelIp.get(nx,ny)==fgValue){
								++nbCnt;
								double length=Math.abs(x-nx)+Math.abs(y-ny)==2 ? SQRT2 : 1.0;

								int nedmVal = edmIp.get(nx,ny);
								int ntypeVal = maskIp.get(nx, ny);

								SkeletonNode nbNode=new SkeletonNode(nx+bounds.x,ny+bounds.y,nedmVal, ntypeVal);
								if(!nodeMap.containsKey(nbNode.hashCode())){
									nodeMap.put(nbNode.hashCode(),nbNode);
								}
								else{
									nbNode=nodeMap.get(nbNode.hashCode());
								}

								graph.addEdge(new SkeletonLink(length),node,nbNode);
							}
						} //for nx
					} //for ny
					if(nbCnt==1){
						endNodes.add(node);
					}
				}
			} //for x
		} //for y
	}

	public List<SkeletonNode> getLongestShortestPath(){
        Transformer<SkeletonLink, Double> wtTransformer = new Transformer<SkeletonLink,Double>() {
            public Double transform(SkeletonLink link) {
                return link.getLength();
            }
        };


        DijkstraShortestPath<SkeletonNode, SkeletonLink> alg = new DijkstraShortestPath<SkeletonNode, SkeletonLink>(graph,wtTransformer);
        Double maxLength=0.0;
        SkeletonNode lpStartNode=null;
        SkeletonNode lpEndNode=null;
        for(int i=0;i<endNodes.size();++i){
    		SkeletonNode sNode=endNodes.get(i);
        	for(int j=i+1;j<endNodes.size();++j){
        		SkeletonNode eNode=endNodes.get(j);
        		Number dist=alg.getDistance(sNode,eNode);
        		if(dist==null){
        			continue;
        		}
        		if(dist.doubleValue()>maxLength){
        			maxLength=dist.doubleValue();
        			lpStartNode=sNode;
        			lpEndNode=eNode;
        		}
        	}
        }

        List<SkeletonNode> longestShortestPath=null;
        if(lpStartNode!=null && lpEndNode!=null){
        	List<SkeletonLink> lpLinks = alg.getPath(lpStartNode,lpEndNode);

        	longestShortestPath=new ArrayList<SkeletonNode>();
        	List<SkeletonNode> edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(lpLinks.get(0)));
        	longestShortestPath.add(edgeVertices.get(0));
        	for(SkeletonLink edge:lpLinks){
        		edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(edge));
        		longestShortestPath.add(edgeVertices.get(1));
        	}
        }
        return longestShortestPath;
	}

	public List<SkeletonNode> getShortestPath(SkeletonNode startNode, SkeletonNode endNode) {
		List<SkeletonNode> shortestPath=null;

		Transformer<SkeletonLink, Double> wtTransformer = new Transformer<SkeletonLink,Double>() {
			public Double transform(SkeletonLink link) {
				return link.getLength();
			}
		};

		DijkstraShortestPath<SkeletonNode, SkeletonLink> alg = new DijkstraShortestPath<SkeletonNode, SkeletonLink>(graph,wtTransformer);

		if(startNode!=null && endNode!=null){
			List<SkeletonLink> lpLinks = alg.getPath(startNode,endNode);

			shortestPath=new ArrayList<SkeletonNode>();
			List<SkeletonNode> edgeVertices= new ArrayList<>(graph.getIncidentVertices(lpLinks.get(0)));
			shortestPath.add(edgeVertices.get(0));
			for(SkeletonLink edge:lpLinks){
				edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(edge));
				shortestPath.add(edgeVertices.get(1));
			}
		}
		return shortestPath;
	}

	public List<SkeletonNode> getLongestPathFromNode(SkeletonNode node){
		if(!graph.containsVertex(node)){
			return null;
		}

        Transformer<SkeletonLink,Double> lenTransformer = SkeletonLink::getLength;
        DijkstraShortestPath<SkeletonNode, SkeletonLink> alg = new DijkstraShortestPath<>(graph, lenTransformer);

		Double maxLength=0.0;
		SkeletonNode endNode=null;
        for(int i=0;i<endNodes.size();++i){
			Double length=(Double)alg.getDistance(node,endNodes.get(i));
			if(length!=null){
				if(length>maxLength){
					maxLength=length;
					endNode=endNodes.get(i);
				}
			}
        }

        List<SkeletonNode> pNodes=new ArrayList<SkeletonNode>();
        if(endNode!=null){
        	List<SkeletonLink> lpLinks = alg.getPath(node,endNode);
        	List<SkeletonNode> edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(lpLinks.get(0)));
        	pNodes.add(edgeVertices.get(0));
        	for(SkeletonLink edge:lpLinks){
        		edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(edge));
        		pNodes.add(edgeVertices.get(1));
        	}
        }
        else{
        	pNodes=null;
        }

        return pNodes;
	}

	public List<SkeletonNode> getLongestPathContainingNode(SkeletonNode node){
		if(!graph.containsVertex(node)){
			return null;
		}

        Transformer<SkeletonLink,Double> lenTransformer = new Transformer<SkeletonLink,Double>(){
            public Double transform(SkeletonLink link) {
                return link.getLength();
            }
        };

        DijkstraShortestPath<SkeletonNode, SkeletonLink> alg = new DijkstraShortestPath<SkeletonNode, SkeletonLink>(graph,lenTransformer);
        List<SkeletonNode> reachableEndNodes=new ArrayList<SkeletonNode>();
		for(int i=0;i<endNodes.size();++i){
			Double length=(Double)alg.getDistance(node,endNodes.get(i));
			if(length!=null){
				reachableEndNodes.add(endNodes.get(i));
			}
		}

		Double maxLength=0.0;
        SkeletonNode lpStartNode=null;
        SkeletonNode lpEndNode=null;
        for(int i=0;i<reachableEndNodes.size();++i){
    		SkeletonNode sNode=reachableEndNodes.get(i);
        	for(int j=i+1;j<reachableEndNodes.size();++j){
        		SkeletonNode eNode=reachableEndNodes.get(j);
        		Number dist=alg.getDistance(sNode,eNode);

        		if(dist.doubleValue()>maxLength){
        			maxLength=dist.doubleValue();
        			lpStartNode=sNode;
        			lpEndNode=eNode;
        		}
        	}
        }

        List<SkeletonNode> lpNodes=new ArrayList<SkeletonNode>();
        if(lpStartNode!=null && lpEndNode!=null){
            if(node.distanceSq(lpStartNode)>node.distanceSq(lpEndNode)){
            	SkeletonNode tmpNode=lpStartNode;
            	lpStartNode=lpEndNode;
            	lpEndNode=tmpNode;
            }
        	List<SkeletonLink> lpLinks = alg.getPath(lpStartNode,lpEndNode);
        	List<SkeletonNode> edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(lpLinks.get(0)));
        	lpNodes.add(edgeVertices.get(0));
        	for(SkeletonLink edge:lpLinks){
        		edgeVertices=new ArrayList<SkeletonNode>(graph.getIncidentVertices(edge));
        		lpNodes.add(edgeVertices.get(1));
        	}
        }
        else{
        	lpNodes=null;
        }

		return lpNodes;
	}

	public Double getPathLength(SkeletonNode startNode, SkeletonNode endNode){
		if(!nodeMap.containsValue(startNode)){
			return null;
		}
		if(!nodeMap.containsValue(endNode)){
			return null;
		}

		Transformer<SkeletonLink, Double> wtTransformer = new Transformer<SkeletonLink,Double>() {
            public Double transform(SkeletonLink link) {
                return link.getLength();
            }
        };
        DijkstraShortestPath<SkeletonNode, SkeletonLink> alg = new DijkstraShortestPath<SkeletonNode, SkeletonLink>(graph,wtTransformer);
		
		return (Double)alg.getDistance(startNode,endNode);
	}
		
}
