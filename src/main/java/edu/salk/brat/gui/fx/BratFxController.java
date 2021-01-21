package edu.salk.brat.gui.fx;

import edu.salk.brat.gui.fx.log.*;
import edu.salk.brat.parameters.ParamLocation;
import edu.salk.brat.parameters.Parameters;
import edu.salk.brat.run.BratDispatcher;
import edu.salk.brat.utility.ExceptionLog;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Optional;
import java.util.logging.*;
import java.util.prefs.BackingStoreException;


public class BratFxController {
    private final static Logger log = Logger.getLogger("edu.salk.brat");
    private final LogQueue logQueue;
    private BratDispatcher dispatcher;

    //paneBasic
    @FXML
    private TextField txtfldSegDir;
    @FXML
    private TextField txtfldOrigDir;
    @FXML
    private Button btnSegDirBrowse;
    @FXML
    private Button btnOrigDirBrowse;
    @FXML
    private Button btnStart;
    @FXML
    private TextField txtfldExtension;
    @FXML
    private CheckBox chkboxFlipHorizontal;
//    @FXML
//    private CheckBox chkboxTimeSeries;
//    @FXML
//    private CheckBox chkboxDayZero;
//    @FXML
//    private CheckBox chkboxStartPoints;
    @FXML
    private Slider sliderNumThreads;
    @FXML
    private Label lblNumThreads;

    //paneExpert
    @FXML
    private TextField txtfldExpIdentifier;
    @FXML
    private TextField txtfldSetIdentifier;
    @FXML
    private TextField txtfldDayIdentifier;
    @FXML
    private TextField txtfldPlateIdentifier;
    @FXML
    private TextField txtfldImageResolution;
    @FXML
    private TextField txtfldSegLblBackground;
    @FXML
    private TextField txtfldSegLblRoot;
    @FXML
    private TextField txtfldSegLblShoot;
    @FXML
    private TextField txtfldSegLblTransition;
    @FXML
    private TextField txtfldSegLblCenterline;
    @FXML
    private TextField txtfldMinShootPix;
    @FXML
    private TextField txtfldMinRootPix;
    @FXML
    private TextField txtfldMinTransitionPix;
    @FXML
    private TextField txtfldCropRegion;
    @FXML
    private TextField txtfldPlantTreeMaxLevel;
    @FXML
    private ChoiceBox<String> choiceBoxLayout;
    @FXML
    private TextField txtfldCircleDiameter;
    @FXML
    private TextField txtfldLabelSize;
    @FXML
    private TextField txtfldLabelFont;
    @FXML
    private TextField txtfldNumberSize;
    @FXML
    private TextField txtfldNumberFont;
    @FXML
    private ColorPicker clrpckShootRoi;
    @FXML
    private ColorPicker clrpckRootRoi;
    @FXML
    private ColorPicker clrpckSkeleton;
    @FXML
    private ColorPicker clrpckMainRoot;
    @FXML
    private ColorPicker clrpckStartPoint;
    @FXML
    private ColorPicker clrpckEndPoint;
    @FXML
    private ColorPicker clrpckShootCM;
    @FXML
    private ColorPicker clrpckGeneral;
    @FXML
    private ChoiceBox<String> choiceboxLogLevel;
    @FXML
    private Button btnDefaults;

    @FXML
    private LogBox logBox;
    @FXML
    private Accordion accordionMain;
    @FXML
    private TitledPane paneLog;

    @FXML
    private Button btnAddLayout;
    @FXML
    private Button btnDelLayout;

    public BratFxController(LogQueue logQueue) {
        this.logQueue = logQueue;
    }

