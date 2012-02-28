/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.tr;

import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class IsotigsCoverageQualityControl {

    private static final Logger logger = Logger.getLogger("IsotigsCoverageQualityControl");
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

        if (args.length != 3) {
            System.out.println("This program expects three parameters: \n"
                    + "1. Blastx xml filename \n"
                    + "2. Isotigs coverage xml file (output of 'AnalizeIsotigsCoverage' program) \n"
                    + "3. Output quality control results TXT filename ");
        } else {

            File blastFile = new File(args[0]);
            File coverageFile = new File(args[1]);
            File outputFile = new File(args[2]);

            BufferedWriter outBuff = null;

            try {


                outBuff = new BufferedWriter(new FileWriter(outputFile));

                outBuff.write("Quality control results of files: \n");
                outBuff.write("Blast xml file: " + blastFile.getName() + "\n");
                outBuff.write("Isotigs coverage xml file: " + coverageFile.getName() + "\n");


                BufferedReader reader = new BufferedReader(new FileReader(blastFile));
                StringBuilder stBuilder = new StringBuilder();
                String line = null;

                //Reading blastFile
                while ((line = reader.readLine()) != null) {
                    stBuilder.append(line);
                }
                //closing reader
                reader.close();
                //creating blastoutput element
                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());
                //clearing stbuilder
                stBuilder.delete(0, stBuilder.length());

                //Reading isotigs coverage file
                reader = new BufferedReader(new FileReader(coverageFile));

                while ((line = reader.readLine()) != null) {
                    stBuilder.append(line);
                }
                //closing reader
                reader.close();
                XMLElement proteinsXMLElement = new XMLElement(stBuilder.toString());

                List<Element> proteinsList = proteinsXMLElement.asJDomElement().getChildren(ProteinXML.TAG_NAME);

                logger.log(Level.INFO, "Checking number of blast hits and bars...");

                int numberOfHits = 0;
                int numberOfBars = 0;

                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();
                for (Iteration iteration : iterations) {
                    numberOfHits += iteration.getIterationHits().size();
                }

                outBuff.write("\nNumber of Blast hits: " + numberOfHits);

                HashMap<String, ProteinXML> proteinsMap = new HashMap<String, ProteinXML>();

                for (Element proteinElem : proteinsList) {
                    ProteinXML protein = new ProteinXML(proteinElem);
                    proteinsMap.put(protein.getId(), protein);
                    numberOfBars += protein.asJDomElement().getChildren(ContigXML.TAG_NAME).size();
                }

                outBuff.write("\nNumber of Bars: " + numberOfBars);

                if (numberOfBars != numberOfHits) {
                    outBuff.write("\nError --> There should be the same number of hits and bars...");
                } else {
                    outBuff.write("\nBars/hits quality control passed! :)");
                }

                logger.log(Level.INFO, "Checking <number_of_isotigs> tag values...");
                outBuff.write("\n\n<number_of_isotigs> values quality control:");

                boolean numberIsotigsErrorsFound = false;

                for (Element proteinElem : proteinsList) {
                    ProteinXML protein = new ProteinXML(proteinElem);
                    int contigsNumber = protein.asJDomElement().getChildren(ContigXML.TAG_NAME).size();
                    if (protein.getNumberOfIsotigs() != contigsNumber) {
                        outBuff.write("\n Wrong value in protein: " + protein.getId());
                        numberIsotigsErrorsFound = true;
                    }
                }

                if (numberIsotigsErrorsFound) {
                    outBuff.write("\nError --> Number of isotigs tag values control was not passed successfully... :(");
                } else {
                    outBuff.write("\nNumber of isotigs tag control passed! :)");
                }

                logger.log(Level.INFO, "Checking existence of every protein in both files...");
                outBuff.write("\n\nExistence of every proteins in both files quality control:");
                boolean allProteinsFound = true;
                for (Iteration iteration : iterations) {
                    for (Hit hit : iteration.getIterationHits()) {
                        String proteinId = hit.getHitDef().split("\\|")[1];
                        if (proteinsMap.get(proteinId) == null) {
                            allProteinsFound = false;
                            outBuff.write("\nProtein: " + proteinId + " was not found in coverage file!");
                        }
                    }
                }
                if (!allProteinsFound) {
                    outBuff.write("\nError --> Proteins existence control was not passed successfully... :(");
                } else {
                    outBuff.write("\nProteins existence control passed! :)");
                }

                logger.log(Level.INFO, "Checking protein coverage values absolute/percentage...");
                outBuff.write("\n\nProtein coverage values quality control (absolute & percentage):");

                boolean proteinCoverageValuesErrorsFound = false;

                for (Element proteinElem : proteinsList) {

                    ProteinXML protein = new ProteinXML(proteinElem);
                    List<Element> contigs = protein.asJDomElement().getChildren(ContigXML.TAG_NAME);

                    int coveredPositions = 0;
                    for (int i = 1; i <= protein.getLength(); i++) {
                        for (Element contigElem : contigs) {
                            ContigXML contigXML = new ContigXML(contigElem);
                            if (i >= contigXML.getBegin() && i <= contigXML.getEnd()) {
                                coveredPositions++;
                                break;
                            }
                        }
                    }

                    if (protein.getProteinCoverageAbsolute() != coveredPositions) {
                        outBuff.write("\nProtein: " + protein.getId() + " has an incorrect coverage absolute value!");
                        proteinCoverageValuesErrorsFound = true;
                    }

                    double coveragePercentage = (coveredPositions * 100.0)/protein.getLength();
                    if(coveragePercentage != protein.getProteinCoveragePercentage()){
                        outBuff.write("\nProtein: " + protein.getId() + " has an incorrect coverage percentage value!");
                        proteinCoverageValuesErrorsFound = true;
                    }

                }

                if (proteinCoverageValuesErrorsFound) {
                    outBuff.write("\nError --> Protein coverage values control was not passed successfully... :(");
                } else {
                    outBuff.write("\nProtein coverage values control passed! :)");
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
