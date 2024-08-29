import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONArray;
import org.json.JSONObject;

class InvalidIndexError extends Exception {
    public InvalidIndexError(String message) {
        super(message);
    }
}

class UnsupportedHardware extends Exception {
    public UnsupportedHardware(String message) {
        super(message);
    }
}

class UnsupportedFirmware extends Exception {
    public UnsupportedFirmware(String message) {
        super(message);
    }
}

class Data {
    private int index;
    private char type;
    private boolean isConst;
    private Object value;

    public Data(int index, char type, boolean isConst, Object value) {
        this.index = index;
        this.type = type;
        this.isConst = isConst;
        this.value = value;
    }

    public Data(int index, char type) {
        this(index, type, false, null);
    }

    public int getIndex() {
        return index;
    }

    public char getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getSize() {
        // Return the size based on the type
        switch (this.type) {
            case 'B':
                return 1;
            case 'H':
                return 2;
            case 'I':
            case 'f':
                return 4;
            default:
                return 0;
        }
    }
}

enum Commands {
    WRITE, WRITE_ACK, READ, REBOOT, HARD_RESET, EEPROM_WRITE, EEPROM_WRITE_ACK, PING, RESET_ENC, TUNE, MODULE_SCAN, BL_JUMP
}

enum Index {
    Header, DeviceID, DeviceFamily, PackageSize, Command, Status, HardwareVersion, SoftwareVersion, Baudrate,
    OperationMode, TorqueEnable, OutputShaftCPR, OutputShaftRPM, UserIndicator, MinimumPositionLimit,
    MaximumPositionLimit, TorqueLimit, VelocityLimit, PositionFF, VelocityFF, TorqueFF, PositionDeadband,
    VelocityDeadband, TorqueDeadband, PositionOutputLimit, VelocityOutputLimit, TorqueOutputLimit,
    PositionScalerGain, PositionPGain, PositionIGain, PositionDGain, VelocityScalerGain, VelocityPGain,
    VelocityIGain, VelocityDGain, TorqueScalerGain, TorquePGain, TorqueIGain, TorqueDGain, SetPosition,
    PositionControlMode, SCurveSetpoint, ScurveAccel, SCurveMaxVelocity, SCurveTime, SetVelocity,
    SetVelocityAcceleration, SetTorque, SetDutyCycle, SetScanModuleMode, SetManualBuzzer, SetManualServo,
    SetManualRGB, SetManualButton, SetManualLight, SetManualJoystick, SetManualDistance, SetManualQTR,
    SetManualPot, SetManualIMU, Buzzer_1, Buzzer_2, Buzzer_3, Buzzer_4, Buzzer_5, Servo_1, Servo_2,
    Servo_3, Servo_4, Servo_5, RGB_1, RGB_2, RGB_3, RGB_4, RGB_5, PresentPosition, PresentVelocity,
    MotorCurrent, AnalogPort, Button_1, Button_2, Button_3, Button_4, Button_5, Light_1, Light_2,
    Light_3, Light_4, Light_5, Joystick_1, Joystick_2, Joystick_3, Joystick_4, Joystick_5, Distance_1,
    Distance_2, Distance_3, Distance_4, Distance_5, QTR_1, QTR_2, QTR_3, QTR_4, QTR_5, Pot_1, Pot_2, Pot_3,
    Pot_4, Pot_5, IMU_1, IMU_2, IMU_3, IMU_4, IMU_5, connected_bitfield, CRCValue
}

class Red {
    private static final byte _HEADER = 0x55;
    private static final byte _PRODUCT_TYPE = (byte) 0xBA;
    private static final int _PACKAGE_ESSENTIAL_SIZE = 6;

    private int __ack_size = 0;
    private Object _config = null;
    private File _fw_file = null;
    private List<Data> vars;

