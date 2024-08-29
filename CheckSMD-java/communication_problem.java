import com.fazecast.jSerialComm.SerialPort;
import java.util.Scanner;

public class MotorController {

    private SerialPort comPort;
    private MotorDriver driver;

    public MotorController(String portDescriptor, int baudRate) {
        comPort = SerialPort.getCommPort(portDescriptor);
        comPort.setBaudRate(baudRate);
        comPort.openPort();
        driver = new MotorDriver(0, comPort); // Assuming ID 0 for the motor driver
    }

    public void attachDriver() {
        // Assuming method to configure or reset the driver
        System.out.println("Driver attached: " + driver.getInfo());
        System.out.println("Scanning modules: " + driver.scanModules());
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to start.");
        scanner.nextLine();

        int i = 0;
        while (true) {
            System.out.println("Button 1: " + driver.getButtonState(1));
            System.out.println("Button 3: " + driver.getButtonState(3));
            System.out.println("Button 5: " + driver.getButtonState(5));

            driver.setRGB(1, (i == 0) ? 255 : 0, (i == 1) ? 255 : 0, (i == 2) ? 255 : 0);
            driver.setRGB(2, (i == 0) ? 255 : 0, (i == 1) ? 255 : 0, (i == 2) ? 255 : 0);
            driver.setRGB(5, (i == 0) ? 255 : 0, (i == 1) ? 255 : 0, (i == 2) ? 255 : 0);

            i = (i + 1) % 3;

            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Replace "COM3" with your actual serial port
        MotorController controller = new MotorController("COM3", 115200);
        controller.attachDriver();
        controller.run();
    }
}

class MotorDriver {
    private int id;
    private SerialPort port;

    public MotorDriver(int id, SerialPort port) {
        this.id = id;
        this.port = port;
    }

    public String getInfo() {
        // Simulate getting driver info
        return "Motor Driver ID: " + id;
    }

    public String scanModules() {
        // Simulate scanning modules
        return "Modules connected: Example Module";
    }

    public int getButtonState(int buttonId) {
        // Simulate getting button state
        return buttonId % 2; // Random state for demonstration
    }

    public void setRGB(int module, int red, int green, int blue) {
        // Simulate setting RGB color
        System.out.println("Setting RGB " + module + " to R:" + red + " G:" + green + " B:" + blue);
    }
}