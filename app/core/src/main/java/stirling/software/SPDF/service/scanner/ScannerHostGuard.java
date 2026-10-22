package stirling.software.SPDF.service.scanner;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides which hosts the scanner client may contact. The server makes the request on the caller's
 * behalf, so without this a caller could aim it at loopback or at cloud metadata addresses. Only
 * private LAN addresses pass.
 *
 * <p>The returned {@link Target} carries the address that was checked, and callers must connect to
 * it rather than resolving the name again, or a second lookup could return a different address.
 */
public final class ScannerHostGuard {

    private static final int DEFAULT_PORT = 80;

    private static final Pattern HOST_AND_PORT =
            Pattern.compile("^([A-Za-z0-9.-]+|\\[[0-9A-Fa-f:]+\\])(?::(\\d{1,5}))?$");

    private ScannerHostGuard() {}

    /** A checked scanner address. */
    public record Target(InetAddress address, int port) {

        /** Root of the eSCL service on this scanner, with no trailing slash. */
        public String baseUrl() {
            String host = address.getHostAddress();
            if (address.getAddress().length == 16) {
                host = "[" + host + "]";
            }
            return "http://" + host + ":" + port;
        }
    }

    /**
     * Parses {@code host} or {@code host:port} and checks it.
     *
     * @throws IllegalArgumentException if the value is malformed, does not resolve, or resolves to
     *     anything other than a private LAN address
     */
    public static Target resolve(String hostAndPort) {
        if (hostAndPort == null) {
            throw new IllegalArgumentException("Scanner address is required");
        }
        Matcher matcher = HOST_AND_PORT.matcher(hostAndPort.strip());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Scanner address must be a host name or IP address, optionally with a port");
        }

        String host = matcher.group(1);
        int port = matcher.group(2) == null ? DEFAULT_PORT : Integer.parseInt(matcher.group(2));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Scanner port must be between 1 and 65535");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host.replaceAll("^\\[|]$", ""));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Scanner address does not resolve: " + host, e);
        }
        for (InetAddress address : addresses) {
            if (!isPrivateLan(address)) {
                throw new IllegalArgumentException(
                        "Scanner address must be on a private network: " + host);
            }
        }
        return new Target(addresses[0], port);
    }

    static boolean isPrivateLan(InetAddress address) {
        if (address.isSiteLocalAddress()) {
            return true;
        }
        // IPv6 unique local addresses, fc00::/7, which Java does not classify as site-local
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}
