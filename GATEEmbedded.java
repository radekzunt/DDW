
package cz.ctu.fit.ddw;

import java.io.IOException;

/**
 * @author Milan Dojchinovski, Radek Zunt
 */
 
public class GATEEmbedded {

    public static void main(String[] args) throws IOException {
        
        GateClient client = new GateClient();        
        client.run();    
    }
}
