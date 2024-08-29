import com.fazecast.jSerialComm.SerialPort;

public class USBSerialPort {

    public static String getUSBSerialPort() {
        String osName = System.getProperty("os.name").toLowerCase();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            if (osName.startsWith("windows")) {
                if (port.getDescriptivePortName().contains("USB Serial Port")) {
                    return port.getSystemPortName();
                }
            } else if (osName.startsWith("linux")) {
                if (port.getSystemPortName().startsWith("/dev/ttyUSB")) {
                    return port.getSystemPortName();
                }
            }
        }

        return null;
    }
}