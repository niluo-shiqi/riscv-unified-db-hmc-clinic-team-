package org.xtext.example.udb.treetop;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.core.runtime.FileLocator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;

public class TreetopParser {

    private static final TreetopParser INSTANCE = new TreetopParser();

    private final ScriptingContainer ruby;
    private Object parser;

    private TreetopParser() {
        ruby = new ScriptingContainer(LocalContextScope.SINGLETON);

        try {
            // ----------------------------------------------------------------
            // Resolve tools/ruby-gems/idlc/ relative to this OSGi bundle.
            // ----------------------------------------------------------------
            Bundle bundle = FrameworkUtil.getBundle(TreetopParser.class);
            URL    bundleRootUrl = FileLocator.toFileURL(bundle.getEntry("/"));

            // idlc gem directory — contains Gemfile, Gemfile.lock, idlc.gemspec, lib/
            File idlcDir = new File(
                bundleRootUrl.getPath(),
                "../../../../../tools/ruby-gems/idlc"
            ).getCanonicalFile();

            // The Gemfile inside idlc/ is what Bundler reads to resolve deps
            String gemfilePath = new File(idlcDir, "Gemfile").getCanonicalPath();

            // vendor/bundle is where Bundler will install gems on first run.
            // This directory does NOT exist yet on a fresh checkout — Bundler creates it.
            String vendorPath = new File(idlcDir, "vendor/bundle").getCanonicalPath();

            // ----------------------------------------------------------------
            // Set Bundler's environment variables in the JRuby runtime.
            //
            // These are NOT files — they are in-memory environment variables
            // that Bundler reads when it starts. You do not create them;
            // you simply assign them here before calling anything in Bundler.
            //
            //   BUNDLE_GEMFILE  →  which Gemfile to read
            //   BUNDLE_PATH     →  where gems are installed (or should be installed)
            // ----------------------------------------------------------------
            ruby.runScriptlet("ENV['BUNDLE_GEMFILE'] = '" + rb(gemfilePath) + "'");
            ruby.runScriptlet("ENV['BUNDLE_PATH']    = '" + rb(vendorPath)  + "'");
            ruby.runScriptlet("require 'bundler'");

            // ----------------------------------------------------------------
            // Try Bundler.setup — fast path if vendor/bundle already exists.
            // On a fresh checkout it raises GemNotFound, so we fall through to
            // `bundle install`, which creates vendor/bundle, then retry setup.
            // ----------------------------------------------------------------
            ruby.runScriptlet(
                "begin\n" +
                "  Bundler.setup(:default)\n" +
                "rescue Bundler::GemNotFound => _e\n" +
                "  STDERR.puts '[TreetopParser] First run — installing gems into vendor/bundle...'\n" +
                "  require 'bundler/cli'\n" +
                "  Bundler::CLI.start(['install',\n" +
                "    '--gemfile', ENV['BUNDLE_GEMFILE'],\n" +
                "    '--path',    ENV['BUNDLE_PATH']])\n" +
                "  Bundler.reset!\n" +
                "  Bundler.setup(:default)\n" +
                "end"
            );

            // ----------------------------------------------------------------
            // Now require 'idlc'.
            //
            // Bundler.setup has already added idlc/lib (from the gemspec) and
            // all declared dependencies (e.g. treetop) to $LOAD_PATH.
            // This is equivalent to a normal Ruby script doing:
            //
            //   require 'bundler/setup'
            //   require 'idlc'
            // ----------------------------------------------------------------
            ruby.runScriptlet("require 'idlc'");

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise TreetopParser", e);
        }

        // IdlParser is defined inside idlc (loaded via Treetop.load in idlc.rb)
        parser = ruby.runScriptlet("IdlParser.new");
    }

    public static TreetopParser getInstance() {
        return INSTANCE;
    }

    /**
     * Parse {@code input} starting at an optional Treetop rule.
     *
     * @param input     IDL source fragment
     * @param startRule Rule name to use as the root (e.g. "function_call"),
     *                  or {@code null} for the grammar's default root.
     */
    public ValidationError parse(String input, String startRule) {
        // Use ruby.put() to pass Java strings safely — avoids any quoting
        // issues if the IDL input itself contains single/double quotes.
        ruby.put("idl_input", input);

        Object result;
        if (startRule != null && !startRule.isEmpty()) {
            ruby.put("idl_root", startRule);
            result = ruby.runScriptlet(
                "$idl_parser.parse(idl_input, root: idl_root.to_sym)"
            );
        } else {
            result = ruby.callMethod(parser, "parse", input);
        }

        if (result == null) {
            String reason = (String) ruby.callMethod(parser, "failure_reason");
            int    line   = ((Long)  ruby.callMethod(parser, "failure_line"  )).intValue();
            int    column = ((Long)  ruby.callMethod(parser, "failure_column")).intValue();
            return new ValidationError(reason, line, column);
        }
        return null;
    }

    /** Convenience overload using the grammar's default root rule. */
    public ValidationError parse(String input) {
        return parse(input, null);
    }

    // Escape backslashes and single-quotes for use inside a Ruby '...' literal.
    private static String rb(String path) {
        return path.replace("\\", "\\\\").replace("'", "\\'");
    }
}
