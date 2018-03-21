package com.cnet2ad.web;

import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.cnet2ad.semantic.OntologyManager;
import org.processmining.plugins.cnet2ad.semantic.SemanticCnet2AD;
import org.processmining.plugins.cnmining.CNMining;
import org.processmining.plugins.cnmining.Settings;
import org.processmining.plugins.cnet2ad.*;
import org.processmining.models.cnet2ad.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/*
    Parametri:
        -json per abilitare lesportazione in json
        -o per definire il nome dei file di output
            esempio -o foo produrrà: foo.txt, foo.xmi, foo.json
        -dir per specificare la directory di output
            esempio -dir ../
        filename del log da processare

        -sigma Per impostare il sigma log noise
        -ff per impostare il fall factor
        -rtb per impostare il relative to best

        -constraints per impostare il percorso del contenente i vincoli

        -ontology Per impostare il file di output dell'ontologia
        -resources Per annotare il layer con le risorse secondo un livello da 1 a 4
 */

public class Main {
    public static void main(String[] args) {

        // Gestione dei parametri
        if(args.length == 0) {
            System.out.println("[ERROR] specify a log file");
            return;
        }

        ArgManager argManager = new ArgManager(args);
        String logFilename = argManager.log();
        if(logFilename.isEmpty()){
            System.out.println("[ERROR] Need a log");
            return;
        }
        System.out.println("[LOG] " + logFilename);

        boolean exportJson = argManager.flag("-json");
        String outputFilename = argManager.param("-o");
        if(outputFilename.isEmpty())
            outputFilename = "adgraph";

        String outputDir = argManager.param("-dir");
        if(outputDir.isEmpty() == false){
            if(outputDir.endsWith("/") == false)
                outputDir += "/";
        }

        Settings settings = new Settings();
        settings.sigmaLogNoise = 0.5;
        settings.fallFactor = 0.9;
        settings.relativeToBest = 0.75;

        if(argManager.param("-sigma").isEmpty() == false){
            double sigma = Double.parseDouble(argManager.param("-sigma"));
            sigma = Math.min(sigma, 1);
            sigma = Math.max(0, sigma);
            settings.sigmaLogNoise = sigma;
        }

        if(argManager.param("-ff").isEmpty() == false){
            double ff = Double.parseDouble(argManager.param("-ff"));
            ff = Math.min(ff, 1);
            ff = Math.max(0, ff);
            settings.fallFactor = ff;
        }

        if(argManager.param("-rtb").isEmpty() == false){
            double rtb = Double.parseDouble(argManager.param("-rtb"));
            rtb = Math.min(rtb, 1);
            rtb = Math.max(0, rtb);
            settings.relativeToBest = rtb;
        }

        if(argManager.param("-constraints").isEmpty() == false)
        {
            settings.constraintsEnabled = true;
            settings.constraintsFilename = argManager.param("-constraints");
        }

        String ontologyOutputFilename = "";
        if(argManager.param("-ontology").isEmpty() == false)
        {
            ontologyOutputFilename = argManager.param("-ontology");
        }

        XLog log = parseLog(logFilename);
        if( log == null ){
            System.out.println("Unable to parse the log");
            System.out.println("Cnet2ADRESULT=ERROR");
        }

        ADgraph graph = new ADgraph();
        try{
            Object[] data = CNMining.startCNMining(null, log, settings, false);
            Flex causalnet = (Flex)data[0];

            BPMNDiagram bpmn = Flex2BPMN.convert(causalnet);
            
            Cnet2AD mining = new Cnet2AD(bpmn);
            graph = mining.process();
            
            System.out.println("OutputDit = " + outputDir);
            if(exportJson)
                saveFile(outputDir + outputFilename + ".json", graph.toJson());
            saveFile(outputDir + outputFilename + ".uml", graph.toXMI());
            saveFile(outputDir + outputFilename + ".txt", graph.toString());

            System.out.println("Cnet2ADRESULT=SUCCESS");
        }
        catch(Exception e){
            System.out.println("Exception " + e.toString());
            System.out.println("Cnet2ADRESULT=ERROR");
        }
        
        try {
            if(ontologyOutputFilename.isEmpty() == false){

                System.out.println("Launching SemanticCnet2AD...");

                SemanticCnet2AD semanticAlgorithm = new SemanticCnet2AD(log);
                String ontology="";
                if (Integer.parseInt(argManager.param("-resources"))>2) {
                    String businessOntology= argManager.param("-o");
                    System.out.println("ECCO IL FILE DELL'Ontologia di contesto: " + "public/uploads/ontology/" + businessOntology + ".business.owl");
                    ontology = semanticAlgorithm.annotate("SemanticCnet2AD.ontology.base.owl", ontologyOutputFilename, "public/uploads/ontology/" + businessOntology + ".business.owl");
                }else
                    ontology = semanticAlgorithm.annotate("SemanticCnet2AD.ontology.base.owl", ontologyOutputFilename,"");
                if(ontology.equals("ERROR")){
                    System.out.println("SemanticCnet2ADRESULT=ERROR");
                }
                else {
                    System.out.println("SemanticCnet2ADRESULT=SUCCESS");
                }

                if(argManager.flag("-resources"))
                {
                    if( argManager.param("-resources").equals("2"))
                        semanticAlgorithm.annotateResources(graph,"2");
                    else if( argManager.param("-resources").equals("3"))
                        semanticAlgorithm.annotateResources(graph,"3");
                    else if( argManager.param("-resources").equals("4"))
                        //TODO: implement in SemanticCNet2AD.java
                        semanticAlgorithm.annotateResources(graph,"4");

                    System.out.println("OutputDit = " + outputDir);
                    if(exportJson)
                        saveFile(outputDir + outputFilename + ".json", graph.toJson());
                    saveFile(outputDir + outputFilename + ".uml", graph.toXMI());
                    saveFile(outputDir + outputFilename + ".txt", graph.toString());
                }

            }
        }
        catch(Exception e){
            System.out.println("Exception " + e.toString());
            System.out.println("SemanticCnet2ADRESULT=ERROR");
        }
    }

    static XLog parseLog(String filename){
        try {
            XMxmlParser mxmlParser = new XMxmlParser();
            XesXmlParser xesParser= new XesXmlParser();
            List<XLog> logs=null;
            File file = new File(filename);
            if(mxmlParser.canParse(file))
                logs = mxmlParser.parse(file);
            else if(xesParser.canParse(file))
                logs= xesParser.parse(file);
            else
                System.out.println("Error, cannot parse input file.");
            System.out.println(logs.size());

            return logs.iterator().next();
        }
        catch(Exception e){
            System.out.println("exception" + e.toString());
            return null;
        }
    }

    public static void saveFile(String filename, String content) throws Exception {
        System.out.println("Exporting File: " + filename + "...");
        File ec = new File(filename);
        if (ec.exists()) {
            ec.delete();
        }
        ec.createNewFile();
        try
        {
            Files.write(FileSystems.getDefault().getPath(
                    ".", new String[] { filename }),
                    content.getBytes(), new OpenOption[] {
                            StandardOpenOption.APPEND
                    }
            );
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}