    public Red(int ID) throws Exception {
        if (ID > 255 || ID < 0) {
            throw new IllegalArgumentException("Device ID can not be higher than 254 or lower than 0!");
        }

        vars = new ArrayList<>();
        vars.add(new Data(Index.Header.ordinal(), 'B', false, (int) _HEADER));
        vars.add(new Data(Index.DeviceID.ordinal(), 'B'));
        vars.add(new Data(Index.DeviceFamily.ordinal(), 'B', false, (int) _PRODUCT_TYPE));
        vars.add(new Data(Index.PackageSize.ordinal(), 'B'));
        vars.add(new Data(Index.Command.ordinal(), 'B'));
        vars.add(new Data(Index.Status.ordinal(), 'B'));
        vars.add(new Data(Index.HardwareVersion.ordinal(), 'I'));
        vars.add(new Data(Index.SoftwareVersion.ordinal(), 'I'));
        vars.add(new Data(Index.Baudrate.ordinal(), 'I'));
        vars.add(new Data(Index.OperationMode.ordinal(), 'B'));
        vars.add(new Data(Index.TorqueEnable.ordinal(), 'B'));
        vars.add(new Data(Index.OutputShaftCPR.ordinal(), 'f'));
        vars.add(new Data(Index.OutputShaftRPM.ordinal(), 'f'));
        vars.add(new Data(Index.UserIndicator.ordinal(), 'B'));
        vars.add(new Data(Index.MinimumPositionLimit.ordinal(), 'i'));
        vars.add(new Data(Index.MaximumPositionLimit.ordinal(), 'i'));
        vars.add(new Data(Index.TorqueLimit.ordinal(), 'H'));
        vars.add(new Data(Index.VelocityLimit.ordinal(), 'H'));
        vars.add(new Data(Index.PositionFF.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityFF.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueFF.ordinal(), 'f'));
        vars.add(new Data(Index.PositionDeadband.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityDeadband.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueDeadband.ordinal(), 'f'));
        vars.add(new Data(Index.PositionOutputLimit.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityOutputLimit.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueOutputLimit.ordinal(), 'f'));
        vars.add(new Data(Index.PositionScalerGain.ordinal(), 'f'));
        vars.add(new Data(Index.PositionPGain.ordinal(), 'f'));
        vars.add(new Data(Index.PositionIGain.ordinal(), 'f'));
        vars.add(new Data(Index.PositionDGain.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityScalerGain.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityPGain.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityIGain.ordinal(), 'f'));
        vars.add(new Data(Index.VelocityDGain.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueScalerGain.ordinal(), 'f'));
        vars.add(new Data(Index.TorquePGain.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueIGain.ordinal(), 'f'));
        vars.add(new Data(Index.TorqueDGain.ordinal(), 'f'));
        vars.add(new Data(Index.SetPosition.ordinal(), 'f'));
        vars.add(new Data(Index.PositionControlMode.ordinal(), 'B'));
        vars.add(new Data(Index.SCurveSetpoint.ordinal(), 'f'));
        vars.add(new Data(Index.ScurveAccel.ordinal(), 'f'));
        vars.add(new Data(Index.SCurveMaxVelocity.ordinal(), 'f'));
        vars.add(new Data(Index.SCurveTime.ordinal(), 'f'));
        vars.add(new Data(Index.SetVelocity.ordinal(), 'f'));
        vars.add(new Data(Index.SetVelocityAcceleration.ordinal(), 'f'));
        vars.add(new Data(Index.SetTorque.ordinal(), 'f'));
        vars.add(new Data(Index.SetDutyCycle.ordinal(), 'f'));
        vars.add(new Data(Index.SetScanModuleMode.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualBuzzer.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualServo.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualRGB.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualButton.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualLight.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualJoystick.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualDistance.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualQTR.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualPot.ordinal(), 'B'));
        vars.add(new Data(Index.SetManualIMU.ordinal(), 'B'));
        vars.add(new Data(Index.Buzzer_1.ordinal(), 'i'));
        vars.add(new Data(Index.Buzzer_2.ordinal(), 'i'));
        vars.add(new Data(Index.Buzzer_3.ordinal(), 'i'));
        vars.add(new Data(Index.Buzzer_4.ordinal(), 'i'));
        vars.add(new Data(Index.Buzzer_5.ordinal(), 'i'));
        vars.add(new Data(Index.Servo_1.ordinal(), 'B'));
        vars.add(new Data(Index.Servo_2.ordinal(), 'B'));
        vars.add(new Data(Index.Servo_3.ordinal(), 'B'));
        vars.add(new Data(Index.Servo_4.ordinal(), 'B'));
        vars.add(new Data(Index.Servo_5.ordinal(), 'B'));
        vars.add(new Data(Index.RGB_1.ordinal(), 'i'));
        vars.add(new Data(Index.RGB_2.ordinal(), 'i'));
        vars.add(new Data(Index.RGB_3.ordinal(), 'i'));
        vars.add(new Data(Index.RGB_4.ordinal(), 'i'));
        vars.add(new Data(Index.RGB_5.ordinal(), 'i'));
        vars.add(new Data(Index.PresentPosition.ordinal(), 'f'));
        vars.add(new Data(Index.PresentVelocity.ordinal(), 'f'));
        vars.add(new Data(Index.MotorCurrent.ordinal(), 'f'));
        vars.add(new Data(Index.AnalogPort.ordinal(), 'H'));
        vars.add(new Data(Index.Button_1.ordinal(), 'B'));
        vars.add(new Data(Index.Button_2.ordinal(), 'B'));
        vars.add(new Data(Index.Button_3.ordinal(), 'B'));
        vars.add(new Data(Index.Button_4.ordinal(), 'B'));
        vars.add(new Data(Index.Button_5.ordinal(), 'B'));
        vars.add(new Data(Index.Light_1.ordinal(), 'H'));
        vars.add(new Data(Index.Light_2.ordinal(), 'H'));
        vars.add(new Data(Index.Light_3.ordinal(), 'H'));
        vars.add(new Data(Index.Light_4.ordinal(), 'H'));
        vars.add(new Data(Index.Light_5.ordinal(), 'H'));
        vars.add(new Data(Index.Joystick_1.ordinal(), 'i'));
        vars.add(new Data(Index.Joystick_2.ordinal(), 'i'));
        vars.add(new Data(Index.Joystick_3.ordinal(), 'i'));
        vars.add(new Data(Index.Joystick_4.ordinal(), 'i'));
        vars.add(new Data(Index.Joystick_5.ordinal(), 'i'));
        vars.add(new Data(Index.Distance_1.ordinal(), 'H'));
        vars.add(new Data(Index.Distance_2.ordinal(), 'H'));
        vars.add(new Data(Index.Distance_3.ordinal(), 'H'));
        vars.add(new Data(Index.Distance_4.ordinal(), 'H'));
        vars.add(new Data(Index.Distance_5.ordinal(), 'H'));
        vars.add(new Data(Index.QTR_1.ordinal(), 'i'));
        vars.add(new Data(Index.QTR_2.ordinal(), 'i'));
        vars.add(new Data(Index.QTR_3.ordinal(), 'i'));
        vars.add(new Data(Index.QTR_4.ordinal(), 'i'));
        vars.add(new Data(Index.QTR_5.ordinal(), 'i'));
        vars.add(new Data(Index.Pot_1.ordinal(), 'B'));
        vars.add(new Data(Index.Pot_2.ordinal(), 'B'));
        vars.add(new Data(Index.Pot_3.ordinal(), 'B'));
        vars.add(new Data(Index.Pot_4.ordinal(), 'B'));
        vars.add(new Data(Index.Pot_5.ordinal(), 'B'));
        vars.add(new Data(Index.IMU_1.ordinal(), 'f'));
        vars.add(new Data(Index.IMU_2.ordinal(), 'f'));
        vars.add(new Data(Index.IMU_3.ordinal(), 'f'));
        vars.add(new Data(Index.IMU_4.ordinal(), 'f'));
        vars.add(new Data(Index.IMU_5.ordinal(), 'f'));
        vars.add(new Data(Index.connected_bitfield.ordinal(), 'I'));
        vars.add(new Data(Index.CRCValue.ordinal(), 'I'));

        this.vars.get(Index.DeviceID.ordinal()).setValue(ID);
    }

