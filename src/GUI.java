import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by lahmann on 2017-01-16.
 */
public class GUI extends JFrame implements WindowListener, ActionListener{

    /**
     * Number of points on the progress bars that represent the readings
     */
    private final int VOLTAGE_RESOLUTION = 1000;
    private final int CURRENT_RESOLUTION = 1000;

    /**
     * Number of cycles where we're willing to accept a discrepancy between our settings and our readings
     * After this number is exceeded, a software interlock will be tripped
     */
    private final int NUM_POLL_PERIODS_BEFORE_INTERLOCK = 10;

    /**
     * The difference between our target voltage and our voltage we read that we consider to be non-suspicious
     * If this difference is exceeded for too long a software interlock will be tripped
     */
    private final double ACCEPTABLE_VOLTAGE_DIFFERENCE = 1.0;       // kV

    /**
     * Strings used to identify actions
     */
    private static final String ON_BUTTON_ACTION   = "ON_BUTTON_ACTION";
    private static final String OFF_BUTTON_ACTION  = "OFF_BUTTON_ACTION";
    private static final String SET_VOLTAGE_ACTION = "SET_VOLTAGE_ACTION";
    private static final String CONFIG_ACTION      = "CONFIG_ACTION";

    /**
     * Controller Object that interacts with the Acromag
     */
    private Controller controller;

    /**
     * State object (glorified struct) for the power supply state
     */
    private PowerSupplyState state;

    /**
     * Swing components of the main window (this GUI Object)
     */
    private JPanel mainWindowPanel;
    private GridBagConstraints constraints;
    private JToggleButton onButton, offButton;
    private JButton setVoltageButton, configButton;
    private JProgressBar voltageReading, currentReading;
    private JLabel statusLabel;

    /**
     * Swing components of the Set Voltage window
     */
    private JPanel setVoltagePanel;
    private JSpinner targetVoltageSpinner, targetCurrentSpinner;

    /**
     * Swing components of the Configuration window
     */
    private JPanel configPanel;
    private JTextField[] ipAddressFields = new JTextField[4];
    private JTextField modbusPortField, pollPeriodField;
    private JTextField maxVoltageField, maxCurrentField;
    private JSpinner referenceVoltageChannelSpinner;
    private JSpinner voltageMonitorChannelSpinner;
    private JSpinner currentMonitorChannelSpinner;
    private JSpinner hvEnableChannelSpinner;
    private JSpinner voltageControlChannelSpinner;
    private JSpinner currentControlChannelSpinner;



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

        Configuration.loadConfiguration();

        /**
         * Build the windows
         */
        buildMainWindow();
        buildSetVoltageWindow();
        buildConfigWindow();


        /**
         * Set up a connection with the Acromag
         */
        statusLabel.setText("Attempting to connect to " + Configuration.getAcromagIpAddress());
        statusLabel.setForeground(Color.RED);
        this.controller = new Controller(Configuration.getAcromagIpAddress(), Configuration.getModbusPort());


