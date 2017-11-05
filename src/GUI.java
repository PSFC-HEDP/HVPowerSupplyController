

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main class the implements the GUI for the HVPS Controller
 *
 */
public class GUI extends JFrame implements WindowListener{

    // Contact information of the current software maintainer
    private final String AUTHOR_CONTACT = "Brandon Lahmann (lahmann@mit.edu)";

    // Number of points on the progress bars that represent the readings
    private final int PROGRESS_BAR_RESOLUTION = 1000;

    // Number of cycles where we're willing to accept a discrepancy between our settings and our readings
    // After this number is exceeded, a software interlock will be tripped
    private final int NUM_POLL_PERIODS_BEFORE_INTERLOCK = 10;

    // The difference between our target voltage and our voltage we read that we consider to be non-suspicious
    // If this difference is exceeded for too long a software interlock will be tripped
    private final double ACCEPTABLE_VOLTAGE_DIFFERENCE = 1.0;       // kV

    // Preset quick condition times in minutes
    private final int[] QUICK_CONDITION_TIMES = new int[] {5, 10, 15, 30, 60};

    // Controller that interacts with the Acromag
    private AcromagController controller = new AcromagController();

    // Internal state objects
    private PowerSupplyState hvState = new PowerSupplyState();
    private LaserDiodeState  ldState = new LaserDiodeState();

    // Swing components of the main window (this GUI Object)
    private GridBagConstraints constraints;
    private JMenu quickConditionMenu;
    private JMenuItem configurationMenuItem;
    private JToggleButton hvOnButton, hvOffButton, ldOnButton, ldOffButton;
    private JButton setVoltageButton, setLdCurrentButton, abortConditionButton;
    private JProgressBar voltageReading, currentReading, ldCurrentReading;
    private JLabel statusLabel;

    // Swing components of the Set Voltage window
    private JPanel setVoltagePanel;
    private JSpinner targetVoltageSpinner;

    // Swing components of the Set LD Current window
    private JPanel setDiodeCurrentPanel;
    private JSpinner targetDiodeCurrentSpinner;

    // Swing components of the Configuration window
    private JPanel configPanel;
    private JTextField[] ipAddressFields = new JTextField[4];
    private JTextField modbusPortField, pollPeriodField;
    private JTextField maxVoltageField;

    private JSpinner referenceVoltageChannelSpinner;
    private JSpinner voltageMonitorChannelSpinner;
    private JSpinner currentMonitorChannelSpinner;
    private JSpinner hvEnableChannelSpinner;
    private JSpinner voltageControlChannelSpinner;
    private JSpinner currentControlChannelSpinner;
    private JSpinner ldEnableChannelSpinner;
    private JSpinner ldCurrentControlChannelSpinner;



    /**
     * Main method that simply calls the GUI constructor
     */
    public static void main(String ... args){
        new GUI();
    }


    /**
     * Default constructor that initializes the GUI and calls the mainLoop function
     */
    private GUI(){
        super("HV Power Supply Controller");
        initialize();
        mainLoop();
    }


    /**
     * Initialization Functions
     */
    private void initialize(){

        // Load the configuration
        Configuration.loadConfiguration();

        // Build the windows
        buildMainWindow();
        buildSetPowerSupplyVoltageWindow();
        buildSetLaserDiodeCurrentWindow();
        buildConfigWindow();

        // Initialize our states
        hvState.setEnabled(false);
        ldState.setEnabled(false);

        // Lock the system until connection is confirmed
        lockSystem();

    }

