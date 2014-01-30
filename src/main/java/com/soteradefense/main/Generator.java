package com.soteradefense.main;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.TimeStamp;

public class Generator {
	
	private double resolutionLat;
	private double resolutionLon;
	private String inputFile;
	private String outFile;
	
//	private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final Color minColor = Color.green;
	private final Color maxColor = Color.red;
	
	Generator(double resLat, double resLon, String inFile) {
		resolutionLat = resLat/2;
		resolutionLon = resLon/2;
		inputFile = inFile;
		outFile = inputFile.substring(0, inputFile.length()-4)+".kml";
	}
	
	private String zeroPadString(String data, int padding) {
		while(data.length() < padding) {
			data = "0"+data;
		}
		
		return data;
	}
	
	private void CreateStyleFromValue(Document doc, Double val, Double min, Double max) {
		Style newStyle = doc.createAndAddStyle().withId(val.toString());
		
		val = Math.log(val);
		min = Math.log(min);
		max = Math.log(max);
		
		//choose color based on value
		double interp = (val - min)/(max - min);
		
		int r = (int)((maxColor.getRed() * interp) + (minColor.getRed() * (1 - interp)));
		int g = (int)((maxColor.getGreen() * interp) + (minColor.getGreen() * (1 - interp)));
		int b = (int)((maxColor.getBlue() * interp) + (minColor.getBlue() * (1 - interp)));
				
		Color interpColor = new Color(r, g, b);
		String hexR = zeroPadString(Integer.toHexString(interpColor.getRed()), 2);
		String hexG = zeroPadString(Integer.toHexString(interpColor.getGreen()), 2);
		String hexB = zeroPadString(Integer.toHexString(interpColor.getBlue()), 2);
		
		String hex = "ff"+hexB+hexG+hexR;
		
		newStyle.createAndSetPolyStyle().withColor(hex).withOutline(false);
	}
	
	public int generateKML() throws Exception {
		
		Kml outkml = new Kml();
		Document document = new Document();
		outkml.setFeature(document);
		
		document.setName(outFile);
		document.setOpen(true);
		
		ArrayList<Double> styles = new ArrayList<Double>();
		
		//read file in
		File inFile = new File(inputFile);
		
		//get value min/max
		BufferedReader fileReader = new BufferedReader(new FileReader(inFile));
		
		Double min = null;
		Double max = null;
		String line = fileReader.readLine();
		line = fileReader.readLine();
		while(line != null) {
			
			String[] elements = line.split("\t", -1);
			
			try {
				Double value = new Double(elements[2]);
				
				if(min == null || min > value) {
					min = value;
				}
				
				if(max == null || max < value) {
					max = value;
				}
			} catch(Exception e) {
				System.err.println("Failed to parse record: "+line);
			}
			
			line = fileReader.readLine();
		}
		fileReader.close();
		
		//reset reader for kml creating pass
		fileReader = new BufferedReader(new FileReader(inFile));
		
		int polygons = 0;
		//for each line (after header)
		line = fileReader.readLine();
		line = fileReader.readLine();
		while(line != null) {
			
			String[] elements = line.split("\t", -1);
			
			try {
				double lat = Double.parseDouble(elements[0]);
				double lon = Double.parseDouble(elements[1]);
				Double value = new Double(elements[2]);
//				Date date = df.parse(elements[3]);
			
				//if new value, create new style
				if(!styles.contains(value)) {
					CreateStyleFromValue(document, value, min, max);
					styles.add(value);
				}
				
				//determine bounds of square
				double leftLon = lon - resolutionLon;
				double rightLon = lon + resolutionLon;
				double upperLat = lat + resolutionLat;
				double lowerLat = lat - resolutionLat;
			
				//write kml object to file			
				TimeStamp stamp = new TimeStamp();
				stamp.setWhen(elements[3]);
				
				Placemark placemark = document.createAndAddPlacemark().withStyleUrl("#"+value.toString()).withTimePrimitive(stamp);
				
				Polygon polygon = placemark.createAndSetPolygon().withExtrude(true)
						.withAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
				
				polygon.createAndSetOuterBoundaryIs().createAndSetLinearRing()
					.addToCoordinates(leftLon, upperLat)
					.addToCoordinates(leftLon, lowerLat)
					.addToCoordinates(rightLon, lowerLat)
					.addToCoordinates(rightLon, upperLat)
					.addToCoordinates(leftLon, upperLat);
				
				polygons++;
				
			} catch(Exception e) {
				System.err.println("Failed to parse record: "+line);
				e.printStackTrace();
			}
		
			line = fileReader.readLine();
		}
		
		fileReader.close();
		
		outkml.marshal(new File(outFile));
		
		return polygons;
	}

	public static void main(String[] args) {
		
		if(args.length < 3 || args.length > 3) {
			System.err.println(
		            "Usage: java -jar micropath-kml-0.0.1-SNAPSHOT-jar-with-dependencies.jar {Latitude Resolution} {Longitude Resolution} {Input File}\n");
		} else {
			try {
				
				System.out.println("Attempting to generate kml....");
				
				double lat = Double.parseDouble(args[0]);
				double lon = Double.parseDouble(args[1]);
				String inFile = args[2];
				
				Generator gen = new Generator(lat, lon, inFile);
				
				int polys = gen.generateKML();
				
				System.out.println("Finished generating kml ("+polys+" polygons)");
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
