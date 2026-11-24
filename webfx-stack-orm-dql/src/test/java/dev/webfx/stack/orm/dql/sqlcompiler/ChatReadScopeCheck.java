package dev.webfx.stack.orm.dql.sqlcompiler;

import dev.webfx.stack.orm.dql.sqlcompiler.lci.mock.CompilerDomainModelReaderMock;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.PostgresSyntax;
import dev.webfx.stack.orm.expression.parser.ExpressionParser;
import dev.webfx.stack.orm.expression.parser.lci.mock.ParserDomainModelReaderMock;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.platform.console.spi.ConsoleProvider;
import dev.webfx.platform.service.SingleServiceProvider;

/**
 * That the chat read-scope conditions reach their rows without navigating.
 *
 * <p>{@link ClientReadScopeCheck} proves the mechanism in general; this one pins the four statement shapes
 * the chat feature actually sends, because they are the first rules written against a real schema and two of
 * them have nowhere local to stand.
 *
 * <h3>Why these rules are subqueries</h3>
 *
 * <p>Only {@code Conversation} carries the member: {@code viewer_person_id}, populated on every row. A chat
 * message does not — its own {@code person_id} is the message's AUTHOR, null on 37.6% of production rows
 * (every {@code system} message and most notes), so a rule testing it would blank every status event in every
 * thread for everybody. A reaction is a hop further still. The member is therefore one join away for
 * {@code ChatMessage} and two for {@code ChatMessageReaction} — and a scope condition that navigates is
 * refused by name in {@code SqlBuild.compileJoinScopePredicate}.
 *
 * <p>The way out is the one the front-office ownership rule already uses: move the hop INSIDE a subquery, so
 * the scoped row contributes one local column and the subquery's table goes in its own {@code FROM} as
 * {@code tt1}, registering nothing on the outer build.
 *
 * <p><b>The last check is the reason to read this file.</b> It re-states the same rule as a dot path and
 * requires the compiler to refuse it. Writing it found that the compiler did not: the guard meant to refuse
 * navigation tested a flag that could never be set, and an {@code ON} clause came out referencing an alias
 * introduced by a later join. {@code SqlBuild} now counts the joins a scope predicate registers, and that
 * check is what this case holds in place.
 *
 * <h3>The alternative this made unnecessary</h3>
 *
 * <p>Before it was probed, the plan's fallback was to denormalise {@code viewer_person_id} and
 * {@code event_id} onto {@code chat_message} and {@code chat_message_reaction} — a migration, a trigger, a
 * backfill and two column classifications — on the assumption that a subquery might count as navigation. It
 * does not. The checks below are what withdrew that migration, which is why they are worth keeping even
 * though they duplicate the general form.
 *
 * <h3>Reading the SQL</h3>
 *
 * <p>The mock derives a foreign class by capitalising the field name, so {@code ChatMessageReaction.message}
 * resolves to a class called {@code Message} rather than {@code ChatMessage}. That is the mock's convention,
 * not the domain model's, and it is why the reaction rules below say {@code Message}. It cost a false
 * "refused" the first time these were run.
 *
 * @author Claude Code
 */
public class ChatReadScopeCheck {

    static int pass = 0, fail = 0;

    /** The member's own conversations, the hop moved inside a subquery so nothing navigates. */
    private static final String MEMBER_CONVERSATIONS =
        "(select id from Conversation where viewerPerson=CALLER_PERSON)";

