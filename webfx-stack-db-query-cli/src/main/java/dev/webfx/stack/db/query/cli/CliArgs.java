package dev.webfx.stack.db.query.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A declared-options command line, parsed with no dependency and no annotations.
 *
 * <h3>Why not picocli, which the design note named</h3>
 *
 * <p>Because adding it would cost more than it saves, and the cost is the kind this repository has already
 * paid twice. A new third-party library in {@code webfx-stack-fork} means a {@code <library>} entry, a package
 * mapping and a version — and {@code webfx update} rewrites every pom in the repository from those
 * declarations, having twice dropped a hand-written dependency pin on the way through (once silently
 * un-pinning a Jackson artifact that only broke at runtime, on one code path). Against that: three
 * subcommands and nine options, which is this file.
 *
 * <p>If the tool ever grows the kind of surface a framework earns — nested subcommands, completion, generated
 * manuals — swapping this out is a local change, because nothing outside the parsing itself knows it is here.
 *
 * <h3>What it refuses</h3>
 *
 * <p>Unknown options and missing values, by name, rather than ignoring them. An instrument whose
 * {@code --audiance} typo is silently dropped runs the other arm and says it passed; that is precisely the
 * failure shape the audience assertion exists for, and the command line is where it would start.
 *
 * @author Claude Code
 */
public final class CliArgs {

    /** What a command accepts. Declared, so anything else is an error rather than a shrug. */
    public static final class Spec {
        private final Set<String> flags = new LinkedHashSet<>();
        private final Set<String> options = new LinkedHashSet<>();
        private final Set<String> repeatable = new LinkedHashSet<>();

        /** An option that takes no value: {@code --rows}. */
        public Spec flag(String name) {
            flags.add(name);
            return this;
        }

        /** An option that takes a value: {@code --limit 100}, {@code --limit=100}, {@code -c "select ..."}. */
        public Spec option(String name) {
            options.add(name);
            return this;
        }

        /**
         * An option that may be given more than once: {@code --param 1857 --param 6}.
         *
         * <p>Separate from {@link #option} so that a repeat of something that should appear once — a second
         * {@code --audience}, say — is still a mistake somebody is told about, rather than a last-one-wins.
         */
        public Spec repeatable(String name) {
            repeatable.add(name);
            return this;
        }

        public CliArgs parse(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            Map<String, List<String>> repeats = new LinkedHashMap<>();
            Set<String> set = new LinkedHashSet<>();
            List<String> positionals = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg == null)
                    continue;
                if (!arg.startsWith("-") || "-".equals(arg)) {
                    positionals.add(arg);
                    continue;
                }
                String name = arg.startsWith("--") ? arg.substring(2) : arg.substring(1);
                String inlineValue = null;
                int equals = name.indexOf('=');
                if (equals >= 0) {
                    inlineValue = name.substring(equals + 1);
                    name = name.substring(0, equals);
                }
                if (flags.contains(name)) {
                    if (inlineValue != null)
                        throw new IllegalArgumentException("--" + name + " takes no value, but '" + inlineValue + "' was given.");
                    set.add(name);
                } else if (options.contains(name) || repeatable.contains(name)) {
                    String value = inlineValue;
                    if (value == null) {
                        if (i + 1 >= args.length)
                            throw new IllegalArgumentException("--" + name + " needs a value.");
                        value = args[++i];
                    }
                    if (repeatable.contains(name))
                        repeats.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
                    else if (values.put(name, value) != null)
                        throw new IllegalArgumentException("--" + name + " was given more than once.");
                } else {
                    throw new IllegalArgumentException("Unknown option '" + arg + "'. Accepted here: "
                                                       + describe() + ".");
                }
            }
            return new CliArgs(values, repeats, set, positionals);
        }

        private String describe() {
            List<String> all = new ArrayList<>();
            for (String flag : flags)
                all.add("--" + flag);
            for (String option : options)
                all.add("--" + option + " <value>");
            for (String option : repeatable)
                all.add("--" + option + " <value> (repeatable)");
            return all.isEmpty() ? "(none)" : String.join(", ", all);
        }
    }

    private final Map<String, String> values;
    private final Map<String, List<String>> repeats;
    private final Set<String> flags;
    private final List<String> positionals;

    private CliArgs(Map<String, String> values, Map<String, List<String>> repeats, Set<String> flags,
                    List<String> positionals) {
        this.values = values;
        this.repeats = repeats;
        this.flags = flags;
        this.positionals = positionals;
    }

    /** Every value given for a repeatable option, in the order they were given. */
    public List<String> values(String name) {
        List<String> given = repeats.get(name);
        return given == null ? List.of() : List.copyOf(given);
    }

    public boolean flag(String name) {
        return flags.contains(name);
    }

    public String value(String name, String fallback) {
        String value = values.get(name);
        return value == null ? fallback : value;
    }

    /** A value that has no sensible default, so its absence is the operator's to fix. */
    public String required(String name, String why) {
        String value = values.get(name);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("--" + name + " is required: " + why);
        return value;
    }

    public int intValue(String name, int fallback) {
        String value = values.get(name);
        if (value == null)
            return fallback;
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " must be a whole number, not '" + value + "'.");
        }
    }

    public List<String> positionals() {
        return positionals;
    }

    public String positional(int index, String fallback) {
        return index < positionals.size() ? positionals.get(index) : fallback;
    }
}