    private void buildMainWindow(){

        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        // Advanced menu
        JMenu advancedMenu = new JMenu("Advanced");
        menuBar.add(advancedMenu);

        // Quick Condition menu
        quickConditionMenu = new JMenu("Quick Condition");
        advancedMenu.add(quickConditionMenu);

        // Configuration menu item
        configurationMenuItem = new JMenuItem("Configuration");
        configurationMenuItem.addActionListener(actionEvent -> configButtonClicked());
        advancedMenu.add(configurationMenuItem);

        // Quick condition menu items
        JMenuItem[] quickConditionOptions = new JMenuItem[QUICK_CONDITION_TIMES.length];
        for (int i = 0; i < quickConditionOptions.length; i++){
            final int time = QUICK_CONDITION_TIMES[i];
            quickConditionOptions[i] = new JMenuItem(time + " min Condition");
            quickConditionOptions[i].addActionListener(actionEvent -> hvState.startConditioning(time));
            quickConditionMenu.add(quickConditionOptions[i]);
        }

        // Create the component panel
        JPanel mainWindowPanel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;



        // ----- LINE # 1 ("Power Supply" | On | Off | Set Voltage | Configuration) ----- //
            int xPos = 0, yPos = 0;
            setConstraints(xPos, yPos, 2, 1);
            setPadding(10, 10, 10, 10);
            mainWindowPanel.add(new JLabel("Power Supply :", JLabel.LEFT), constraints);


            // Create the hv on button
            hvOnButton = new JToggleButton("On", false);
            hvOnButton.addActionListener(e -> hvState.setEnabled(true));

            xPos+=2;
            setConstraints(xPos, yPos, 1, 1);
            setPadding(10, 0, 10, 10);
            mainWindowPanel.add(hvOnButton, constraints);


            // Create the hv off button to the right
            hvOffButton = new JToggleButton("Off", false);
            hvOffButton.addActionListener(e -> hvState.setEnabled(false));

            xPos++;
            setConstraints(xPos, yPos, 1, 1);
            setPadding(0, 10, 10, 10);
            mainWindowPanel.add(hvOffButton, constraints);


            // Create the set voltage button to the right
            setVoltageButton = new JButton("Set Voltage");
            setVoltageButton.addActionListener(e -> setVoltageButtonClicked());

            xPos++;
            setConstraints(xPos, yPos, 2, 1);
            setPadding(10, 10, 10, 10);
            mainWindowPanel.add(setVoltageButton, constraints);


            // Create an abort condition button exactly on top of the set voltage button
            abortConditionButton = new JButton("Abort!");
            abortConditionButton.addActionListener(e -> hvState.abortConditioning());
            abortConditionButton.setVisible(false);

            setConstraints(xPos, yPos, 2, 1);
            setPadding(10, 10, 10, 10);
            mainWindowPanel.add(abortConditionButton, constraints);



        // ----- LINE # 2 (Voltage Reading Label) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 5, 5);
            mainWindowPanel.add(new JLabel("Voltage Reading", JLabel.CENTER), constraints);



        // ----- LINE # 3 (Voltage Reading Progress Bar) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 0, 0);

            // Build the voltage reading bar
            voltageReading = new JProgressBar(0, PROGRESS_BAR_RESOLUTION);
            voltageReading.setValue(0);
            voltageReading.setStringPainted(true);
            voltageReading.setString("- kV");
            voltageReading.setPreferredSize(new Dimension(4, 30));
            mainWindowPanel.add(voltageReading, constraints);



        // ----- LINE # 4 (Current Reading Label) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 10, 5);
            mainWindowPanel.add(new JLabel("Current Reading", JLabel.CENTER), constraints);



        // ----- LINE # 5 (Current Reading Progress Bar) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 0, 10);

            // Build the current reading bar
            currentReading = new JProgressBar(0, PROGRESS_BAR_RESOLUTION);
            currentReading.setValue(0);
            currentReading.setStringPainted(true);
            currentReading.setString("- mA");
            currentReading.setPreferredSize(new Dimension(4, 30));
            mainWindowPanel.add(currentReading, constraints);



        // ----- LINE # 6 (Separator) -----
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 5, 15);
            mainWindowPanel.add(new JSeparator(), constraints);



