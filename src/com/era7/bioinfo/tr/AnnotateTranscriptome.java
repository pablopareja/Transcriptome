/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.tr;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.uniprot.UniprotProteinRetreiver;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.PredictedGene;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author ppareja
 */
public class AnnotateTranscriptome implements Executable {

    public static final String UNIREF_90_BASE_URL = "http://www.uniprot.org/uniref/UniRef90_";
    public static final String ANNOTATION_FILE_SUFIX = "_annotation.txt";
    public static final String ANNOTATION_FILE_HEADER = "id_isotig\tbegin\tend\tstrand\tuniprot_id\tuniprot_url\teValue\thsp_consistency\n";
    public static final String DATA_FILE_SUFIX = "_data.txt";
    public static final String DATA_FILE_HEADER = "UniprotId\tProtein names\tGene names\tOrganism\tGene Ontology ID\tGene Ontology\tKeywords\tInterPro\tEC numbers\tDomains\tProtein family\tMapped PubMed ID\tPubMed ID\tLength\tSequence\n";
    public static final String SEPARATOR = "\t";

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
        if (args.length != 1) {
            System.out.println("This program expects one parameters: \n"
                    + "1. Input Blast XML file \n");
        } else {

            File blastFile = new File(args[0]);


            try {

                //This hashmap is used for caching of protein data so that more than
                //necessary calls to the WS are not performed
                HashMap<String, PredictedGene> cachedUniprotData = new HashMap<String, PredictedGene>();

                System.out.println("Reading blast file...");
                BufferedReader reader = new BufferedReader(new FileReader(blastFile));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stBuilder.append(line);
                }
                reader.close();
                System.out.println("Building blastoutput xml....");
                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());

                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();
                HashMap<String, ArrayList<Iteration>> sampleIterationsMap = new HashMap<String, ArrayList<Iteration>>();

                //Now that we have the iterations we have to group them by their code
                //(first position iteration query def)

                for (Iteration iteration : iterations) {

                    if (iteration.getQueryDef() != null) {
                        
                        String sampleId = iteration.getQueryDef().split("\\|")[0];
                        ArrayList<Iteration> sampleIterations = sampleIterationsMap.get(sampleId);
                        if (sampleIterations == null) {
                            sampleIterations = new ArrayList<Iteration>();
                            sampleIterationsMap.put(sampleId, sampleIterations);
                        }
                        sampleIterations.add(iteration);
                        
                    }


                }
                System.out.println(sampleIterationsMap.size() + " different samples detected...");

                System.out.println("Emptying general iterations array...");
                iterations.clear();
                System.out.println("Done!");

                Set<String> samplesKeySet = sampleIterationsMap.keySet();

                for (String currentSampleKey : samplesKeySet) {
                    System.out.println("Looping over blast iterations from sample " + currentSampleKey);

                    Set<String> uniprotIds = new HashSet<String>();

                    //---------------------FILES VARS----------------------------
                    File annotationTxtFile = new File(blastFile.getName().split("\\.")[0] + "_" + currentSampleKey + ANNOTATION_FILE_SUFIX);
                    File dataTxtFile = new File(blastFile.getName().split("\\.")[0] + "_" + currentSampleKey + DATA_FILE_SUFIX);

                    BufferedWriter annotationWriter = new BufferedWriter(new FileWriter(annotationTxtFile));
                    annotationWriter.write(ANNOTATION_FILE_HEADER);

                    BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataTxtFile));
                    dataWriter.write(DATA_FILE_HEADER);
                    //--------------------------------------------------------------

                    for (Iteration iteration : sampleIterationsMap.get(currentSampleKey)) {
                        System.out.println(iteration.getQueryDef());
                        
                        String isotigId = iteration.getQueryDef().split("\\|")[1];
                        ArrayList<Hit> hits = iteration.getIterationHits();
                        if (hits.size() > 0) {
                            Hit hit = hits.get(0);
                            ArrayList<Hsp> hsps = hit.getHitHsps();
                            double minEvalue = 1000000;
                            int startPosition = 1000000000;
                            int endPosition = -100000000;
                            boolean strandInconsistency = false;
                            boolean strand = hsps.get(0).getQueryFrame() > 0;
                            for (Hsp hsp : hsps) {
                                if (hsp.getEvalueDoubleFormat() < minEvalue) {
                                    minEvalue = hsp.getEvalueDoubleFormat();
                                }
                                if (hsp.getQueryFrom() < startPosition) {
                                    startPosition = hsp.getQueryFrom();
                                }
                                if (hsp.getQueryTo() > endPosition) {
                                    endPosition = hsp.getQueryTo();
                                }
                                if (strand != (hsp.getQueryFrame() > 0)) {
                                    strandInconsistency = true;
                                }
                            }
                            //System.out.println("hit.getHitDef() = " + hit.getHitDef());
                            String uniprotId = hit.getHitDef().split("\\|")[1].trim();
                            //System.out.println("uniprotId = " + uniprotId);
                            String uniprotUrl = UNIREF_90_BASE_URL + uniprotId;
                            String strandSt = "";
                            if (strand) {
                                strandSt = PredictedGene.POSITIVE_STRAND;
                            } else {
                                strandSt = PredictedGene.NEGATIVE_STRAND;
                            }

                            annotationWriter.write(isotigId + SEPARATOR
                                    + startPosition + SEPARATOR
                                    + endPosition + SEPARATOR
                                    + strandSt + SEPARATOR
                                    + uniprotId + SEPARATOR
                                    + uniprotUrl + SEPARATOR
                                    + minEvalue + SEPARATOR
                                    + !strandInconsistency + "\n");

                            uniprotIds.add(uniprotId);

                        }

                    }

                    annotationWriter.close();
                    System.out.println("Annotation file successfully created! :)");
                    System.out.println("Starting with protein data retreiving...");

                    for (String string : uniprotIds) {

                        PredictedGene tempGene = cachedUniprotData.get(string);
                        if (tempGene == null) {
                            tempGene = new PredictedGene();
                            tempGene.setAnnotationUniprotId(string);
                            tempGene.setId(string);
                            tempGene = UniprotProteinRetreiver.getUniprotDataFor(tempGene, true);
                            cachedUniprotData.put(string, tempGene);

                            System.out.println(string + " data retrieved! ");
                        } else {
                            System.out.println("Protein data was cached :D");
                        }

                        dataWriter.write(tempGene.getAnnotationUniprotId() + SEPARATOR
                                + tempGene.getProteinNames() + SEPARATOR
                                + tempGene.getGeneNames() + SEPARATOR
                                + tempGene.getOrganism() + SEPARATOR
                                + tempGene.getGeneOntologyId() + SEPARATOR
                                + tempGene.getGeneOntology() + SEPARATOR
                                + tempGene.getKeywords() + SEPARATOR
                                + tempGene.getInterpro() + SEPARATOR
                                + tempGene.getEcNumbers() + SEPARATOR
                                + tempGene.getDomains() + SEPARATOR
                                + tempGene.getProteinFamily() + SEPARATOR
                                + "" + SEPARATOR + //eso es el mapped pubmed id que no se de donde sacarlo
                                tempGene.getPubmedId() + SEPARATOR
                                + tempGene.getLength() + SEPARATOR
                                + tempGene.getSequence() + "\n");


                    }

                    dataWriter.close();
                    System.out.println("Data file successfully created! :)");

                }

                System.out.println("Done with all samples!! :)");

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
