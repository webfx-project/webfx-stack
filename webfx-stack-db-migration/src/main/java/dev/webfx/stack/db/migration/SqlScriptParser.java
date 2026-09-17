package dev.webfx.stack.db.migration;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a SQL script into its individual statements, as required by the SubmitService API which executes one
 * SQL command per statement (prepared statements can't contain several commands). The tokenizer understands
 * {@code --} line comments, nested {@code /* *&#47;} block comments, string literals (including {@code E''}
 * escape strings), quoted identifiers and dollar-quoted bodies, so a {@code ;} inside any of those (e.g. in a
 * PL/pgSQL function body) never splits a statement.
 *
 * @author Bruno Salmon
 */
public final class SqlScriptParser {

    /**
     * One statement of a script: the verbatim text to send to the database (comments kept), plus a normalized
     * form used for linting (uppercased, comments removed, quoted contents masked, whitespace collapsed).
     */
    public static final class SqlStatement {

        private final String text;
        private final String normalized;

        private SqlStatement(String text, String normalized) {
            this.text = text;
            this.normalized = normalized;
        }

        public String getText() {
            return text;
        }

        public String getNormalized() {
            return normalized;
        }
    }

    private SqlScriptParser() {
    }

    public static List<SqlStatement> splitStatements(String sql) {
        List<SqlStatement> statements = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        StringBuilder normalized = new StringBuilder();
        int i = 0, n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            // Line comment
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int end = sql.indexOf('\n', i);
                if (end < 0)
                    end = n;
                text.append(sql, i, end);
                normalized.append(' ');
                i = end;
                continue;
            }
            // Block comment (they nest in PostgreSQL)
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int start = i, depth = 1;
                i += 2;
                while (i < n && depth > 0) {
                    if (sql.charAt(i) == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else if (sql.charAt(i) == '*' && i + 1 < n && sql.charAt(i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else
                        i++;
                }
                if (depth > 0)
                    throw new IllegalArgumentException("Unterminated block comment");
                text.append(sql, start, i);
                normalized.append(' ');
                continue;
            }
            // String literal (with '' escape; E'' escape strings also honour backslash escapes)
            if (c == '\'') {
                boolean escapeString = i > 0 && (sql.charAt(i - 1) == 'E' || sql.charAt(i - 1) == 'e')
                    && (i < 2 || !isIdentifierChar(sql.charAt(i - 2)));
                int start = i++;
                while (true) {
                    if (i >= n)
                        throw new IllegalArgumentException("Unterminated string literal");
                    char sc = sql.charAt(i);
                    if (escapeString && sc == '\\') {
                        i += 2;
                    } else if (sc == '\'') {
                        if (i + 1 < n && sql.charAt(i + 1) == '\'') {
                            i += 2; // '' escape, still inside the literal
                        } else {
                            i++;
                            break;
                        }
                    } else
                        i++;
                }
                text.append(sql, start, i);
                normalized.append("'?'");
                continue;
            }
            // Quoted identifier (with "" escape)
            if (c == '"') {
                int start = i++;
                while (true) {
                    if (i >= n)
                        throw new IllegalArgumentException("Unterminated quoted identifier");
                    if (sql.charAt(i) == '"') {
                        if (i + 1 < n && sql.charAt(i + 1) == '"') {
                            i += 2; // "" escape, still inside the identifier
                        } else {
                            i++;
                            break;
                        }
                    } else
                        i++;
                }
                text.append(sql, start, i);
                normalized.append("\"?\"");
                continue;
            }
            // Dollar-quoted body ($$...$$ or $tag$...$tag$) — e.g. a PL/pgSQL function body
            if (c == '$') {
                int tagEnd = findDollarTagEnd(sql, i);
                if (tagEnd > 0) {
                    String delimiter = sql.substring(i, tagEnd + 1);
                    int close = sql.indexOf(delimiter, tagEnd + 1);
                    if (close < 0)
                        throw new IllegalArgumentException("Unterminated dollar-quoted string " + delimiter);
                    int end = close + delimiter.length();
                    text.append(sql, i, end);
                    normalized.append("$?$");
                    i = end;
                    continue;
                }
            }
            // Top-level statement separator
            if (c == ';') {
                addStatement(statements, text, normalized);
                i++;
                continue;
            }
            text.append(c);
            normalized.append(Character.toUpperCase(c));
            i++;
        }
        addStatement(statements, text, normalized); // trailing statement without a final ;
        return statements;
    }

    private static void addStatement(List<SqlStatement> statements, StringBuilder text, StringBuilder normalized) {
        String normalizedStatement = normalized.toString().replaceAll("\\s+", " ").trim();
        if (!normalizedStatement.isEmpty())
            statements.add(new SqlStatement(text.toString().trim(), normalizedStatement));
        text.setLength(0);
        normalized.setLength(0);
    }

    // Returns the index of the closing $ of a dollar-quote opening delimiter starting at `start` ($$ or
    // $tag$ where the tag is an identifier), or -1 if this $ doesn't open a dollar quote (e.g. a $1 parameter).
    private static int findDollarTagEnd(String sql, int start) {
        int i = start + 1, n = sql.length();
        if (i < n && sql.charAt(i) == '$')
            return i;
        if (i >= n || !Character.isLetter(sql.charAt(i)) && sql.charAt(i) != '_')
            return -1;
        while (i < n && isIdentifierChar(sql.charAt(i)))
            i++;
        return i < n && sql.charAt(i) == '$' ? i : -1;
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