    public int getAckSize() {
        return this.__ack_size;
    }

    public byte[] setVariables(List<Integer> indexList, List<Object> valueList, boolean ack) throws Exception {
        vars.get(Index.Command.ordinal()).setValue(ack ? Commands.WRITE_ACK : Commands.WRITE);

        StringBuilder fmtStr = new StringBuilder("<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat));
        for (int i = 0; i < indexList.size(); i++) {
            vars.get(indexList.get(i)).setValue(valueList.get(i));
            fmtStr.append("B").append(vars.get(indexList.get(i)).getType());
        }

        __ack_size = ByteBuffer.wrap(fmtStr.toString().getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit();

        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.toString().getBytes());

        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));

        CRC32 crc = new CRC32();
        crc.update(structOut.array());

        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] getVariables(List<Integer> indexList) throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.READ);

        StringBuilder fmtStr = new StringBuilder("<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat));
        fmtStr.append("B".repeat(indexList.size()));

        __ack_size = ByteBuffer.wrap(fmtStr.toString().getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit()
                + ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).limit()
                + ByteBuffer.wrap(indexList.stream().map(vars::get).map(Data::getType).reduce("", String::concat).getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit();

        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.toString().getBytes());

        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));

        CRC32 crc = new CRC32();
        crc.update(structOut.array());

        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] reboot() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.REBOOT);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = 0;

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] factoryReset() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.HARD_RESET);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = 0;

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] EEPROMWrite(boolean ack) throws Exception {
        vars.get(Index.Command.ordinal()).setValue(ack ? Commands.EEPROM_WRITE_ACK : Commands.EEPROM_WRITE);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = ByteBuffer.wrap(fmtStr.getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit() + vars.get(Index.CRCValue.ordinal()).getSize();

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] ping() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.PING);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = ByteBuffer.wrap(fmtStr.getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit() + vars.get(Index.CRCValue.ordinal()).getSize();

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] resetEncoder() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.RESET_ENC);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = ByteBuffer.wrap(fmtStr.getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit() + vars.get(Index.CRCValue.ordinal()).getSize();

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] tune() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.TUNE);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = 0;

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] scanModules() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.MODULE_SCAN);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = ByteBuffer.wrap(fmtStr.getBytes()).order(ByteOrder.LITTLE_ENDIAN).limit() + vars.get(Index.CRCValue.ordinal()).getSize();

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] enterBootloader() throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.BL_JUMP);
        String fmtStr = "<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat);
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.getBytes());
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());
        __ack_size = 0;

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }

    public byte[] updateDriverId(int id) throws Exception {
        vars.get(Index.Command.ordinal()).setValue(Commands.WRITE);
        StringBuilder fmtStr = new StringBuilder("<" + vars.subList(0, 6).stream().map(Data::getType).reduce("", String::concat) + "B" + vars.get(Index.DeviceID.ordinal()).getType());
        ByteBuffer structOut = ByteBuffer.allocate(fmtStr.length());
        structOut.put(fmtStr.toString().getBytes());
        structOut.putInt(Index.DeviceID.ordinal(), id);
        structOut.put(Index.PackageSize.ordinal(), (byte) (structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize()));
        CRC32 crc = new CRC32();
        crc.update(structOut.array());
        vars.get(Index.CRCValue.ordinal()).setValue(crc.getValue());

        ByteBuffer result = ByteBuffer.allocate(structOut.capacity() + vars.get(Index.CRCValue.ordinal()).getSize());
        result.put(structOut.array());
        result.put(ByteBuffer.allocate(vars.get(Index.CRCValue.ordinal()).getSize()).order(ByteOrder.LITTLE_ENDIAN).putInt((int) vars.get(Index.CRCValue.ordinal()).getValue()));

        return result.array();
    }
}

