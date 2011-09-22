/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.tr;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.Pair;
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
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class CharacterizeTranscriptome implements Executable{


    public static final String ABSOLUTE_VALUE_SUFIX = "_absolute.txt";
    public static final String NORMALIZED_VALUE_SUFIX = "_normalized.txt";

    public static final String SEPARATOR = "\t";

    public static final String SAMPLE_HEADER = "Sample ";
    public static final String PROTEIN_ID_HEADER = "ProteinID";


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
        if (args.length != 11) {
            System.out.println("This program expects eleven parameters: \n"
                    + "1. Input TXT file including transcriptome 1 annotation\n"
                    + "2. Input TXT file including Uniprot data from detected proteins in transcriptome 1\n"
                    + "3. Input FNA file with contig headers from isotigs of transcriptome 1 \n"
                    + "4. Input TXT file including associations isotig-contig from transcriptome 1\n"
                    + "5. Input TXT file including transcriptome 2 annotation\n"
                    + "6. Input TXT file including Uniprot data from detected proteins in transcriptome 2\n"
                    + "7. Input FNA file with contig headers from isotigs of transcriptome 2  \n"
                    + "8. Input TXT file including associations isotig-contig from transcriptome 2 \n"
                    + "9. Prefix for TXT output filePrefijo del archivo txt de salida \n"
                    + "10. Sample 1 output file header prefix\n"
                    + "11. Sample 2 output file header prefix\n");
        } else {


            String sample1HeaderPrefix = args[9];
            String sample2HeaderPrefix = args[10];

            File annotationTr1File = new File(args[0]);
            File uniprotTr1File = new File(args[1]);
            File fnaTr1File = new File(args[2]);
            File isotigTr1File = new File(args[3]);

            File annotationTr2File = new File(args[4]);
            File uniprotTr2File = new File(args[5]);
            File fnaTr2File = new File(args[6]);
            File isotigTr2File = new File(args[7]);

            File outNormalizedFile = new File(args[8]+NORMALIZED_VALUE_SUFIX);
            File outAbsoluteFile = new File(args[8]+ABSOLUTE_VALUE_SUFIX);

            System.out.println("Calculating values for transcriptome 1 ....");

            HashMap<String, Pair<Double,Double>> resultsTr1 = calculaNivelesExpresion(annotationTr1File,
                                                uniprotTr1File, fnaTr1File, isotigTr1File);

            System.out.println("Done! :D");

            System.out.println("Calculating values for transcriptome 2 ....");

            HashMap<String, Pair<Double,Double>> resultsTr2 = calculaNivelesExpresion(annotationTr2File,
                                                uniprotTr2File, fnaTr2File, isotigTr2File);

            System.out.println("Done! :D");

            //----OUT NORMALIZED FILE-------------
            try{
                BufferedWriter outBuffNormalized = new BufferedWriter(new FileWriter(outNormalizedFile));
                BufferedWriter outBuffAbsolute = new BufferedWriter(new FileWriter(outAbsoluteFile));
                outBuffNormalized.write(PROTEIN_ID_HEADER + SEPARATOR +
                                        SAMPLE_HEADER + sample1HeaderPrefix + SEPARATOR +
                                        SAMPLE_HEADER + sample2HeaderPrefix + SEPARATOR + "\n");
                outBuffAbsolute.write(PROTEIN_ID_HEADER + SEPARATOR +
                                        SAMPLE_HEADER + sample1HeaderPrefix + SEPARATOR +
                                        SAMPLE_HEADER + sample2HeaderPrefix + SEPARATOR + "\n");
                Set<String> tr1KeySet = resultsTr1.keySet();
                Set<String> tr2KeySet = resultsTr2.keySet();

                Set<String> unionKeySet = new HashSet<String>();
                unionKeySet.addAll(tr2KeySet);
                unionKeySet.addAll(tr1KeySet);
                for (String unionKey : unionKeySet) {
                    Pair<Double,Double> tr1Pair = null;
                    Pair<Double,Double> tr2Pair = null;
                    tr1Pair = resultsTr1.get(unionKey);
                    tr2Pair = resultsTr2.get(unionKey);

                    String tr1Abs,tr1Norm,tr2Abs,tr2Norm;
                    if(tr1Pair == null){
                        tr1Abs = "0.0";
                        tr1Norm = "0.0";
                    }else{
                        tr1Abs = String.valueOf(tr1Pair.getValue1());
                        tr1Norm = String.valueOf(tr1Pair.getValue2());
                    }
                    if(tr2Pair == null){
                        tr2Abs = "0.0";
                        tr2Norm = "0.0";
                    }else{
                        tr2Abs = String.valueOf(tr2Pair.getValue1());
                        tr2Norm = String.valueOf(tr2Pair.getValue2());
                    }
                    outBuffAbsolute.write(unionKey + SEPARATOR + tr1Abs + SEPARATOR + tr2Abs + "\n");
                    outBuffNormalized.write(unionKey + SEPARATOR + tr1Norm + SEPARATOR + tr2Norm + "\n");
                }

                outBuffNormalized.close();
                outBuffAbsolute.close();


                System.out.println("Files created successfully! :)");
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }


    private static HashMap<String, Pair<Double,Double>> calculaNivelesExpresion(File annotationFile,
                        File uniprotFile,
                        File fnaFile,
                        File isotigFile){



        //The absolute value is the first and the normalized value is the second one.
        HashMap<String,Pair<Double,Double>> results = new HashMap<String, Pair<Double,Double>>();
        
        try{

            ArrayList<String> uniprotIds = new ArrayList<String>();
            HashMap<String,Integer> uniprotLengths = new HashMap<String, Integer>();

            //First of all I have to get the uniprot ids without any repetition
            // (I use the uniprot file for data )
            //----------------------UNIPROT FILE------------------------------
            BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
            String line = null;
            //Header line
            reader.readLine();
            while((line = reader.readLine()) != null){
                String[] columns = line.split("\\t");
                uniprotIds.add(columns[0]);
                if(columns.length < 14){
                    System.out.println("line = " + line);
                }
                
                Integer proteinLength = Integer.parseInt(columns[13]);
                uniprotLengths.put(columns[0], (proteinLength * 3));
            }
            reader.close();

            //Now I need the relationship between uniprot ids and isotigs
            HashMap<String,HashSet<String>> uniprotIsotigsMap = new HashMap<String, HashSet<String>>();

            //---------------------ANNOTATION FILE----------------------------
            reader = new BufferedReader(new FileReader(annotationFile));
            //Header line
            reader.readLine();
            while((line = reader.readLine()) != null){
                String[] columns = line.split("\\t");
                String isotigId = columns[0];
                String UniProTID = columns[4];                
                HashSet<String> isotigs = uniprotIsotigsMap.get(UniProTID);
                if(isotigs == null){
                    isotigs = new HashSet<String>();
                    uniprotIsotigsMap.put(UniProTID , isotigs);
                }
                isotigs.add(isotigId);
            }
            reader.close();
            //-------------------------------------------------------------


            HashMap<String,HashSet<String>> isotigsContigsMap = new HashMap<String, HashSet<String>>();

            System.out.println("ISOTIGS FILE");

            //---------------------ISOTIGS FILE---------------------------
            reader = new BufferedReader(new FileReader(isotigFile));
            while((line = reader.readLine()) != null){
                String[] columns = line.split("\\t");
                String isotigId = columns[0];
                String contigId = columns[5];

                System.out.println("isotigId = " + isotigId + "\tcontigId = " + contigId);

                HashSet<String> contigsSet = isotigsContigsMap.get(isotigId);
                if(contigsSet == null){
                    contigsSet = new HashSet<String>();
                    isotigsContigsMap.put(isotigId, contigsSet);
                }
                contigsSet.add(contigId);
            }
            reader.close();
            //---------------------------------------------------------------

            HashMap<String,Double> contigReads = new HashMap<String, Double>();

            //------------------FNA FILE-------------------------------
            reader = new BufferedReader(new FileReader(fnaFile));
            while((line = reader.readLine()) != null){
                if(line.startsWith(">")){
                    String[] columns = line.split(" +");
                    String contigId = columns[0].substring(1);
                    String numReadsStr = columns[2];
                    Double numReads = Double.parseDouble(numReadsStr.split("=")[1]);
                    contigReads.put(contigId, numReads);
                    //System.out.println("columns.length = " + columns.length);
                    //System.out.println("numReads = " + numReads);
                }
            }
            reader.close();
            //-----------------------------------------------------------

            for (String currentUniprotId : uniprotIds) {

                Double resAbsolute = 0.0;
                Double resNormalized = 0.0;

                HashSet<String> contigsAlreadyCounted = new HashSet<String>();
                
                HashSet<String> isotigsSet = uniprotIsotigsMap.get(currentUniprotId);
                for (String currentIsotigId : isotigsSet) {
                    System.out.println("currentIsotigId = " + currentIsotigId);
                    HashSet<String> contigsSet = isotigsContigsMap.get(currentIsotigId);
                    System.out.println("contigsSet = " + contigsSet);
                    for (String currentContigId : contigsSet) {
                        if(!contigsAlreadyCounted.contains(currentContigId)){
                            System.out.println("currentContigId = " + currentContigId);
                            resAbsolute += contigReads.get(currentContigId);
                        }                        
                        contigsAlreadyCounted.add(currentContigId);
                    }
                }
                resNormalized = resAbsolute / uniprotLengths.get(currentUniprotId);
                Pair<Double,Double> pair = new Pair<Double, Double>(resAbsolute, resNormalized);
                results.put(currentUniprotId, pair);

            }

        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        return results;

    }

}
