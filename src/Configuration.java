import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by lahmann on 2017-01-17.
 */
public class Configuration {

    private static final File configFile = new File("../lib/config.cfg");

    private static String acromagIpAddress;
    private static Integer modbusPort;
    private static Integer pollPeriod;
    
    private static Integer referenceVoltageChannel;
    private static Integer voltageMonitorChannel;
    private static Integer currentMonitorChannel;
    
    private static Integer hvEnableChannel;
    private static Integer voltageControlChannel;
    private static Integer currentControlChannel;
    
    private static Double maxPowerSupplyVoltage;    // kV
    private static Double maxPowerSupplyCurrent;    // mA
    
    private static Integer mainWindowPosX;
    private static Integer mainWindowPosY;

    protected static void loadConfiguration(){

        try {
            Scanner s = new Scanner(configFile);
            s.useDelimiter(";|\\n");

            while (s.hasNext()){

                switch (s.next()) {
                    case "acromagIpAddress":
                        acromagIpAddress = s.next();
                        break;
                    case "modbusPort":
                        modbusPort = Integer.valueOf(s.next());
                        break;
                    case "pollPeriod":
                        pollPeriod = Integer.valueOf(s.next());
                        break;
                    case "referenceVoltageChannel":
                        referenceVoltageChannel = Integer.valueOf(s.next());
                        break;
                    case "voltageMonitorChannel":
                        voltageMonitorChannel = Integer.valueOf(s.next());
                        break;
                    case "currentMonitorChannel":
                        currentMonitorChannel = Integer.valueOf(s.next());
                        break;
                    case "hvEnableChannel":
                        hvEnableChannel = Integer.valueOf(s.next());
                        break;
                    case "voltageControlChannel":
                        voltageControlChannel = Integer.valueOf(s.next());
                        break;
                    case "currentControlChannel":
                        currentControlChannel = Integer.valueOf(s.next());
                        break;
                    case "maxPowerSupplyVoltage":
                        maxPowerSupplyVoltage = Double.valueOf(s.next());
                        break;
                    case "maxPowerSupplyCurrent":
                        maxPowerSupplyCurrent = Double.valueOf(s.next());
                        break;
                    case "mainWindowPosX":
                        mainWindowPosX = Integer.valueOf(s.next());
                        break;
                    case "mainWindowPosY":
                        mainWindowPosY = Integer.valueOf(s.next());
                        break;
                }
            }
        }

        catch (IOException exception){

            System.err.println("Program could not located the config file.");
            System.err.println("It was expected to be at " + configFile.getAbsolutePath());
            System.err.println("Closing...");
            System.exit(-1);

        }



    }

    protected static void writeConfiguration(){

        try {
            FileWriter w = new FileWriter(configFile);

            w.write("acromagIpAddress;" + acromagIpAddress);
            w.write("\nmodbusPort;" + modbusPort);
            w.write("\npollPeriod;" + pollPeriod);

            w.write("\nreferenceVoltageChannel;" + referenceVoltageChannel);
            w.write("\nvoltageMonitorChannel;" + voltageMonitorChannel);
            w.write("\ncurrentMonitorChannel;" + currentMonitorChannel);

            w.write("\nhvEnableChannel;" + hvEnableChannel);
            w.write("\nvoltageControlChannel;" + voltageControlChannel);
            w.write("\ncurrentControlChannel;" + currentControlChannel);

            w.write("\nmaxPowerSupplyVoltage;" + maxPowerSupplyVoltage);
            w.write("\nmaxPowerSupplyCurrent;" + maxPowerSupplyCurrent);

            w.write("\nmainWindowPosX;" + mainWindowPosX);
            w.write("\nmainWindowPosY;" + mainWindowPosY);

            w.close();
        }
        catch (IOException exception){

        }

    }

    protected static String getAcromagIpAddress() {
        return acromagIpAddress;
    }

    protected static Integer getModbusPort() {
        return modbusPort;
    }

    protected static Integer getPollPeriod() {
        return pollPeriod;
    }

    protected static Integer getReferenceVoltageChannel() {
        return referenceVoltageChannel;
    }

    protected static Integer getVoltageMonitorChannel() {
        return voltageMonitorChannel;
    }

    protected static Integer getCurrentMonitorChannel() {
        return currentMonitorChannel;
    }

    protected static Integer getHvEnableChannel() {
        return hvEnableChannel;
    }

    protected static Integer getVoltageControlChannel() {
        return voltageControlChannel;
    }

    protected static Integer getCurrentControlChannel() {
        return currentControlChannel;
    }

    protected static Double getMaxPowerSupplyVoltage() {
        return maxPowerSupplyVoltage;
    }

    protected static Double getMaxPowerSupplyCurrent() {
        return maxPowerSupplyCurrent;
    }

    protected static Integer getMainWindowPosX() {
        return mainWindowPosX;
    }

    protected static Integer getMainWindowPosY() {
        return mainWindowPosY;
    }

    public static void setAcromagIpAddress(String acromagIpAddress) {
        Configuration.acromagIpAddress = acromagIpAddress;
    }

    public static void setModbusPort(Integer modbusPort) {
        Configuration.modbusPort = modbusPort;
    }

    public static void setPollPeriod(Integer pollPeriod) {
        Configuration.pollPeriod = pollPeriod;
    }

    public static void setReferenceVoltageChannel(Integer referenceVoltageChannel) {
        Configuration.referenceVoltageChannel = referenceVoltageChannel;
    }

    public static void setVoltageMonitorChannel(Integer voltageMonitorChannel) {
        Configuration.voltageMonitorChannel = voltageMonitorChannel;
    }

    public static void setCurrentMonitorChannel(Integer currentMonitorChannel) {
        Configuration.currentMonitorChannel = currentMonitorChannel;
    }

    public static void setHvEnableChannel(Integer hvEnableChannel) {
        Configuration.hvEnableChannel = hvEnableChannel;
    }

    public static void setVoltageControlChannel(Integer voltageControlChannel) {
        Configuration.voltageControlChannel = voltageControlChannel;
    }

    public static void setCurrentControlChannel(Integer currentControlChannel) {
        Configuration.currentControlChannel = currentControlChannel;
    }

    public static void setMaxPowerSupplyVoltage(Double maxPowerSupplyVoltage) {
        Configuration.maxPowerSupplyVoltage = maxPowerSupplyVoltage;
    }

    public static void setMaxPowerSupplyCurrent(Double maxPowerSupplyCurrent) {
        Configuration.maxPowerSupplyCurrent = maxPowerSupplyCurrent;
    }

    public static void setMainWindowPosX(Integer mainWindowPosX) {
        Configuration.mainWindowPosX = mainWindowPosX;
    }

    public static void setMainWindowPosY(Integer mainWindowPosY) {
        Configuration.mainWindowPosY = mainWindowPosY;
    }
}
