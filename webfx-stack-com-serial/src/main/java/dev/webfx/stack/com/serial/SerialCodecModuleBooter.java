package dev.webfx.stack.com.serial;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.serial.spi.SerialCodec;
import dev.webfx.platform.util.collection.Collections;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class SerialCodecModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-platform-shared-serial";
    }

    @Override
    public int getBootLevel() {
        return SERIAL_CODEC_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        StringBuilder sb = new StringBuilder();
        List<SerialCodec> serialCodecs = Collections.listOf(ServiceLoader.load(SerialCodec.class));
        for (SerialCodec serialCodec : serialCodecs) {
            SerialCodecManager.registerSerialCodec(serialCodec);
            sb.append(sb.length() == 0 ? serialCodecs.size() + " serial codecs provided for: " : ", ").append(serialCodec.getJavaClass().getSimpleName());
        }
        Console.log(sb);
    }
}
