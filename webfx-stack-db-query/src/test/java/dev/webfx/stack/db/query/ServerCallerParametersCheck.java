package dev.webfx.stack.db.query;

import dev.webfx.platform.console.spi.ConsoleProvider;
import dev.webfx.platform.service.SingleServiceProvider;

import java.util.Arrays;

/**
 * That a caller's identity reaches the query as a parameter value, and only when it is wanted.
 *
 * <p>The case worth reading is the last one. A push subscription is keyed by its QueryArgument, so if two
 * callers running the same statement produced equal arguments they would share one subscription and one
 * would be served the other's rows. Putting the value in the parameters — where equality can see it — is
 * what makes them separate, and this asserts it rather than trusting that it stayed true.
 *
 * <p>The identity case matters almost as much in the other direction: attaching to a statement that uses no
 * caller term would make every argument caller-specific and give every viewer of a shared query a stream of
 * their own, which is deduplication gone and push with it.
 */
public class ServerCallerParametersCheck {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        // The guard REPORTS every refusal through Console, and with no provider registered that logging throws
        // a ServiceConfigurationError which masks whatever the check was actually asserting. A silent provider
        // lets the assertions through. (This was hidden until now behind an earlier failure in this same main.)
        SingleServiceProvider.registerServiceProvider(ConsoleProvider.class, new ConsoleProvider() {
            @Override public void log(String message) { }
            @Override public void error(String message, Throwable error) { }
        });

        // Object, not a chain of ints: the conditional expression would otherwise be typed int and unbox the
        // null answer for every reserved name this resolver does not supply.
        ServerCallerParameters.registerResolver(name ->
            "caller.person".equals(name) ? (Object) 101 : "caller.account".equals(name) ? (Object) 202 : null);

        QueryArgument plain = argument("select id from Document d where event=$1", 7);
        check("a statement naming no caller term is untouched", ServerCallerParameters.attach(plain) == plain);

        QueryArgument using = argument("select id from Document d where person=CALLER_PERSON and event=$1", 7);
        QueryArgument attached = ServerCallerParameters.attach(using);
        // Every reserved name is attached, including the ones this resolver does not answer: a name with a null
        // value is bound as null and matches nothing, where a name left off entirely would be a term the
        // binding seam cannot resolve and would refuse the whole statement by name.
        check("the caller's own values are appended, in reserved-name order",
              Arrays.equals(attached.getParameters(), new Object[]{7, 101, 202, null, null}));
        check("and named, so the binding seam can find them",
              Arrays.equals(attached.getParameterNames(), new String[]{"caller.person", "caller.account",
                  "caller.grantedEvents", "caller.grantedOrganizations"}));
        check("the caller's positional values are left where they were",
              attached.getParameters()[0].equals(7));

        // Over-attaching costs a value nobody binds; under-attaching would be a term with no value, so the
        // text test is deliberately the loose one.
        QueryArgument mentions = argument("select id from Document d where ref='CALLER_PERSON'", 7);
        check("a literal mentioning the term attaches too, which is the safe direction",
              ServerCallerParameters.attach(mentions) != mentions);

        // THE property the design rests on.
        ServerCallerParameters.registerResolver(name -> "caller.person".equals(name) ? (Object) 999 : null);
        QueryArgument otherCaller = ServerCallerParameters.attach(
            argument("select id from Document d where person=CALLER_PERSON and event=$1", 7));
        check("two callers running the same statement are NOT equal arguments",
              !attached.equals(otherCaller));
        check("and do not collide in a hash map, which is how subscriptions are keyed",
              attached.hashCode() != otherCaller.hashCode());