    public static void main(String[] args) {
        // Without a silent provider, the parser's own logging throws a ServiceConfigurationError that masks
        // every real parse refusal — see ClientReadScopeCheck, which learned this first.
        SingleServiceProvider.registerServiceProvider(ConsoleProvider.class, new ConsoleProvider() {
            @Override public void error(String message, Throwable error) { }
        });

        // ── Conversation: the only root with a local answer ──────────────────────────────────────────────
        ClientReadScope.registerProvider((dc, token) ->
            "Conversation".equals(String.valueOf(dc))
                ? ExpressionParser.parseExpression("viewerPerson=CALLER_PERSON", dc, model())
                : null);

        // The back office reaches Conversation through a dot path on every inbox read, so this rule meets the
        // join injection point constantly. A local column test lands in the ON clause, parenthesised.
        check("Conversation reached by a join is scoped in its ON clause",
              "select id from ChatMessage m where conversation.event=$1",
              "select m.id, m.id from chat_message as m"
            + " join conversation j2 on j2.id=m.conversation_id and (j2.viewer_person_id=$2)"
            + " where j2.event_id=$1");

        // ── ChatMessage: one hop, so the rule is a subquery ──────────────────────────────────────────────
        ClientReadScope.registerProvider((dc, token) ->
            isChatMessage(dc)
                ? ExpressionParser.parseExpression("conversation in " + MEMBER_CONVERSATIONS, dc, model())
                : null);

        // The front-office thread read. The caller's own conversation=$1 survives: a scope condition is
        // AND-ed, so it can only narrow further — which is why this statement leaks nothing even when two
        // differently-scoped callers share one push subscription.
        check("ChatMessage at the select root keeps the caller's own filter",
              "select id, content from ChatMessage m where conversation=$1 and kind!=$2",
              "select m.id, m.id, m.content from chat_message as m"
            + " where m.conversation_id=$1 and m.kind!=$2"
            + " and (m.conversation_id in (select tt1.id from conversation as tt1 where tt1.viewer_person_id=$3))");

        // THE ONE THAT DECIDED IT: the scoped entity reached as a JOINED table, which is where
        // compileJoinScopePredicate refuses a navigating condition. The subquery arm survives it.
        check("ChatMessage reached by a join takes its subquery rule into the ON clause",
              "select id from ChatMessageReaction r where message.conversation=$1",
              "select r.id, r.id from chat_message_reaction as r"
            + " join message j2 on j2.id=r.message_id"
            + " and (j2.conversation_id in (select tt1.id from conversation as tt1 where tt1.viewer_person_id=$2))"
            + " where j2.conversation_id=$1");

        // ── ChatMessageReaction: two hops, so two nested subqueries ──────────────────────────────────────
        ClientReadScope.registerProvider((dc, token) ->
            "ChatMessageReaction".equals(String.valueOf(dc))
                ? ExpressionParser.parseExpression(
                    "message in (select id from Message where conversation in " + MEMBER_CONVERSATIONS + ")",
                    dc, model())
                : null);

        // The reactions read lives in shared/ and is issued by BOTH apps with the same parameter, so it is
        // the statement most likely to have two scopes meet on it. Two levels deep is fine: scope-inside-
        // scope is off for the whole build tree, so neither level is scoped again by the entity it reads.
        check("ChatMessageReaction scopes two subqueries deep, contributing one local column",
              "select id, emoji from ChatMessageReaction r where message.conversation=$1",
              "select r.id, r.id, r.emoji from chat_message_reaction as r"
            + " join message j2 on j2.id=r.message_id where j2.conversation_id=$1"
            + " and (r.message_id in (select tt1.id from message as tt1"
            + " where tt1.conversation_id in (select ttt1.id from conversation as ttt1"
            + " where ttt1.viewer_person_id=$2)))");

        // ── The back-office arm: granted events, not the organisation ────────────────────────────────────
        // conversation.organization_id is NULL on every production row, so the boundary is the event — and
        // event.organization would be a PATH, which is the refusal below.
        ClientReadScope.registerProvider((dc, token) ->
            isChatMessage(dc)
                ? ExpressionParser.parseExpression(
                    "conversation in (select id from Conversation where event in CALLER_GRANTED_EVENTS)",
                    dc, model())
                : null);

        check("the back-office arm binds granted events as an array, not an in-list",
              "select id from ChatMessage m where conversation=$1",
              "select m.id, m.id from chat_message as m where m.conversation_id=$1"
            + " and (m.conversation_id in (select tt1.id from conversation as tt1 where tt1.event_id=any($2)))");

        // ── THE BACK-OFFICE ARM, which has TWO arms and is the one that will actually run ───────────────
        // A grant row names an event, an event type, or neither, and an organisation-wide one never widens
        // into granted events — 33 of 43 staging grant rows are organisation-wide and 18 of 25 back-office
        // users hold no event grant at all. So `event in CALLER_GRANTED_EVENTS` alone binds an empty array
        // for most operators and the support inbox returns nothing. The second arm reaches the organisation
        // through a SUBQUERY, because event.organization is a path and a path is refused at both points.
        //
        // Both arms in one term is what makes the parentheses load-bearing: appended bare, the caller's own
        // filter would regroup as `(<filter> and A) or B` and return rows it excluded. The root point gained
        // those parentheses in 7d79a577; this pins that they hold for the rule that depends on them.
        ClientReadScope.registerProvider((dc, token) ->
            "Conversation".equals(String.valueOf(dc))
                ? ExpressionParser.parseExpression(
                    "event in CALLER_GRANTED_EVENTS"
                  + " or event in (select id from Event where organization in CALLER_GRANTED_ORGANIZATIONS)",
                    dc, model())
                : null);

        check("the back-office arm at the root keeps the caller's own filter outside its or",
              "select id, status from Conversation c where type=$1",
              "select c.id, c.id, c.status from conversation as c where c.type=$1"
            + " and (c.event_id=any($2) or c.event_id in"
            + " (select tt1.id from event as tt1 where tt1.organization_id=any($3)))");

        check("and survives the join the back-office inbox makes on every read",
              "select id from ChatMessage m where conversation.event=$1",
              "select m.id, m.id from chat_message as m"
            + " join conversation j2 on j2.id=m.conversation_id"
            + " and (j2.event_id=any($2) or j2.event_id in"
            + " (select tt1.id from event as tt1 where tt1.organization_id=any($3)))"
            + " where j2.event_id=$1");

        // ── THE BOUNDARY. Delete a subquery above, write the dot path it replaces, and this is the
        // refusal that comes back. It is also the case that found the hole it now covers.
        //
        // Until SqlBuild was fixed this did NOT refuse. compileJoinScopePredicate tested a re-entrancy flag
        // that could not fire — the flag was set together with compilingScopeCondition, and the method's
        // FIRST line returns null whenever that is set, so the nested registration left before the flag was
        // read and the throw was unreachable. What came out instead was an ON clause referencing an alias
        // introduced by a LATER join:
        //
        //   join message j2 on j2.id=r.message_id and (j3.viewer_person_id=$2)
        //   join conversation j3 on j3.id=j2.conversation_id
        //
        // which Postgres rejects with `missing FROM-clause entry for table "j3"` (verified against the shape
        // on 2026-11-09). It failed closed, but at execution with a syntax-shaped error rather than here
        // with the cause named. The guard now counts the joins registered while the predicate compiles,
        // because the nested registration never comes back through that method at all.
        //
        // Probed by deleting the guard: this check fails with exactly the SQL above, and passes with it.
        //
        // AND REFUSED AT THE ROOT TOO, which it was not at first. At the select root a navigating
        // condition compiles to valid AND correct SQL — the join is registered before the where that reads
        // it, and when the statement reaches the same entity by two paths the scope binds the one the rule
        // named. So refusing it rejects something that works. It is refused anyway, because allowing it at
        // one point and refusing it at the other is the worse of both: a navigating rule would pass every
        // test whose statement reads the entity as a root, ship, and throw the first time some other
        // statement joined it. Both points now say the same sentence, with a suffix naming which refused.
        ClientReadScope.registerProvider((dc, token) ->
            isChatMessage(dc)
                ? ExpressionParser.parseExpression("conversation.viewerPerson=CALLER_PERSON", dc, model())
                : null);

        checkRefused("a dot-path rule is refused at the select root",
              "select id, content from ChatMessage m where conversation=$1");

        checkRefused("a dot-path rule is refused once the entity is joined",
              "select id from ChatMessageReaction r where message.conversation=$1");

        System.out.println(fail == 0 ? "\nALL " + pass + " PASS" : "\n" + fail + " FAILED");
        if (fail > 0)
            throw new AssertionError(fail + " chat scope checks failed");
    }

