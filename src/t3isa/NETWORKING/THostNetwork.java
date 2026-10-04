/*
 * The MIT License
 *
 * Copyright 2026 Slam.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package t3isa.NETWORKING;

/**
 *
 * @author Slam
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// Clase para funcionalidades de redes
public final class THostNetwork {

    private THostNetwork() {
    }

    public static List<NetworkInterface> getInterfaces() throws IOException {
        List<NetworkInterface> result = new ArrayList<>();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces != null && interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp()) {
                result.add(ni);
            }
        }

        return result;
    }

    public static NetworkInterface getInterface(String name) throws IOException {

        NetworkInterface ni = NetworkInterface.getByName(name);

        if (ni == null) {
            throw new IOException("Interfaz no encontrada: " + name);
        }

        return ni;
    }

    public static String getMAC(NetworkInterface ni) {

        if (ni == null) {
            return null;
        }

        try {
            byte[] mac = ni.getHardwareAddress();

            if (mac == null || mac.length == 0) {
                return null;
            }

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < mac.length; i++) {
                if (i > 0) {
                    result.append(':');
                }
                result.append(String.format("%02X", mac[i] & 0xFF));
            }
            return result.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static List<String> getAddresses(NetworkInterface ni) {
        List<String> result = new ArrayList<>();

        if (ni == null) {
            return result;
        }

        ni.getInterfaceAddresses().forEach(address -> {
            InetAddress inet = address.getAddress();
            if (inet != null) {
                result.add(inet.getHostAddress());
            }
        });

        return result;
    }

    public static String resolve(String host) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        return address.getHostAddress();
    }

    public static InetAddress[] resolveAll(String host) throws IOException {
        return InetAddress.getAllByName(host);
    }

    public static String whois(String host, String server, int port) throws IOException {
        try (Socket socket = new Socket(server, port)) {
            socket.setSoTimeout(10000);

            socket.getOutputStream().write((host + "\r\n").getBytes());
            socket.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }

            return result.toString();
        }
    }

    public static String wget(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
