import net.wimpi.modbus.facade.ModbusTCPMaster;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

import java.net.InetAddress;


/**
 * Class the handles all writing and reading to the Acromag unit
 */
class AcromagController {

    private final int MAX_DATA_VALUE = 30000;
    private final double MIN_ACCEPTABLE_REFERENCE_VOLTAGE = 9.0;

    private TCPMasterConnection connection;
    private ModbusTCPMaster master;


    AcromagController(){
    }


    /**
     * Basic constructor
     * @param ipAddress IP address that will be used to attempt to establish communication with the Acromag
     * @param port Modbus port to use in communications
     */
    AcromagController(String ipAddress, Integer port) throws Exceptions.AcromagConnectionException {
        this(ipAddress, port, Configuration.getPollPeriod());
    }


    /**
     * Basic constructor
     * @param ipAddress IP address that will be used to attempt to establish communication with the Acromag
     * @param port Modbus port to use in communications
     * @param timeout Time (ms) before we give up trying to connect
     */
    AcromagController(String ipAddress, Integer port, Integer timeout) throws Exceptions.AcromagConnectionException {

        try {
            connection = new TCPMasterConnection(InetAddress.getByName(ipAddress));
            connection.setTimeout(timeout);
            connection.setPort(port);
            connection.connect();

            master = new ModbusTCPMaster(ipAddress, port);
            master.connect();
        }
        catch (Exception e) {
            disconnect();
            throw new Exceptions.AcromagConnectionException(ipAddress);
        }
    }


    /**
     * Check the state of the connection between the software and the Acromag
     * @return true if connected, false otherwise
     */
    boolean isConnected(){
        if (connection == null || master == null) return false;
        return connection.isConnected();
    }


    /**
     * Return the current IP address as a string
     * @return the IP address as a string
     */
    String getAddress(){
        return connection.getAddress().toString();
    }


    /**
     * Disconnect from the Acromag
     */
    void disconnect(){
        if (connection != null) connection.close();
        if (master != null)     master.disconnect();
    }


    /**
     * Method that sets the enabled state through the "HV ENABLE" channel
     * @param isOn Desired state (true is on / false is off) of the HVPS
     * @throws Exceptions.ReadInputVoltageException
     * @throws Exceptions.WriteOutputVoltageException
     */
    void setPowerSupplyEnable(boolean isOn) throws Exceptions.ReadInputVoltageException, Exceptions.WriteOutputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException {
        if (isOn){
            setChannelOutputVoltage(Configuration.getHvEnableChannel(), getReferenceVoltage());
        }else{
            // If we're turning off the HVPS we also should zero out the voltage and current
            setChannelOutputVoltage(Configuration.getHvEnableChannel(), 0.0);
            setPowerSupplyVoltage(0.0);
            setPowerSupplyCurrent(0.0);
        }
    }


    /**
     * Method that sets the voltage through the "LOCAL V-CONTROL" channel
     * @param voltage Desired voltage (kV) of the HVPS
     * @throws Exceptions.ReadInputVoltageException
     * @throws Exceptions.WriteOutputVoltageException
     */
    void setPowerSupplyVoltage(double voltage) throws Exceptions.ReadInputVoltageException, Exceptions.WriteOutputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException {
        // Sanity check
        voltage = Math.min(Constants.getPowerSupplyMaxVoltage(), voltage);
        voltage = Math.min(Configuration.getMaxAllowablePowerSupplyVoltage(), voltage);

        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (voltage / Constants.getPowerSupplyMaxVoltage()) * referenceMax;

        setChannelOutputVoltage(Configuration.getVoltageControlChannel(), acromagVoltage);
    }


    /**
     * Method that sets the current through the "LOCAL I-CONTROL" channel
     * @param current Desired current (mA) of the HVPS
     * @throws Exceptions.ReadInputVoltageException
     * @throws Exceptions.WriteOutputVoltageException
     */
    void setPowerSupplyCurrent(double current) throws Exceptions.ReadInputVoltageException, Exceptions.WriteOutputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException{
        // Sanity check
        current = Math.min(Constants.getPowerSupplyMaxCurrent(), current);

        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (current / Constants.getPowerSupplyMaxCurrent()) * referenceMax;

        setChannelOutputVoltage(Configuration.getCurrentControlChannel(), acromagVoltage);
    }


    /**
     * Method that sets turns the laser diode on/off
     * @param isOn Desired state (true is on / false is off) of the laser diode
     * @throws Exceptions.ReadInputVoltageException
     * @throws Exceptions.WriteOutputVoltageException
     */
    void setLdEnable(boolean isOn) throws Exceptions.ReadInputVoltageException, Exceptions.WriteOutputVoltageException, Exceptions.AcromagConnectionException {
        if (isOn){
            setChannelOutputVoltage(Configuration.getLdEnableChannel(), Constants.getLaserDiodeOnVoltage());
        }else{
            // If we're turning off the laser diode we should zero out the current
            setChannelOutputVoltage(Configuration.getLdEnableChannel(), 0.0);
            setLaserDiodeCurrent(0.0);
        }
    }


    /**
     * Method that sets the current to the laser diode
     * @param current Desired current (mA) to the laser diode
     * @throws Exceptions.ReadInputVoltageException
     * @throws Exceptions.WriteOutputVoltageException
     */
    void setLaserDiodeCurrent(double current) throws Exceptions.ReadInputVoltageException, Exceptions.WriteOutputVoltageException, Exceptions.AcromagConnectionException {
        // Sanity check
        current = Math.min(Constants.getLaserDiodeMaxCurrent(), current);

        double acromagVoltage = current * Constants.getVoltagePerLdCurrent();
        setChannelOutputVoltage(Configuration.getCurrentControlChannel(), acromagVoltage);
    }


