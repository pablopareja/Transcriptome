/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.tr;

import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class MergeBlastFiles {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args){
        if (args.length != 3) {
            System.out.println("This program expects three parameters: \n"
                    + "1. First Blast XML file \n"
                    + "2. Second Blast XML file \n"
                    + "3. Output resulting Blast XML file after merging both input files\n");
        } else {


            File inFile1 = new File(args[0]);
            File inFile2 = new File(args[1]);
            File outFile = new File(args[2]);

            try{

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

                System.out.println("Reading blast file 1...");
                BufferedReader inBuff1 = new BufferedReader(new FileReader(inFile1));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while ((line = inBuff1.readLine()) != null) {
                    stBuilder.append(line);
                }
                inBuff1.close();
                System.out.println("Building blastoutput xml 1....");
                BlastOutput blastOutput1 = new BlastOutput(stBuilder.toString());                

                System.out.println("Reading blast file 2...");
                BufferedReader inBuff2 = new BufferedReader(new FileReader(inFile2));
                line = null;
                stBuilder = new StringBuilder();
                while ((line = inBuff2.readLine()) != null) {
                    stBuilder.append(line);
                }
                inBuff1.close();
                System.out.println("Building blastoutput xml 2....");
                BlastOutput blastOutput2 = new BlastOutput(stBuilder.toString());

                ArrayList<Iteration> allIterations = blastOutput1.getBlastOutputIterations();
                allIterations.addAll(blastOutput2.getBlastOutputIterations());

                System.out.println("Detaching iterations...");

                for (Iteration iteration : allIterations) {
                    iteration.detach();
                }

                outBuff.write("<BlastOutput>\n");
                
                for (XMLElement tempElem  : blastOutput1.getChildren()) {
                    if(!tempElem.getName().equals(BlastOutput.BLAST_OUTPUT_ITERATIONS_TAG_NAME)){
                        outBuff.write(tempElem.toString() + "\n");
                    }
                }
                
                outBuff.write("<BlastOutput_iterations>\n");

                for (Iteration iteration : allIterations) {
                    outBuff.write(iteration.toString() + "\n");
                }
                
                outBuff.write("</BlastOutput_iterations>\n");
                outBuff.write("</BlastOutput>\n");
                outBuff.close();


                System.out.println("Done!");

            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }

}
