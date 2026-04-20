package org.xtext.example.udb.parser.antlr;

import java.util.ArrayDeque;
import java.util.Deque;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.eclipse.xtext.parser.antlr.AbstractIndentationTokenSource;
import org.xtext.example.udb.parser.antlr.internal.InternalUdbParser;

public class UdbTokenSource extends AbstractIndentationTokenSource {

    private final Deque<Token> pendingTokens = new ArrayDeque<>();
    private final Deque<Token> lookaheadBuffer = new ArrayDeque<>();

    private boolean inStringBlock = false; // for multi-line strings
    private boolean lastWasColon = false;

    public UdbTokenSource(TokenSource delegate) {
        super(delegate);
    }

    @Override
    public Token nextToken() {
        // Always drain pre-queued tokens first
        if (!pendingTokens.isEmpty()) {
            return pendingTokens.poll();
        }

        Token token = super.nextToken();

        // While in a string block, every token is handled by the block consumer
        if (inStringBlock) {
            return handleBlockScalarToken(token);
        }

        // Track whether the last real token was a ':' (excluding hidden/whitespace tokens)
        if (isHiddenOrNewline(token)) {
            return token;
        }

        if (isFieldSeparator(token)) {
            lastWasColon = true;
            return token;
        }

        // If '|' is immediately after ':', then we're in a multi-line string
        if (lastWasColon && token.getText().equals("|")) {
            lastWasColon = false;
            inStringBlock = true;

            // Inject STRING_FOLLOWS before the '|' so the parser can match
            // the StringBlock rule
            pendingTokens.add(token); // re-queue the '|' to emit after STRING_FOLLOWS
            return syntheticToken(
                InternalUdbParser.RULE_STRING_FOLLOWS,
                "synthetic:STRING_FOLLOWS",
                token
            );
        }

        lastWasColon = false;
        return token;
    }


    /*
     * Multi-line string handling
     */
    private Token handleBlockScalarToken(Token token) {
        // First DEDENT exits string block — emit it so the parser
        // sees the DEDENT and closes the StringBlock rule
        if (token.getType() == InternalUdbParser.RULE_DEDENT) {
        	inStringBlock = false;
            return token;
        }

        // INDENT after the '|' line is structural — pass it through so
        // the parser's INDENT expectation in StringBlock is satisfied
        if (token.getType() == InternalUdbParser.RULE_INDENT) {
            return token;
        }

        // Newlines between lines are structural separators — pass through
        if (isNewline(token)) {
            return token;
        }

        // EOF inside a block scalar — exit cleanly
        if (token.getType() == Token.EOF) {
        	inStringBlock = false;
            return token;
        }

        // Any other token is the first real token on a content line —
        // consume the rest of the line and emit as UNQUOTED_STRING
        return consumeRestOfLineAsUnquotedString(token);
    }


    /*
     * Consume an entire line as an unquoted string
     */
    private Token consumeRestOfLineAsUnquotedString(Token startToken) {
        StringBuilder sb = new StringBuilder();
        sb.append(startToken.getText());

        while (true) {
            Token next = peekRawToken();

            if (next.getType() == Token.EOF
                    || next.getType() == InternalUdbParser.RULE_INDENT
                    || next.getType() == InternalUdbParser.RULE_DEDENT
                    || isNewline(next)) {
                break;
            }

            consumeRawToken();
            sb.append(next.getText());
        }

        return syntheticToken(
            InternalUdbParser.RULE_UNQUOTED_STRING,
            sb.toString().stripTrailing(),
            startToken
        );
    }


    /*
     * Lookahead buffer
     */
    private Token peekRawToken() {
        if (lookaheadBuffer.isEmpty()) {
            lookaheadBuffer.add(super.nextToken());
        }
        return lookaheadBuffer.peek();
    }

    private Token consumeRawToken() {
        if (!lookaheadBuffer.isEmpty()) {
            return lookaheadBuffer.poll();
        }
        return super.nextToken();
    }


    /*
     * Helper Functions
     */
    private boolean isFieldSeparator(Token token) {
    	String text = token.getText();
    	
    	if (text == null) return false;
    	
        return token.getText().equals(":");
    }

    private boolean isHiddenOrNewline(Token token) {
        return token.getChannel() == Token.HIDDEN_CHANNEL || isNewline(token);
    }

    private boolean isNewline(Token token) {
        String text = token.getText();
        
        if (text == null) return false;
        
        return text.equals("\n") || text.equals("\r") || text.equals("\r\n");
    }

    private Token syntheticToken(int type, String text, Token copyFrom) {
        CommonToken t = new CommonToken(copyFrom);
        t.setType(type);
        t.setText(text);
        t.setChannel(Token.DEFAULT_CHANNEL);
        return t;
    }


   /*
    * INDENT/DEDENT logic for whitespace awareness 
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
}
