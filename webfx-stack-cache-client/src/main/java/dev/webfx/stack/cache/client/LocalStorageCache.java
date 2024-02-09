package dev.webfx.stack.cache.client;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.storage.LocalStorage;
import dev.webfx.stack.cache.Cache;
import dev.webfx.stack.com.serial.SerialCodecManager;

/**
 * @author Bruno Salmon
 */
public class LocalStorageCache implements Cache {

    private static final String FORMAT = "json";
    private static final String SCALAR_KEY = "$SCALAR";

    @Override
    public void put(String key, Object value) {
        if (value == null) {
            LocalStorage.removeItem(key);
        } else {
            Object o = SerialCodecManager.encodeToJson(value);
            if (!(o instanceof ReadOnlyAstObject)) {
                o = AST.createObject().set(SCALAR_KEY, o);
            }
            LocalStorage.setItem(key, AST.formatObject((ReadOnlyAstObject) o, FORMAT));
        }
    }

    @Override
    public Object get(String key) {
        String item = LocalStorage.getItem(key);
        if (item == null) {
            return null;
        } else {
            ReadOnlyAstObject o = AST.parseObject(item, FORMAT);
            if (o.has(SCALAR_KEY))
                o = o.get(SCALAR_KEY);
            return SerialCodecManager.decodeFromJson(o);
        }
    }

    private static LocalStorageCache INSTANCE;

    public static LocalStorageCache get() {
        if (INSTANCE == null)
            INSTANCE = new LocalStorageCache();
        return INSTANCE;
    }

}
