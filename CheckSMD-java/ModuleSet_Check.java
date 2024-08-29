import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class MotorTest {
    private static Master m;
    private static int modulesID = 1;
    private static boolean[] moduleChecks = new boolean[10]; // Button, Light, Buzzer, Joystick, Distance, QTR, Servo, Potentiometer, RGB, IMU

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String port = USBSerialPort.getUSBSerialPort(); // Implement USBSerialPort to handle OS-based port selection
        m = new Master(port);

        m.attach(new Red(0));
        List<String> modulesList = m.scanModules(0);
        System.out.println(modulesList);

        checkModules(modulesList);

        System.out.println("\n\n CHECK TABLE \n\n");
        printModuleStatus();

        System.out.println("Press Enter to start...");
        scanner.nextLine();

        clearScreen();

        int buttonCnt = 0;
        while (true) {
            int button = m.getButton(0, modulesID);
            int light = m.getLight(0, modulesID);
            int distance = m.getDistance(0, modulesID);
            int joystick = m.getJoystick(0, modulesID);
            List<Boolean> qtr = m.getQTR(0, modulesID);
            int pot = m.getPotentiometer(0, modulesID);
            List<Float> imu = m.getIMU(0, modulesID);

            if (button == 1) {
                buttonCnt++;
                if (buttonCnt == 5) buttonCnt = 0;
            }

            int qtrToServo = qtrToServoPosition(qtr);

            m.setBuzzer(0, modulesID, (button == 1) ? distance * 10 : 0);
            handleRGB(button);
            m.setServo(0, modulesID, qtrToServo);

            printSensorData(button, light, distance, joystick, qtr, pot, imu);
            clearScreen();
        }
    }

    private static void checkModules(List<String> modulesList) {
        moduleChecks[0] = modulesList.contains("Button_1");
        moduleChecks[1] = modulesList.contains("Light_1");
        moduleChecks[2] = modulesList.contains("Buzzer_1");
        moduleChecks[3] = modulesList.contains("Joystick_1");
        moduleChecks[4] = modulesList.contains("Distance_1");
        moduleChecks[5] = modulesList.contains("QTR_1");
        moduleChecks[6] = modulesList.contains("Servo_1");
        moduleChecks[7] = modulesList.contains("Pot_1");
        moduleChecks[8] = modulesList.contains("RGB_1");
        moduleChecks[9] = modulesList.contains("IMU_1");
    }

    private static void printModuleStatus() {
        String[] moduleNames = {"Button", "Light", "Buzzer", "Joystick", "Distance", "QTR", "Servo", "Potentiometer", "RGB", "IMU"};
        for (int i = 0; i < moduleChecks.length; i++) {
            System.out.println(moduleNames[i] + ":\t\t" + (moduleChecks[i] ? "True" : "False"));
        }
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static int qtrToServoPosition(List<Boolean> qtr) {
        if (qtr.size() < 3) return 0;
        return (qtr.get(0) ? 30 : 0) + (qtr.get(1) ? 60 : 0) + (qtr.get(2) ? 90 : 0);
    }

    private static void handleRGB(int button) {
        if (button == 1) {
            m.setRGB(0, modulesID, 255, 0, 0);
        } else if (button == 2) {
            m.setRGB(0, modulesID, 0, 255, 0);
        } else if (button == 3) {
            m.setRGB(0, modulesID, 0, 0, 255);
        } else if (button == 4) {
            m.setRGB(0, modulesID, 255, 255, 255);
        } else {
            m.setRGB(0, modulesID, 0, 0, 0);
        }
    }

    private static void printSensorData(int button, int light, int distance, int joystick, List<Boolean> qtr, int pot, List<Float> imu) {
        System.out.println("Button: " + button);
        System.out.println("Light: " + light);
        System.out.println("Distance: " + distance);
        System.out.println("Joystick: " + joystick);
        System.out.println("QTR: " + qtr);
        System.out.println("Potentiometer: " + pot);
        System.out.println("IMU: " + imu);
    }
}