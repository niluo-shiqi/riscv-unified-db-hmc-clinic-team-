package org.xtext.example.udb.parser.antlr;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.eclipse.xtext.parser.antlr.AbstractIndentationTokenSource;
import org.xtext.example.udb.parser.antlr.internal.InternalUdbParser;

import java.util.ArrayDeque;
import java.util.Deque;

public class UdbTokenSource extends AbstractIndentationTokenSource {

    // Tokens we emit ourselves sit in this queue; nextToken() drains it first.
    private final Deque<Token> pending = new ArrayDeque<>();

    // True while we have just emitted ':' and are watching for '|'
    private boolean awaitingPipeAfterColon = false;

    public UdbTokenSource(TokenSource delegate) {
        super(delegate);
    }

    /*
     * Indentation logic for whitespace awareness
     */
    @Override
    protected boolean shouldSplitTokenImpl(Token token) {
        return token.getType() == InternalUdbParser.RULE_WS;
    }

    @Override
    protected int getBeginTokenType() {
        return InternalUdbParser.RULE_INDENT;
    }

    @Override
    protected int getEndTokenType() {
        return InternalUdbParser.RULE_DEDENT;
    }

    /*
     * Lexer customization for multi-line strings
     */
    @Override
    public Token nextToken() {
        if (!pending.isEmpty()) {
            return pending.poll();
        }

        Token token = super.nextToken();
        int type = token.getType();

        boolean isTrivial = (type == InternalUdbParser.RULE_WS || type == Token.EOF);

        if (!isTrivial) {
            if (awaitingPipeAfterColon) {
                awaitingPipeAfterColon = false;
                if (type == 119) { // '|'
                    return consumeMultilineBlock(token);
                }
            }
            if (type == 112) { // ':'
                awaitingPipeAfterColon = true;
            }
        }

        return token;
    }

    private Token consumeMultilineBlock(Token pipeToken) {
        StringBuilder text = new StringBuilder();
        int depth = 0;
        boolean started = false;
        Token lastToken = pipeToken; // track the last real token for stop index

        while (true) {
            Token t = super.nextToken();
            int type = t.getType();

            if (type == Token.EOF) {
                pending.add(t);
                break;
            }

            if (type == InternalUdbParser.RULE_INDENT) {
                depth++;
                started = true;
                continue;
            }

            if (type == InternalUdbParser.RULE_DEDENT) {
                depth--;
                if (started && depth < 1) {
                    pending.add(t);
                    break;
                }
                continue;
            }

            if (started) {
                String fragment = t.getText();
                if (fragment != null) {
                    text.append(fragment);
                }
                lastToken = t;
            }
        }

        int start = ((CommonToken) pipeToken).getStartIndex();

		// Build the token attached to the real stream so indices are valid
		CommonToken result = new CommonToken(
		        pipeToken.getInputStream(),
		        InternalUdbParser.RULE_MULTILINE_STRING,
		        Token.DEFAULT_CHANNEL,
		        start,
		        start); // start == stop → zero-width in source
		
		// Now detach the stream and fix the text so getText() returns our value,
		// not inputStream.substring(start, start)
		result.setText(trimTrailingNewline(text.toString()));
		result.setInputStream(null); // detach: getText() now returns the stored text field
		
		result.setLine(pipeToken.getLine());
		result.setCharPositionInLine(pipeToken.getCharPositionInLine());

        return result;
    }

    private static String trimTrailingNewline(String s) {
        if (s.endsWith("\r\n")) return s.substring(0, s.length() - 2);
        if (s.endsWith("\n") || s.endsWith("\r")) return s.substring(0, s.length() - 1);
        return s;
    }
}