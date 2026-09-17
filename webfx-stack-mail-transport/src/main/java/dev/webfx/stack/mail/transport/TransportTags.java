package dev.webfx.stack.mail.transport;

/**
 * Well-known keys for {@link TransportMessage#getTags()}. Tags are opaque provider hints:
 * the API never interprets them, each provider reads the ones it understands. Centralizing
 * the key names here keeps callers (e.g. the Modality mailer, which resolves the SMTP route
 * from per-account DB rows) free of compile dependencies on provider modules.
 *
 * @author Bruno Salmon
 */
public final class TransportTags {

    // SMTP route (read by the "smtp" provider; values come from the caller's per-account config/DB)
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_SSL = "smtp.ssl";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";

    private TransportTags() {}
}
