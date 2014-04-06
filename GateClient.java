
package cz.ctu.fit.ddw;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Milan Dojchinovski, Radek Zunt
 */
 
public class GateClient {
    
    // corpus pipeline
    private static SerialAnalyserController annotationPipeline = null;
    
    // whether the GATE is initialised
    private static boolean isGateInitilised = false;
    private static Exception Exception;
    
    public void run() throws IOException{
        
        if(!isGateInitilised){
            
            // initialise GATE
            initialiseGate();            
        }        

        try {                
            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            // create an instance of a English Tokeniser processing resource
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");

            // create an instance of a Sentence Splitter processing resource
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
            
            ProcessingResource AnnieGazetteerPR = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
            
            // locate the JAPE grammar file
            File japeOrigFile = new File("D:\\gate2\\jape.jape");
            java.net.URI japeURI = japeOrigFile.toURI();
            
            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                // set the grammar location
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                // set the grammar encoding
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }
            
            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(tokenizerPR);
            annotationPipeline.add(sentenceSplitterPR);
            
            annotationPipeline.add(AnnieGazetteerPR);
            
            annotationPipeline.add(japeTransducerPR);
            
            System.out.println("Insert a web page: ");
            
            String website = null;
            try {
                website = insertWeb();
            } catch (Exception ex) {
                Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
            String webContent = null;
            try {
                webContent = savePage(website);
            } catch (IOException ex) {
                Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            String vstup = webContent.replace(" ", "-");
            
            // create a document
            Document document = Factory.newDocument(vstup);
            
            // create a corpus and add the document
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);

            // set the corpus to the pipeline
            annotationPipeline.setCorpus(corpus);

            //run the pipeline
            annotationPipeline.execute();

            // loop through the documents in the corpus
            for(int i=0; i< corpus.size(); i++){

                Document doc = corpus.get(i);

                // get the default annotation set
                AnnotationSet as_default = doc.getAnnotations();

                FeatureMap futureMap = null;
                // get all Token annotations
                AnnotationSet annSetTokens = as_default.get("Number",futureMap);
                //System.out.println("Number of phone number annotations: " + annSetTokens.size());

                ArrayList tokenAnnotations = new ArrayList(annSetTokens);
                
                System.out.println("Found Czech phone numbers at " + website + ":");
                
                // looop through the Token annotations
                for(int j = 0; j < tokenAnnotations.size(); ++j) {

                    // get a token annotation
                    Annotation token = (Annotation)tokenAnnotations.get(j);

                    // get the underlying string for the Token
                    Node isaStart = token.getStartNode();
                    Node isaEnd = token.getEndNode();
                    String underlyingString = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    
                    String result = underlyingString.replace("-", "").replace("+", "").replace("420", "");
                    
                    if (result.length() != 0) System.out.println(result);
                    
                    // get the features of the token
                    FeatureMap annFM = token.getFeatures();
                    
                    // get the value of the "string" feature
                    //String value = (String)annFM.get((Object)"string");
                    //System.out.println("Token: " + value);
                }
            }
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void initialiseGate() {
        
        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files\\GATE_Developer_7.1");
            Gate.setGateHome(gateHomeFile);
            
            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins            
            File pluginsHome = new File("C:\\Program Files\\GATE_Developer_7.1\\plugins");
            Gate.setPluginsHome(pluginsHome);            
            
            // set user config file (optional)
            // Eg. /Applications/GATE_Developer_7.0/user.xml
            Gate.setUserConfigFile(new File("C:\\Program Files\\GATE_Developer_7.1", "user.xml"));            
            
            // initialise the GATE library
            Gate.init();
            
            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);
            
            // flag that GATE was successfuly initialised
            isGateInitilised = true;
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    
 public static String savePage(final String URL) throws IOException {
    String line = "", all = "";
    URL myUrl = null;
    BufferedReader in = null;
    try {
        myUrl = new URL(URL);
        in = new BufferedReader(new InputStreamReader(myUrl.openStream()));

        while ((line = in.readLine()) != null) {
            all += line;
        }
    } finally {
        if (in != null) {
            in.close();
        }
    }

    return all;
}

 private static String insertWeb() throws IOException, Exception {

        byte [] pole=new byte [100];
        String retezec;

        try {
            System.in.read(pole);
        } catch (IOException vyjimka) {
            //System.err.println(vyjimka);
            System.out.println("\nNo web page read...");
            throw vyjimka;
        }

       retezec = new String(pole);
        
        try {
            isUrl(retezec);
        } catch (Exception vyjimka2) {
            //System.err.println(vyjimka);
            System.out.println("\nWrong web page format...");
            throw vyjimka2;
        }
        return retezec.trim();

}

public static boolean isUrl(String str) throws java.lang.Exception{
    Pattern urlPattern = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\\\\\))+[\\\\w\\\\d:#@%/;$()~_?\\\\+-=\\\\\\\\\\\\.&]*)",Pattern.CASE_INSENSITIVE);
    Matcher matcher = urlPattern.matcher(str);
    if (matcher.find())
         return true;
    else
        throw Exception;
    } 
 
  
}


