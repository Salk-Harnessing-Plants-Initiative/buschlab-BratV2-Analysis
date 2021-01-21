package edu.salk.brat.run;

import edu.salk.brat.gui.fx.log.LogQueue;
import edu.salk.brat.parameters.ParamLocation;
import edu.salk.brat.parameters.Parameters;
import ij.plugin.PlugIn;
import javafx.application.Platform;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import static java.awt.GraphicsEnvironment.isHeadless;
import static javafx.application.Application.launch;


public class Brat_Analysis implements PlugIn {
    final private static Logger log=Logger.getLogger("edu.salk.brat");

    public static void main(String[] args) {
        try {
            Parameters.read(ParamLocation.basic);
        }
        catch (IllegalStateException e){
            log.warning(e.getMessage());
        }
        try {
            Parameters.read(ParamLocation.advanced);
        }
        catch (IllegalStateException e) {
            log.warning(e.getMessage());
        }
        if (!Parameters.checkDefaultLayout()) {
            try {
                Parameters.createDefaultLayout();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }

        BratDispatcher dispatcher = new BratDispatcher();
        Package pack = Package.getPackage("edu.salk.brat.run");
        String version = pack.getImplementationVersion();
        if (version == null) {
        }

        String hdls = System.getenv("BRAT_RUNHEADLESS");
        if(hdls == null){
            hdls = "false";
        }
        if (isHeadless() || Objects.equals(hdls.toLowerCase(), "true")) {
            dispatcher.initLogger(null, version);
            dispatcher.runHeadless();
        } else {
//            LogQueue logQueue = new LogQueue(1_000_000);
//            dispatcher.initLogger(logQueue, version);
            try {
                launch(edu.salk.brat.gui.fx.BratFxApp.class);
            }
            catch (IllegalStateException e) {
                log.warning(String.format("%s %s", e.getCause(), e.getMessage()));
            }
        }
    }

    @Override
    public void run(String s) {
        main(null);
    }
}
//public class Brat_V2 implements PlugIn{
//	public static void main(String[] args){
//		BratDispatcher dispatcher=new BratDispatcher();
//		dispatcher.dispatch();
//	}
//
//    @Override
//    public void run(String arg) {
//        main(null);
//    }
//
//}
