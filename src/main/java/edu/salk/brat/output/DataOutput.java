package edu.salk.brat.output;

import edu.salk.brat.analysis.Plant;
import edu.salk.brat.analysis.graph.SkeletonNode;
import edu.salk.brat.parameters.Parameters;
import edu.salk.brat.utility.ExceptionLog;
import edu.salk.brat.utility.FileUtils;
import edu.salk.brat.utility.StringConverter;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class DataOutput {
	private final static Logger log= Logger.getLogger(DataOutput.class.getName());
//	private static final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
//	private static final Preferences prefs_expert = prefs_simple.node("expert");
	private static final String outputDirectory = new File(Parameters.baseDirectory.getValue()).getAbsolutePath() +
		System.getProperty("file.separator") + "brat-output";

	public static void writePlateDiags(ImageProcessor srcIp, Collection<Plant> plants, int time, String filenamePart, Rectangle diagOffset){
		int offX = 0;
		int offY = 0;
		if (diagOffset != null){
			offX = diagOffset.x;
			offY = diagOffset.y;
		}

		try {
			FileUtils.assertFolder(outputDirectory);
		} catch (IOException e) {
			log.severe(String.format("Error creating output directory: %s! \n%s",outputDirectory, ExceptionLog.StackTraceToString(e)));
			return;
		}
		ImageProcessor diagIp=srcIp.duplicate();
		for(Plant plant:plants){
			if(plant==null) continue;

			Roi shootRoi=plant.getShootRoi(time);
			if(shootRoi!=null){
				if (diagOffset != null){
					shootRoi = new ShapeRoi(shootRoi);
					shootRoi.setLocation(shootRoi.getBounds().x + diagOffset.x, shootRoi.getBounds().y + diagOffset.y);
				}
				diagIp.setColor(Color.decode(Parameters.shootRoiColor.getValue()));
				diagIp.draw(shootRoi);
			}

			Roi rootRoi=plant.getRootRoi(time);
			if(rootRoi!=null){
				if (diagOffset != null){
					rootRoi = new ShapeRoi(rootRoi);
					rootRoi.setLocation(rootRoi.getBounds().x + diagOffset.x, rootRoi.getBounds().y + diagOffset.y);
				}
				diagIp.setColor(Color.decode(Parameters.rootRoiColor.getValue()));
				diagIp.draw(rootRoi);
			}

			Roi transitionRoi=plant.getTransitionRoi(time);
			if(transitionRoi!=null){
				if (diagOffset != null){
					transitionRoi = new ShapeRoi(transitionRoi);
					transitionRoi.setLocation(transitionRoi.getBounds().x + diagOffset.x, transitionRoi.getBounds().y + diagOffset.y);
				}
				diagIp.setColor(Color.decode(Parameters.generalColor.getValue()));
				diagIp.draw(transitionRoi);
			}

			SkeletonNode startPt=plant.getStartNode(time);
			int circleDiameter=Integer.parseInt(Parameters.circleDiameter.getValue());
			if(startPt!=null){
				diagIp.setColor(Color.decode(Parameters.startPtColor.getValue()));
				diagIp.fillOval(startPt.getX()+offX-circleDiameter/2,startPt.getY()+offY-circleDiameter/2,circleDiameter,circleDiameter);
			}

			SkeletonNode endPt=plant.getEndNode(time);
			if(endPt!=null){
				diagIp.setColor(Color.decode(Parameters.endPtColor.getValue()));
				//				diagIp.drawOval(endPt.x-PlateSet.CIRCLEDIAMETER/2,endPt.y-PlateSet.CIRCLEDIAMETER/2,PlateSet.CIRCLEDIAMETER,PlateSet.CIRCLEDIAMETER);
				diagIp.fillOval(endPt.getX()+offX-circleDiameter/2,endPt.getY()+offY-circleDiameter/2,circleDiameter,circleDiameter);
				diagIp.setColor(Color.decode(Parameters.generalColor.getValue()));
				diagIp.setFont(Font.decode(Parameters.labelFont.getValue()));
				diagIp.drawString(plant.getPlantID(),endPt.getX()+offX,endPt.getY()+offY+10+Integer.parseInt(Parameters.labelSize.getValue()));
			}

			List<SkeletonNode> skeleton=plant.getSkeleton(time);
			if(skeleton!=null){
				diagIp.setColor(Color.decode(Parameters.skeletonColor.getValue()));
				for (SkeletonNode aSkeleton : skeleton) {
					diagIp.drawPixel(aSkeleton.getX()+offX, aSkeleton.getY()+offY);
				}
			}

			List<SkeletonNode> mainPath=plant.getRootMainPath(time);
			if(mainPath!=null){
				diagIp.setColor(Color.decode(Parameters.mainRootColor.getValue()));
				for (SkeletonNode aMainPath : mainPath) {
					diagIp.drawPixel(aMainPath.getX()+offX, aMainPath.getY()+offY);
				}
			}
		}

		String savePath=new File(outputDirectory,String.format("Object_Diagnostics_%s.jpg",filenamePart)).getAbsolutePath();
		writeDiagnosticImage(savePath,diagIp);
	}

	public static void writeTraits(Collection<Plant> plants,Integer time,String filenamePart) {
		try {
			FileUtils.assertFolder(outputDirectory);
		} catch (IOException e) {
			log.severe(String.format("Error creating output directory: %s! \n%s",outputDirectory, ExceptionLog.StackTraceToString(e)));
			return;
		}
		String outputPath=new File(outputDirectory,String.format("Object_Measurements_%s.txt",filenamePart)).getAbsolutePath();
		DecimalFormat f = new DecimalFormat("####0.000");

		BufferedWriter output=null;
		try{
			output = new BufferedWriter(new FileWriter(outputPath));

			String newline = System.getProperty("line.separator");
			String nullStr="null";
			String nanStr="nan";

			for(Plant plant:plants){
				if(plant==null)
					continue;

				List<Object> traitList=plant.getTraitsAsList(time);
				if(traitList==null)
					continue;

				StringBuilder sb=new StringBuilder();
				sb.append(plant.getPlantID());
				for (Object aTraitList : traitList) {
					sb.append("\t");
					if (aTraitList == null) {
						sb.append(nullStr);
					} else if (aTraitList instanceof Double) {
						if (((Double) aTraitList).isNaN()) {
							sb.append(nanStr);
						} else {
							sb.append(f.format(aTraitList));
						}
					} else if (aTraitList instanceof String) {
						sb.append(aTraitList);
					}
				}
				sb.append(newline);
				log.fine(String.format("Plant %s: writing traits",plant.getPlantID()));
				output.write(sb.toString());
				output.flush();
			}

		}
		catch(IOException e)
		{
//			e.printStackTrace();
//			StringWriter sw = new StringWriter();
//			e.printStackTrace(new PrintWriter(sw));
//			String stackTrace = sw.toString();
			log.severe(String.format("Error writing file!\n%s",ExceptionLog.StackTraceToString(e)));
		}
		finally{
			if(output!=null){
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void writeSinglePlantDiagnostics(ImageProcessor srcIp,Collection<Plant> plants,Integer time,String filenamePart, Rectangle diagOffset){
		for(Plant plant:plants){
			if(plant==null){
				continue;
			}
			String savePath=new File(outputDirectory,String.format("Plant_%s_Object_Diagnostics_%s.jpg",plant.getPlantID(),filenamePart)).getAbsolutePath();
			ImageProcessor diagIp=createSinglePlantDiagnostic(srcIp,plant,time,100,1000,12,2, diagOffset);
			if(diagIp==null)
				continue;
			writeDiagnosticImage(savePath,diagIp);
		}
	}
	
	private static ImageProcessor createSinglePlantDiagnostic(ImageProcessor srcIp,Plant plant,int time,int border,int newHeight,int plantsPerRow,int plantRows, Rectangle diagOffset){
		if(plant.getRootRoi(time)==null)
			return null;

		ImageProcessor diagIp=srcIp.duplicate();

		ShapeRoi shootRoi=new ShapeRoi(plant.getShootRoi(time));
		if(shootRoi!=null){
			if (diagOffset != null){
				shootRoi.setLocation(shootRoi.getBounds().x+diagOffset.x, shootRoi.getBounds().y+diagOffset.y);
			}
			diagIp.setColor(Color.decode(Parameters.shootRoiColor.getValue()));
			diagIp.draw(shootRoi);
		}

		ShapeRoi rootRoi=new ShapeRoi(plant.getRootRoi(time));
		if(rootRoi!=null){
			if (diagOffset != null){
				rootRoi.setLocation(rootRoi.getBounds().x+diagOffset.x, rootRoi.getBounds().y+diagOffset.y);
			}
			diagIp.setColor(Color.decode(Parameters.rootRoiColor.getValue()));
			diagIp.draw(rootRoi);
		}

		ShapeRoi transitionRoi=new ShapeRoi(plant.getTransitionRoi(time));
		if(transitionRoi!=null){
			if (diagOffset != null){
				transitionRoi.setLocation(transitionRoi.getBounds().x+diagOffset.x, transitionRoi.getBounds().y+diagOffset.y);
			}
			diagIp.setColor(Color.decode(Parameters.generalColor.getValue()));
			diagIp.draw(transitionRoi);
		}

		ShapeRoi centerlineRoi=new ShapeRoi(plant.getCenterlineRoi(time));
		if(centerlineRoi!=null){
			if (diagOffset != null){
				centerlineRoi.setLocation(centerlineRoi.getBounds().x+diagOffset.x, centerlineRoi.getBounds().y+diagOffset.y);
			}
//			diagIp.setColor(Color.decode(Parameters.generalColor.getValue()));
//			diagIp.draw(transitionRoi);
		}


		int offX=0;
		int offY=0;
		if (diagOffset != null) {
			offX = diagOffset.x;
			offY = diagOffset.y;
		}
		SkeletonNode sNode=plant.getStartNode(time);
		int circleDiameter=Integer.parseInt(Parameters.circleDiameter.getValue());
		if(sNode!=null){
			diagIp.setColor(Color.decode(Parameters.startPtColor.getValue()));
//			diagIp.drawOval(this.startPt.x-Parameters.CIRCLEDIAMETER/2,this.startPt.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);
			diagIp.fillOval(sNode.getX()+offX-circleDiameter/2,sNode.getY()+offY-circleDiameter/2,circleDiameter,circleDiameter);
		}

		SkeletonNode eNode=plant.getEndNode(time);
		if(eNode!=null){
			diagIp.setColor(Color.decode(Parameters.endPtColor.getValue()));
//			diagIp.drawOval(rootEndNode.x-Parameters.CIRCLEDIAMETER/2,rootEndNode.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);
			diagIp.fillOval(eNode.getX()+offX-circleDiameter/2,eNode.getY()+offY-circleDiameter/2,circleDiameter,circleDiameter);
			diagIp.setColor(Color.decode(Parameters.generalColor.getValue()));
			diagIp.setFont(Font.decode(Parameters.labelFont.getValue()));
			diagIp.drawString(plant.getPlantID(),eNode.getX()+offX,eNode.getY()+offY+10+Integer.parseInt(Parameters.labelSize.getValue()));
		}

		Collection<SkeletonNode> skeleton=plant.getSkeleton(time);
		if(skeleton!=null){
			diagIp.setColor(Color.decode(Parameters.skeletonColor.getValue()));
			for(SkeletonNode node:skeleton){
				diagIp.drawPixel(node.getX()+offX,node.getY()+offY);
			}
		}

		List<SkeletonNode> rootMainPath=plant.getRootMainPath(time);
		if(rootMainPath!=null){
			diagIp.setColor(Color.decode(Parameters.mainRootColor.getValue()));
			for(int i=0;i<rootMainPath.size();++i){
				diagIp.drawPixel(rootMainPath.get(i).getX()+offX,rootMainPath.get(i).getY()+offY);
			}
		}

//		Point shootCoM=this.topology.getShootCoM();
//		if(shootCoM!=null){
//			diagIp.setColor(Parameters.SHOOTCM_COLOR);
//			diagIp.drawOval(shootCoM.x-Parameters.CIRCLEDIAMETER/2,shootCoM.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);
//		}

		ShapeRoi combinedRoi=shootRoi.or(rootRoi).or(centerlineRoi);
		Rectangle roiRect=combinedRoi.getBounds();
		Rectangle drawRect=new Rectangle(roiRect.x-border,roiRect.y-border,roiRect.width+2*border,roiRect.height+2*border);

		int dx=drawRect.x+drawRect.width-diagIp.getWidth();
		if(dx>0) drawRect.width-=dx;
		if(drawRect.x<0) drawRect.x=0;
		int dy=drawRect.y+drawRect.height-diagIp.getHeight();
		if(dy>0) drawRect.height-=dy;
		if(drawRect.y<0) drawRect.y=0;

		ImageProcessor dstIp1=srcIp.duplicate();
		dstIp1.setColor(Color.red);
		dstIp1.fill(new Roi(drawRect));

		//draw default numbers
		int numberOffsetX1=150;
		int numberOffsetY1=dstIp1.getHeight()/16+150;//drawRect.y-(int)(TopoDiags.DEFAULTNUMBERSIZE*1.1);
		int numberOffsetY2=dstIp1.getHeight()/2-(int)(Integer.parseInt(Parameters.numberSize.getValue())*1.1);
		int deltaX=(dstIp1.getWidth()-2*numberOffsetX1)/(plantsPerRow+1);
		int deltaY=(dstIp1.getHeight()-2*numberOffsetY1)/(plantRows);
		dstIp1.setColor(Color.white);
		dstIp1.setFont(Font.decode(Parameters.numberFont.getValue()));
		for(int j=0;j<plantRows;++j){
			for(int i=0;i<plantsPerRow;++i){
				dstIp1.drawString(Integer.toString(i+1+(j*plantsPerRow)),numberOffsetX1+(i+1)*deltaX-Integer.parseInt(Parameters.numberSize.getValue())/2,numberOffsetY1+(j*deltaY),Color.blue);
				//dstIp1.drawString(Integer.toString(i+1+12),numberOffsetX1+(i+1)*deltaX-TopoDiags.DEFAULTNUMBERSIZE/2,numberOffsetY2,Color.blue);
			}
		}
		int newWidth=(int)((double)newHeight/(double)diagIp.getHeight()*(double)(diagIp.getWidth()));
		dstIp1=dstIp1.resize(newWidth);

		ImageProcessor dstIp2=diagIp.duplicate();
		dstIp2.setRoi(drawRect);
//		ImageProcessor dstIp2=tmpIp2.crop();
		dstIp2=dstIp2.crop();
		newWidth=(int)((double)newHeight/(double)dstIp2.getHeight()*(double)(dstIp2.getWidth()));
		dstIp2=dstIp2.resize(newWidth);


		ImageProcessor dstIp3=srcIp.duplicate();
		dstIp3.setRoi(drawRect);
		dstIp3=dstIp3.crop();
		dstIp3=dstIp3.resize(newWidth);

		ImageProcessor dstIp=new ColorProcessor(dstIp1.getWidth()+dstIp2.getWidth()+dstIp3.getWidth(),newHeight);
		dstIp.copyBits(dstIp1, 0, 0, Blitter.COPY);
		dstIp.copyBits(dstIp2, dstIp1.getWidth(), 0, Blitter.COPY);
		dstIp.copyBits(dstIp3, dstIp1.getWidth()+dstIp2.getWidth(),0,Blitter.COPY);

		return dstIp;
	}
	
//	public static void writeCoordinates(double plateRotation,double scalefactor,Point2D refPt,Shape plateShape,Collection<Plant> plants,Integer time,String filenamePart){
//		try {
//			FileUtils.assertFolder(outputDirectory);
//		} catch (IOException e) {
//			log.severe(String.format("Error creating output directory: %s! \n%s",outputDirectory, ExceptionLog.StackTraceToString(e)));
//		}
//		PlateCoordinates pc=new PlateCoordinates();
//		pc.rotation=plateRotation;
//		pc.scalefactor=scalefactor;
//		pc.refPt=refPt;
//		pc.plateShape=plateShape;
//
//			for(Plant plant:plants){
//				if(plant==null){
//					continue;
//				}
//				String plantID=plant.getPlantID();
//				Roi shootRoi = plant.getShootRoi(time);
//				Roi rootRoi = plant.getRootRoi(time);
//				Roi transitionRoi = plant.getTransitionRoi(time);
//				Roi centerlineRoi = plant.getCenterlineRoi(time);
//				List<Point> shootPixels=null;
//				if(plant.getShootRoi(time)!=null){
//					ImageProcessor mask=plant.getShootRoi(time).getMask();
//					Rectangle bounds=plant.getShootRoi(time).getBounds();
//					shootPixels=new ArrayList<Point>();
//					for(int y=0;y<bounds.height;++y){
//						for(int x=0;x<bounds.width;++x){
//							if(mask.get(x,y)>0){
//								shootPixels.add(new Point(x+bounds.x,y+bounds.y));
//							}
//						}
//					}
//				}
//
//				List<Point> rootPixels=null;
//				if(plant.getRootRoi(time)!=null){
//					ImageProcessor mask=plant.getRootRoi(time).getMask();
//					Rectangle bounds=plant.getRootRoi(time).getBounds();
//					rootPixels=new ArrayList<Point>();
//					for(int y=0;y<bounds.height;++y){
//						for(int x=0;x<bounds.width;++x){
//							if(mask.get(x,y)>0){
//								rootPixels.add(new Point(x+bounds.x,y+bounds.y));
//							}
//						}
//					}
//				}
//
//				pc.plantCoordinates.put(plantID,new ArrayList<Object>());
//				pc.plantCoordinates.get(plantID).add(shootPixels);
//				pc.plantCoordinates.get(plantID).add(rootPixels);
//				pc.plantCoordinates.get(plantID).add(plant.getRootMainPathPoints(time));
//				log.fine(String.format("writing coordinates plant %s", plantID));
//			}
//		}
//
//		String outputPath=new File(outputDirectory,String.format("Object_Coordinates_%s.ser",filenamePart)).getAbsolutePath();
//		ObjectOutputStream oos=null;
//		try{
//			oos=new ObjectOutputStream(new FileOutputStream(outputPath));
//			oos.writeObject(pc);
//		} catch (IOException e) {
//			log.warning(String.format("Could not write Coordinates.\n%s",e.getMessage()));
////					e1.printStackTrace();
//		}
//		finally {
//			try {
//				oos.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	public static void writeDiagnosticImage(final String diagPath,final ImageProcessor diagIp){
		ImagePlus diagImage = new ImagePlus("Plant Diag",diagIp);
		//diagImage.show();
		if(diagPath!=null)
		try {
			log.fine(String.format("creating diagnostic image: %s",diagPath));
			FileUtils.assertFolder(new File(diagPath).getParent());
			FileSaver filesaver = new FileSaver(diagImage);
			//filesaver.saveAsTiff(diagPath);
			filesaver.saveAsJpeg(diagPath);
		} catch (IOException e) {
			log.severe(String.format("Error writing diagnostic image %s.\n%s", diagPath, ExceptionLog.StackTraceToString(e)));
			return;
		}
//		diagImage.close();
	}

//	private Font getFontFromConfigString(String strConfig){
//		String[] strCols = strConfig.split("-");
//		String type=strCols[0];
//		int style = Font.
//		return new Font(strCols[0],strCols[1],strCols[2]);
//	}
}

