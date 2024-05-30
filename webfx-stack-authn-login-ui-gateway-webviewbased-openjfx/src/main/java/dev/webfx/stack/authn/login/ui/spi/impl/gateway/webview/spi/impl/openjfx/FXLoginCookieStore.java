package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx;

import dev.webfx.platform.storagelocation.StorageLocation;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public class FXLoginCookieStore implements CookieStore {

    protected final Path cookieFilePath = Paths.get(StorageLocation.getInternalStorageLocation(), "login-cookies.json");
    private final CookieStore memoryStore = new CookieManager().getCookieStore();

    public FXLoginCookieStore() {
        try {
            // Reading the json file as text
            String jsonText = new String(Files.readAllBytes(cookieFilePath));
            // Parsing it to get a json object
            ReadOnlyAstObject json = Json.parseObject(jsonText);
            // Getting all its keys (each key correspond to an uri)
            ReadOnlyAstArray keys = json.keys();
            // Iterating all keys
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.getElement(i); // Getting the key as a string
                URI uri = URI.create(key); // transforming it into an uri
                // Asking the serial coded manager to decode the key value, which is an array of cookies
                Object[] httpCookies = SerialCodecManager.decodeFromJson(json.get(key));
                // Adding all these cookies to the memory store (unless they have expired)
                for (Object c : httpCookies) {
                    HttpCookie httpCookie = (HttpCookie) c;
                    if (!httpCookie.hasExpired())
                        memoryStore.add(uri, httpCookie);
                }
            }
        } catch (FileNotFoundException | NoSuchFileException e) {
            // That's normal to happen on first time, so no trace
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            AstObject json = AST.createObject();
            // For each effective uri, we put a json array of all serialized cookies
            getURIs().stream().map(this::getEffectiveURI).forEach(uri ->
                    json.setArray(uri.toASCIIString(), SerialCodecManager.encodeJavaArrayToAstArray(get(uri).toArray())));
            // We store that into the json file
            Files.writeString(cookieFilePath, Json.formatNode(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private URI getEffectiveURI(URI uri) {
        try {
            return new URI("https", uri.getHost(),null,null, null);
        } catch (URISyntaxException ignored) {
            return uri;
        }
    }

    private static final Map<String, Long> WHEN_CREATED = new HashMap<>();

    static long getWhenCreated(HttpCookie cookie) {
        Long whenCreated = WHEN_CREATED.get(cookie.getName());
        return whenCreated != null ? whenCreated : setWhenCreated(cookie);
    }

    static long setWhenCreated(HttpCookie cookie) {
        long now = System.currentTimeMillis();
        WHEN_CREATED.put(cookie.getName(), now);
        return now;
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        memoryStore.add(uri, cookie);
        setWhenCreated(cookie);
        save();
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return memoryStore.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies() {
        return memoryStore.getCookies();
    }

    @Override
    public List<URI> getURIs() {
        return memoryStore.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        boolean removed = memoryStore.remove(uri, cookie);
        if (removed)
            save();
        return removed;
    }

    @Override
    public boolean removeAll() {
        boolean removed = memoryStore.removeAll();
        if (removed)
            save();
        return removed;
    }
}
