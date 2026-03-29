package org.xtext.example.udb.treetop;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.core.runtime.FileLocator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;

import org.xtext.udb.jruby.JRubyBundleHelper;

public class TreetopParser {

    private static final TreetopParser INSTANCE = new TreetopParser();

    private final ScriptingContainer ruby;
    private Object parser;

    private TreetopParser() {
        ruby = new ScriptingContainer(LocalContextScope.SINGLETON);

        try {
            // Point JRuby at the vendored gems dir instead of the system gem path
            String gemsPath = JRubyBundleHelper.getGemsPath();
            ruby.runScriptlet(
                "Gem.paths = { 'GEM_HOME' => '" + gemsPath + "', " +
                "              'GEM_PATH' => '" + gemsPath + "' }"
            );
            
            // Resolve the .treetop grammar file
            Bundle bundle = FrameworkUtil.getBundle(TreetopParser.class);
            URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
            String bundleRootPath = bundleRoot.getPath();
            String idlcPath = bundleRootPath + 
            	    "../../../../../tools/ruby-gems/idlc/lib/idlc";
            	idlcPath = new File(idlcPath).getCanonicalPath();	
            	
	        ruby.runScriptlet("$LOAD_PATH.unshift('" + idlcPath + "')");
	        ruby.runScriptlet("require 'treetop'");
	        ruby.runScriptlet("require '" + idlcPath + "/syntax_node.rb'");
	        ruby.runScriptlet("require '" + idlcPath + "/ast.rb'");
	        ruby.runScriptlet("require '" + idlcPath + "/ast_decl.rb'");
	        ruby.runScriptlet("require '" + idlcPath + "/type.rb'");
	        ruby.runScriptlet("require '" + idlcPath + "/interfaces.rb'");
	        ruby.runScriptlet("require '" + idlcPath + "/symbol_table.rb'");
	        
	        String grammarPath = idlcPath + "/idl.treetop";
            ruby.runScriptlet("Treetop.load('" + grammarPath + "')");
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve gems path", e);
        }
        
        parser = ruby.runScriptlet("IdlParser.new");
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