        // ── A client naming a server-supplied value itself ──
        // attach APPENDS the server's values after the client's names, and the binding takes the FIRST value
        // with a given name — so a forged name would be bound in place of the server's. Shown here on the
        // attached argument, then refused at the guard, which every client query passes before attach.
        QueryArgument forged = QueryArgument.builder().setDataSourceId("ds").setLanguage("DQL")
                .setStatement("select id from Document d where person=CALLER_PERSON")
                .setParameters(999).setParameterNames("caller.person").build();
        QueryArgument forgedAttached = ServerCallerParameters.attach(forged);
        int firstMatch = Arrays.asList(forgedAttached.getParameterNames()).indexOf("caller.person");
        check("without the guard, the FIRST caller.person bound would be the client's forged 999 (why it is refused)",
              Integer.valueOf(999).equals(forgedAttached.getParameters()[firstMatch]));
        check("the guard refuses a client query naming caller.person",
              ClientQueryGuard.SERVER_PARAMETER_REFUSED.equals(ClientQueryGuard.refusalReason(forged)));
        check("whatever the statement says — the attach decision is a text test the client controls",
              ClientQueryGuard.SERVER_PARAMETER_REFUSED.equals(ClientQueryGuard.refusalReason(
                  QueryArgument.builder().copy(forged).setStatement("select id from Event e").build())));
        check("and any caller.* name, including terms registered later",
              ClientQueryGuard.SERVER_PARAMETER_REFUSED.equals(ClientQueryGuard.refusalReason(
                  QueryArgument.builder().copy(forged).setParameterNames("caller.somethingNew").build())));
        check("a client's ordinary names still pass",
              ClientQueryGuard.refusalReason(QueryArgument.builder().copy(forged).setParameterNames("eventId").build()) == null);
        check("as do unnamed parameters", ClientQueryGuard.refusalReason(plain) == null);

        // ── THE SCOPED CALLER, where the text test stops being sound ─────────────────────────────────────
        // A data scope condition is injected by the COMPILER and appears nowhere in the client's statement,
        // so a statement reading a scoped entity needs caller.person while saying nothing of the kind. The
        // text test cannot see that; the inspector compiles and can. Faked here — the real answer comes from
        // DqlClientQueryInspector, which needs a domain model this module has no business owning.
        ServerCallerParameters.registerResolver(name -> "caller.person".equals(name) ? (Object) 555 : null);
        ClientQueryGuard.registerInspector(new ClientQueryGuard.Inspector() {
            @Override public String refusalReason(QueryArgument argument) { return null; }
            @Override public String[] serverCallerNamesNeeded(QueryArgument argument, Object scopeToken) {
                // "ChatMessage is scoped by the member, Event is not" — what a provider's rules amount to.
                return argument.getStatement().contains("ChatMessage")
                    ? new String[] {"caller.person"} : new String[0];
            }
        });

        QueryArgument scopedRead = argument("select id, content from ChatMessage m where conversation=$1", 42);
        QueryArgument scopedAttached = ServerCallerParameters.attach(scopedRead, "member");
        check("a statement that never mentions a caller term still gets one, when a scope will inject it",
              Arrays.asList(scopedAttached.getParameterNames()).contains("caller.person"));
        // Named parameters sit ABOVE the caller's positional block, so an index into the NAMES array is not
        // an index into the PARAMETERS array - it is offset by however many positional ones came first.
        // Getting this wrong is what made this check fail the first time it ran, reading the client's own 42.
        check("and the value is the caller's, so the injected predicate has something to bind",
              Integer.valueOf(555).equals(namedValue(scopedAttached, "caller.person")));
        check("without the token it is NOT attached - which is the bug this closes",
              !Arrays.asList(ServerCallerParameters.attach(scopedRead).getParameterNames() == null
                  ? new String[0] : ServerCallerParameters.attach(scopedRead).getParameterNames())
                  .contains("caller.person"));

        // The other direction, and the one that costs money if it is wrong: a scoped CALLER reading an
        // UNSCOPED entity must still produce an argument equal to everyone else's, or every viewer of a
        // shared query gets a subscription of their own.
        QueryArgument unscopedRead = argument("select id from Event e where id=$1", 7);
        check("a scoped caller reading an unscoped entity is left shareable",
              ServerCallerParameters.attach(unscopedRead, "member") == unscopedRead);
        check("so two differently-scoped callers still share that subscription",
              ServerCallerParameters.attach(unscopedRead, "member")
                  .equals(ServerCallerParameters.attach(unscopedRead, "backoffice")));

