import smd.red.Master;
import smd.red.Red;
import java.util.Scanner;

public class DeviceTest {
    private static final Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        // Initialize necessary components
        String port = OSModules.getUSBSerialPort();
        Master master = new Master(port, 115200);
        Red device = new Red(0);
        master.attach(device);

        // Starting
        System.out.print("CPR = ");
        double cpr = scanner.nextDouble();
        System.out.print("RPM = ");
        double rpm = scanner.nextDouble();

        // Set up the test conditions
        int id = 0;

        // Communication test
        boolean communicate = testCommunication(master, id);

        // EEPROM and reboot test
        boolean eeprom = testEEPROMAndReboot(master, id);

        // Factory reset test
        boolean factoryReset = testFactoryReset(master, id);

        // Motor control tests
        boolean motorRotation = testMotorRotation(master, id, cpr, rpm);
        boolean encoderRead = testEncoderRead(master, id);

        // Print results
        System.out.println("Communication: " + colorizeBoolean(communicate));
        System.out.println("EEPROM: " + colorizeBoolean(eeprom));
        System.out.println("Reboot: " + colorizeBoolean(eeprom));
        System.out.println("Factory Reset: " + colorizeBoolean(factoryReset));
        System.out.println("Motor Rotation: " + colorizeBoolean(motorRotation));
        System.out.println("Encoder Read: " + colorizeBoolean(encoderRead));

        // Keep program running until terminated
        while (true) {
            // Maybe some operation here
        }
    }

    private static boolean testCommunication(Master master, int id) {
        try {
            return master.getDriverInfo(id) != null;
        } catch (Exception e) {
            System.out.println("COMMUNICATION exception!");
            return false;
        }
    }

    private static boolean testEEPROMAndReboot(Master master, int id) {
        try {
            master.updateDriverId(id, 66);
            id = 66;
            master.attach(new Red(id));
            Thread.sleep(500);
            return master.getDriverInfo(id) != null;
        } catch (Exception e) {
            System.out.println("EEPROM, REBOOT exception!");
            return false;
        }
    }

    private static boolean testFactoryReset(Master master, int id) {
        try {
            master.factoryReset(id);
            Thread.sleep(500);
            return master.getDriverInfo(id) != null;
        } catch (Exception e) {
            System.out.println("FACTORY RESET exception!");
            return false;
        }
    }

    private static boolean testMotorRotation(Master master, int id, double cpr, double rpm) {
        try {
            master.setShaftCpr(id, cpr);
            master.setShaftRpm(id, rpm);
            master.setOperationMode(id, OperationMode.PWM);
            double initialPosition = master.getPosition(id);
            master.setDutyCycle(id, 90);
            master.enableTorque(id, true);
            Thread.sleep(1000);
            return master.getPosition(id) - initialPosition > (rpm / 120) * cpr;
        } catch (Exception e) {
            System.out.println("MOTOR ROTATION, ENCODER exception!");
            return false;
        }
    }

    private static boolean testEncoderRead(Master master, int id) {
        try {
            double initialPosition = master.getPosition(id);
            Thread.sleep(2000);
            return Math.abs(master.getPosition(id) - initialPosition) > 10;
        } catch (Exception e) {
            System.out.println("ENCODER READ exception!");
            return false;
        }
    }

    private static String colorizeBoolean(boolean value) {
        return value ? "True" : "False";
    }
}