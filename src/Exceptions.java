class Exceptions {


    static class AcromagConnectionException extends Exception {
        AcromagConnectionException(String ipAddress){
            super(String.format("Failed to establish a connection with Acromag at %s", ipAddress));
        }
    }

    static class BadReferenceVoltageException extends Exception {
        BadReferenceVoltageException(int referenceChannel, double referenceVoltage) {
            super(String.format("Ref voltage (from channel %d) is %.2f V, which is too low", referenceChannel, referenceVoltage));
        }
    }

    static class WriteOutputVoltageException extends Exception {
        WriteOutputVoltageException(int channel, double voltage, int address) {
            super(String.format("Failed to write value of %.2f V to output channel %d (0x%04X)", voltage, channel, address));
        }
    }

    static class ReadInputVoltageException extends Exception {
        ReadInputVoltageException(int channel, int address) {
            super(String.format("Failed trying to read value from input channel %d (0x%04X)", channel, address));
        }
    }

    static class InconsistentReadingsException extends Exception {
        InconsistentReadingsException(double voltageReading, double voltageSetting){
            super(String.format("Inconsistency between voltage reading (%.2f V) and voltage setting (%.2f V)", voltageReading, voltageSetting));
        }
    }
}