class Master {
    private static final int _BROADCAST_ID = 0xFF;
    private static final String __RELEASE_URL = "https://api.github.com/repos/Acrome-Smart-Motor-Driver/SMD-Red-Firmware/releases/{version}";

    private List<Integer> __attached_drivers;
    private List<Red> __driver_list;
    private int __baudrate;
    private double __post_sleep;
    private SerialPort __ph;

    public Master(String portname, int baudrate) throws Exception {
        if (baudrate > 12500000 || baudrate < 3053) {
            throw new IllegalArgumentException("Baudrate must be between 3.053 KBits/s and 12.5 MBits/s.");
        }

        __attached_drivers = new ArrayList<>();
        __driver_list = new ArrayList<>(256);
        for (int i = 0; i < 256; i++) {
            __driver_list.add(new Red(255));
        }

        __baudrate = baudrate;
        __post_sleep = (10.0 / baudrate) * 12;
        __ph = SerialPort.getCommPort(portname);
        __ph.setBaudRate(__baudrate);
        __ph.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
        __ph.openPort();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            __ph.closePort();
        } finally {
            super.finalize();
        }
    }

    private void __writeBus(byte[] data) {
        __ph.writeBytes(data, data.length);
    }

    private byte[] __readBus(int size) {
        byte[] buffer = new byte[size];
        __ph.readBytes(buffer, buffer.length);
        return buffer;
    }

    public List<Integer> attached() {
        return __attached_drivers;
    }

    public String getLatestFwVersion() throws IOException {
        URL url = new URL(__RELEASE_URL.replace("{version}", "latest"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject json = new JSONObject(response.toString());
        return json.getString("tag_name");
    }

    public boolean updateFwVersion(int id, String version) throws Exception {
        File fwFile = File.createTempFile("fw", ".bin");

        if (version.isEmpty()) {
            version = "latest";
        } else {
            version = "tags/" + version;
        }

        URL url = new URL(__RELEASE_URL.replace("{version}", version));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject json = new JSONObject(response.toString());
        JSONArray assets = json.getJSONArray("assets");

        String fwDlUrl = null;
        String md5DlUrl = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset.getString("name").endsWith(".bin")) {
                fwDlUrl = asset.getString("browser_download_url");
            } else if (asset.getString("name").endsWith(".md5")) {
                md5DlUrl = asset.getString("browser_download_url");
            }
        }

        if (fwDlUrl == null || md5DlUrl == null) {
            throw new Exception("Could not find requested firmware file! Check your connection to GitHub.");
        }

        URL fwUrl = new URL(fwDlUrl);
        connection = (HttpURLConnection) fwUrl.openConnection();
        connection.setRequestMethod("GET");
        Files.copy(connection.getInputStream(), Paths.get(fwFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);

        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = Files.readAllBytes(Paths.get(fwFile.getAbsolutePath()));
        String md5Fw = new String(md5Digest.digest(md5Bytes));

        URL md5Url = new URL(md5DlUrl);
        connection = (HttpURLConnection) md5Url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader md5In = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String md5Retrieved = md5In.readLine().split(" ")[0];
        md5In.close();

        if (!md5Fw.equals(md5Retrieved)) {
            throw new Exception("MD5 Mismatch!");
        }

        enterBootloader(id);
        Thread.sleep(100);

        // Upload binary
        stm32loader_main("-p", __ph.getSystemPortName(), "-b", "115200", "-e", "-w", "-v", fwFile.getAbsolutePath());

        fwFile.delete();

        return true;
    }

    public void updateDriverBaudrate(int id, int br) throws Exception {
        if (br < 3053 || br > 12500000) {
            throw new IllegalArgumentException(br + " is not in acceptable range!");
        }

        setVariables(id, List.of(new Object[]{Index.Baudrate.ordinal(), br}), false);
        Thread.sleep((long) __post_sleep);
        eepromWrite(id, false);
        Thread.sleep((long) __post_sleep);
        reboot(id);
    }

    public int getDriverBaudrate(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.Baudrate.ordinal())).get(0);
    }

    public void updateMasterBaudrate(int br) throws Exception {
        if (br < 3053 || br > 12500000) {
            throw new IllegalArgumentException(br + " is not in acceptable range!");
        }

        __ph.closePort();
        __ph.setBaudRate(br);
        __ph.openPort();

        __post_sleep = (10.0 / br) * 12;
    }

    public void attach(Red driver) {
        __driver_list.set(driver.vars.get(Index.DeviceID.ordinal()).getValue(), driver);
    }

    public void detach(int id) {
        if (id < 0 || id > 255) {
            throw new IllegalArgumentException(id + " is not a valid ID!");
        }
        __driver_list.set(id, new Red(255));
    }

    public List<Object> setVariables(int id, List<Object[]> idxValPairs, boolean ack) throws Exception {
        if (id < 0 || id > 255) {
            throw new IllegalArgumentException(id + " is not a valid ID!");
        }

        if (id != __driver_list.get(id).vars.get(Index.DeviceID.ordinal()).getValue()) {
            throw new IllegalArgumentException(id + " is not an attached ID!");
        }

        if (idxValPairs.isEmpty()) {
            throw new IndexOutOfBoundsException("Given id, value pair list is empty!");
        }

        List<Integer> indexList = new ArrayList<>();
        List<Object> valueList = new ArrayList<>();
        for (Object[] pair : idxValPairs) {
            indexList.add((Integer) pair[0]);
            valueList.add(pair[1]);
        }

        __writeBus(__driver_list.get(id).setVariables(indexList, valueList, ack));
        if (ack) {
            if (__readAck(id)) {
                List<Object> writtenValues = new ArrayList<>();
                for (int index : indexList) {
                    writtenValues.add(__driver_list.get(id).vars.get(index).getValue());
                }
                return writtenValues;
            }
        }
        Thread.sleep((long) __post_sleep);
        return null;
    }

    public List<Object> getVariables(int id, List<Integer> indexList) throws Exception {
        if (id < 0 || id > 254) {
            throw new IllegalArgumentException(id + " is not a valid ID!");
        }

        if (id == _BROADCAST_ID) {
            throw new IllegalArgumentException("Can't read with broadcast ID!");
        }

        if (id != __driver_list.get(id).vars.get(Index.DeviceID.ordinal()).getValue()) {
            throw new IllegalArgumentException(id + " is not an attached ID!");
        }

        if (indexList.isEmpty()) {
            throw new IndexOutOfBoundsException("Given index list is empty!");
        }

        __writeBus(__driver_list.get(id).getVariables(indexList));
        Thread.sleep((long) __post_sleep);
        if (__readAck(id)) {
            List<Object> readValues = new ArrayList<>();
            for (int index : indexList) {
                readValues.add(__driver_list.get(id).vars.get(index).getValue());
            }
            return readValues;
        }
        return null;
    }

    public void reboot(int id) throws Exception {
        __writeBus(__driver_list.get(id).reboot());
        Thread.sleep((long) __post_sleep);
    }

    public void factoryReset(int id) throws Exception {
        __writeBus(__driver_list.get(id).factoryReset());
        Thread.sleep((long) __post_sleep);
    }

    public void eepromWrite(int id, boolean ack) throws Exception {
        __writeBus(__driver_list.get(id).EEPROMWrite(ack));
        Thread.sleep((long) __post_sleep);

        if (ack) {
            if (!__readAck(id)) {
                throw new Exception("EEPROM write acknowledgment failed");
            }
        }
    }

    public boolean ping(int id) throws Exception {
        __writeBus(__driver_list.get(id).ping());
        Thread.sleep((long) __post_sleep);
        return __readAck(id);
    }

    public void resetEncoder(int id) throws Exception {
        __writeBus(__driver_list.get(id).resetEncoder());
        Thread.sleep((long) __post_sleep);
    }

    public List<String> scanModules(int id) throws Exception {
        List<Integer> _ID_OFFSETS = List.of(
            List.of(1, Index.Button_1.ordinal()),
            List.of(6, Index.Light_1.ordinal()),
            List.of(11, Index.Buzzer_1.ordinal()),
            List.of(16, Index.Joystick_1.ordinal()),
            List.of(21, Index.Distance_1.ordinal()),
            List.of(26, Index.QTR_1.ordinal()),
            List.of(31, Index.Servo_1.ordinal()),
            List.of(36, Index.Pot_1.ordinal()),
            List.of(41, Index.RGB_1.ordinal()),
            List.of(46, Index.IMU_1.ordinal())
        );

        __writeBus(__driver_list.get(id).scanModules());
        Thread.sleep(5500);

        long connected = 0;
        for (int i = 0; i < 10; i++) {
            List<Integer> values = getVariables(id, List.of(Index.connected_bitfield.ordinal()));
            if (values != null) {
                connected = ((long) values.get(1) << 32) | values.get(0);
                break;
            }
        }

        if (connected == 0) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (int addr = 0; addr < 64; addr++) {
            if ((connected & (1L << addr)) != 0) {
                result.add(Index.values()[addr - _ID_OFFSETS.get((addr - 1) / 5).get(0) + _ID_OFFSETS.get((addr - 1) / 5).get(1)].name());
            }
        }
        return result;
    }

    public void setConnectedModules(int id, List<String> modules) throws Exception {
        List<String> filteredModules = new ArrayList<>(new HashSet<>(modules));

        int ManualBuzzer_Byte = 0;
        int ManualServo_Byte = 0;
        int ManualRGB_Byte = 0;
        int ManualButton_Byte = 0;
        int ManualLight_Byte = 0;
        int ManualJoystick_Byte = 0;
        int ManualDistance_Byte = 0;
        int ManualQTR_Byte = 0;
        int ManualPot_Byte = 0;
        int ManualIMU_Byte = 0;

        try {
            for (String module : filteredModules) {
                int moduleId = Integer.parseInt(module.substring(module.lastIndexOf("_") + 1));
                if (moduleId < 1 || moduleId > 5) {
                    throw new IllegalArgumentException(module + " invalid module ID! it should be between 1 and 5. for ex: 'Buzzer_2'");
                }

                switch (module.substring(0, module.lastIndexOf("_"))) {
                    case "Buzzer":
                        ManualBuzzer_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Servo":
                        ManualServo_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "RGB":
                        ManualRGB_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Button":
                        ManualButton_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Light":
                        ManualLight_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Joystick":
                        ManualJoystick_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Distance":
                        ManualDistance_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "QTR":
                        ManualQTR_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "Pot":
                        ManualPot_Byte += Math.pow(2, moduleId - 1);
                        break;
                    case "IMU":
                        ManualIMU_Byte += Math.pow(2, moduleId - 1);
                        break;
                    default:
                        throw new IllegalArgumentException(module + " is not a Module with ID! for ex: 'Button_2'");
                }
            }
        } catch (Exception e) {
            throw new Exception("Error in setting connected modules", e);
        }

        setVariables(id, List.of(new Object[]{Index.SetScanModuleMode.ordinal(), 1}), false);

        setVariables(id, List.of(new Object[]{Index.SetManualBuzzer.ordinal(), ManualBuzzer_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualServo.ordinal(), ManualServo_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualRGB.ordinal(), ManualRGB_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualButton.ordinal(), ManualButton_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualLight.ordinal(), ManualLight_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualJoystick.ordinal(), ManualJoystick_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualDistance.ordinal(), ManualDistance_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualQTR.ordinal(), ManualQTR_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualPot.ordinal(), ManualPot_Byte}), false);
        setVariables(id, List.of(new Object[]{Index.SetManualIMU.ordinal(), ManualIMU_Byte}), false);

        __writeBus(__driver_list.get(id).scanModules());
        Thread.sleep((long) __post_sleep);
    }

    public void enterBootloader(int id) throws Exception {
        __writeBus(__driver_list.get(id).enterBootloader());
        Thread.sleep((long) __post_sleep);
    }

    public Map<String, String> getDriverInfo(int id) throws Exception {
        Map<String, String> st = new HashMap<>();
        List<Object> data = getVariables(id, List.of(Index.HardwareVersion.ordinal(), Index.SoftwareVersion.ordinal()));
        if (data != null) {
            st.put("HardwareVersion", String.format("v%d.%d.%d", ((int) data.get(0) >> 16) & 0xFF, ((int) data.get(0) >> 8) & 0xFF, (int) data.get(0) & 0xFF));
            st.put("SoftwareVersion", String.format("v%d.%d.%d", ((int) data.get(1) >> 16) & 0xFF, ((int) data.get(1) >> 8) & 0xFF, (int) data.get(1) & 0xFF));

            __driver_list.get(id)._config = st;
            return st;
        }
        return null;
    }

    public void updateDriverId(int id, int id_new) throws Exception {
        if (id < 0 || id > 254) {
            throw new IllegalArgumentException(id + " is not a valid ID!");
        }

        if (id_new < 0 || id_new > 254) {
            throw new IllegalArgumentException(id_new + " is not a valid ID argument!");
        }

        __writeBus(__driver_list.get(id).updateDriverId(id_new));
        Thread.sleep((long) __post_sleep);
        eepromWrite(id_new, false);
        Thread.sleep((long) __post_sleep);
        reboot(id);
    }

    public void enableTorque(int id, boolean en) throws Exception {
        setVariables(id, List.of(new Object[]{Index.TorqueEnable.ordinal(), en ? 1 : 0}), false);
        Thread.sleep((long) __post_sleep);
    }

    public void pidTuner(int id) throws Exception {
        __writeBus(__driver_list.get(id).tune());
        Thread.sleep((long) __post_sleep);
    }

    public void setOperationMode(int id, OperationMode mode) throws Exception {
        setVariables(id, List.of(new Object[]{Index.OperationMode.ordinal(), mode.ordinal()}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getOperationMode(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.OperationMode.ordinal())).get(0);
    }

    public void setShaftCpr(int id, float cpr) throws Exception {
        setVariables(id, List.of(new Object[]{Index.OutputShaftCPR.ordinal(), cpr}), false);
        Thread.sleep((long) __post_sleep);
    }

    public float getShaftCpr(int id) throws Exception {
        return (float) getVariables(id, List.of(Index.OutputShaftCPR.ordinal())).get(0);
    }

    public void setShaftRpm(int id, float rpm) throws Exception {
        setVariables(id, List.of(new Object[]{Index.OutputShaftRPM.ordinal(), rpm}), false);
        Thread.sleep((long) __post_sleep);
    }

    public float getShaftRpm(int id) throws Exception {
        return (float) getVariables(id, List.of(Index.OutputShaftRPM.ordinal())).get(0);
    }

    public void setUserIndicator(int id) throws Exception {
        setVariables(id, List.of(new Object[]{Index.UserIndicator.ordinal(), 1}), false);
        Thread.sleep((long) __post_sleep);
    }

    public void setPositionLimits(int id, int plmin, int plmax) throws Exception {
        setVariables(id, List.of(new Object[]{Index.MinimumPositionLimit.ordinal(), plmin}, new Object[]{Index.MaximumPositionLimit.ordinal(), plmax}), false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getPositionLimits(int id) throws Exception {
        return getVariables(id, List.of(Index.MinimumPositionLimit.ordinal(), Index.MaximumPositionLimit.ordinal()));
    }

    public void setTorqueLimit(int id, int tl) throws Exception {
        setVariables(id, List.of(new Object[]{Index.TorqueLimit.ordinal(), tl}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getTorqueLimit(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.TorqueLimit.ordinal())).get(0);
    }

    public void setVelocityLimit(int id, int vl) throws Exception {
        setVariables(id, List.of(new Object[]{Index.VelocityLimit.ordinal(), vl}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getVelocityLimit(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.VelocityLimit.ordinal())).get(0);
    }

    public void setPosition(int id, int sp) throws Exception {
        setVariables(id, List.of(new Object[]{Index.PositionControlMode.ordinal(), 0}, new Object[]{Index.SetPosition.ordinal(), sp}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getPosition(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.PresentPosition.ordinal())).get(0);
    }

    public void goTo(int id, int targetPosition, float time, float maxSpeed, float accel, boolean blocking, int encoderTickCloseCounter) throws Exception {
        setVariables(id, List.of(new Object[]{Index.PositionControlMode.ordinal(), 1}), false);
        setVariables(id, List.of(new Object[]{Index.SCurveTime.ordinal(), time}, new Object[]{Index.SCurveMaxVelocity.ordinal(), maxSpeed}, new Object[]{Index.ScurveAccel.ordinal(), accel}), false);
        setVariables(id, List.of(new Object[]{Index.SCurveSetpoint.ordinal(), targetPosition}), false);
        Thread.sleep((long) __post_sleep);

        while (blocking) {
            if (Math.abs(targetPosition - getPosition(id)) <= encoderTickCloseCounter) {
                break;
            }
        }
    }

    public void goToConstantSpeed(int id, int targetPosition, float speed, boolean blocking, int encoderTickCloseCounter) throws Exception {
        setVariables(id, List.of(new Object[]{Index.VelocityControlMode.ordinal(), 1}), false);
        setVariables(id, List.of(new Object[]{Index.SCurveMaxVelocity.ordinal(), speed}, new Object[]{Index.ScurveAccel.ordinal(), MotorConstants.MAX_ACCEL}), false);
        setVariables(id, List.of(new Object[]{Index.SCurveSetpoint.ordinal(), targetPosition}), false);
        Thread.sleep((long) __post_sleep);

        while (blocking) {
            if (Math.abs(targetPosition - getPosition(id)) <= encoderTickCloseCounter) {
                break;
            }
        }
    }

    public void setVelocity(int id, float sp, float accel) throws Exception {
        if (accel == MotorConstants.MAX_ACCEL) {
            accel = 0;
            setVariables(id, List.of(new Object[]{Index.SetVelocityAcceleration.ordinal(), accel}), false);
            setVariables(id, List.of(new Object[]{Index.SetVelocity.ordinal(), sp}), false);
        } else if (accel == 0) {
            setVariables(id, List.of(new Object[]{Index.SetVelocity.ordinal(), sp}), false);
        } else {
            setVariables(id, List.of(new Object[]{Index.SetVelocityAcceleration.ordinal(), accel}), false);
            setVariables(id, List.of(new Object[]{Index.SetVelocity.ordinal(), sp}), false);
        }
        Thread.sleep((long) __post_sleep);
    }

    public float getVelocity(int id) throws Exception {
        return (float) getVariables(id, List.of(Index.PresentVelocity.ordinal())).get(0);
    }

    public void setTorque(int id, float sp) throws Exception {
        setVariables(id, List.of(new Object[]{Index.SetTorque.ordinal(), sp}), false);
        Thread.sleep((long) __post_sleep);
    }

    public float getTorque(int id) throws Exception {
        return (float) getVariables(id, List.of(Index.MotorCurrent.ordinal())).get(0);
    }

    public void setDutyCycle(int id, float pct) throws Exception {
        setVariables(id, List.of(new Object[]{Index.SetDutyCycle.ordinal(), pct}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getAnalogPort(int id) throws Exception {
        return (int) getVariables(id, List.of(Index.AnalogPort.ordinal())).get(0);
    }

    public void setControlParametersPosition(int id, Float p, Float i, Float d, Float db, Float ff, Float ol) throws Exception {
        List<Integer> indexList = List.of(Index.PositionPGain.ordinal(), Index.PositionIGain.ordinal(), Index.PositionDGain.ordinal(), Index.PositionDeadband.ordinal(), Index.PositionFF.ordinal(), Index.PositionOutputLimit.ordinal());
        List<Float> valList = List.of(p, i, d, db, ff, ol);

        List<Object[]> params = new ArrayList<>();
        for (int j = 0; j < indexList.size(); j++) {
            if (valList.get(j) != null) {
                params.add(new Object[]{indexList.get(j), valList.get(j)});
            }
        }

        setVariables(id, params, false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getControlParametersPosition(int id) throws Exception {
        return getVariables(id, List.of(Index.PositionPGain.ordinal(), Index.PositionIGain.ordinal(), Index.PositionDGain.ordinal(), Index.PositionDeadband.ordinal(), Index.PositionFF.ordinal(), Index.PositionOutputLimit.ordinal()));
    }

    public void setControlParametersVelocity(int id, Float p, Float i, Float d, Float db, Float ff, Float ol) throws Exception {
        List<Integer> indexList = List.of(Index.VelocityPGain.ordinal(), Index.VelocityIGain.ordinal(), Index.VelocityDGain.ordinal(), Index.VelocityDeadband.ordinal(), Index.VelocityFF.ordinal(), Index.VelocityOutputLimit.ordinal());
        List<Float> valList = List.of(p, i, d, db, ff, ol);

        List<Object[]> params = new ArrayList<>();
        for (int j = 0; j < indexList.size(); j++) {
            if (valList.get(j) != null) {
                params.add(new Object[]{indexList.get(j), valList.get(j)});
            }
        }

        setVariables(id, params, false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getControlParametersVelocity(int id) throws Exception {
        return getVariables(id, List.of(Index.VelocityPGain.ordinal(), Index.VelocityIGain.ordinal(), Index.VelocityDGain.ordinal(), Index.VelocityDeadband.ordinal(), Index.VelocityFF.ordinal(), Index.VelocityOutputLimit.ordinal()));
    }

    public void setControlParametersTorque(int id, Float p, Float i, Float d, Float db, Float ff, Float ol) throws Exception {
        List<Integer> indexList = List.of(Index.TorquePGain.ordinal(), Index.TorqueIGain.ordinal(), Index.TorqueDGain.ordinal(), Index.TorqueDeadband.ordinal(), Index.TorqueFF.ordinal(), Index.TorqueOutputLimit.ordinal());
        List<Float> valList = List.of(p, i, d, db, ff, ol);

        List<Object[]> params = new ArrayList<>();
        for (int j = 0; j < indexList.size(); j++) {
            if (valList.get(j) != null) {
                params.add(new Object[]{indexList.get(j), valList.get(j)});
            }
        }

        setVariables(id, params, false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getControlParametersTorque(int id) throws Exception {
        return getVariables(id, List.of(Index.TorquePGain.ordinal(), Index.TorqueIGain.ordinal(), Index.TorqueDGain.ordinal(), Index.TorqueDeadband.ordinal(), Index.TorqueFF.ordinal(), Index.TorqueOutputLimit.ordinal()));
    }

    public int getButton(int id, int moduleId) throws Exception {
        int index = moduleId + Index.Button_1.ordinal() - 1;
        if (index < Index.Button_1.ordinal() || index > Index.Button_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for button module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (int) ret.get(0) : 0;
    }

    public int getLight(int id, int moduleId) throws Exception {
        int index = moduleId + Index.Light_1.ordinal() - 1;
        if (index < Index.Light_1.ordinal() || index > Index.Light_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for light module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (int) ret.get(0) : 0;
    }

    public void setBuzzer(int id, int moduleId, int noteFrequency) throws Exception {
        if (noteFrequency < 0) {
            throw new InvalidIndexError("Note frequency cannot be negative!");
        }

        int index = moduleId + Index.Buzzer_1.ordinal() - 1;
        if (index < Index.Buzzer_1.ordinal() || index > Index.Buzzer_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for buzzer module");
        }
        setVariables(id, List.of(new Object[]{index, noteFrequency}), false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getJoystick(int id, int moduleId) throws Exception {
        int index = moduleId + Index.Joystick_1.ordinal() - 1;
        if (index < Index.Joystick_1.ordinal() || index > Index.Joystick_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for joystick module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (List<Object>) ret.get(0) : null;
    }

    public int getDistance(int id, int moduleId) throws Exception {
        int index = moduleId + Index.Distance_1.ordinal() - 1;
        if (index < Index.Distance_1.ordinal() || index > Index.Distance_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for distance module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (int) ret.get(0) : 0;
    }

    public List<Boolean> getQtr(int id, int moduleId) throws Exception {
        int index = moduleId + Index.QTR_1.ordinal() - 1;
        if (index < Index.QTR_1.ordinal() || index > Index.QTR_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for QTR module");
        }

        List<Object> data = getVariables(id, List.of(index));
        if (data != null) {
            return List.of(
                    (data.get(0) instanceof Integer && ((int) data.get(0) & 1) != 0),
                    (data.get(0) instanceof Integer && ((int) data.get(0) & 2) != 0),
                    (data.get(0) instanceof Integer && ((int) data.get(0) & 4) != 0)
            );
        }
        return null;
    }

    public void setServo(int id, int moduleId, int val) throws Exception {
        if (val < 0 || val > 255) {
            throw new IllegalArgumentException("Value should be in range [0, 255]");
        }

        int index = moduleId + Index.Servo_1.ordinal() - 1;
        if (index < Index.Servo_1.ordinal() || index > Index.Servo_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for servo module");
        }

        setVariables(id, List.of(new Object[]{index, val}), false);
        Thread.sleep((long) __post_sleep);
    }

    public int getPotentiometer(int id, int moduleId) throws Exception {
        int index = moduleId + Index.Pot_1.ordinal() - 1;
        if (index < Index.Pot_1.ordinal() || index > Index.Pot_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for potentiometer module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (int) ret.get(0) : 0;
    }

    public void setRgb(int id, int moduleId, int red, int green, int blue) throws Exception {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("RGB color values must be in range 0 - 255");
        }

        int colorRgb = red + green * (int) Math.pow(2, 8) + blue * (int) Math.pow(2, 16);

        int index = moduleId + Index.RGB_1.ordinal() - 1;
        if (index < Index.RGB_1.ordinal() || index > Index.RGB_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for RGB module");
        }

        setVariables(id, List.of(new Object[]{index, colorRgb}), false);
        Thread.sleep((long) __post_sleep);
    }

    public List<Object> getImu(int id, int moduleId) throws Exception {
        int index = moduleId + Index.IMU_1.ordinal() - 1;
        if (index < Index.IMU_1.ordinal() || index > Index.IMU_5.ordinal()) {
            throw new InvalidIndexError("Invalid index for IMU module");
        }

        List<Object> ret = getVariables(id, List.of(index));
        return ret != null ? (List<Object>) ret.get(0) : null;
    }
}