    @FXML
    private void initialize() {
        initLogger("3.0.9");
        setBasicParameters();
        setAdvancedParameters();
        btnSegDirBrowse.setOnAction(actionEvent -> directorySelect(Parameters.baseDirectory));
        btnOrigDirBrowse.setOnAction(actionEvent -> directorySelect(Parameters.origDirectory));
        btnStart.setOnAction(actionEvent -> startRun());

        choiceBoxLayout.setItems(FXCollections.observableArrayList(Parameters.availableLayouts.getValue().split(";")));
        choiceboxLogLevel.setItems(FXCollections.observableArrayList("OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL"));

        btnAddLayout.setOnAction(actionEvent -> layoutImport());
        btnDelLayout.setOnAction(actionEvent -> layoutDelete());
        btnDefaults.setOnAction(actionEvent -> setDefaults());

        lblNumThreads.textProperty().bind(Bindings.format("Threads: %2.0f", sliderNumThreads.valueProperty()));
        lblNumThreads.setStyle("-fx-font-family: monospace;");
        logBox.setLogQueue(logQueue);

        choiceboxLogLevel.getSelectionModel().selectedItemProperty().addListener((observableValue, o, t1) -> {
            Level newLogLevel = Level.parse(t1);
            Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
            while (loggerNames.hasMoreElements()) {
                String name = loggerNames.nextElement();
                if (name.contains("edu.salk.brat")) {
                    Logger l = Logger.getLogger(name);
                    for (Handler h : l.getHandlers()) {
                        if (h instanceof ConsoleHandler) {
                            h.setLevel(newLogLevel);
                        }
                    }
                }
            }
        });

        accordionMain.expandedPaneProperty().addListener((observableValue, o, t1) -> {
            if (o != null) {
                o.setCollapsible(true);
            }
            if (t1 != null) {
                Platform.runLater(() -> t1.setCollapsible(false));
            }
        });

        logBox.setOnExportAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Log To File");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File logfile = chooser.showSaveDialog(logBox.getScene().getWindow());
            if (logfile != null) {
                SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
                ObservableList<LogRecord> logItems = logBox.getLogItems();
                BufferedWriter bw = null;
                try {
                    bw = new BufferedWriter(new FileWriter(logfile));
                    for (LogRecord logRecord : logItems) {
                        String context = String.format("%s %s.%s@%d", logRecord.getLevel(), logRecord.getSourceClassName(), logRecord.getSourceMethodName(), logRecord.getThreadID());
                        String timestamp = timestampFormatter.format(logRecord.getMillis());
                        String message = logRecord.getMessage();
                        message.replace("\n", ";");
                        bw.write(String.format("%s %s: %s\n", timestamp, context, message));
                    }
                } catch (IOException e) {
                    log.severe(String.format("Error writing to file: %s.\n%s", logfile.getName(), ExceptionLog.StackTraceToString(e)));
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        });
    }

    private void setAdvancedParameters() {
        txtfldExpIdentifier.setText(Parameters.experimentIdentifier.getValue());
        txtfldSetIdentifier.setText(Parameters.setIdentifier.getValue());
        txtfldDayIdentifier.setText(Parameters.timePtIdentifier.getValue());
        txtfldPlateIdentifier.setText(Parameters.plateIdentifier.getValue());
        txtfldImageResolution.setText(Parameters.resolution.getValue());
        txtfldSegLblBackground.setText(Parameters.segLblBackground.getValue());
        txtfldSegLblRoot.setText(Parameters.segLblRoot.getValue());
        txtfldSegLblShoot.setText(Parameters.segLblShoot.getValue());
        txtfldSegLblTransition.setText(Parameters.segLblTransition.getValue());
        txtfldSegLblCenterline.setText(Parameters.segLblCenterline.getValue());
        txtfldMinShootPix.setText(Parameters.minShootPix.getValue());
        txtfldMinRootPix.setText(Parameters.minRootPix.getValue());
        txtfldMinTransitionPix.setText(Parameters.minTransitionPix.getValue());
        txtfldCropRegion.setText(Parameters.cropRegion.getValue());
        txtfldPlantTreeMaxLevel.setText(Parameters.plantTreeMaxLevel.getValue());
        choiceBoxLayout.setValue(Parameters.selectedLayout.getValue());
        txtfldCircleDiameter.setText(Parameters.circleDiameter.getValue());
        txtfldLabelSize.setText(Parameters.labelSize.getValue());
        txtfldLabelFont.setText(Parameters.labelFont.getValue());
        txtfldNumberSize.setText(Parameters.numberSize.getValue());
        txtfldNumberFont.setText(Parameters.numberFont.getValue());
        clrpckShootRoi.setValue(Color.web(Parameters.shootRoiColor.getValue()));
        clrpckRootRoi.setValue(Color.web(Parameters.rootRoiColor.getValue()));
        clrpckSkeleton.setValue(Color.web(Parameters.skeletonColor.getValue()));
        clrpckMainRoot.setValue(Color.web(Parameters.mainRootColor.getValue()));
        clrpckStartPoint.setValue(Color.web(Parameters.startPtColor.getValue()));
        clrpckEndPoint.setValue(Color.web(Parameters.endPtColor.getValue()));
        clrpckShootCM.setValue(Color.web(Parameters.shootCMColor.getValue()));
        clrpckGeneral.setValue(Color.web(Parameters.generalColor.getValue()));
        choiceboxLogLevel.setValue(Parameters.logLevel.getValue());
    }

