package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.conf.ConfigurationService;

/**
 * @author Bruno Salmon
 */
public class ConfigurationModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-conf";
    }

    @Override
    public int getBootLevel() {
        return CONF_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // We instantiate the configuration service provider, and call its boot method
        ConfigurationService.getProvider().boot();
    }

}
