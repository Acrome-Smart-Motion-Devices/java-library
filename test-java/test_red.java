import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class TestRed {

    Red device;
    
    @BeforeEach
    void setUp() {
        device = Mockito.mock(Red.class);
    }

    @Test
    void testPing() {
        when(device.ping()).thenReturn(true);
        assertTrue(device.ping());
        verify(device).ping();
    }

    @Test
    void testReboot() {
        doNothing().when(device).reboot();
        device.reboot();
        verify(device).reboot();
    }

    @Test
    void testEEPROMWrite() {
        doNothing().when(device).EEPROMWrite();
        device.EEPROMWrite();
        verify(device).EEPROMWrite();
    }
}

class TestMaster {

    Master master;
    Red mockDevice;

    @BeforeEach
    void setUp() {
        master = new Master("COM3", 115200);
        mockDevice = Mockito.mock(Red.class);
        master.attachDevice(mockDevice);
    }

    @Test
    void testPing() {
        when(mockDevice.ping()).thenReturn(true);
        assertTrue(master.pingDevice(1));
        verify(mockDevice).ping();
    }

    @Test
    void testReboot() {
        doNothing().when(mockDevice).reboot();
        master.rebootDevice(1);
        verify(mockDevice).reboot();
    }

    @Test
    void testUpdateBaudRate() {
        doNothing().when(mockDevice).setBaudRate(anyInt());
        master.updateDeviceBaudRate(1, 9600);
        verify(mockDevice).setBaudRate(9600);
    }
}