
/**
 * Static class that simply hold the memory addresses of the ES2152 Acromag
 * See pages 73-92 of the user manual for reference
 */
class Constants {

    private static Double POWER_SUPPLY_MAX_VOLTAGE = 50.0;  // kV
    private static Double POWER_SUPPLY_MAX_CURRENT = 1.5;   // mA

    private static Double LASER_DIODE_ON_VOLTAGE  = 4.0;    // V
    private static Double LASER_DIODE_MAX_CURRENT = 20.0;   // mA
    private static Double VOLTAGE_PER_LD_CURRENT  = 0.01;   // V per mA

    private final static int[] INPUT_CHANNEL_CONFIG_ADDRESS = {
            0x0003, 0x0004, 0x0005, 0x0006,
            0x0007, 0x0008, 0x0009, 0x000A,
            0x000B, 0x000C, 0x000D, 0x000E,
            0x000F, 0x0010, 0x0011, 0x0012
    };

    private final static int[] INPUT_CHANNEL_DATA_ADDRESS = {
            0x0033, 0x0034, 0x0035, 0x0036,
            0x0037, 0x0038, 0x0039, 0x003A,
            0x003B, 0x003C, 0x003D, 0x003E,
            0x003F, 0x0040, 0x0041, 0x0042};

    private final static int[] OUTPUT_CHANNEL_CONFIG_ADDRESS = {
            0x0020, 0x0021, 0x0022, 0x0023,
            0x0024, 0x0025, 0x0026, 0x0027,
            0x0028, 0x0029, 0x002A, 0x002B,
            0x002C, 0x002D, 0x002E, 0x002F
    };

    private final static int[] OUTPUT_CHANNEL_DATA_ADDRESS = {
            0x015E, 0x015F, 0x0160, 0x0161,
            0x0162, 0x0163, 0x0164, 0x0165,
            0x0166, 0x0167, 0x0168, 0x0169,
            0x016A, 0x016B, 0x016C, 0x016D
    };

    static Double getPowerSupplyMaxVoltage() {
        return POWER_SUPPLY_MAX_VOLTAGE;
    }

    static Double getPowerSupplyMaxCurrent() {
        return POWER_SUPPLY_MAX_CURRENT;
    }

    static Double getLaserDiodeOnVoltage() {
        return LASER_DIODE_ON_VOLTAGE;
    }

    static Double getLaserDiodeMaxCurrent() {
        return LASER_DIODE_MAX_CURRENT;
    }

    static Double getVoltagePerLdCurrent() {
        return VOLTAGE_PER_LD_CURRENT;
    }

    static int getInputChannelConfigAddress(int channel) {
        return INPUT_CHANNEL_CONFIG_ADDRESS[channel];
    }

    static int getInputChannelDataAddress(int channel) {
        return INPUT_CHANNEL_DATA_ADDRESS[channel];
    }

    static int getOutputChannelConfigAddress(int channel) {
        return OUTPUT_CHANNEL_CONFIG_ADDRESS[channel];
    }

    static int getOutputChannelDataAddress(int channel) {
        return OUTPUT_CHANNEL_DATA_ADDRESS[channel];
    }
}