    /**
     * Method that returns the voltage (kV) being inferred from the "V-MONITOR" channel
     * @return HVPS voltage (kV) currently being inferred
     * @throws Exceptions.ReadInputVoltageException
     */
    double getPowerSupplyVoltage() throws Exceptions.ReadInputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getVoltageMonitorChannel());

        return (acromagVoltage / referenceMax) * Constants.getPowerSupplyMaxVoltage();
    }


    /**
     * Method that returns the current (mA) being inferred from the "I-MONITOR" channel
     * @return HVPS current (mA) currently being inferred
     * @throws Exceptions.ReadInputVoltageException
     */
    double getPowerSupplyCurrent() throws Exceptions.ReadInputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getCurrentMonitorChannel());

        return (acromagVoltage / referenceMax) * Constants.getPowerSupplyMaxCurrent();
    }


    /**
     * Method that returns the voltage being read on the "REFERENCE" channel
     * @return HVPS IO reference "10 volts" (V) currently being read
     * @throws Exceptions.ReadInputVoltageException
     */
    private double getReferenceVoltage() throws Exceptions.ReadInputVoltageException, Exceptions.AcromagConnectionException, Exceptions.BadReferenceVoltageException {
        double referenceVoltage = getAcromagInputVoltage(Configuration.getReferenceVoltageChannel());

        if (referenceVoltage < MIN_ACCEPTABLE_REFERENCE_VOLTAGE) throw new Exceptions.BadReferenceVoltageException(Configuration.getReferenceVoltageChannel(), referenceVoltage);
        return referenceVoltage;
    }


    /**
     * Method that sets the voltage of the Acromag's output voltage channel corresponding to
     * the specified channelID. Actual addresses are hard coded in the Constants Class
     * @param channelID Integer ID (0-15) of the input channel
     * @return Voltage (V) currently being read by the input channel
     * @throws Exceptions.ReadInputVoltageException
     */
    private double getAcromagInputVoltage(int channelID) throws Exceptions.ReadInputVoltageException, Exceptions.AcromagConnectionException{

        if (!isConnected()) throw new Exceptions.AcromagConnectionException(Configuration.getAcromagIpAddress());

        // Get the addresses we need from the dictionary
        int configAddress = Constants.getInputChannelConfigAddress(channelID);
        int dataAddress   = Constants.getInputChannelDataAddress(channelID);


        // Read the config register
        InputRegister configRegister = null;
        try {
            configRegister = master.readInputRegisters(configAddress, 1)[0];
        }catch (Exception e) {
            throw new Exceptions.ReadInputVoltageException(channelID, configAddress);
        }


        // Read the data register
        InputRegister dataRegister = null;
        try{
            dataRegister = master.readInputRegisters(dataAddress, 1)[0];
        }
        catch (Exception e){
            throw new Exceptions.ReadInputVoltageException(channelID, dataAddress);
        }


        // The voltage corresponding to the max data value is either 5V or 10V depending on whether the
        // 0th bit of the config register is 0 or 1 respectively
        double maxVoltage;
        int bit = getBit(configRegister.toShort(), 0);
        if (bit == 0){
            maxVoltage = 5.0;
        }else{
            maxVoltage = 10.0;
        }


        // Return the corresponding voltage
        return maxVoltage * ((double) dataRegister.toShort() / (double) MAX_DATA_VALUE);
    }


    /**
     * Method that sets the voltage of the Acromag's output voltage channel corresponding to
     * the specified channelID. Actual addresses are hard coded in the Constants Class
     * @param channelID Integer ID (0-15) of the output channel
     * @param voltage Desired voltage (V) of the output channel
     * @throws Exceptions.WriteOutputVoltageException
     * @throws Exceptions.ReadInputVoltageException
     */
    void setChannelOutputVoltage(int channelID, double voltage) throws Exceptions.WriteOutputVoltageException, Exceptions.ReadInputVoltageException, Exceptions.AcromagConnectionException {

        if (!isConnected()) throw new Exceptions.AcromagConnectionException(Configuration.getAcromagIpAddress());

        // Get the addresses we need from the dictionary
        int configAddress = Constants.getOutputChannelConfigAddress(channelID);
        int dataAddress   = Constants.getOutputChannelDataAddress(channelID);


        // Read the config register
        Register configRegister = null;
        try {
            configRegister = master.readMultipleRegisters(configAddress, 1)[0];
        }
        catch (Exception e){
            throw new Exceptions.ReadInputVoltageException(channelID, configAddress);
        }


        // The voltage corresponding to the max data value is either 5V or 10V depending on whether the
        // 0th bit of the config register is 1 or 0 respectively
        double maxVoltage;
        int bit = getBit(configRegister.toShort(), 0);
        if (bit == 0){
            maxVoltage = 10.0;
        }else{
            maxVoltage = 5.0;
        }


        // Calculate the data value corresponding to the requested voltage
        int dataValue = (int) (MAX_DATA_VALUE * (voltage / maxVoltage));


        // Set that value to the data channel
        try {
            Register dataRegister = new SimpleRegister(dataValue);
            master.writeSingleRegister(dataAddress, dataRegister);
        }
        catch (Exception e){
            throw new Exceptions.WriteOutputVoltageException(channelID, voltage, dataAddress);
        }
    }


    /**
     * Convenience function for getting the nth bit for an integer
     * @param integer Any positive integer
     * @param n Index of the bit of interest
     * @return Value (0 or 1) of the nth bit of the integer
     */
    private int getBit(int integer, int n){
        return (integer >> n) & 1;
    }
}
