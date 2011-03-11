/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.tr;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.ProteinXML;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class AnalyzeIsotigsCoverage implements Executable {

    private static final Logger logger = Logger.getLogger("AnalyzeIsotigsCoverage");
    private static FileHandler fh;

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("This program expects two parameters: \n"
                    + "1. Blastx xml filename \n"
                    + "2. Output xml filename \n");
        } else {

            File blastFile = new File(args[0]);
            File outputFile = new File(args[1]);

            BufferedWriter outBuff = null;

            try {

                outBuff = new BufferedWriter(new FileWriter(outputFile));
                outBuff.write("<proteins>\n");

                logger.log(Level.INFO, "Reading blast file...");
                BufferedReader reader = new BufferedReader(new FileReader(blastFile));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stBuilder.append(line);
                }
                reader.close();
                logger.log(Level.INFO, "Building blastoutput xml....");
                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());

                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();
                //Map with isotigs/contigs per protein
                HashMap<String, ArrayList<ContigXML>> proteinContigs = new HashMap<String, ArrayList<ContigXML>>();
                //Protein info map
                HashMap<String, ProteinXML> proteinInfoMap = new HashMap<String, ProteinXML>();

                for (Iteration iteration : iterations) {
                    String contigNameSt = iteration.getUniprotIdFromQueryDef();
                    ContigXML contig = new ContigXML();
                    contig.setId(contigNameSt);

                    ArrayList<Hit> hits = iteration.getIterationHits();
                    for (Hit hit : hits) {
                        String proteinIdSt = hit.getHitDef().split("\\|")[1];

                        ArrayList<ContigXML> contigsArray = proteinContigs.get(proteinIdSt);


                        if (contigsArray == null) {
                            //Creating contigs array
                            contigsArray = new ArrayList<ContigXML>();
                            proteinContigs.put(proteinIdSt, contigsArray);
                            //Creating protein info
                            ProteinXML proteinXML = new ProteinXML();
                            proteinXML.setId(proteinIdSt);
                            proteinXML.setLength(hit.getHitLen());
                            proteinInfoMap.put(proteinIdSt, proteinXML);
                        }

                        ArrayList<Hsp> hsps = hit.getHitHsps();
                        int hspMinHitFrom = 1000000000;
                        int hspMaxHitTo = -1;

                        //---Figuring out the isotig/contig positions
                        for (Hsp hsp : hsps) {
                            int hspFrom = hsp.getHitFrom();
                            int hspTo = hsp.getHitTo();
//                            System.out.println("hsp = " + hsp);
//                            System.out.println("hsp.getHitFrame() = " + hsp.getHitFrame());
//                            if (hsp.getQueryFrame() < 0) {
//                                hspFrom = hsp.getHitTo();
//                                hspTo = hsp.getHitFrom();
//                            }

                            if (hspFrom < hspMinHitFrom) {
                                hspMinHitFrom = hspFrom;
                            }
                            if (hspTo > hspMaxHitTo) {
                                hspMaxHitTo = hspTo;
                            }
                        }
                        //-------------------

                        contig.setBegin(hspMinHitFrom);
                        contig.setEnd(hspMaxHitTo);
                        if(contig.getBegin() > contig.getEnd()){
                            contig.setBegin(hspMaxHitTo);
                            contig.setEnd(hspMinHitFrom);
                        }
                        contigsArray.add(contig);


                    }
                }               
                

                for (String proteinKey : proteinInfoMap.keySet()) {
                    //---calculating coverage and creating output xml----

                    ProteinXML proteinXML = proteinInfoMap.get(proteinKey);                  

                    ArrayList<ContigXML> contigs = proteinContigs.get(proteinKey);
                    for (ContigXML contigXML : contigs) {
                        proteinXML.addChild(contigXML);
                    }

                    proteinXML.setNumberOfIsotigs(contigs.size());

                    int coveredPositions = 0;
                    for(int i=1;i<=proteinXML.getLength();i++){
                        for (ContigXML contigXML : contigs) {
                            if(i>= contigXML.getBegin() && i<= contigXML.getEnd()){
                                coveredPositions++;
                                break;
                            }
                        }
                    }

                    proteinXML.setProteinCoverageAbsolute(coveredPositions);
                    proteinXML.setProteinCoveragePercentage((coveredPositions * 100.0)/proteinXML.getLength());

                    outBuff.write(proteinXML.toString() + "\n");

                }


            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage());
                StackTraceElement[] trace = e.getStackTrace();
                for (StackTraceElement stackTraceElement : trace) {
                    logger.log(Level.SEVERE, stackTraceElement.toString());
                }
            } finally {
                logger.log(Level.INFO, "Closing output file...");
                try {
                    outBuff.write("</proteins>\n");
                    outBuff.close();
                    logger.log(Level.INFO, "Done! :)");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    StackTraceElement[] trace = e.getStackTrace();
                    for (StackTraceElement stackTraceElement : trace) {
                        logger.log(Level.SEVERE, stackTraceElement.toString());
                    }
                }
            }


        }
    }
}
