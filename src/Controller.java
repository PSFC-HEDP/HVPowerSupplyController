import net.wimpi.modbus.facade.ModbusTCPMaster;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

import java.net.InetAddress;

/**
 * Created by lahmann on 2017-01-16.
 */
class Controller {

    private final int MAX_DATA_VALUE = 30000;
    private final double MIN_ACCEPTABLE_REFERENCE_VOLTAGE = 9.0;

    private TCPMasterConnection connection;
    private ModbusTCPMaster master;


    Controller(String ipAddress, Integer port){
        try {
            connection = new TCPMasterConnection(InetAddress.getByName(ipAddress));
            connection.setPort(port);
            connection.connect();

            master = new ModbusTCPMaster(ipAddress, port);
            master.connect();
        }
        catch (Exception e){
            connection.close();
            master.disconnect();
        }
    }

    /**
     * This function checks to see if there's a connection be checking the reference voltage value
     * If the reference voltage value can't be read or is too low, the connection cannot be considered reliable
     */
    boolean isConnected(){
        if (connection.isConnected()) {
            try {
                return (getReferenceVoltage() > MIN_ACCEPTABLE_REFERENCE_VOLTAGE);
            } catch (Exception error) {
                return false;
            }
        }
        return false;
    }

    String getAddress(){
        return connection.getAddress().toString();
    }

    void disconnect(){
        connection.close();
        master.disconnect();
    }

    void setPowerSupplyEnable(boolean isOn) throws Exception {
        if (isOn){
            setAcromagOutputVoltage(Configuration.getHvEnableChannel(), getReferenceVoltage());
        }else{
            setAcromagOutputVoltage(Configuration.getHvEnableChannel(), 0.0);
            setPowerSupplyVoltage(0.0);
            setPowerSupplyCurrent(0.0);
        }
    }

    void setPowerSupplyVoltage(double voltage) throws Exception {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (voltage / Configuration.getMaxPowerSupplyVoltage()) * referenceMax;

        setAcromagOutputVoltage(Configuration.getVoltageControlChannel(), acromagVoltage);
    }

    void setPowerSupplyCurrent(double current) throws Exception {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (current / Configuration.getMaxPowerSupplyCurrent()) * referenceMax;

        setAcromagOutputVoltage(Configuration.getCurrentControlChannel(), acromagVoltage);
    }

    double getPowerSupplyVoltage() throws Exception {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getVoltageMonitorChannel());

        return (acromagVoltage / referenceMax) * Configuration.getMaxPowerSupplyVoltage();
    }

    double getPowerSupplyCurrent() throws Exception {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getCurrentMonitorChannel());

        return (acromagVoltage / referenceMax) * Configuration.getMaxPowerSupplyCurrent();
    }

    private double getReferenceVoltage() throws Exception {
        // return 10.0;
        return getAcromagInputVoltage(Configuration.getReferenceVoltageChannel());
    }


    /**
     * Method that sets the voltage of the Acromag's output voltage channel corresponding to
     * the specified channelID. Actual addresses are hard coded in the AddressDictionary Class
     */
    private double getAcromagInputVoltage(int channelID) throws Exception{

        /**
         * Get the addresses we need from the dictionary
         */
        int configAddress = AddressDictionary.getInputChannelConfigAddress(channelID);
        int dataAddress   = AddressDictionary.getInputChannelDataAddress(channelID);


        /**
         * Read the config and data registers
         */
        InputRegister configRegister = master.readInputRegisters(configAddress, 1)[0];
        InputRegister dataRegister = master.readInputRegisters(dataAddress, 1)[0];


        /**
         * The voltage corresponding to the max data value is either 5V or 10V depending on whether the
         * 0th bit of the config register is 0 or 1 respectively
         */
        double maxVoltage;
        int bit = getBit(configRegister.toShort(), 0);
        if (bit == 0){
            maxVoltage = 5.0;
        }else{
            maxVoltage = 10.0;
        }


        /**
         * Return the corresponding voltage
         */
        return maxVoltage * ((double) dataRegister.toShort() / (double) MAX_DATA_VALUE);
    }


    /**
     * Method that sets the voltage of the Acromag's output voltage channel corresponding to
     * the specified channelID. Actual addresses are hard coded in the AddressDictionary Class
     */
    private void setAcromagOutputVoltage(int channelID, double voltage) throws Exception{

        /**
         * Get the addresses we need from the dictionary
         */
        int configAddress = AddressDictionary.getOutputChannelConfigAddress(channelID);
        int dataAddress   = AddressDictionary.getOutputChannelDataAddress(channelID);


        /**
         * Read the config register
         */

        Register configRegister = master.readMultipleRegisters(configAddress, 1)[0];


        /**
         * The voltage corresponding to the max data value is either 5V or 10V depending on whether the
         * 0th bit of the config register is 1 or 0 respectively
         */
        double maxVoltage;
        int bit = getBit(configRegister.toShort(), 0);
        if (bit == 0){
            maxVoltage = 10.0;
        }else{
            maxVoltage = 5.0;
        }


        /**
         * Calculate the data value corresponding to the requested voltage
         */
        int dataValue = (int) (MAX_DATA_VALUE * (voltage / maxVoltage));


        /**
         * Set that value to the data channel
         */
        Register dataRegister = new SimpleRegister(dataValue);
        master.writeSingleRegister(dataAddress, dataRegister);
    }


    /**
     * Convenience function nth bit from integer
     */
    private int getBit(int integer, int n){
        return (integer >> n) & 1;
    }





}
