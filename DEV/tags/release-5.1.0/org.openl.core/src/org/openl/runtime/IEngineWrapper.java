package org.openl.runtime;

import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * 
 * @author snshor
 * IEngineWrapper provides a lightweight wrapper around Engine instance that implements interface T. One instance of wrapper
 * should be used for a single-threaded execution of the engine. Engine wrappers are produced by  {@link EngineFactory#newInstance()} method 
 *
 * @param <T>
 */

public interface IEngineWrapper<T>
{
    /**
     * 
     * @return Runtime Environment that is necessary to execute OpenL code. One instance of {@link IRuntimeEnv} works only in 
     * single-threaded mode. 
     */
    IRuntimeEnv getRuntimeEnv();
    
    /**
     * Instance of engine object (usually {@link DynamicObject})
     * @return
     */
    Object getInstance();

    
    /**
     * Reference to the factory that created this wrapper
     * @return
     */
    
    EngineFactory<T> getFactory();
}
