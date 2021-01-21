package edu.salk.brat.output;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class PlateCoordinates implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -2826357954582508727L;
	public double rotation;
	public double scalefactor;
	public Point2D refPt;
	public Shape plateShape;

	public Map<String, java.util.List<Object>> plantCoordinates=new HashMap<String, java.util.List<Object>>();
}