    private void setBasicParameters() {
        txtfldSegDir.setText(Parameters.baseDirectory.getValue());
        txtfldOrigDir.setText(Parameters.origDirectory.getValue());
        txtfldExtension.setText(Parameters.fileExtension.getValue());
        chkboxFlipHorizontal.setSelected(Boolean.parseBoolean(Parameters.flipHorizontal.getValue()));
//        chkboxTimeSeries.setSelected(Boolean.parseBoolean(Parameters.useSets.getValue()));
//        chkboxDayZero.setSelected(Boolean.parseBoolean(Parameters.haveDayZeroImage.getValue()));
//        chkboxStartPoints.setSelected(Boolean.parseBoolean(Parameters.haveStartPoints.getValue()));
        sliderNumThreads.setValue(Integer.parseInt(Parameters.numThreads.getValue()));
    }

    @FXML
    protected void directorySelect(Parameters type) {
        String currentSelection = null;
        if (type == Parameters.baseDirectory) {
            currentSelection = txtfldSegDir.getText();
        }
        else if (type == Parameters.origDirectory) {
            currentSelection = txtfldOrigDir.getText();
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Base Directory");
        if (new File(currentSelection).isDirectory()) {
            chooser.setInitialDirectory(new File(currentSelection));
        } else {
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        File dir = chooser.showDialog(txtfldSegDir.getScene().getWindow());
        if (dir != null) {
            if (type == Parameters.baseDirectory) {
                txtfldSegDir.setText(dir.getAbsolutePath());
                txtfldOrigDir.setText(dir.getParentFile().getAbsolutePath());
            }
            else if(type == Parameters.origDirectory){
                txtfldOrigDir.setText(dir.getAbsolutePath());
            }
        }
    }

    @FXML
    private void layoutImport() {
        LayoutDialog ld = new LayoutDialog();
        ld.addDialog();
//        ld.importLayout();
    }

    @FXML
    private void layoutDelete() {
        LayoutDialog ld = new LayoutDialog();
        String selectedLayout = choiceBoxLayout.getSelectionModel().getSelectedItem();
        if (selectedLayout.equals(Parameters.selectedLayout.getDefaultValue())){
            ld.errorDialog("Layout Modification Error", "Operation not allowed.", "The default layout cannot be removed.");
        }
        else {
            ld.deleteDialog(choiceBoxLayout.getSelectionModel().getSelectedItem());
        }
    }


    @FXML
    protected void startRun() {
        updatePreferences();
        accordionMain.setExpandedPane(paneLog);
        dispatcher = new BratDispatcher();
        dispatcher.runFromGUI();
    }

    public void shutdown() {
        dispatcher.shutdown();
        Platform.exit();
    }

    @FXML
    protected void setDefaults() {
        Parameters.reset(ParamLocation.advanced);
        setAdvancedParameters();
    }

    private void updatePreferences() {
        Parameters.baseDirectory.setValue(txtfldSegDir.getText());
        Parameters.origDirectory.setValue(txtfldOrigDir.getText());
        Parameters.fileExtension.setValue(txtfldExtension.getText());
        Parameters.flipHorizontal.setValue(String.valueOf(chkboxFlipHorizontal.isSelected()));
//        Parameters.useSets.setValue(String.valueOf(chkboxTimeSeries.isSelected()));
//        Parameters.haveDayZeroImage.setValue(String.valueOf(chkboxDayZero.isSelected()));
//        Parameters.haveStartPoints.setValue(String.valueOf(chkboxStartPoints.isSelected()));
        Parameters.numThreads.setValue(String.valueOf((int) sliderNumThreads.getValue()));

        Parameters.experimentIdentifier.setValue(txtfldExpIdentifier.getText());
        Parameters.setIdentifier.setValue(txtfldSetIdentifier.getText());
        Parameters.timePtIdentifier.setValue(txtfldDayIdentifier.getText());
        Parameters.plateIdentifier.setValue(txtfldPlateIdentifier.getText());
        Parameters.logLevel.setValue(choiceboxLogLevel.getValue());
        Parameters.resolution.setValue(txtfldImageResolution.getText());

        Parameters.segLblBackground.setValue(txtfldSegLblBackground.getText());
        Parameters.segLblRoot.setValue(txtfldSegLblRoot.getText());
        Parameters.segLblShoot.setValue(txtfldSegLblShoot.getText());
        Parameters.segLblTransition.setValue(txtfldSegLblTransition.getText());
        Parameters.segLblCenterline.setValue(txtfldSegLblCenterline.getText());

        // root detection
        Parameters.minTransitionPix.setValue(txtfldMinTransitionPix.getText());
        Parameters.minShootPix.setValue(txtfldMinShootPix.getText());
        Parameters.minRootPix.setValue(txtfldMinRootPix.getText());
        Parameters.cropRegion.setValue(txtfldCropRegion.getText());
        // diagnostic images
        Parameters.plantTreeMaxLevel.setValue(txtfldPlantTreeMaxLevel.getText());
        Parameters.selectedLayout.setValue(choiceBoxLayout.getValue()); // number of pixels included in calculation of tracking properties (e.g. tracking angle)
        Parameters.shootRoiColor.setValue(colorToHex(clrpckShootRoi.getValue()));
        Parameters.rootRoiColor.setValue(colorToHex(clrpckRootRoi.getValue()));
        Parameters.skeletonColor.setValue(colorToHex(clrpckSkeleton.getValue()));
        Parameters.mainRootColor.setValue(colorToHex(clrpckMainRoot.getValue()));
        Parameters.startPtColor.setValue(colorToHex(clrpckStartPoint.getValue()));
        Parameters.endPtColor.setValue(colorToHex(clrpckEndPoint.getValue()));
        Parameters.shootCMColor.setValue(colorToHex(clrpckShootCM.getValue()));
        Parameters.generalColor.setValue(colorToHex(clrpckGeneral.getValue()));
        Parameters.labelSize.setValue(txtfldLabelSize.getText());
        Parameters.labelFont.setValue(txtfldLabelFont.getText());
        Parameters.circleDiameter.setValue(txtfldCircleDiameter.getText());
        Parameters.numberSize.setValue(txtfldNumberSize.getText());
        Parameters.numberFont.setValue(txtfldNumberFont.getText());


        try {
            Parameters.write(ParamLocation.basic);
            Parameters.write(ParamLocation.advanced);
            //log.fine("Updated Preferences.");
        } catch (BackingStoreException e) {
            log.severe(ExceptionLog.StackTraceToString(e));
            e.printStackTrace();
        }

    }

    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }


    class LayoutDialog {
        private Dialog<Pair<String, String>> dialog;
        private TextField txtfldLayoutName;
        private TextField txtfldFilepath;
        private String layoutName;
        private String layoutImportPath;
        private Spinner<Integer> spnRows;
        private Spinner<Integer> spnCols;

        private void chooseFile() {
            FileChooser ch = new FileChooser();
            ch.setTitle("Import Plate Layout");
            ch.setInitialDirectory(new File(System.getProperty("user.home")));

            File file = ch.showOpenDialog(txtfldFilepath.getScene().getWindow());

            if (file != null) {
                txtfldFilepath.setText(file.getAbsolutePath());
            }
        }

        void addDialog() {
            dialog = new Dialog<>();
            dialog.setTitle("Import Layout");
            dialog.setHeaderText("Import Layout from FIJI Point ROI list.");

            // Set the icon (must be included in the project).
//        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));
            ButtonType importButtonType = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
            ButtonType browseButtonType = new ButtonType("Browse", ButtonBar.ButtonData.BIG_GAP);
            dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

            Button btnBrowse = new Button();
            btnBrowse.setText("Browse");

            spnRows = new Spinner<>();
            spnCols = new Spinner<>();
            SpinnerValueFactory<Integer> rowValFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2);
            SpinnerValueFactory<Integer> colValFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 12);
            spnRows.setValueFactory(rowValFactory);
            spnCols.setValueFactory(colValFactory);
            spnRows.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            spnCols.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10, 10, 10, 10));

            txtfldLayoutName = new TextField();
            txtfldLayoutName.setPromptText("Layout name");
            txtfldFilepath = new TextField();
            txtfldFilepath.setPromptText("File path");

            grid.add(new Label("Layout name:"), 0, 0);
            grid.add(txtfldLayoutName, 1, 0);
            grid.add(new Label("Point ROI List:"), 0, 1);
            grid.add(txtfldFilepath, 1, 1);
            grid.add(btnBrowse, 2, 1);
            grid.add(new Label("Rows:"), 0, 2);
            grid.add(spnRows, 1, 2);
            grid.add(new Label("Cols:"), 0, 3);
            grid.add(spnCols, 1, 3);

            Node importButton = dialog.getDialogPane().lookupButton(importButtonType);
            Node browseButton = dialog.getDialogPane().lookupButton(browseButtonType);
            importButton.setDisable(true);

            txtfldLayoutName.textProperty().addListener((observable, oldvalue, newvalue) -> {
                importButton.setDisable((newvalue.trim().isEmpty() || txtfldFilepath.getText().trim().isEmpty()));
            });
            txtfldFilepath.textProperty().addListener((observable, oldvalue, newvalue) -> {
                importButton.setDisable((newvalue.trim().isEmpty() || txtfldLayoutName.getText().trim().isEmpty()));
            });

            dialog.getDialogPane().setStyle("-fx-base: rgb(200, 200, 255);");
            dialog.getDialogPane().setContent(grid);
            Platform.runLater(() -> txtfldLayoutName.requestFocus());
            btnBrowse.setOnAction(actionEvent -> chooseFile());

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == importButtonType) {
                    return new Pair<>(txtfldLayoutName.getText(), txtfldFilepath.getText());
                }
                return null;
            });

            Optional<Pair<String, String>> result = dialog.showAndWait();
            result.ifPresent(layoutnamePath -> {
                layoutName = layoutnamePath.getKey();
                layoutImportPath = layoutnamePath.getValue();

                if (layoutName != null && layoutImportPath != null) {
                    int rows = spnRows.getValue();
                    int cols = spnCols.getValue();
                    try {
                        Parameters.importIJRoiLayout(layoutName, layoutImportPath, rows, cols);
                    } catch (IOException e) {
                        errorDialog("Layout Operation Error", "Failed to import Layout!", e.getMessage());
                        return;
                    } catch (BackingStoreException e) {
                        e.printStackTrace();
                        return;
                    }
                    ObservableList<String> chList= choiceBoxLayout.getItems();
                    chList.add(layoutName);
                    choiceBoxLayout.setValue(Parameters.selectedLayout.getValue());
                }
            });

        }

        void deleteDialog(String selectedLayout) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Layout");
            alert.setHeaderText("Are you sure you want to remove this Layout?");
            alert.setContentText(selectedLayout);
            alert.getDialogPane().setStyle("-fx-base: rgb(200, 200, 255);");

            Optional<ButtonType> option = alert.showAndWait();
            if (option.isPresent() && option.get() == ButtonType.OK) {
                try {
                    Parameters.removeLayout(selectedLayout);
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                    return;
                }
                ObservableList<String> chList= choiceBoxLayout.getItems();
                chList.remove(selectedLayout);
                choiceBoxLayout.setValue(Parameters.selectedLayout.getValue());

            }

        }

        void errorDialog(String title, String header, String content) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.getDialogPane().setStyle("-fx-base: rgb(200, 200, 255);");
            alert.showAndWait();
        }
    }

    private void initLogger(final String version) {
        Logger rootLog = LogManager.getLogManager().getLogger("");
        rootLog.setLevel(Level.ALL);

        Handler[] curHandlers = rootLog.getHandlers();
        for (Handler h : curHandlers) {
            rootLog.removeHandler(h);
        }

        Logger log = Logger.getLogger("edu.salk.brat");

        Level logLevel = Level.parse(Parameters.logLevel.getValue());
        log.setLevel(Level.ALL);

        curHandlers = log.getHandlers();
        for (Handler h : curHandlers) {
            if (h instanceof ConsoleHandler || h instanceof BratFxLogHandler) {
                log.removeHandler(h);
            }
        }

        Handler h = new ConsoleHandler();
        h.setLevel(logLevel);
        if (logLevel.intValue() >= Level.FINE.intValue()) {
            h.setFormatter(new SingleLineFormatter());
        } else {
            h.setFormatter(new DebugLogFormatter());
        }
        h.setLevel(logLevel);
        log.addHandler(h);

        if (logQueue != null) {
            BratFxLogHandler guiLogHandler = new BratFxLogHandler(logQueue);
            guiLogHandler.setLevel(Level.ALL);
            log.addHandler(guiLogHandler);
        }
        log.info("Brat Logging started.");
        log.info(String.format("Brat version %s", version));
    }

}