package edu.salk.brat.gui.fx;

import edu.salk.brat.gui.fx.log.BratFxLogHandler;
import edu.salk.brat.gui.fx.log.LogQueue;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.prefs.Preferences;

public class BratFxApp extends Application {
	private final static Logger log=Logger.getLogger("edu.salk.brat");
	private final static ClassLoader classloader = BratFxApp.class.getClassLoader();

    private DoubleProperty fontSize = new SimpleDoubleProperty(10);
	private IntegerProperty reds = new SimpleIntegerProperty(200);
	private IntegerProperty greens = new SimpleIntegerProperty(200);
	private IntegerProperty blues = new SimpleIntegerProperty(255);
	private Scene scene;

    public static void main(String[] args){
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
//        Logger log=Logger.getLogger("edu.salk.brat");

//        LogQueue logQueue = null;
//        for(Handler h:log.getHandlers()){
//            if(h instanceof BratFxLogHandler){
//                logQueue =((BratFxLogHandler) h).getLogQueue();
//            }
//        }

//		Logger log = Logger.getLogger(Brat_V2.class.getName());
//		BratFxLogHandler logHandler = new BratFxLogHandler(logQueue);
//		logHandler.setLevel(Level.ALL);
//		log.addHandler(logHandler);
//        log.info("Hello1");
//        log.warning("Don't pick up alien hitchhickers1");
//
//         for (int x = 0; x < 2; x++) {
//             Thread generatorThread = new Thread(
//                     () -> {
//                         for (; ; ) {
//                             log.fine("fine");
//                             log.finer("finer");
//                             log.finest("finest");
//                             log.severe("severe");
//                             log.warning("warning");
//                             log.info("info");
//                             log.config("config");
//                             try {
//                                 Thread.sleep(500);
//                             } catch (InterruptedException e) {
//                                 e.printStackTrace();
//                             }
//                         }
//                     }
//             );
//             generatorThread.setDaemon(true);
//             generatorThread.start();
//         }
		LogQueue logQueue = new LogQueue(1000000);
		BratFxController bratFxController = new BratFxController(logQueue);

		FXMLLoader loader = new FXMLLoader(this.getClass().getClassLoader().getResource("bratanalysisgui.fxml"));
		loader.setController(bratFxController);

		URL logViewCss=this.getClass().getClassLoader().getResource("log-view.css");

		VBox root = loader.load();
		scene = new Scene(root, 1000, 800);

		if(logViewCss!=null){
			scene.getStylesheets().add(logViewCss.toExternalForm());
		}

		scene.widthProperty().addListener(new FontSizeHandler());
		scene.heightProperty().addListener(new FontSizeHandler());

//		fontSize.bind(scene.widthProperty().multiply(scene.widthProperty()).add(scene.heightProperty().multiply(scene.heightProperty())).divide(100000));
		root.styleProperty().bind(Bindings.concat("-fx-font-size: ", fontSize.asString(), ";"
				,"-fx-base: rgb(",reds.asString(),",",greens.asString(),",",blues.asString(),");"));

		stage.setTitle("Brat-Analysis");
		stage.setScene(scene);
		stage.setOnHidden(e -> {
			bratFxController.shutdown();
			Platform.exit();
		});
		stage.show();

	}

	private class FontSizeHandler implements ChangeListener {
		@Override
		public void changed(ObservableValue observableValue, Object o, Object t1) {
			if(scene == null){
				return;
			}
			double w=scene.getWidth();
			double h=scene.getHeight();
			double newSize=Math.min(w,h)/60;

			if(newSize<20) {
				fontSize.setValue(newSize);
			}
		}
	}

}
