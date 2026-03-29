package org.xtext.example.udb.treetop;

import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;

public class TreetopParser {

	private static final TreetopParser INSTANCE = new TreetopParser();

    private final ScriptingContainer ruby;
    private Object parser;
    private String lastError;

    private TreetopParser() {
        ruby = new ScriptingContainer(LocalContextScope.SINGLETON);
        ruby.runScriptlet("require 'treetop'");
        ruby.runScriptlet("Treetop.load('/path/to/tools/ruby-gems/idlc/lib/idlc/idl.treetop')");
        parser = ruby.runScriptlet("IdlParser.new");  // class name from your .treetop file
    }

    public static TreetopParser getInstance() {
        return INSTANCE;
    }

    public ValidationError parse(String input) {
        Object result = ruby.callMethod(parser, "parse", input);
        if (result == null) {
            String reason = (String) ruby.callMethod(parser, "failure_reason");
            int line      = ((Long)  ruby.callMethod(parser, "failure_line")).intValue();
            int column    = ((Long)  ruby.callMethod(parser, "failure_column")).intValue();
            return new ValidationError(reason, line, column);
        }
        return null;
    }
}