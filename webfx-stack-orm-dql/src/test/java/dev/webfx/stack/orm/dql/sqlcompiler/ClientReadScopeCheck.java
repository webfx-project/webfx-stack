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
 * That a scope condition reaches every select in a statement, not just the outermost one.
 *
 * <p>This is the claim the mechanism rests on. Scope injected only at the top would leave a subquery as the
 * way round it, and subqueries are the reason the question came up: the read inventory shows client
 * statements reading through {@code exists(…)} and {@code in (select …)} in ninety-odd places. Injecting
 * where the compiler builds each select makes the reach structural — there is one place a select is built —
 * rather than a list of constructs someone has to keep complete.
 *
 * <p>The unscoped case is here to stay: with no provider, or no token, the SQL must be byte-identical to
 * what it was. Every statement the system sends today is that case.
 */
public class ClientReadScopeCheck {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        // The parser logs through Console before rethrowing, and with no provider registered that logging
        // throws a ServiceConfigurationError of its own - which MASKS every parse refusal as "cannot
        // instantiate ConsoleProvider". A silent provider lets the real exception through.
        SingleServiceProvider.registerServiceProvider(ConsoleProvider.class, new ConsoleProvider() {
            @Override public void error(String message, Throwable error) { }
        });

        // A provider that scopes Document only, so the checks can also show it leaves other entities alone.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Document".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("organization=" + scopeToken, domainClass, model())
                : null);

        check("no token: unchanged", "select id from Document d where event=$1", null,
              "select d.id, d.id from document as d where d.event=$1");

        check("the root select is scoped", "select id from Document d where event=$1", 5,
              "select d.id, d.id from document as d where d.event=$1 and (d.organization=5)");

        check("a select with no where of its own is still scoped", "select id from Document d", 5,
              "select d.id, d.id from document as d where (d.organization=5)");

        // The one that matters: the subquery is a Document read too, and it gets the same condition.
        check("a subquery selecting the same entity is scoped as well",
              "select id from DocumentLine dl where document in (select id from Document d where event=$1)", 5,
              "select dl.id, dl.id from document_line as dl where dl.document in (select d.id from document as d where d.event=$1 and (d.organization=5))");

        // And an entity the provider does not scope is left exactly as it was.
        check("an unscoped entity is untouched", "select id from DocumentLine dl where document=$1", 5,
              "select dl.id, dl.id from document_line as dl where dl.document=$1");

        // THE SECOND INJECTION POINT. A table reached through a dot path gets no select of its own, so the
        // condition added to every select never reaches it: d.person.email reads a Person through a join.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Person".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("organization=" + scopeToken, domainClass, model())
                : null);

        check("a table reached by a join is scoped in its ON clause",
              "select id from Document d where person.email=$1", 5,
              "select d.id, d.id from document as d join person j2 on j2.id=d.person_id and (j2.organization=5) where j2.email=$1");

        check("with no token, the join is written exactly as before",
              "select id from Document d where person.email=$1", null,
              "select d.id, d.id from document as d join person j2 on j2.id=d.person_id where j2.email=$1");

        // THE CORRELATION TERM. The rule for Person is not a column test - a person is readable because they
        // booked one of the events in scope - so the condition has to point back at the person row it is
        // filtering. 'this' is that pointer, and the alias it needs is the one the compiler chose for the
        // OUTER select, which the subquery's own build does not know.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Person".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("exists(select Document where event=" + scopeToken + " and person=this)", domainClass, model())
                : null);

        check("'this' correlates a scope subquery back to the scoped row",
              "select id from Person p where email=$1", 5,
              "select p.id, p.id from person as p where p.email=$1 and (exists(select tt1.id from document as tt1 where tt1.event=5 and tt1.person_id=p.id))");

        check("'this' still correlates when the scoped row is reached by a join",
              "select id from Document d where person.email=$1", 5,
              "select d.id, d.id from document as d join person j2 on j2.id=d.person_id and (exists(select tt1.id from document as tt1 where tt1.event=5 and tt1.person_id=j2.id)) where j2.email=$1");

        // The rule as it will really be written: the events in scope arrive as ONE array parameter rather
        // than being pasted in as a literal, and it has to be a named one - a server-authored rule cannot
        // know how many positional slots the client's own statement already took.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Person".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("exists(select Document where event in ?scopeEvents and person=this)", domainClass, model())
                : null);

        check("the rule as it will be written: an array parameter and a correlation",
              "select id from Person p where email=$1", 5,
              "select p.id, p.id from person as p where p.email=$1 and (exists(select tt1.id from document as tt1 where tt1.event=any($2) and tt1.person_id=p.id))");

        // THE KEYWORD FORM, which is what a rule will really be written with: a term the client cannot
        // forge, not a name in the channel the client fills. `in` has to read it as an ARRAY - a scalar
        // `in ($n)` cannot be bound an array by the driver, so this is a runtime bind failure if the
        // compiler does not know the term supplies many values.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Person".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("exists(select Document where event in CALLER_GRANTED_EVENTS and person=this)", domainClass, model())
                : null);

        check("the array-valued caller term compiles to = any(), not in ()",
              "select id from Person p where email=$1", 5,
              "select p.id, p.id from person as p where p.email=$1 and (exists(select tt1.id from document as tt1 where tt1.event=any($2) and tt1.person_id=p.id))");

        // The bare form is only for a term that supplies MANY values. A scalar one on that path, or a plain
        // column, is a mistake that used to be a parse error and must stay one - otherwise the widening
        // needed for the line above would cost every statement in the system the error it used to get.
        checkParseRefused("a scalar caller term may not take the bare 'in' path", "id in CALLER_PERSON");
        checkParseRefused("a column may not either", "id in organization");
        checkParses("a parameter still may", "id in $1");
        checkParses("and so does a list", "id in (1, 2)");

        // No scope inside a scope. The condition above reads Document, and Document is scoped too - but the
        // condition is server-authored and already says what it means, so it is applied as written. Without
        // this, a pair of conditions that reach each other would recurse until the stack gave out.
        ClientReadScope.registerProvider((domainClass, scopeToken) -> {
            String name = String.valueOf(domainClass);
            if ("Person".equals(name))
                return ExpressionParser.parseExpression("exists(select Document where person=this)", domainClass, model());
            if ("Document".equals(name))
                return ExpressionParser.parseExpression("exists(select Person where document=this)", domainClass, model());
            return null;
        });

        check("a condition is not scoped again by the entity it reads",
              "select id from Person p where email=$1", 5,
              "select p.id, p.id from person as p where p.email=$1 and (exists(select tt1.id from document as tt1 where tt1.person_id=p.id))");

        // THE OWNERSHIP FORM, which is what the FRONT OFFICE needs and the back office does not. A
        // front-office caller holds no grants, so CALLER_GRANTED_* arrive empty and every scoping arm is
        // false for every row - their rule has to say "yours" instead of "in scope". The system already
        // defines that, in accountCanAccessPersonOrders, whose body is
        // `p?.frontendAccount=a or p?.accountPerson?.frontendAccount=a` - but not in a form that can be
        // injected: the second arm is a two-hop PATH, a path registers a join, and that is what
        // compileJoinScopePredicate refuses by name. Re-expressed here with the hop moved inside a
        // subquery, so both arms read a local column of the scoped row and neither navigates.
        //
        // `in (select ...)` rather than `exists(...)`: the subquery is UNCORRELATED - which people an
        // account has does not depend on the row being filtered - so it needs no `this`, and `this.field`
        // is a grammar nothing has asked for. These two checks are what says that form survives both
        // injection points; only exists(...) had been shown to, and "it is a subquery too" is the kind of
        // reasoning that shipped `event.state = text` to production.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Person".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression(
                    "frontendAccount=CALLER_ACCOUNT or accountPerson in (select id from Person where frontendAccount=CALLER_ACCOUNT)",
                    domainClass, model())
                : null);

        // THE JOIN POINT: it parenthesises what it injects (SqlBuild emits " and (" + scopePredicate + ")"),
        // so the two arms stay one term inside the ON clause. The subquery arm registers nothing on the outer
        // build - its table goes in its own FROM as tt1 - so compileJoinScopePredicate does not refuse it.
        check("the ownership rule, subquery arm and all, survives a table reached by a join",
              "select id from Document d where person.email=$1", 5,
              "select d.id, d.id from document as d join person j2 on j2.id=d.person_id"
            + " and (j2.frontend_account_id=$2 or j2.account_person_id in"
            + " (select tt1.id from person as tt1 where tt1.frontend_account_id=$2))"
            + " where j2.email=$1");

        // THE ROOT POINT DOES NOT, AND THIS CHECK FAILS UNTIL IT DOES. It appends the condition with the
        // separator " and " and nothing else (ExpressionSqlCompiler, the compileExpression call after
        // setScopeRow), so a rule with a top-level `or` is mis-grouped: `where <caller's filter> and A or B`
        // is read by SQL as `(<caller's filter> and A) or B`, and every row satisfying B comes back whether
        // or not it satisfied the caller. Not a disclosure for THIS rule - both arms are ownership - but the
        // caller's own query silently stops meaning what it says, and on the back-office document rule
        // (`event = any(...) or organization = any(...)`) it returns a whole granted organisation's rows to a
        // statement that asked for one person's.
        //
        // Every rule checked above is a single term - one `=`, one exists(...) - which is why nothing caught
        // this. Two of the three rules the catalogue has written down are not.
        //
        // The expectation below is the CORRECT sql, parenthesised the way the join point already does it.
        check("the ownership rule is parenthesised at the root, so the caller's own filter still binds",
              "select id from Person p where email=$1", 5,
              "select p.id, p.id from person as p where p.email=$1"
            + " and (p.frontend_account_id=$2 or p.account_person_id in"
            + " (select tt1.id from person as tt1 where tt1.frontend_account_id=$2))");

        // THE CLAUSE THE ROOT POINT NOW BORROWS. Parenthesising means compiling the condition to text
        // first, and compileToScratchSqlText does that by swapping the HAVING builder out, compiling into
        // a fresh one, and putting the original back. The join point has used that method for a while, but
        // only ever for a predicate on a joined table - a root-level condition on a GROUPED statement is
        // new, and the failure modes are quiet ones: a statement that loses its own `having`, or one that
        // grows an empty ` having ` and stops parsing. Both shapes are checked, with a two-arm rule so the
        // parentheses are in play at the same time.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Document".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression("organization=" + scopeToken + " or event=99", domainClass, model())
                : null);

        check("a scoped statement keeps its own having",
              "select organization, count(1) from Document d where event=$1 group by organization having count(1)>2", 5,
              "select min(d.id), min(d.organization), count(1) from document as d"
            + " where d.event=$1 and (d.organization=5 or d.event=99)"
            + " group by d.organization having count(1)>2");

        check("and a grouped statement with no having does not grow one",
              "select organization, count(1) from Document d where event=$1 group by organization", 5,
              "select min(d.id), min(d.organization), count(1) from document as d"
            + " where d.event=$1 and (d.organization=5 or d.event=99)"
            + " group by d.organization");

        // THE SAME REWRITE FOR THE FRONT-OFFICE DOCUMENT RULE, which the catalogue states as
        // accountCanAccessPersonOrders(CALLER_ACCOUNT, person) and which therefore navigates
        // Document -> Person -> Person. Anchored on document's own person_id instead, the whole ownership
        // test moves inside the subquery and the scoped row contributes one local column.
        //
        // Two subqueries deep, which is fine and worth having a check for: scope-inside-scope is off for the
        // whole build tree (compilingScopeCondition delegates to the top parent), so neither level is scoped
        // again by the entity it reads.
        //
        // It also has no top-level `or`, so it PASSES at the root today - which is the other half of why the
        // missing parentheses went unnoticed. A single-term rule cannot show the defect.
        ClientReadScope.registerProvider((domainClass, scopeToken) ->
            "Document".equals(String.valueOf(domainClass))
                ? ExpressionParser.parseExpression(
                    "person in (select id from Person where frontendAccount=CALLER_ACCOUNT"
                  + " or accountPerson in (select id from Person where frontendAccount=CALLER_ACCOUNT))",
                    domainClass, model())
                : null);

        check("the front-office document rule, re-expressed, needs no navigation",
              "select id from Document d where event=$1", 5,
              "select d.id, d.id from document as d where d.event=$1"
            + " and (d.person_id in (select tt1.id from person as tt1 where tt1.frontend_account_id=$2"
            + " or tt1.account_person_id in (select ttt1.id from person as ttt1 where ttt1.frontend_account_id=$2)))");

        // And outside a condition the term has no referent, so it is refused rather than compiled to
        // something plausible. No client statement can carry it - scope conditions are server-authored -
        // but a provider that returned a bare 'this' would otherwise emit a silently wrong column.
        ClientReadScope.registerProvider((domainClass, scopeToken) -> null);
        checkRefused("'this' outside a scope condition is refused",
              "select id from Person p where id=this", 5);

        System.out.println(fail == 0 ? "\nALL " + pass + " PASS" : "\n" + fail + " FAILED");
        if (fail > 0)
            throw new AssertionError(fail + " scope injection checks failed");
    }

    /**
     * Document.person declared foreign up front, because the mock otherwise learns FK-ness lazily - the
     * first dot navigation registers it - so the same field compiled to "person" in one check and
     * "person_id" in another depending on which ran first.
     */
    private static CompilerDomainModelReaderMock compilerModel() {
        return new CompilerDomainModelReaderMock()
                .declareAsForeignField("Document", "person")
                .declareAsForeignField("Person", "frontendAccount")
                .declareAsForeignField("Person", "accountPerson");
    }

    private static ParserDomainModelReaderMock model() {
        return new ParserDomainModelReaderMock()
                .declareFields("Document", "id,ref,event,person,organization")
                .declareFields("Person", "id,email,organization,document,frontendAccount,accountPerson")
                .declareFields("DocumentLine", "id,document,item");
    }

    private static void checkParseRefused(String what, String expression) {
        try {
            ExpressionParser.parseExpression(expression, "Person", model());
            fail++;
            System.out.println("  FAIL " + what + " — parsed instead of being refused");
        } catch (Throwable t) {
            // The parser wraps what a builder throws, so the refusal is looked for in the cause chain
            // rather than at the top: catching IllegalArgumentException alone reports every real refusal
            // as the wrong kind of failure.
            if (isCausedByIllegalArgument(t)) {
                pass++;
                System.out.println("  ok   " + what);
            } else {
                fail++;
                System.out.println("  FAIL " + what + " — refused, but with " + t);
            }
        }
    }

    private static boolean isCausedByIllegalArgument(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause())
            if (cause instanceof IllegalArgumentException)
                return true;
        return false;
    }

    private static void checkParses(String what, String expression) {
        try {
            ExpressionParser.parseExpression(expression, "Person", model());
            pass++;
            System.out.println("  ok   " + what);
        } catch (Throwable t) {
            fail++;
            System.out.println("  FAIL " + what + " — " + t);
        }
    }

    private static void checkRefused(String what, String dql, Object scopeToken) {
        try {
            DqlStatement<?> statement = ExpressionParser.parseStatement(dql, model());
            SqlCompiled compiled = ExpressionSqlCompiler.compileForScope(
                statement, scopeToken, PostgresSyntax.get(), false, false, false, compilerModel());
            fail++;
            System.out.println("  FAIL " + what);
            System.out.println("         compiled instead to " + compiled.getSql());
        } catch (IllegalArgumentException e) {
            pass++;
            System.out.println("  ok   " + what);
        } catch (Throwable t) {
            fail++;
            System.out.println("  FAIL " + what + " — refused, but with " + t);
        }
    }

    private static void check(String what, String dql, Object scopeToken, String expectedSql) {
        try {
            DqlStatement<?> statement = ExpressionParser.parseStatement(dql, model());
            SqlCompiled compiled = ExpressionSqlCompiler.compileForScope(
                statement, scopeToken, PostgresSyntax.get(), false, false, false, compilerModel());
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
            System.out.println("  FAIL " + what + " — " + t);
        }
    }
}
