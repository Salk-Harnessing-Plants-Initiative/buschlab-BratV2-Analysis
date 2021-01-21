package edu.salk.brat.analysis.graph;

import java.awt.*;

public class SkeletonNode {
	private int x;
	private int y;
	private Integer dmapValue;
	private Integer type; // 1=root, 2=shoot, 3=transition
	
	public SkeletonNode(int x,int y){
		this.x=x;
		this.y=y;
	}
	
	public SkeletonNode(Point pt){
		this.x=pt.x;
		this.y=pt.y;
	}
	
	public SkeletonNode(int x,int y,int dmapValue, int type){
		this.x=x;
		this.y=y;
		this.dmapValue=dmapValue;
		this.type = type;
	}
	

	@Override
	public int hashCode() {
		return (int)(y<<16)+x;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SkeletonNode other = (SkeletonNode) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getDMapValue(){
		return dmapValue;
	}

	public int getType() { return type; }
	
	public Point toPoint(){
		return new Point(x,y);
	}
	
	public String toString(){
		return "V("+x+","+y+")";
	}
	
	public Double distanceSq(double x,double y){
		double dx=this.x-x;
		double dy=this.y-y;
		return dx*dx+dy*dy;
	}
	
	public Double distanceSq(SkeletonNode oNode){
		if(oNode==null){
			return null;
		}
		double dx=x-oNode.getX();
		double dy=y-oNode.getY();
		return dx*dx+dy*dy;
	}
	
	public Double distance(SkeletonNode oNode){
		if(oNode==null){
			return null;
		}
		return Math.sqrt(distanceSq(oNode));
	}
}
