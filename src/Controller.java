import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.facade.ModbusTCPMaster;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by lahmann on 2017-01-16.
 */
class Controller {

    private final int MAX_DATA_VALUE = 30000;
    private TCPMasterConnection connection;
    private ModbusTCPMaster master;


    Controller(String ipAddress, Integer port) throws UnknownHostException{
        connection = new TCPMasterConnection(InetAddress.getByName(ipAddress));
        connection.setPort(port);

        master = new ModbusTCPMaster(ipAddress, port);
    }

    boolean isConnected(){
        return connection.isConnected();
    }

    String getAddress(){
        return connection.getAddress().toString();
    }

    void disconnect(){
        connection.close();
        master.disconnect();
    }

    void setPowerSupplyEnable(boolean isOn) throws ModbusException {
        if (isOn){
            setAcromagOutputVoltage(Configuration.getHvEnableChannel(), getReferenceVoltage());
        }else{
            setAcromagOutputVoltage(Configuration.getHvEnableChannel(), 0.0);
            setPowerSupplyVoltage(0.0);
            setPowerSupplyCurrent(0.0);
        }
    }

    void setPowerSupplyVoltage(double voltage) throws ModbusException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (voltage / Configuration.getMaxPowerSupplyVoltage()) * referenceMax;

        setAcromagOutputVoltage(Configuration.getVoltageControlChannel(), acromagVoltage);
    }

    void setPowerSupplyCurrent(double current) throws ModbusException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = (current / Configuration.getMaxPowerSupplyCurrent()) * referenceMax;

        setAcromagOutputVoltage(Configuration.getCurrentControlChannel(), acromagVoltage);
    }

    double getPowerSupplyVoltage() throws ModbusException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getVoltageMonitorChannel());

        return (acromagVoltage / referenceMax) * Configuration.getMaxPowerSupplyVoltage();
    }

    double getPowerSupplyCurrent() throws ModbusException {
        double referenceMax = getReferenceVoltage();
        double acromagVoltage = getAcromagInputVoltage(Configuration.getCurrentMonitorChannel());

        return (acromagVoltage / referenceMax) * Configuration.getMaxPowerSupplyCurrent();
    }

    private double getReferenceVoltage() throws ModbusException {
        // return 10.0;
        return getAcromagInputVoltage(Configuration.getReferenceVoltageChannel());
    }


    /**
     * Method that sets the voltage of the Acromag's output voltage channel corresponding to
     * the specified channelID. Actual addresses are hard coded in the AddressDictionary Class
     */
    private double getAcromagInputVoltage(int channelID) throws ModbusException{

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
    private void setAcromagOutputVoltage(int channelID, double voltage) throws ModbusException{

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
