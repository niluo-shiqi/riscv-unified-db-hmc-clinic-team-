package org.xtext.udb.jruby;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.core.runtime.FileLocator;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class JRubyBundleHelper {

    public static String getGemsPath() throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(JRubyBundleHelper.class);
        URL gemsUrl = bundle.getEntry("/gems");
        
        // Resolve bundleentry:// to a real filesystem URL
        URL resolvedUrl = FileLocator.resolve(gemsUrl);
        
        // Convert to a plain path string
        return new File(resolvedUrl.getPath()).getAbsolutePath();
    }
}