        // An inspector that cannot say must not leave the query with an unbound slot.
        ClientQueryGuard.registerInspector(argument -> null); // the lambda form: no override, default null
        check("an inspector that cannot say makes attach conservative, not empty-handed",
              Arrays.asList(ServerCallerParameters.attach(scopedRead, "member").getParameterNames())
                  .contains("caller.person"));

        // ── A VALUE THE SERVER DOES NOT HAVE YET ─────────────────────────────────────────────────────
        // UNAVAILABLE is the answer null used to stand in for, and the two are opposite. Null means there
        // is no such caller, and matching no rows is right. UNAVAILABLE means the value is not worked out
        // yet for a caller who may read a great deal, and matching no rows is a lie they cannot see — the
        // one that emptied the staging back-office inbox on 11 November.
        ServerCallerParameters.registerResolver(name ->
            "caller.grantedEvents".equals(name) ? ServerCallerParameters.UNAVAILABLE
                : "caller.person".equals(name) ? (Object) 555 : null);
        ClientQueryGuard.registerInspector(new ClientQueryGuard.Inspector() {
            @Override public String refusalReason(QueryArgument argument) { return null; }
            @Override public String[] serverCallerNamesNeeded(QueryArgument argument, Object scopeToken) {
                // "Document is scoped by the caller's granted events, Event is not."
                return argument.getStatement().contains("Document")
                    ? new String[] {"caller.grantedEvents"} : new String[0];
            }
        });
        QueryArgument grantedRead = argument("select id from Document d where event=$1", 7);
        check("a statement KNOWN to need an unavailable value is refused, not run with null bound",
              ServerCallerParameters.CALLER_VALUE_UNAVAILABLE_REFUSED.equals(
                  refusalOf(grantedRead, "backoffice")));
        check("and the refusal tells the caller to retry, because the miss starts the computation",
              ServerCallerParameters.CALLER_VALUE_UNAVAILABLE_REFUSED.contains("retry"));
        check("a statement needing nothing is untouched, whatever the resolver cannot answer",
              refusalOf(unscopedRead, "backoffice") == null
                  && ServerCallerParameters.attach(unscopedRead, "backoffice") == unscopedRead);

        // THE GAP, held open on purpose and measured rather than argued about: with no answer from the
        // inspector the attached names are a guess, so an unavailable value is bound as null rather than
        // refusing every read in a window where most of them need nothing.
        ClientQueryGuard.registerInspector(argument -> null);
        check("where the names are only a guess, an unavailable value is bound as null, not refused",
              refusalOf(grantedRead, "backoffice") == null
                  && namedValue(ServerCallerParameters.attach(grantedRead, "backoffice"),
                                "caller.grantedEvents") == null);
        check("and the text-test path, which is a guess for the same reason, does the same",
              namedValue(ServerCallerParameters.attach(
                  argument("select id from Document d where event in CALLER_GRANTED_EVENTS")),
                  "caller.grantedEvents") == null);

        System.out.println(fail == 0 ? "\nALL " + pass + " PASS" : "\n" + fail + " FAILED");
        if (fail > 0)
            throw new AssertionError(fail + " caller parameter checks failed");
    }

    /** The refusal attaching this argument produces, or null when it attaches without one. */
    private static String refusalOf(QueryArgument argument, Object scopeToken) {
        try {
            ServerCallerParameters.attach(argument, scopeToken);
            return null;
        } catch (ServerCallerParameters.CallerValueUnavailableException e) {
            return e.getMessage();
        }
    }

    /** The value bound to a named parameter: names sit above the positional block, not alongside it. */
    private static Object namedValue(QueryArgument argument, String name) {
        String[] names = argument.getParameterNames();
        Object[] parameters = argument.getParameters();
        int nameIndex = Arrays.asList(names).indexOf(name);
        return nameIndex < 0 ? null : parameters[parameters.length - names.length + nameIndex];
    }

    private static QueryArgument argument(String statement, Object... parameters) {
        return QueryArgument.builder().setDataSourceId("ds").setLanguage("DQL")
                .setStatement(statement).setParameters(parameters).build();
    }

    private static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else    { fail++; System.out.println("  FAIL " + what); }
    }
}
