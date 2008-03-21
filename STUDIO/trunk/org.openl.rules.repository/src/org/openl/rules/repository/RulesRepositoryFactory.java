package org.openl.rules.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Repository Factory.  It is Abstract Factory.
 * <p>
 * Takes init values from repository.properties file.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RulesRepositoryFactory {
    private static final Log log = LogFactory.getLog(RulesRepositoryFactory.class);

    public static final String PROP_FILE = "rules-repository.properties";

    /** default value is <code>null</code> -- fail first */
    private static final ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString("repository.factory", null);

    private static RRepositoryFactory repFactory;

    // TODO: add support for other types of concrete factories
    public static synchronized RRepository getRepositoryInstance() throws RRepositoryException {
        if (repFactory == null) {
            initFactory();
        }

        return repFactory.getRepositoryInstance();
    }

    private static void initFactory() throws RRepositoryException {
        ConfigSet confSet = SysConfigManager.getConfigManager().locate(PROP_FILE);
        confSet.updateProperty(confRepositoryFactoryClass);

        String className = confRepositoryFactoryClass.getValue();
        try {
            Class<?> c = Class.forName(className);
            Object obj = c.newInstance();
            repFactory = (RRepositoryFactory) obj;
            // initialize
            repFactory.initialize(confSet);
        } catch (Exception e) {
            String msg = "Failed to initialize RulesRepositoryFactory!";
            log.error(msg, e);
            throw new RRepositoryException(msg, e);
        }
    }
}
