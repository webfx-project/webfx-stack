package dev.webfx.stack.shareddata.storage;

import dev.webfx.stack.shareddata.LocalMap;
import dev.webfx.platform.storage.spi.StorageProvider;
import dev.webfx.platform.util.Strings;

/**
 * @author Bruno Salmon
 */
public final class StorageLocalMap implements LocalMap<String, String> {

    private final StorageProvider storageProvider;

    public StorageLocalMap(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public String get(Object key) {
        return storageProvider.getItem(Strings.toString(key));
    }

    @Override
    public String put(String key, String value) {
        String previousValue = get(key);
        storageProvider.setItem(key, value);
        return previousValue;
    }

    @Override
    public String remove(Object key) {
        String previousValue = get(key);
        storageProvider.removeItem(Strings.toString(key));
        return previousValue;
    }

    @Override
    public void clear() {
        storageProvider.clear();
    }

    @Override
    public int size() {
        return storageProvider.getLength();
    }

    @Override
    public boolean isEmpty() {
        return storageProvider.getLength() == 0;
    }

    @Override
    public void close() {
        clear();
    }
}
