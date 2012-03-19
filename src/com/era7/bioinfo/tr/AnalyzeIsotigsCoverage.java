/*
 * Copyright (C) 2010-2012  "Oh no sequences!"
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.era7.bioinfo.tr;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.blast.BlastExporter;
import com.era7.lib.bioinfoxml.BlastOutput;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
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

                outBuff.write(BlastExporter.exportBlastXMLtoIsotigsCoverage(blastOutput));

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