        // ----- LINE # 7 ("Laser Diode" | LD On | LD Off | Set Current) -----
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 2, 1);
            setPadding(10, 10, 10, 10);
            mainWindowPanel.add(new JLabel("Laser Diode :", JLabel.LEFT), constraints);


            // Create the ld on button to the right
            ldOnButton = new JToggleButton("On", false);
            ldOnButton.addActionListener(e -> ldState.setEnabled(true));

            xPos+=2;
            setConstraints(xPos, yPos, 1, 1);
            setPadding(10, 0, 10, 10);
            mainWindowPanel.add(ldOnButton, constraints);


            // Create the hv off button to the right
            ldOffButton = new JToggleButton("Off", false);
            ldOffButton.addActionListener(e -> ldState.setEnabled(false));


            xPos++;
            setConstraints(xPos, yPos, 1, 1);
            setPadding(0, 10, 10, 10);
            mainWindowPanel.add(ldOffButton, constraints);


            // Create the set ld current button to the right
            setLdCurrentButton = new JButton("Set Current");
            setLdCurrentButton.addActionListener(e -> setDiodeCurrentButtonClicked());


            xPos++;
            setConstraints(xPos, yPos, 1, 1);
            setPadding(10, 10, 10, 10);
            mainWindowPanel.add(setLdCurrentButton, constraints);



        // ----- LINE # 8 (LD Current Setting Label) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 5, 5);
            mainWindowPanel.add(new JLabel("Current Setting", JLabel.CENTER), constraints);



        // ----- LINE # 9 (LD Current Progress Bar) ----- //
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 0, 10);

            // Build the voltage reading bar
            ldCurrentReading = new JProgressBar(0, PROGRESS_BAR_RESOLUTION);
            ldCurrentReading.setValue(0);
            ldCurrentReading.setStringPainted(true);
            ldCurrentReading.setString("- mA");
            ldCurrentReading.setPreferredSize(new Dimension(4, 30));
            mainWindowPanel.add(ldCurrentReading, constraints);



        // ----- LINE # 10 (Separator) -----
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 5, 15);
            mainWindowPanel.add(new JSeparator(), constraints);


        // ----- LINE # 11 (Status bar) -----
            xPos = 0; yPos++;
            setConstraints(xPos, yPos, 6, 1);
            setPadding(10, 10, 10, 10);

            // Build the status label
            statusLabel = new JLabel("Initializing GUI ... ", JLabel.LEFT);
            statusLabel.setForeground(Color.RED);
            mainWindowPanel.add(statusLabel, constraints);



        // Add the component pane to this frame
        add(mainWindowPanel);
        pack();



        // Set up the window
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        setLocation(Configuration.getMainWindowPosX(), Configuration.getMainWindowPosY());
        setResizable(false);
        setVisible(true);
    }

    private void buildSetPowerSupplyVoltageWindow(){

        /**
         * Create the component panel
         */
        setVoltagePanel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        /**
         * Build target voltage spinner
         */
        setConstraints(0, 0, 1, 1);
        setPadding(10, 10, 10, 10);
        setVoltagePanel.add(new JLabel("Target Voltage: ", JLabel.LEFT), constraints);

        setConstraints(1, 0, 1, 1);
        setPadding(10, 10, 10, 10);

        double maxVoltage = Configuration.getMaxAllowablePowerSupplyVoltage();
        SpinnerNumberModel voltageModel = new SpinnerNumberModel(0.0, 0.0, maxVoltage, 0.5);
        targetVoltageSpinner = new JSpinner(voltageModel);

        JSpinner.NumberEditor voltageEditor = new JSpinner.NumberEditor(targetVoltageSpinner,"0.0  kV   ");
        targetVoltageSpinner.setEditor(voltageEditor);
        setVoltagePanel.add(targetVoltageSpinner, constraints);

    }

    private void buildSetLaserDiodeCurrentWindow(){

        /**
         * Create the component panel
         */
        setDiodeCurrentPanel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        /**
         * Build target voltage spinner
         */
        setConstraints(0, 0, 1, 1);
        setPadding(10, 10, 10, 10);
        setDiodeCurrentPanel.add(new JLabel("Target Current: ", JLabel.LEFT), constraints);

        setConstraints(1, 0, 1, 1);
        setPadding(10, 10, 10, 10);

        double maxCurrent = Constants.getLaserDiodeMaxCurrent();
        SpinnerNumberModel currentModel = new SpinnerNumberModel(0.0, 0.0, maxCurrent, 0.1);
        targetDiodeCurrentSpinner = new JSpinner(currentModel);

        JSpinner.NumberEditor currentEditor = new JSpinner.NumberEditor(targetDiodeCurrentSpinner,"0.0  mA   ");
        targetDiodeCurrentSpinner.setEditor(currentEditor);
        setDiodeCurrentPanel.add(targetDiodeCurrentSpinner, constraints);

    }

    private void buildConfigWindow(){

        int currentHeight = 0;

        /**
         * Create the component panel
         */
        configPanel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        /**
         * Build "communications" separator
         */

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 10, 0);
        configPanel.add(new JLabel("Communications", JLabel.RIGHT), constraints);
        currentHeight++;

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 0, 0);
        configPanel.add(new JSeparator(), constraints);
        currentHeight++;


        /**
         * Build Acromag IP Address Text Fields
         */
        String[] addressStrings = Configuration.getAcromagIpAddress().split("\\.");

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Acromag Address : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        ipAddressFields[0] = new JTextField(addressStrings[0]);
        ipAddressFields[0].setHorizontalAlignment(JTextField.CENTER);
        ipAddressFields[0].setPreferredSize(new Dimension(40, 30));
        configPanel.add(ipAddressFields[0], constraints);

        setConstraints(2, currentHeight, 1, 1);
        setPadding(0, 0, 5, 5);
        ipAddressFields[1] = new JTextField(addressStrings[1]);
        ipAddressFields[1].setHorizontalAlignment(JTextField.CENTER);
        ipAddressFields[1].setPreferredSize(new Dimension(40, 30));
        configPanel.add(ipAddressFields[1], constraints);

        setConstraints(3, currentHeight, 1, 1);
        setPadding(0, 0, 5, 5);
        ipAddressFields[2] = new JTextField(addressStrings[2]);
        ipAddressFields[2].setHorizontalAlignment(JTextField.CENTER);
        ipAddressFields[2].setPreferredSize(new Dimension(40, 30));
        configPanel.add(ipAddressFields[2], constraints);

        setConstraints(4, currentHeight, 1, 1);
        setPadding(0, 10, 5, 5);
        ipAddressFields[3] = new JTextField(addressStrings[3]);
        ipAddressFields[3].setHorizontalAlignment(JTextField.CENTER);
        ipAddressFields[3].setPreferredSize(new Dimension(40, 30));
        configPanel.add(ipAddressFields[3], constraints);
        currentHeight++;


        /**
         * Build Modbus Port Text Field
         */
        Integer modbusPort = Configuration.getModbusPort();

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Modbus Port : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        modbusPortField = new JTextField(modbusPort.toString());
        modbusPortField.setHorizontalAlignment(JTextField.CENTER);
        modbusPortField.setPreferredSize(new Dimension(40, 30));
        configPanel.add(modbusPortField, constraints);
        currentHeight++;



        /**
         * Build Poll Period Text Field
         */
        Integer pollPeriod = Configuration.getPollPeriod();

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Poll Period (ms) : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        pollPeriodField = new JTextField(pollPeriod.toString());
        pollPeriodField.setHorizontalAlignment(JTextField.CENTER);
        pollPeriodField.setPreferredSize(new Dimension(40, 30));
        configPanel.add(pollPeriodField, constraints);
        currentHeight++;


        /**
         * Build "Acromag Channels" separator
         */

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 10, 0);
        configPanel.add(new JLabel("Acromag Input Channels", JLabel.RIGHT), constraints);
        currentHeight++;

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 0, 0);
        configPanel.add(new JSeparator(), constraints);
        currentHeight++;


        /**
         * Build the Acromag input channel spinners
         */

        // Reference Voltage Channel
        SpinnerNumberModel referenceVoltageModel = new SpinnerNumberModel(0, 0, 15, 1);
        referenceVoltageModel.setValue(Configuration.getReferenceVoltageChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Reference Voltage : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        referenceVoltageChannelSpinner = new JSpinner(referenceVoltageModel);
        referenceVoltageChannelSpinner.setPreferredSize(new Dimension(50, 30));
        referenceVoltageChannelSpinner.setEditor(new JSpinner.NumberEditor(referenceVoltageChannelSpinner, "0    "));
        configPanel.add(referenceVoltageChannelSpinner, constraints);
        currentHeight++;

        // Voltage Monitor Channel
        SpinnerNumberModel voltageMonitorModel = new SpinnerNumberModel(0, 0, 15, 1);
        voltageMonitorModel.setValue(Configuration.getVoltageMonitorChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Voltage Monitor : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        voltageMonitorChannelSpinner = new JSpinner(voltageMonitorModel);
        voltageMonitorChannelSpinner.setPreferredSize(new Dimension(50, 30));
        voltageMonitorChannelSpinner.setEditor(new JSpinner.NumberEditor(voltageMonitorChannelSpinner, "0    "));
        configPanel.add(voltageMonitorChannelSpinner, constraints);
        currentHeight++;

        // Current Monitor Channel
        SpinnerNumberModel currentMonitorModel = new SpinnerNumberModel(0, 0, 15, 1);
        currentMonitorModel.setValue(Configuration.getCurrentMonitorChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Current Monitor : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        currentMonitorChannelSpinner = new JSpinner(currentMonitorModel);
        currentMonitorChannelSpinner.setPreferredSize(new Dimension(50, 30));
        currentMonitorChannelSpinner.setEditor(new JSpinner.NumberEditor(currentMonitorChannelSpinner, "0    "));
        configPanel.add(currentMonitorChannelSpinner, constraints);
        currentHeight++;


        /**
         * Build "Acromag Channels" separator
         */

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 10, 0);
        configPanel.add(new JLabel("Acromag Output Channels", JLabel.RIGHT), constraints);
        currentHeight++;

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 0, 0);
        configPanel.add(new JSeparator(), constraints);
        currentHeight++;


        /**
         * Build the Acromag output channel spinners
         */

        // HV Enable Channel
        SpinnerNumberModel hvEnableModel = new SpinnerNumberModel(0, 0, 15, 1);
        hvEnableModel.setValue(Configuration.getHvEnableChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("HV Enable : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        hvEnableChannelSpinner = new JSpinner(hvEnableModel);
        hvEnableChannelSpinner.setPreferredSize(new Dimension(50, 30));
        hvEnableChannelSpinner.setEditor(new JSpinner.NumberEditor(hvEnableChannelSpinner, "0    "));
        configPanel.add(hvEnableChannelSpinner, constraints);
        currentHeight++;

        // Voltage Control Channel
        SpinnerNumberModel voltageControlModel = new SpinnerNumberModel(0, 0, 15, 1);
        voltageControlModel.setValue(Configuration.getVoltageControlChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Voltage Control : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        voltageControlChannelSpinner = new JSpinner(voltageControlModel);
        voltageControlChannelSpinner.setPreferredSize(new Dimension(50, 30));
        voltageControlChannelSpinner.setEditor(new JSpinner.NumberEditor(voltageControlChannelSpinner, "0    "));
        configPanel.add(voltageControlChannelSpinner, constraints);
        currentHeight++;

        // Current Monitor Channel
        SpinnerNumberModel currentControlModel = new SpinnerNumberModel(0, 0, 15, 1);
        currentControlModel.setValue(Configuration.getCurrentControlChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Current Control : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        currentControlChannelSpinner = new JSpinner(currentControlModel);
        currentControlChannelSpinner.setPreferredSize(new Dimension(50, 30));
        currentControlChannelSpinner.setEditor(new JSpinner.NumberEditor(currentControlChannelSpinner, "0    "));
        configPanel.add(currentControlChannelSpinner, constraints);
        currentHeight++;


        /**
         * Build "Laser Diode" separator
         */

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 10, 0);
        configPanel.add(new JLabel("Laser Diode Control Channels", JLabel.RIGHT), constraints);
        currentHeight++;

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 0, 0);
        configPanel.add(new JSeparator(), constraints);
        currentHeight++;


        // LD Enable Channel
        SpinnerNumberModel ldEnableModel = new SpinnerNumberModel(0, 0, 15, 1);
        ldEnableModel.setValue(Configuration.getLdEnableChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("LD Enable : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        ldEnableChannelSpinner = new JSpinner(ldEnableModel);
        ldEnableChannelSpinner.setPreferredSize(new Dimension(50, 30));
        ldEnableChannelSpinner.setEditor(new JSpinner.NumberEditor(ldEnableChannelSpinner, "0    "));
        configPanel.add(ldEnableChannelSpinner, constraints);
        currentHeight++;


        // LD Current Monitor Channel
        SpinnerNumberModel ldCurrentControlModel = new SpinnerNumberModel(0, 0, 15, 1);
        ldCurrentControlModel.setValue(Configuration.getLdCurrentControlChannel());

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("LD Current Control : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        ldCurrentControlChannelSpinner = new JSpinner(ldCurrentControlModel);
        ldCurrentControlChannelSpinner.setPreferredSize(new Dimension(50, 30));
        ldCurrentControlChannelSpinner.setEditor(new JSpinner.NumberEditor(ldCurrentControlChannelSpinner, "0    "));
        configPanel.add(ldCurrentControlChannelSpinner, constraints);
        currentHeight++;


        /**
         * Build "Power Supply Limits" separator
         */

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 10, 0);
        configPanel.add(new JLabel("Power Supply Limits", JLabel.RIGHT), constraints);
        currentHeight++;

        setConstraints(0, currentHeight, 5, 1);
        setPadding(10, 10, 0, 0);
        configPanel.add(new JSeparator(), constraints);
        currentHeight++;


        /**
         * Build Max Power Supply Voltage Text Field
         */
        Double maxPowerSupplyVoltage = Configuration.getMaxAllowablePowerSupplyVoltage();

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Max Voltage (kV) : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        maxVoltageField = new JTextField(maxPowerSupplyVoltage.toString());
        maxVoltageField.setHorizontalAlignment(JTextField.CENTER);
        maxVoltageField.setPreferredSize(new Dimension(40, 30));
        configPanel.add(maxVoltageField, constraints);

    }



    /**
     * Main function loop
     */
    private void mainLoop(){

        int interlockCounter = 0;
        while (this.isVisible()) {
            try {

                // Pause between polls
                Thread.sleep(Configuration.getPollPeriod());

                // If we're not connected, attempt to make a new connection
                if (!controller.isConnected()) {
                    // Notify the user that we are not connected
                    statusLabel.setText("Attempting to connect to Acromag at " + Configuration.getAcromagIpAddress() + " ...");
                    statusLabel.setForeground(Color.RED);

                    controller = new AcromagController(Configuration.getAcromagIpAddress(), Configuration.getModbusPort());
                }

                // Update the Acromag settings to match our internal state
                controller.setPowerSupplyEnable(hvState.isEnabled());
                controller.setPowerSupplyVoltage(hvState.getVoltageSetting());
                controller.setPowerSupplyCurrent(hvState.getCurrentSetting());

                controller.setLdEnable(ldState.isEnabled());
                controller.setLaserDiodeCurrent(ldState.getCurrentSetting());


                // Get the readings from the Acromag and update our power supply state
                hvState.setVoltageReading(controller.getPowerSupplyVoltage());
                hvState.setCurrentReading(controller.getPowerSupplyCurrent());


                // Verify that the reading and settings are matching
                if (hvState.getVoltageReading() + ACCEPTABLE_VOLTAGE_DIFFERENCE < hvState.getVoltageSetting())
                    interlockCounter++;
                else if (hvState.getVoltageReading() - ACCEPTABLE_VOLTAGE_DIFFERENCE > hvState.getVoltageSetting())
                    interlockCounter++;
                else
                    interlockCounter = 0;


                // If the counter has reached our threshold throw an exception to trip the interlock
                if (interlockCounter >= NUM_POLL_PERIODS_BEFORE_INTERLOCK)
                    throw new Exceptions.InconsistentReadingsException(hvState.getVoltageReading(), hvState.getVoltageSetting());


                // Update voltage progress bar value
                double fraction = (hvState.getVoltageReading() / Configuration.getMaxAllowablePowerSupplyVoltage());
                fraction = Math.max(0, fraction);
                fraction = Math.min(1, fraction);

                voltageReading.setString(String.format("%.2f kV", (-1) * hvState.getVoltageReading()));
                voltageReading.setValue((int) (PROGRESS_BAR_RESOLUTION * fraction));


                // Update current progress bar value
                fraction = (hvState.getCurrentReading() / Constants.getPowerSupplyMaxCurrent());
                fraction = Math.max(0, fraction);
                fraction = Math.min(1, fraction);

                currentReading.setString(String.format("%.2f mA", hvState.getCurrentReading()));
                currentReading.setValue((int) (PROGRESS_BAR_RESOLUTION * fraction));


                // Update diode current progress bar value
                fraction = (ldState.getCurrentSetting() / Constants.getLaserDiodeMaxCurrent());
                fraction = Math.max(0, fraction);
                fraction = Math.min(1, fraction);

                ldCurrentReading.setString(String.format("%.2f mA", ldState.getCurrentSetting()));
                ldCurrentReading.setValue((int) (PROGRESS_BAR_RESOLUTION * fraction));


                // If we made it to the end, there are no errors. Unlock the system for the user.
                unlockSystem();
            }

            // Something has gone wrong
            catch (Exception e) {

                // We want to lock the system regardless of error
                lockSystem();

                // Print the error message to the terminal
                writeErrorMessage(e.getMessage());

                // Make an attempt to turn off the system directly (if we're here this will likely fail)
                try {
                    controller.setPowerSupplyEnable(false);
                    controller.setLdEnable(false);
                } catch (Exception error) {
                    // Inform the user we failed
                    writeErrorMessage("Controller is unable to confirm the state of the HVPS");
                } finally {

                    // This means we have an issue connecting to the Acromag
                    if (e instanceof Exceptions.AcromagConnectionException) {
                        // Without a connection, there's nothing more that can be done
                    }


                    // This means that despite being connected, we somehow failed to communicate with the Acromag
                    // I precisely timed disconnection could trip this
                    else if (e instanceof Exceptions.ReadInputVoltageException | e instanceof Exceptions.WriteOutputVoltageException) {
                        // Without a connection, there's nothing more that can be done
                    }


                    // This means we have a connection to the Acromag but the HVPS appears to be off or disconnected
                    else if (e instanceof Exceptions.BadReferenceVoltageException) {
                        // Notify the user that there's something wrong with the connection
                        statusLabel.setText("Bad connection between Acromag and HVPS.");
                        statusLabel.setForeground(Color.RED);
                    }


                    // This means that our connection is fine, but the HVPS is not behaving the way we're requesting
                    // Most likely it's a hardware interlock (the door) but could also indicate hardware issues
                    else if (e instanceof Exceptions.InconsistentReadingsException) {

                        // Since the door may have been opened, force the user to address the issue for safety reasons
                        String message = "The voltage readings are inconsistent with this controller's expectations.\n";
                        message += "This is likely due to the door interlock being tripped.\n";
                        message += "\n";
                        message += "The HV Power Supply has been attempted to be turned off.\n";
                        message += "To continue, VISUALLY verify that all personal have evacuated the vault before clearing this message";

                        JOptionPane.showMessageDialog(this, message, "Interlock Tripped!", JOptionPane.ERROR_MESSAGE);
                    }


                    // This means we hit an exception that hasn't been accounted for
                    else {

                        // Let the user know we're in unknown territory
                        writeErrorMessage("Controller hit an unidentified exception, possibly a runtime error...");
                        writeErrorMessage("Dumping stack trace:");
                        e.printStackTrace();

                        System.err.println();
                        this.dispose();

                        writeErrorMessage("Contact " + AUTHOR_CONTACT + " if the issue persist");


                    }
                }
            }
        }
    }

    private void setVoltageButtonClicked(){
        int result = JOptionPane.showConfirmDialog(this,
                setVoltagePanel, "Select a target voltage", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            hvState.setVoltageSetting(Double.valueOf(targetVoltageSpinner.getValue().toString()));
        }
    }

    private void setDiodeCurrentButtonClicked(){
        int result = JOptionPane.showConfirmDialog(this,
                setDiodeCurrentPanel, "Select a target current", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            ldState.setCurrentSetting(Double.valueOf(targetDiodeCurrentSpinner.getValue().toString()));
        }
    }

    private void configButtonClicked(){
        int result = showConfigurationWindow();

        if (result == JOptionPane.OK_OPTION){
            String message = "Incorrectly changing the configuration of this system can result in a variety of serious safety hazards.\n" +
                    "Ensure that you absolutely know what you are doing before by confirming these changes.\n" +
                    "If you have any doubts at all please select \"No\" and contact " + AUTHOR_CONTACT + ".\n" +
                    "\n" +
                    "Are you sure you would like to save these changes?";

            int doubleCheck = JOptionPane.showConfirmDialog(null, message, "Warning!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (doubleCheck == JOptionPane.YES_OPTION) {
                updateConfiguration();
            }
        }

    }

    private int showConfigurationWindow(){

        String ipAddress = Configuration.getAcromagIpAddress();
        String[] values = ipAddress.split(".");

        for (int i = 0; i < values.length; i++){
            ipAddressFields[i].setText(values[i]);
        }

        modbusPortField.setText(Configuration.getModbusPort().toString());
        pollPeriodField.setText(Configuration.getPollPeriod().toString());

        referenceVoltageChannelSpinner.setValue(Configuration.getReferenceVoltageChannel());
        voltageMonitorChannelSpinner.setValue(Configuration.getVoltageMonitorChannel());
        currentMonitorChannelSpinner.setValue(Configuration.getCurrentMonitorChannel());

        hvEnableChannelSpinner.setValue(Configuration.getHvEnableChannel());
        voltageControlChannelSpinner.setValue(Configuration.getVoltageControlChannel());
        currentControlChannelSpinner.setValue(Configuration.getCurrentControlChannel());

        ldEnableChannelSpinner.setValue(Configuration.getLdEnableChannel());
        ldCurrentControlChannelSpinner.setValue(Configuration.getLdCurrentControlChannel());

        maxVoltageField.setText(Configuration.getMaxAllowablePowerSupplyVoltage().toString());

        return JOptionPane.showConfirmDialog(this, configPanel, "Configuration Options", JOptionPane.OK_CANCEL_OPTION);
    }

    private void updateConfiguration(){

        String ipAddress = ipAddressFields[0].getText() +
                "." + ipAddressFields[1].getText() +
                "." + ipAddressFields[2].getText() +
                "." + ipAddressFields[3].getText();

        Configuration.setAcromagIpAddress(ipAddress);
        Configuration.setModbusPort(Integer.valueOf(modbusPortField.getText()));
        Configuration.setPollPeriod(Integer.valueOf(pollPeriodField.getText()));

        Configuration.setReferenceVoltageChannel(Integer.valueOf(
                referenceVoltageChannelSpinner.getValue().toString()));
        Configuration.setVoltageMonitorChannel(Integer.valueOf(
                voltageMonitorChannelSpinner.getValue().toString()));
        Configuration.setCurrentMonitorChannel(Integer.valueOf(
                currentMonitorChannelSpinner.getValue().toString()));

        Configuration.setHvEnableChannel(Integer.valueOf(
                hvEnableChannelSpinner.getValue().toString()));
        Configuration.setVoltageControlChannel(Integer.valueOf(
                voltageControlChannelSpinner.getValue().toString()));
        Configuration.setCurrentControlChannel(Integer.valueOf(
                currentControlChannelSpinner.getValue().toString()));

        Configuration.setLdEnableChannel(Integer.valueOf(
                ldEnableChannelSpinner.getValue().toString()));
        Configuration.setLdCurrentControlChannel(Integer.valueOf(
                ldCurrentControlChannelSpinner.getValue().toString()));

        Configuration.setMaxAllowablePowerSupplyVoltage(Double.valueOf(maxVoltageField.getText()));

        Configuration.setMainWindowPosX(this.getX());
        Configuration.setMainWindowPosY(this.getY());

        Configuration.writeConfiguration();
    }


    // ***********************************************************
    // Inherited WindowListener methods that we'll use for cleanup
    // ***********************************************************

    public void windowClosing(WindowEvent e) {
        this.setVisible(false);
        this.dispose();
    }

    public void windowClosed(WindowEvent e) {
        updateConfiguration();
        Configuration.writeConfiguration();
        controller.disconnect();
    }



    // **************************************************
    // Inherited WindowListener methods that we don't use
    // **************************************************

    public void windowOpened(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }


    // *****************************
    // Convenience wrapper functions
    // *****************************

    private void setConstraints(int x, int y, int width, int height){
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth  = width;
        constraints.gridheight = height;
    }

    private void setPadding(int left, int right, int top, int bottom){
        constraints.insets = new Insets(top, left, bottom, right);
    }

    private void lockSystem(){

        // Disable both internal states
        hvState.setEnabled(false);
        ldState.setEnabled(false);

        // Disable everything that's not the config button
        quickConditionMenu.setEnabled(false);

        hvOnButton.setEnabled(false);
        hvOffButton.setEnabled(false);
        setVoltageButton.setEnabled(false);

        ldOnButton.setEnabled(false);
        ldOffButton.setEnabled(false);
        setLdCurrentButton.setEnabled(false);


        // Blank out all of the readings
        voltageReading.setString("- kV");
        voltageReading.setValue(0);

        currentReading.setString("- mA");
        currentReading.setValue(0);

        ldCurrentReading.setString("- mA");
        ldCurrentReading.setValue(0);
    }

    private void unlockSystem(){
        // If we just force our states to "reset" their enabled state, they'll refresh the GUI automatically
        hvState.setEnabled(hvState.isEnabled());
        ldState.setEnabled(ldState.isEnabled());

        // Show that we are connected
        statusLabel.setText("Connected to " + controller.getAddress());
        statusLabel.setForeground(Color.BLACK);
    }

    private void writeErrorMessage(String message){
        long timeMs = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy (HH:mm:ss) - ");

        message = simpleDateFormat.format(new Date(timeMs)) + message;
        System.err.println(message);
    }


    // **********************
    // Internal state classes
    // **********************

    class PowerSupplyState{

        private Timer rampVoltageTimer;

        private boolean enabled;
        private boolean conditioning;

        private double voltageSetting;
        private double currentSetting;

        private double voltageReading;
        private double currentReading;


        void setEnabled(boolean enabled) {
            this.enabled = enabled;

            // If we're turning if off we should zero all the settings and kill any conditioning
            if (!enabled){
                if (conditioning)   abortConditioning();
                this.voltageSetting = 0.0;
                this.currentSetting = 0.0;
            }

            // On button should be selected but not enabled when the system is "on"
            hvOnButton.setEnabled(!enabled);
            hvOnButton.setSelected(enabled);

            // Off button should not be selected but be enabled when the system is "on"
            hvOffButton.setEnabled(enabled);
            hvOffButton.setSelected(!enabled);

            // Set voltage should be enabled if the system is 'on"
            setVoltageButton.setEnabled(enabled);

            // Configuration options should not be enabled when the system is "on"
            configurationMenuItem.setEnabled(!enabled);
        }

        void startConditioning(int conditionTime){

            // Calculate our voltage steps
            final double maxV = Configuration.getMaxAllowablePowerSupplyVoltage();
            final int totalTime_ms = conditionTime * 60 * 1000;

            final int dt = Configuration.getPollPeriod();
            final double dV = dt * maxV / totalTime_ms;

            // Verify that the user would like to start conditioning
            String message = String.format("Start a %d min conditioning to %.1f kV?", conditionTime, maxV);
            int result = JOptionPane.showConfirmDialog(null,  message, "Conditioning", JOptionPane.YES_NO_OPTION);


            if (result == JOptionPane.YES_OPTION) {

                // Update the state
                this.conditioning = true;

                // Update some GUI elements
                quickConditionMenu.setEnabled(false);
                setVoltageButton.setVisible(false);
                abortConditionButton.setVisible(true);



                // Start the timer
                rampVoltageTimer = new Timer(dt, e -> hvState.rampVoltage(dV));
            }
        }

        void abortConditioning(){
            writeErrorMessage("Attempting to abort conditioning... ");
            stopConditioning();
            writeErrorMessage("Abort successful!");
        }

        void stopConditioning(){

            // Kill the timer
            rampVoltageTimer.stop();

            // Update the state
            this.conditioning = false;

            // Update some GUI elements
            quickConditionMenu.setEnabled(true);
            setVoltageButton.setVisible(true);
            abortConditionButton.setVisible(false);
        }

        void rampVoltage(double dV){
            double newVoltage = this.voltageSetting + dV;
            if (newVoltage > Configuration.getMaxAllowablePowerSupplyVoltage()){
                newVoltage = Configuration.getMaxAllowablePowerSupplyVoltage();
                stopConditioning();
            }
            this.voltageSetting = newVoltage;
        }

        void setVoltageSetting(double voltageSetting) {
            this.voltageSetting = voltageSetting;
        }

        void setCurrentSetting(double currentSetting) {
            this.currentSetting = currentSetting;
        }

        void setVoltageReading(double voltageReading) {
            this.voltageReading = voltageReading;
        }

        void setCurrentReading(double currentReading) {
            this.currentReading = currentReading;
        }

        boolean isEnabled() {
            return enabled;
        }

        public boolean isConditioning() {
            return conditioning;
        }

        double getVoltageSetting() {
            return voltageSetting;
        }

        double getCurrentSetting() {
            return currentSetting;
        }

        double getVoltageReading() {
            return voltageReading;
        }

        double getCurrentReading() {
            return currentReading;
        }
    }

    class LaserDiodeState {

        private boolean enabled;
        private double currentSetting;

        void setEnabled(boolean enabled) {
            this.enabled = enabled;

            // If we're turning if off we should zero all the settings
            if (!enabled){
                this.currentSetting = 0.0;
            }

            // On button should be selected but not enabled when the system is "on"
            ldOnButton.setEnabled(!enabled);
            ldOnButton.setSelected(enabled);

            // Off button should not be selected but be enabled when the system is "on"
            ldOffButton.setEnabled(enabled);
            ldOffButton.setSelected(!enabled);

            // Set current should be enabled if the system is 'on"
            setLdCurrentButton.setEnabled(enabled);
        }

        void setCurrentSetting(double currentSetting) {
            this.currentSetting = currentSetting;
        }

        boolean isEnabled() {
            return enabled;
        }

        double getCurrentSetting() {
            return currentSetting;
        }
    }

}
