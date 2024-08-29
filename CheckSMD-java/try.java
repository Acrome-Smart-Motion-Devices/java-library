import smd.red.Master;
import smd.red.Red;
import osModules.OSModules;

public class DeviceSetup {
    public static void main(String[] args) {
        // Initialize the serial port and master controller
        String port = OSModules.getUSBSerialPort();
        System.out.println("Port: " + port);
        Master master = new Master(port, 115200);

        // Attach device with ID 0
        Red device = new Red(0);
        master.attach(device);

        // Print driver information
        System.out.println("Driver Info: " + master.getDriverInfo(0));

        // Scan for modules and print the results
        System.out.println("Scanned Modules: " + master.scanModules(0));

        // Additional code based on Python comments
        /*
        master.setOperationMode(0, OperationMode.POSITION);
        master.enableTorque(0, true);

        master.goTo(0, 40000);
        */
    }
}