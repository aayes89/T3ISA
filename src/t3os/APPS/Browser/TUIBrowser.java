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
package t3os.APPS.Browser;

/**
 *
 * @author Slam
 */
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import t3isa.DEVICE.TKeyboardDevice;
import t3isa.NETWORKING.THostNetwork;
import t3os.APPS.Browser.Images.T3HtmlImage;
import t3os.APPS.Browser.Images.T3HtmlImageBMP;
import t3os.APPS.Browser.Images.T3HtmlImageDecoder;
import t3os.APPS.Browser.Images.T3HtmlImageGIF;
import t3os.APPS.Browser.Images.T3HtmlImagePNG;
import t3os.UI.TUI;
import t3os.UI.TUIElement;

/**
 * Navegador web de T3OS.
 *
 * Interfaz gráfica basada exclusivamente en TUI. El motor HTML se integrará de
 * forma independiente.
 *
 * @author Slam
 */
public final class TUIBrowser extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int TOOLBAR_HEIGHT = 32;
    private static final int STATUS_HEIGHT = 20;
    private int rendererX = Integer.MIN_VALUE;
    private int rendererY = Integer.MIN_VALUE;
    private int rendererWidth = -1;
    private int rendererHeight = -1;

    private static final int COLOR_TITLE = 0x00303030;
    private static final int COLOR_TOOLBAR = 0x00404040;
    private static final int COLOR_BACKGROUND = 0x00FFFFFF;
    private static final int COLOR_BORDER = 0x00808080;
    private static final int COLOR_TEXT = 0x00FFFFFF;
    private static final int COLOR_FIELD = 0x00080808;
    private static final int COLOR_BUTTON = 0x00505050;
    private static final int COLOR_DISABLED = 0x00707070;
    private static final int COLOR_STATUS = 0x00282828;
    private static final int COLOR_CURSOR = 0x00FFFFFF;

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLL_MIN_THUMB = 24;

    private static final int COLOR_SCROLL_TRACK = 0x00D0D0D0;
    private static final int COLOR_SCROLL_THUMB = 0x00606060;
    private static final int COLOR_SCROLL_BORDER = 0x00808080;

    private int scrollY;
    private int contentHeight;
    private boolean draggingScrollbar;
    private int scrollbarDragOffset;
    private boolean resizing = false;

    private int resizeStartX;
    private int resizeStartY;
    private int resizeStartWidth;
    private int resizeStartHeight;

    private static final int RESIZE_SIZE = 12;
    private static final int MIN_WIDTH = 360;
    private static final int MIN_HEIGHT = 240;

    private static final int BUTTON_WIDTH = 32;
    private static final int GO_WIDTH = 36;
    private static final int CLOSE_WIDTH = 24;

    private String address = "about:blank";
    private String status = "Listo";
    private String pageTitle = "T3Browser";

    private boolean addressFocused;
    private boolean dragging;
    private boolean closeRequested;

    private int dragOffsetX;
    private int dragOffsetY;
    private int addressCursor;

    private T3HtmlNode document;
    private T3HtmlRenderer htmlRenderer;
    private final Set<String> requestedImages = new HashSet<>();
    private final Map<String, T3HtmlImage> loadedImages = new ConcurrentHashMap<>();
    private volatile long navigationId = 0;
    private Button pressedButton = Button.NONE;

    String html0 = """
                <html>
                  <head>
                      <style>
                          body {   background-color: #ffffff; color: #202020; }
                          nav { display: flex; flex-direction: row; background-color: #2c2c2c; padding: 10px 15px; }
                          .nav-link { color: #ffffff; text-decoration: none; margin-right: 15px; }
                          .container { max-width: 1200px; margin: 0 auto; }
                          .float-right { float: right; }
                          .main-content { margin-right: 200px; }
                      </style>
                  </head>
                  <body>                      
                      <div class="container">
                          <div class="main-content">                              
                              <h1>DNS-Web Browser</h1>
                              <p>                                 
                                 Aplicación diseñada para navegar en el anonimato.<br>
                                 Utiliza técnicas de ofuscación avanzadas para proteger tus datos.<br>
                                 <h2>- Cómo usar -</h2>
                                 <h3>Navegación segura:</h3>     
                                 - Escriba en la barra superior la dirección que desea visualizar y presione ENTER.
                                 <h3>Navegación insegura:</h3>     
                                 - Escriba en la barra superior la dirección que desea visualizar y presione en el botón "Navegación sin protección".
                              </p>
                          </div>
                          <div class="float-right">                              
                          </div>
                      </div>
                   <br><br><br><br><br><br><br><br><br><br><br><br><br><br>
                      <footer>
                          Made by Slam 2024 All Rights Reserved &copy;
                      </footer>
                  </body>
                  </html>                  
                  """;

    private enum Button {
        NONE,
        BACK,
        FORWARD,
        RELOAD,
        GO,
        CLOSE
    }

    public TUIBrowser(int x, int y, int width, int height) {
        super(x, y, width, height);

        if (width < 360 || height < 240) {
            throw new IllegalArgumentException("TUIBrowser requiere al menos 360x240 píxeles");
        }

        addressCursor = address.length();
    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        drawWindow(ui);
        drawTitleBar(ui);
        drawToolbar(ui);
        drawContent(ui);
        drawStatusBar(ui);
        //testBMP("", ui);
        //testPNG("", ui);
        //testGIF("", ui);
    }

    private void drawWindow(TUI ui) {
        ui.fillRect(x, y, width, height, COLOR_BACKGROUND);
        ui.drawRect(x, y, width, height, COLOR_BORDER);
    }

    private void drawTitleBar(TUI ui) {
        ui.fillRect(x + 1, y + 1, width - 2, TITLE_HEIGHT, COLOR_TITLE);
        ui.drawText(pageTitle, x + 8, y + 7, COLOR_TEXT);

        int closeX = x + width - CLOSE_WIDTH;
        int closeY = y + 1;

        ui.fillRect(closeX, closeY, CLOSE_WIDTH - 1, TITLE_HEIGHT - 1, COLOR_BUTTON);

        int cx = closeX + 8;
        int cy = y + 7;

        ui.drawLine(cx, cy, cx + 7, cy + 7, COLOR_TEXT);
        ui.drawLine(cx + 7, cy, cx, cy + 7, COLOR_TEXT);
    }

    private void drawToolbar(TUI ui) {
        int toolbarY = y + TITLE_HEIGHT;

        ui.fillRect(x + 1, toolbarY, width - 2, TOOLBAR_HEIGHT, COLOR_TOOLBAR);

        int bx = x + 5;
        int by = toolbarY + 4;
        int bh = TOOLBAR_HEIGHT - 8;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, "<", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 3;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, ">", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 3;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, "R", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 6;

        int goX = x + width - GO_WIDTH - 5;
        int fieldX = bx;
        int fieldWidth = goX - fieldX - 4;

        if (fieldWidth < 40) {
            fieldWidth = 40;
        }

        ui.fillRect(fieldX, by, fieldWidth, bh, COLOR_FIELD);
        ui.drawRect(fieldX, by, fieldWidth, bh, addressFocused ? COLOR_TEXT : COLOR_BORDER);

        int textX = fieldX + 5;
        int textY = by + 8;

        ui.drawText(address, textX, textY, COLOR_TEXT);

        if (addressFocused) {
            int visibleCursor = Math.min(addressCursor, address.length());
            int cursorX = textX + visibleCursor * 8;

            int rightLimit = fieldX + fieldWidth - 3;
            if (cursorX <= rightLimit) {
                ui.drawLine(cursorX, by + 5, cursorX, by + bh - 5, COLOR_CURSOR);
            }
        }

        drawButton(ui, goX, by, GO_WIDTH, bh, "Ir", COLOR_BUTTON);
    }

    private void drawButton(TUI ui, int bx, int by, int bw, int bh, String label, int color) {
        ui.fillRect(bx, by, bw, bh, color);
        ui.drawRect(bx, by, bw, bh, COLOR_BORDER);

        int textX = bx + (bw - label.length() * 8) / 2;
        int textY = by + (bh - 16) / 2 + 4;

        ui.drawText(label, textX, textY, COLOR_TEXT);
    }

    private void drawContent(TUI ui) {
        int contentX = x + 2;
        int contentY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 1;
        int contentWidth = width - 4 - SCROLLBAR_WIDTH;
        int viewportHeight = height - TITLE_HEIGHT - TOOLBAR_HEIGHT - STATUS_HEIGHT - 3;

        ui.fillRect(contentX, contentY, contentWidth, viewportHeight, COLOR_BACKGROUND);

        // Cargar el HTML de prueba una sola vez.
        if (document == null) {
            T3HtmlParser parser = new T3HtmlParser(html0);
            document = parser.parse();

            pageTitle = "DNS-Web Browser";
            status = "Documento HTML cargado";
        }

        if (document == null) {
            ui.drawText("No hay documento HTML cargado", contentX + 8, contentY + 20, COLOR_TEXT);

            contentHeight = viewportHeight;
            scrollY = 0;
            return;
        }

        // Recrear el renderizador solo si cambia el área de contenido.
        if (htmlRenderer == null
                || rendererX != contentX
                || rendererY != contentY
                || rendererWidth != contentWidth
                || rendererHeight != viewportHeight) {

            htmlRenderer = new T3HtmlRenderer(ui, contentX, contentY, contentWidth, viewportHeight);

            rendererX = contentX;
            rendererY = contentY;
            rendererWidth = contentWidth;
            rendererHeight = viewportHeight;

            // Restaurar las imágenes al crear el renderizador.
            for (Map.Entry<String, T3HtmlImage> entry : loadedImages.entrySet()) {
                htmlRenderer.setImage(entry.getKey(), entry.getValue());
            }
        }

        // Obtener la altura total calculada por el renderizador.
        htmlRenderer.setScrollY(0);
        htmlRenderer.render(document);
        contentHeight = Math.max(viewportHeight, htmlRenderer.getContentHeight());

        // Limitar el desplazamiento al rango válido.
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        // Renderizar la página con el desplazamiento actual.
        htmlRenderer.setScrollY(scrollY);
        htmlRenderer.render(document);

        // Dibujar la barra de desplazamiento vertical.
        drawScrollbar(ui, contentX + contentWidth, contentY, SCROLLBAR_WIDTH, viewportHeight);
    }

    private void drawStatusBar(TUI ui) {
        int sy = y + height - STATUS_HEIGHT;

        ui.fillRect(x + 1, sy, width - 2, STATUS_HEIGHT - 1, COLOR_STATUS);
        ui.drawLine(x + 1, sy, x + width - 2, sy, COLOR_BORDER);
        ui.drawText(status, x + 7, sy + 3, COLOR_TEXT);
    }

    @Override
    public void mouseDown(int button, int mx, int my) {
        if (!visible || !enabled || button != 1) {
            return;
        }

        int localX = mx - x;
        int localY = my - y;

        pressedButton = Button.NONE;

        if (localY < 0 || localY >= height || localX < 0 || localX >= width) {
            addressFocused = false;
            return;
        }

        if (localY < TITLE_HEIGHT) {
            addressFocused = false;

            if (localX >= width - CLOSE_WIDTH) {
                pressedButton = Button.CLOSE;
                return;
            }

            dragging = true;
            dragOffsetX = localX;
            dragOffsetY = localY;
            return;
        }

        int toolbarY = TITLE_HEIGHT;

        if (localY >= toolbarY && localY < toolbarY + TOOLBAR_HEIGHT) {
            int by = toolbarY + 4;
            int bh = TOOLBAR_HEIGHT - 8;

            if (localY < by || localY >= by + bh) {
                addressFocused = false;
                return;
            }

            int bx = 5;

            if (localX >= bx && localX < bx + BUTTON_WIDTH) {
                addressFocused = false;
                pressedButton = Button.BACK;
                return;
            }

            bx += BUTTON_WIDTH + 3;

            if (localX >= bx && localX < bx + BUTTON_WIDTH) {
                addressFocused = false;
                pressedButton = Button.FORWARD;
                return;
            }

            bx += BUTTON_WIDTH + 3;

            if (localX >= bx && localX < bx + BUTTON_WIDTH) {
                addressFocused = false;
                pressedButton = Button.RELOAD;
                return;
            }

            bx += BUTTON_WIDTH + 6;

            int goX = width - GO_WIDTH - 5;

            if (localX >= goX && localX < goX + GO_WIDTH) {
                addressFocused = false;
                pressedButton = Button.GO;
                return;
            }

            if (localX >= bx && localX < goX) {
                addressFocused = true;

                int charPosition = (localX - bx - 5) / 8;
                addressCursor = Math.max(0, Math.min(charPosition, address.length()));
                return;
            }

            addressFocused = false;
            return;
        }
        int contentTop = TITLE_HEIGHT + TOOLBAR_HEIGHT + 1;
        int contentBottom = height - STATUS_HEIGHT - 2;
        int scrollbarX = width - 2 - SCROLLBAR_WIDTH;

        if (localY >= contentTop && localY < contentBottom && localX >= scrollbarX && localX < width - 2) {

            int trackHeight = contentBottom - contentTop;
            int thumbHeight = contentHeight <= trackHeight ? trackHeight : Math.max(SCROLL_MIN_THUMB, trackHeight * trackHeight / contentHeight);

            thumbHeight = Math.min(trackHeight, thumbHeight);

            int maxScroll = Math.max(0, contentHeight - trackHeight);
            int thumbRange = trackHeight - thumbHeight;

            int thumbY = contentTop;
            if (maxScroll > 0) {
                thumbY += (int) ((long) scrollY * thumbRange / maxScroll);
            }

            if (localY >= thumbY && localY < thumbY + thumbHeight) {
                draggingScrollbar = true;
                scrollbarDragOffset = localY - thumbY;
            } else {
                int targetThumbY = localY - thumbHeight / 2;
                targetThumbY = Math.max(contentTop, Math.min(targetThumbY, contentBottom - thumbHeight));

                scrollY = thumbRange > 0 ? (int) ((long) (targetThumbY - contentTop) * maxScroll / thumbRange) : 0;
            }

            addressFocused = false;
            return;
        }

        addressFocused = false;
    }

    @Override
    public void mouseMove(int mx, int my) {
        if (draggingScrollbar) {
            int contentTop = TITLE_HEIGHT + TOOLBAR_HEIGHT + 1;
            int contentBottom = height - STATUS_HEIGHT - 2;
            int trackHeight = contentBottom - contentTop;

            if (trackHeight <= 0) {
                return;
            }

            int thumbHeight = contentHeight <= trackHeight ? trackHeight : Math.max(SCROLL_MIN_THUMB, (int) ((long) trackHeight * trackHeight / Math.max(1, contentHeight)));

            thumbHeight = Math.min(trackHeight, thumbHeight);

            int thumbRange = trackHeight - thumbHeight;
            int maxScroll = Math.max(0, contentHeight - trackHeight);

            if (thumbRange <= 0 || maxScroll <= 0) {
                scrollY = 0;
                return;
            }

            int targetThumbY = my - y - scrollbarDragOffset;
            targetThumbY = Math.max(contentTop, Math.min(targetThumbY, contentBottom - thumbHeight));

            scrollY = (int) ((long) (targetThumbY - contentTop) * maxScroll / thumbRange);

            return;
        }

        if (dragging) {
            setPosition(mx - dragOffsetX, my - dragOffsetY);
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button != 1) {
            return;
        }

        dragging = false;
        draggingScrollbar = false;

        Button action = pressedButton;
        pressedButton = Button.NONE;

        switch (action) {
            case CLOSE:
                closeRequested = true;
                visible = false;
                break;

            case BACK:
                status = "Historial anterior pendiente";
                break;

            case FORWARD:
                status = "Historial siguiente pendiente";
                break;

            case RELOAD:
                navigate();
                break;

            case GO:
                navigate();
                break;

            case NONE:
            default:
                break;
        }
    }

    public void keyPressed(TKeyboardDevice.Key key) {
        if (key == null || !visible || !enabled || !addressFocused) {
            return;
        }

        int code = key.getCode();
        char ch = key.getCharacter();

        switch (code) {
            case KeyEvent.VK_ENTER:
                addressFocused = false;
                navigate();
                return;

            case KeyEvent.VK_ESCAPE:
                addressFocused = false;
                return;

            case KeyEvent.VK_BACK_SPACE:
                if (addressCursor > 0) {
                    address = address.substring(0, addressCursor - 1) + address.substring(addressCursor);
                    addressCursor--;
                }
                return;

            case KeyEvent.VK_DELETE:
                if (addressCursor < address.length()) {
                    address = address.substring(0, addressCursor) + address.substring(addressCursor + 1);
                }
                return;

            case KeyEvent.VK_LEFT:
                addressCursor = Math.max(0, addressCursor - 1);
                return;

            case KeyEvent.VK_RIGHT:
                addressCursor = Math.min(address.length(), addressCursor + 1);
                return;

            case KeyEvent.VK_HOME:
                addressCursor = 0;
                return;

            case KeyEvent.VK_END:
                addressCursor = address.length();
                return;

            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_ALT_GRAPH:
            case KeyEvent.VK_META:
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_INSERT:
                return;

            case KeyEvent.VK_SPACE:
                insertAddressCharacter(' ');
                return;

            default:
                break;
        }

        if (Character.isISOControl(ch) || Character.isWhitespace(ch)) {
            return;
        }

        insertAddressCharacter(ch);
    }

    private void insertAddressCharacter(char ch) {
        address = address.substring(0, addressCursor) + ch + address.substring(addressCursor);
        addressCursor++;
    }

    private void navigate() {
        String target = address.trim();

        if (target.isEmpty()) {
            status = "Introduce una dirección";
            return;
        }

        if (!target.matches("(?i)^https?://.*")
                && !"about:blank".equalsIgnoreCase(target)) {
            target = "http://" + target;
        }

        if ("about:blank".equalsIgnoreCase(target)) {
            synchronized (this) {
                ++navigationId;
                synchronized (requestedImages) {
                    requestedImages.clear();
                }
                loadedImages.clear();

                address = "about:blank";
                addressCursor = address.length();
                document = null;
                htmlRenderer = null;
                rendererX = Integer.MIN_VALUE;
                rendererY = Integer.MIN_VALUE;
                rendererWidth = -1;
                rendererHeight = -1;
                scrollY = 0;
                contentHeight = 0;
                pageTitle = "T3Browser";
                status = "Página en blanco";
            }
            return;
        }

        final URI uri;
        try {
            uri = URI.create(target);

            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))) {
                status = "URL no válida";
                return;
            }
        } catch (IllegalArgumentException ex) {
            status = "URL no válida";
            return;
        }

        final String url = uri.toString();
        final long currentNavigation;

        synchronized (this) {
            currentNavigation = ++navigationId;

            synchronized (requestedImages) {
                requestedImages.clear();
            }
            loadedImages.clear();

            address = url;
            addressCursor = address.length();
            status = "Descargando página...";
            pageTitle = "T3Browser";
        }

        Thread downloader = new Thread(() -> {
            try {
                String html = THostNetwork.wget(url);

                if (html == null || html.isBlank()) {
                    throw new IOException("La página no contiene HTML");
                }

                T3HtmlParser parser = new T3HtmlParser(html);
                T3HtmlNode parsedDocument = parser.parse();

                if (parsedDocument == null) {
                    throw new IOException("No se pudo interpretar el HTML");
                }

                synchronized (this) {
                    if (navigationId != currentNavigation) {
                        return;
                    }

                    document = parsedDocument;
                    htmlRenderer = null;

                    rendererX = Integer.MIN_VALUE;
                    rendererY = Integer.MIN_VALUE;
                    rendererWidth = -1;
                    rendererHeight = -1;

                    scrollY = 0;
                    contentHeight = 0;
                    pageTitle = uriTitle(url);
                    status = "Página cargada";
                }

                loadImages(parsedDocument, uri, currentNavigation);

            } catch (Exception ex) {
                synchronized (this) {
                    if (navigationId != currentNavigation) {
                        return;
                    }

                    status = "Error al cargar: " + ex.getMessage();
                }
            }
        }, "T3Browser-Wget");

        downloader.setDaemon(true);
        downloader.start();
    }

    private void loadImages(T3HtmlNode node, URI pageUri, long currentNavigation) {
        if (node == null || pageUri == null) {
            return;
        }

        if ("img".equalsIgnoreCase(node.getTagName())) {
            String src = node.getAttributes().get("src");

            if (src != null && !src.isBlank()) {
                try {
                    URI imageUri = pageUri.resolve(src.trim()).normalize();

                    String scheme = imageUri.getScheme();

                    // Solo descargar imágenes HTTP o HTTPS.
                    if (imageUri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                        requestImage(imageUri.toString(), currentNavigation);
                    }
                } catch (IllegalArgumentException ex) {
                    System.err.println("URL de imagen inválida: " + src);
                }
            }
        }

        for (T3HtmlNode child : node.getChildren()) {
            loadImages(child, pageUri, currentNavigation);
        }
    }

    private void requestImage(String imageUrl, long currentNavigation) {
        synchronized (requestedImages) {
            if (!requestedImages.add(imageUrl)) {
                return;
            }
        }

        Thread downloader = new Thread(() -> {
            try {
                byte[] data = THostNetwork.wgetBytes(imageUrl);

                if (data == null || data.length == 0) {
                    throw new IOException("La imagen está vacía");
                }

                T3HtmlImage image = T3HtmlImageDecoder.decode(data, imageUrl);

                synchronized (this) {
                    if (navigationId != currentNavigation) {
                        return;
                    }

                    loadedImages.put(imageUrl, image);

                    if (htmlRenderer != null) {
                        htmlRenderer.setImage(imageUrl, image);
                    }

                    status = "Página cargada";
                }

            } catch (Exception ex) {
                System.err.println("No se pudo cargar la imagen " + imageUrl + ": " + ex.getMessage());
            }
        }, "T3Browser-Image");

        downloader.setDaemon(true);
        downloader.start();
    }

    public void testBMP(String url, TUI ui) {
        try {
            //byte[] data = THostNetwork.wgetBytes(url);
            byte[] data = new byte[]{
                (byte) 0x42, (byte) 0x4D, (byte) 0xF6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x13, (byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x13, (byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, 0x00
            };

            T3HtmlImage image = T3HtmlImageBMP.decode(data);

            ui.drawImagePixels(x, y, image.getWidth(), image.getHeight(), image.getPixels());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void testPNG(String url, TUI ui) {
        try {
            //byte[] data = THostNetwork.wgetBytes(url);
            byte[] pngBytes = new byte[]{
                -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
                0, 0, 0, 8, 0, 0, 0, 8, 8, 2, 0, 0, 0, 75, 109, 41,
                -36, 0, 0, 0, 28, 73, 68, 65, 84, 120, -100, 99, -8, -1, -1,
                63, 3, 3, 3, 38, -55, -128, 85, -12, -1, -1, -1, 56, 68,
                7, 88, 7, 0, 93, 115, 95, -95, 45, 0, -2, -23, 0, 0, 0,
                0, 73, 69, 78, 68, -82, 66, 96, -126
            };

            T3HtmlImage image = T3HtmlImagePNG.decode(pngBytes);

            ui.drawImagePixels(x, y, image.getWidth(), image.getHeight(), image.getPixels());
        } catch (Exception ex) {
            System.err.println("Error PNG: " + ex.getMessage());
        }
    }

    public void testGIF(String url, TUI ui) {
        try {
            //byte[] data = THostNetwork.wgetBytes(url);
            byte[] gifBytes = new byte[]{
                71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1, 0, 0,
                0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68,
                1, 0, 59
            };

            T3HtmlImage image = T3HtmlImageGIF.decode(gifBytes);

            ui.drawImagePixels(x, y, image.getWidth(), image.getHeight(), image.getPixels());
        } catch (Exception ex) {
            System.err.println("Error GIF: " + ex.getMessage());
        }
    }

    private String uriTitle(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null ? host : "T3Browser";
        } catch (IllegalArgumentException ex) {
            return "T3Browser";
        }
    }

    public void refresh() {
        System.out.println("Refrescando");
    }

    // Auxiliar para dibujar el scrollbar
    private void drawScrollbar(TUI ui, int sx, int sy, int sw, int sh) {
        ui.fillRect(sx, sy, sw, sh, COLOR_SCROLL_TRACK);
        ui.drawRect(sx, sy, sw, sh, COLOR_SCROLL_BORDER);

        int maxScroll = Math.max(0, contentHeight - sh);
        int thumbHeight;

        if (contentHeight <= sh || contentHeight <= 0) {
            thumbHeight = sh;
        } else {
            thumbHeight = Math.max(SCROLL_MIN_THUMB, sh * sh / contentHeight);
            thumbHeight = Math.min(sh, thumbHeight);
        }

        int thumbRange = sh - thumbHeight;
        int thumbY = sy;

        if (maxScroll > 0) {
            thumbY += (int) ((long) scrollY * thumbRange / maxScroll);
        }

        ui.fillRect(sx + 2, thumbY, sw - 4, thumbHeight, COLOR_SCROLL_THUMB);
        ui.drawRect(sx + 2, thumbY, sw - 4, thumbHeight, COLOR_SCROLL_BORDER);
    }

    // Auxiliar para calcular contenido visible
    private int contentHeightVisible() {
        return Math.max(1, height - TITLE_HEIGHT - TOOLBAR_HEIGHT - STATUS_HEIGHT - 3);
    }

    public boolean hasAddressFocus() {
        return addressFocused;
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null) {
            return;
        }

        this.address = address;
        this.addressCursor = address.length();
    }
}