    /** The mock calls it Message when it is reached through {@code ChatMessageReaction.message}; see javadoc. */
    private static boolean isChatMessage(Object domainClass) {
        String name = String.valueOf(domainClass);
        return "ChatMessage".equals(name) || "Message".equals(name);
    }

    private static void check(String what, String dql, String expectedSql) {
        try {
            SqlCompiled compiled = compile(dql);
            if (expectedSql.equals(compiled.getSql())) {
                pass++;
                System.out.println("  ok   " + what);
            } else {
                fail++;
                System.out.println("  FAIL " + what);
                System.out.println("         got      " + compiled.getSql());
                System.out.println("         expected " + expectedSql);
            }
        } catch (Throwable t) {
            fail++;
            System.out.println("  FAIL " + what + " — threw " + t);
        }
    }

    /** A rule that navigates must be refused, not compiled into something plausible. */
    private static void checkRefused(String what, String dql) {
        try {
            SqlCompiled compiled = compile(dql);
            fail++;
            System.out.println("  FAIL " + what + " — compiled instead: " + compiled.getSql());
        } catch (Throwable t) {
            pass++;
            System.out.println("  ok   " + what);
        }
    }

    private static SqlCompiled compile(String dql) {
        DqlStatement<?> statement = ExpressionParser.parseStatement(dql, model());
        return ExpressionSqlCompiler.compileForScope(
            statement, 5, PostgresSyntax.get(), false, false, false, compilerModel());
    }

    /**
     * FKs declared up front: the mock otherwise learns FK-ness lazily on the first dot navigation, so the
     * same field compiles to "conversation" in one check and "conversation_id" in another depending on
     * which ran first — the trap ClientReadScopeCheck records for Document.person.
     */
    private static CompilerDomainModelReaderMock compilerModel() {
        return new CompilerDomainModelReaderMock()
                .declareAsForeignField("ChatMessage", "conversation")
                .declareAsForeignField("Message", "conversation")
                .declareAsForeignField("ChatMessageReaction", "message")
                .declareAsForeignField("Conversation", "viewerPerson")
                .declareAsForeignField("Conversation", "event")
                .declareAsForeignField("Event", "organization");
    }

    private static ParserDomainModelReaderMock model() {
        return new ParserDomainModelReaderMock()
                .declareFields("Conversation", "id,viewerPerson,event,type,status")
                .declareFields("ChatMessage", "id,conversation,person,kind,content")
                .declareFields("Message", "id,conversation,person,kind,content")
                .declareFields("ChatMessageReaction", "id,message,person,emoji")
                .declareFields("Event", "id,organization,name");
    }
}
