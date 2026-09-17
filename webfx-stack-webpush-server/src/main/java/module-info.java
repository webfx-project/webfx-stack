// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Server-side Web Push (W3C) implementation: VAPID signing, payload
        encryption (RFC 8291), an outbound push client built on the webfx Fetch API,
        and a REST endpoint for SW-initiated subscription rotation. Platform-agnostic —
        Vert.x specifics are handled by the webfx-platform abstractions this module
        depends on. The DB-update operation behind the rotate endpoint is delegated
        to a `WebPushSubscriptionStore` SPI that the host application implements
        (typically using its ORM / EntityStore on whatever entity holds the
        subscription rows).
 */
module webfx.stack.webpush.server {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.web;
    requires org.bouncycastle.provider;
    requires web.push;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.service;
    requires webfx.platform.substitution;
    requires webfx.platform.util.vertx;
    requires webfx.stack.authn;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.routing.router;
    requires webfx.stack.routing.router.vertx;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.webpush;
    exports dev.webfx.stack.webpush.buscall;
    exports dev.webfx.stack.webpush.buscall.serial;
    exports dev.webfx.stack.webpush.handler;
    exports dev.webfx.stack.webpush.impl;
    exports dev.webfx.stack.webpush.rest;
    exports dev.webfx.stack.webpush.spi;

    // Used services
    uses dev.webfx.stack.webpush.spi.WebPushSubscriptionStore;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.webpush.rest.WebPushServerRestModuleBooter;
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.webpush.buscall.SendPushNotificationEndpoint, dev.webfx.stack.webpush.buscall.UnsubscribePushNotificationsEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.webpush.buscall.serial.SendPushNotificationArgumentSerialCodec, dev.webfx.stack.webpush.buscall.serial.SendPushNotificationResultSerialCodec, dev.webfx.stack.webpush.buscall.serial.UnsubscribePushNotificationsArgumentSerialCodec, dev.webfx.stack.webpush.buscall.serial.UnsubscribePushNotificationsResultSerialCodec;

}