package stirling.software.SPDF.service.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;

import stirling.software.SPDF.service.scanner.ScannerHostGuard.Target;

class ScannerHostGuardTest {

    @Test
    void acceptsPrivateAddressWithDefaultPort() {
        Target target = ScannerHostGuard.resolve("192.168.1.50");

        assertEquals(80, target.port());
        assertEquals("http://192.168.1.50:80", target.baseUrl());
    }

    @Test
    void acceptsExplicitPort() {
        Target target = ScannerHostGuard.resolve("10.0.0.7:8080");

        assertEquals(8080, target.port());
    }

    @Test
    void rejectsLoopbackAndMetadataAddresses() {
        assertThrows(IllegalArgumentException.class, () -> ScannerHostGuard.resolve("127.0.0.1"));
        assertThrows(
                IllegalArgumentException.class, () -> ScannerHostGuard.resolve("169.254.169.254"));
    }

    @Test
    void rejectsPublicAddress() {
        assertThrows(IllegalArgumentException.class, () -> ScannerHostGuard.resolve("8.8.8.8"));
    }

    @Test
    void rejectsSchemesPathsAndBadPorts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerHostGuard.resolve("http://192.168.1.50"));
        assertThrows(
                IllegalArgumentException.class, () -> ScannerHostGuard.resolve("192.168.1.50/x"));
        assertThrows(
                IllegalArgumentException.class, () -> ScannerHostGuard.resolve("192.168.1.50:0"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerHostGuard.resolve("192.168.1.50:70000"));
    }

    @Test
    void rejectsMissingAddress() {
        assertThrows(IllegalArgumentException.class, () -> ScannerHostGuard.resolve(null));
    }

    @Test
    void treatsUniqueLocalIpv6AsPrivate() throws Exception {
        assertTrue(ScannerHostGuard.isPrivateLan(InetAddress.getByName("fd00::1")));
        assertFalse(ScannerHostGuard.isPrivateLan(InetAddress.getByName("2001:db8::1")));
    }
}