        /**
         * Initialize the power supply state
         */
        state = new PowerSupplyState();
        state.setOff();

    }

    private void buildMainWindow(){

        /**
         * Create the component panel
         */
        mainWindowPanel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        /**
         * Create the on button
         */
        onButton = new JToggleButton("On", false);
        onButton.addActionListener(this);
        onButton.setActionCommand(ON_BUTTON_ACTION);
        setConstraints(0, 0, 1, 1);
        setPadding(10, 0, 10, 10);
        mainWindowPanel.add(onButton, constraints);


        /**
         * Create the off button
         */
        offButton = new JToggleButton("Off", false);
        offButton.addActionListener(this);
        offButton.setActionCommand(OFF_BUTTON_ACTION);
        offButton.setEnabled(false);
        offButton.setSelected(true);
        setConstraints(1, 0, 1, 1);
        setPadding(0, 10, 10, 10);
        mainWindowPanel.add(offButton, constraints);


        /**
         * Create the set voltage button
         */
        setVoltageButton = new JButton("Set Voltage");
        setVoltageButton.addActionListener(this);
        setVoltageButton.setActionCommand(SET_VOLTAGE_ACTION);
        setVoltageButton.setEnabled(false);
        setConstraints(2, 0, 1, 1);
        setPadding(10, 10, 10, 10);
        mainWindowPanel.add(setVoltageButton, constraints);


        /**
         * Create the config button
         */
        configButton = new JButton("Configuration");
        configButton.addActionListener(this);
        configButton.setActionCommand(CONFIG_ACTION);
        setConstraints(3, 0, 1, 1);
        setPadding(10, 10, 10, 10);
        mainWindowPanel.add(configButton, constraints);


        /**
         * Add a separator between the buttons and the reader
         */
        setConstraints(0, 1, 4, 1);
        setPadding(10, 10, 10, 10);
        mainWindowPanel.add(new JSeparator(), constraints);


        /**
         * Create the voltage reading bar
         */
        setConstraints(0, 2, 4, 1);
        setPadding(10, 10, 0, 0);
        mainWindowPanel.add(new JLabel("Voltage Reading", JLabel.CENTER), constraints);

        voltageReading = new JProgressBar(0, VOLTAGE_RESOLUTION);
        voltageReading.setValue(0);
        voltageReading.setStringPainted(true);
        voltageReading.setString("- kV");
        voltageReading.setPreferredSize(new Dimension(4, 30));
        setConstraints(0, 3, 4, 1);
        setPadding(10, 10, 5, 10);
        mainWindowPanel.add(voltageReading, constraints);


        /**
         * Create the current reading bar
         */
        setConstraints(0, 4, 4, 1);
        setPadding(10, 10, 0, 0);
        mainWindowPanel.add(new JLabel("Current Reading", JLabel.CENTER), constraints);

        currentReading = new JProgressBar(0, CURRENT_RESOLUTION);
        currentReading.setValue(0);
        currentReading.setStringPainted(true);
        currentReading.setString("- mA");
        currentReading.setPreferredSize(new Dimension(4, 30));
        setConstraints(0, 5, 4, 1);
        setPadding(10, 10, 5, 10);
        mainWindowPanel.add(currentReading, constraints);


        /**
         * Create a status bar
         */
        setConstraints(0, 6, 4, 1);
        setPadding(5, 5, 5, 5);

        statusLabel = new JLabel("Initializing GUI ... ", JLabel.LEFT);
        mainWindowPanel.add(statusLabel, constraints);


        /**
         * Add the component pane to this frame
         */
        add(mainWindowPanel);
        pack();


        /**
         * Set up the window
         */
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        setLocation(Configuration.getMainWindowPosX(), Configuration.getMainWindowPosY());
        setResizable(false);
        setVisible(true);

    }

    private void buildSetVoltageWindow(){

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

        double maxVoltage = Configuration.getMaxPowerSupplyVoltage();
        SpinnerNumberModel voltageModel = new SpinnerNumberModel(0.0, 0.0, maxVoltage, 0.5);
        targetVoltageSpinner = new JSpinner(voltageModel);

        JSpinner.NumberEditor voltageEditor = new JSpinner.NumberEditor(targetVoltageSpinner,"0.0  kV   ");
        targetVoltageSpinner.setEditor(voltageEditor);
        setVoltagePanel.add(targetVoltageSpinner, constraints);


        /**
         * Build target current spinner
         */
        setConstraints(0, 1, 1, 1);
        setPadding(10, 10, 10, 10);
        setVoltagePanel.add(new JLabel("Target Current: ", JLabel.LEFT), constraints);

        setConstraints(1, 1, 1, 1);
        setPadding(10, 10, 10, 10);

        double maxCurrent = Configuration.getMaxPowerSupplyCurrent();
        SpinnerNumberModel currentModel = new SpinnerNumberModel(maxCurrent, 0.0, maxCurrent, 0.1);
        targetCurrentSpinner = new JSpinner(currentModel);

        JSpinner.NumberEditor currentEditor = new JSpinner.NumberEditor(targetCurrentSpinner,
                "0.0  mA   ");
        targetCurrentSpinner.setEditor(currentEditor);
        setVoltagePanel.add(targetCurrentSpinner, constraints);

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
        Double maxPowerSupplyVoltage = Configuration.getMaxPowerSupplyVoltage();

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Max Voltage (kV) : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        maxVoltageField = new JTextField(maxPowerSupplyVoltage.toString());
        maxVoltageField.setHorizontalAlignment(JTextField.CENTER);
        maxVoltageField.setPreferredSize(new Dimension(40, 30));
        configPanel.add(maxVoltageField, constraints);
        currentHeight++;


        /**
         * Build Max Power Supply Current Text Field
         */
        Double maxPowerSupplyCurrent = Configuration.getMaxPowerSupplyCurrent();

        setConstraints(0, currentHeight, 1, 1);
        setPadding(10, 10, 5, 5);
        configPanel.add(new JLabel("Max Current (mA) : ", JLabel.RIGHT), constraints);

        setConstraints(1, currentHeight, 1, 1);
        setPadding(10, 0, 5, 5);
        maxCurrentField = new JTextField(maxPowerSupplyCurrent.toString());
        maxCurrentField.setHorizontalAlignment(JTextField.CENTER);
        maxCurrentField.setPreferredSize(new Dimension(40, 30));
        configPanel.add(maxCurrentField, constraints);
        currentHeight++;


    }



    /**
     * Main function loop
     */
    private void mainLoop(){

        int interlockCounter = 0;
        while (this.isVisible()){

            try {
                /**
                 * Pause between polls
                 */
                Thread.sleep(Configuration.getPollPeriod());

                if (controller.isConnected()) {

                    /**
                     * Update our target settings
                     */
                    controller.setPowerSupplyEnable(state.isOn);
                    controller.setPowerSupplyVoltage(state.voltageSetting);
                    controller.setPowerSupplyCurrent(state.currentSetting);


                    /**
                     * Get the readings from the Acromag and push them to the GUI
                     */
                    state.voltageReading = controller.getPowerSupplyVoltage();
                    state.currentReading = controller.getPowerSupplyCurrent();


                    /**
                     * Verify that our reading matching our setting
                     */
                    boolean readingMatches = true;
                    if (state.voltageReading + ACCEPTABLE_VOLTAGE_DIFFERENCE < state.voltageSetting){
                        readingMatches = false;
                    }else if (state.voltageReading - ACCEPTABLE_VOLTAGE_DIFFERENCE > state.voltageSetting){
                        readingMatches = false;
                    }


                    /**
                     * If the readings don't match, update our interlock counter
                     */
                    if (!readingMatches){
                        interlockCounter++;
                    }else{
                        interlockCounter = 0;
                    }


                    /**
                     * If the counter has reached our threshold throw an interlock
                     */
                    if (interlockCounter >= NUM_POLL_PERIODS_BEFORE_INTERLOCK){

                        state.setOff();

                        controller.setPowerSupplyEnable(state.isOn);
                        controller.setPowerSupplyVoltage(state.voltageSetting);
                        controller.setPowerSupplyCurrent(state.currentSetting);

                        String message = "The voltage readings do not agree with what was attempted to be set.\n";
                        message += "This is likely due to the door interlock being tripped.\n";
                        message += "\n";
                        message += "The HV Power Supply has been attempted to be turned off.\n";
                        message += "To continue, please visually verify that all personal have evacuated the vault before clearing this message";

                        JOptionPane.showMessageDialog(this, message, "Interlock Tripped!", JOptionPane.ERROR_MESSAGE);
                    }


                }else{

                    /**
                     * Update state to be off so we can update it immediately when we connect
                     */
                    state.setOff();

                    /**
                     * Attempt to establish a new connection
                     */
                    controller.disconnect();
                    controller = new Controller(Configuration.getAcromagIpAddress(), Configuration.getModbusPort());
                }

                updateGUI();
            }
            catch (Exception error){
                controller.disconnect();
                showErrorPopup(error);

            }
        }

    }

    private void updateGUI(){

        if (controller.isConnected()) {

            /**
             * Show that we are connected
             */
            statusLabel.setText("Connected to " + controller.getAddress());
            statusLabel.setForeground(Color.BLACK);


            /**
             * Update the on/off "switch" based on the state of the system
             */
            onButton.setEnabled(!state.isOn);
            onButton.setSelected(state.isOn);

            offButton.setEnabled(state.isOn);
            offButton.setSelected(!state.isOn);

            setVoltageButton.setEnabled(state.isOn);
            configButton.setEnabled(!state.isOn);

            /**
             * Update progress bar values
             */

            double voltageFraction = (state.voltageReading / Configuration.getMaxPowerSupplyVoltage());
            voltageFraction = Math.max(0, voltageFraction);
            voltageFraction = Math.min(1, voltageFraction);

            double currentFraction = (state.currentReading / Configuration.getMaxPowerSupplyCurrent());
            currentFraction = Math.max(0, currentFraction);
            currentFraction = Math.min(1, currentFraction);

            voltageReading.setString(String.format("%.2f kV", (-1)*state.voltageReading));
            voltageReading.setValue((int) (VOLTAGE_RESOLUTION * voltageFraction));

            currentReading.setString(String.format("%.2f mA", state.currentReading));
            currentReading.setValue((int) (CURRENT_RESOLUTION * currentFraction));

        }else{

            /**
             * Disable everything that's not config
             */
            onButton.setEnabled(false);
            offButton.setEnabled(false);
            setVoltageButton.setEnabled(false);


            /**
             * Show where we are attempting to connect to
             */
            statusLabel.setText("Attempting to connect to " + Configuration.getAcromagIpAddress());
            statusLabel.setForeground(Color.RED);


            /**
             * Show the user that we have no true values to read
             */
            voltageReading.setString("- kV");
            voltageReading.setValue(0);

            currentReading.setString("- mA");
            currentReading.setValue(0);
        }
    }




    /**
     * Functions called by action listener
     */
    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals(ON_BUTTON_ACTION)){
            onButtonClicked();
        }

        if (e.getActionCommand().equals(OFF_BUTTON_ACTION)){
            offButtonClicked();
        }

        if (e.getActionCommand().equals(SET_VOLTAGE_ACTION)){
            setVoltageButtonClicked();
        }

        if (e.getActionCommand().equals(CONFIG_ACTION)){
            configButtonClicked();
        }
    }

    private void onButtonClicked(){
        if (controller.isConnected()) {
            state.setOn();
            updateGUI();
        }else{
            onButton.setSelected(false);
        }
    }

    private void offButtonClicked(){
        if (controller.isConnected()) {
            state.setOff();
            updateGUI();
        }else {
            offButton.setSelected(false);
        }
    }

    private void setVoltageButtonClicked(){
        if (controller.isConnected()) {
            int result = JOptionPane.showConfirmDialog(this,
                    setVoltagePanel, "Set voltage and current",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {

                state.voltageSetting = Double.valueOf(targetVoltageSpinner.getValue().toString());
                state.currentSetting = Double.valueOf(targetCurrentSpinner.getValue().toString());
                updateGUI();

            }
        }
    }

    private void configButtonClicked(){
        int result = JOptionPane.showConfirmDialog(this,
                configPanel, "Configuration Options",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION){
            updateConfiguration();
        }

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

        Configuration.setMaxPowerSupplyVoltage(Double.valueOf(maxVoltageField.getText()));
        Configuration.setMaxPowerSupplyCurrent(Double.valueOf(maxCurrentField.getText()));

        Configuration.setMainWindowPosX(this.getX());
        Configuration.setMainWindowPosY(this.getY());

        controller = new Controller(Configuration.getAcromagIpAddress(), Configuration.getModbusPort());
    }



    /**
     * Function called when the window is closed
     */
    public void windowClosing(WindowEvent e) {
        updateConfiguration();
        Configuration.writeConfiguration();
        controller.disconnect();

        this.dispose();
    }



    /**
     * Implementation methods that I'm not currently using
     */
    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }



    /**
     * Convenience wrapper functions
     */
    private void setConstraints(int x, int y, int width, int height){
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth  = width;
        constraints.gridheight = height;
    }

    private void setPadding(int left, int right, int top, int bottom){
        constraints.insets = new Insets(top, left, bottom, right);
    }

    private void showErrorPopup(Exception error){
        String message = "No message has been programmed for this error yet. :(";
        showErrorPopup(message, error);
    }

    private void showErrorPopup(String message, Exception error){
        error.printStackTrace();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);

        message += "\n\n";
        message += sw.toString();

        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);

    }


    /**
     * Glorified struct to act as our "state machine"
     */
    class PowerSupplyState{

        boolean isOn;

        double voltageSetting;
        double currentSetting;

        double voltageReading;
        double currentReading;

        void setOn(){
            isOn = true;
        }

        void setOff(){
            isOn = false;
            voltageSetting = 0.0;
            currentSetting = 0.0;
        }
    }


}
