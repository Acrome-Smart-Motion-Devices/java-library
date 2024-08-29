
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;

public enum Commands {
    PING(0x00),
    WRITE(0x01),
    WRITE_ACK(0x80 | 0x01),
    READ(0x02),
    EEPROM_WRITE(0x03),
    MODULE_SCAN(0x04),
    REBOOT(0x05),
    RESET_ENC(0x06),
    TUNE(0x07),
    HARD_RESET(0x17),
    ERROR_CLEAR(0x18),
    BL_JUMP(0x30),
    SYNC_WRITE(0x40 | 0x01),
    BULK_WRITE(0x20 | 0x01),
    BULK_READ(0x20 | 0x02),
    ACK(0x80),
    __EEPROM_WRITE_ACK(-1);

    private final int value;

    Commands(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class OperationMode {
    public static final int PWM = 0;
    public static final int Position = 1;
    public static final int Velocity = 2;
    public static final int Torque = 3;
}

public class MotorConstants {
    public static final double MAX_ACCEL = 999999.9999;
}

public enum Index {
    Header,
    DeviceID,
    DeviceFamily,
    PackageSize,
    Command,
    Status,
    HardwareVersion,
    SoftwareVersion,
    Baudrate,
    OperationMode,
    TorqueEnable,
    OutputShaftCPR,
    OutputShaftRPM,
    UserIndicator,
    MinimumPositionLimit,
    MaximumPositionLimit,
    TorqueLimit,
    VelocityLimit,
    PositionFF,
    VelocityFF,
    TorqueFF,
    PositionDeadband,
    VelocityDeadband,
    TorqueDeadband,
    PositionOutputLimit,
    VelocityOutputLimit,
    TorqueOutputLimit,
    PositionScalerGain,
    PositionPGain,
    PositionIGain,
    PositionDGain,
    VelocityScalerGain,
    VelocityPGain,
    VelocityIGain,
    VelocityDGain,
    TorqueScalerGain,
    TorquePGain,
    TorqueIGain,
    TorqueDGain,
    SetPosition,
    PositionControlMode,
    SCurveSetpoint,
    ScurveAccel,
    SCurveMaxVelocity,
    SCurveTime,
    SetVelocity,
    SetVelocityAcceleration,
    SetTorque,
    SetDutyCycle,
    SetScanModuleMode,
    SetManualBuzzer,
    SetManualServo,
    SetManualRGB,
    SetManualButton,
    SetManualLight,
    SetManualJoystick,
    SetManualDistance,
    SetManualQTR,
    SetManualPot,
    SetManualIMU,
    Buzzer_1,
    Buzzer_2,
    Buzzer_3,
    Buzzer_4,
    Buzzer_5,
    Servo_1,
    Servo_2,
    Servo_3,
    Servo_4,
    Servo_5,
    RGB_1,
    RGB_2,
    RGB_3,
    RGB_4,
    RGB_5,
    PresentPosition,
    PresentVelocity,
    MotorCurrent,
    AnalogPort,
    Button_1,
    Button_2,
    Button_3,
    Button_4,
    Button_5,
    Light_1,
    Light_2,
    Light_3,
    Light_4,
    Light_5,
    Joystick_1,
    Joystick_2,
    Joystick_3,
    Joystick_4,
    Joystick_5,
    Distance_1,
    Distance_2,
    Distance_3,
    Distance_4,
    Distance_5,
    QTR_1,
    QTR_2,
    QTR_3,
    QTR_4,
    QTR_5,
    Pot_1,
    Pot_2,
    Pot_3,
    Pot_4,
    Pot_5,
    IMU_1,
    IMU_2,
    IMU_3,
    IMU_4,
    IMU_5,
    connected_bitfield,
    CRCValue;
}

public class _Data {
    private Index index;
    private String type;
    private int size;
    private Object value;
    private boolean rw;

    public _Data(Index index, String varType, boolean rw, Object value) {
        this.index = index;
        this.type = varType;
        this.size = getSize(varType);
        this.value = value;
        this.rw = rw;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (rw) {
            if (type.length() > 1) {
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                this.value = buffer.put(((byte[]) value)).array();
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte) value);
                this.value = buffer.get(0);
            }
        }
    }

    public Index getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    private int getSize(String varType) {
        return switch (varType) {
            case "b", "B" -> 1;
            case "h", "H" -> 2;
            case "i", "I", "f" -> 4;
            case "d" -> 8;
            default -> 0;
        };
    }
}
