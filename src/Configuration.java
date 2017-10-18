import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by lahmann on 2017-01-17.
 */
class Configuration {

    private static final File configFile = new File("./lib/config.cfg");
    //private static final File configFile = new File("/leia/HVPowerSupplyController/lib/config.cfg");

    private static String acromagIpAddress = "192.168.100.57";
    private static Integer modbusPort = 502;
    private static Integer pollPeriod = 1000;
    
    private static Integer referenceVoltageChannel = 8;
    private static Integer voltageMonitorChannel = 1;
    private static Integer currentMonitorChannel = 2;
    
    private static Integer hvEnableChannel = 0;
    private static Integer voltageControlChannel = 1;
    private static Integer currentControlChannel = 2;
    
    private static Double maxPowerSupplyVoltage = 50.0;     // kV
    private static Double maxPowerSupplyCurrent = 1.5;      // mA
    
    private static Integer mainWindowPosX = 100;
    private static Integer mainWindowPosY = 100;

    static void loadConfiguration(){

        try {
            Scanner s = new Scanner(configFile);
            s.useDelimiter(";|\\n");

            while (s.hasNext()) {

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
        catch (FileNotFoundException e){
            /**
             * If we can't find the file, we'll just use the hard coded defaults
             */
        }

    }

    static void writeConfiguration(){

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

    static String getAcromagIpAddress() {
        return acromagIpAddress;
    }

    static Integer getModbusPort() {
        return modbusPort;
    }

    static Integer getPollPeriod() {
        return pollPeriod;
    }

    static Integer getReferenceVoltageChannel() {
        return referenceVoltageChannel;
    }

    static Integer getVoltageMonitorChannel() {
        return voltageMonitorChannel;
    }

    static Integer getCurrentMonitorChannel() {
        return currentMonitorChannel;
    }

    static Integer getHvEnableChannel() {
        return hvEnableChannel;
    }

    static Integer getVoltageControlChannel() {
        return voltageControlChannel;
    }

    static Integer getCurrentControlChannel() {
        return currentControlChannel;
    }

    static Double getMaxPowerSupplyVoltage() {
        return maxPowerSupplyVoltage;
    }

    static Double getMaxPowerSupplyCurrent() {
        return maxPowerSupplyCurrent;
    }

    static Integer getMainWindowPosX() {
        return mainWindowPosX;
    }

    static Integer getMainWindowPosY() {
        return mainWindowPosY;
    }

    static void setAcromagIpAddress(String acromagIpAddress) {
        Configuration.acromagIpAddress = acromagIpAddress;
    }

    static void setModbusPort(Integer modbusPort) {
        Configuration.modbusPort = modbusPort;
    }

    static void setPollPeriod(Integer pollPeriod) {
        Configuration.pollPeriod = pollPeriod;
    }

    static void setReferenceVoltageChannel(Integer referenceVoltageChannel) {
        Configuration.referenceVoltageChannel = referenceVoltageChannel;
    }

    static void setVoltageMonitorChannel(Integer voltageMonitorChannel) {
        Configuration.voltageMonitorChannel = voltageMonitorChannel;
    }

    static void setCurrentMonitorChannel(Integer currentMonitorChannel) {
        Configuration.currentMonitorChannel = currentMonitorChannel;
    }

    static void setHvEnableChannel(Integer hvEnableChannel) {
        Configuration.hvEnableChannel = hvEnableChannel;
    }

    static void setVoltageControlChannel(Integer voltageControlChannel) {
        Configuration.voltageControlChannel = voltageControlChannel;
    }

    static void setCurrentControlChannel(Integer currentControlChannel) {
        Configuration.currentControlChannel = currentControlChannel;
    }

    static void setMaxPowerSupplyVoltage(Double maxPowerSupplyVoltage) {
        Configuration.maxPowerSupplyVoltage = maxPowerSupplyVoltage;
    }

    static void setMaxPowerSupplyCurrent(Double maxPowerSupplyCurrent) {
        Configuration.maxPowerSupplyCurrent = maxPowerSupplyCurrent;
    }

    static void setMainWindowPosX(Integer mainWindowPosX) {
        Configuration.mainWindowPosX = mainWindowPosX;
    }

    static void setMainWindowPosY(Integer mainWindowPosY) {
        Configuration.mainWindowPosY = mainWindowPosY;
    }
}
