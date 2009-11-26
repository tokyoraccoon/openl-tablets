package org.openl.rules.lang.xls.bindibg;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;

public class PropertyLoadingTest {
    
private String __src = "test/rules/PropertyLoadingTest.xls";
    
    private XlsModuleSyntaxNode getTables() {        
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }
    
    @Test
    public void testLoadingDefaultValuesForPreviouslyEmptyProp() {       
        XlsModuleSyntaxNode tables = getTables();
        TableSyntaxNode[] tsns = tables.getXlsTableSyntaxNodesWithoutErrors();
        for (TableSyntaxNode tsn : tsns) {
            if ("Rules void hello1(int hour)".equals(tsn.getDisplayName())) {
                List<TablePropertyDefinition> defaultPropDefinitions = DefaultPropertyDefinitions.getPropertiesToBeSetByDefault();
                assertEquals("Check that number of properties that must have default values" +
                		"equals number of properties in tsn.",
                		tsn.getTableProperties().getNumberOfProperties(), defaultPropDefinitions.size());                
                List<String> defaultPropDefinitionsNames = new ArrayList<String>();
                for (TablePropertyDefinition dataPropertyDefinition : defaultPropDefinitions) {
                    defaultPropDefinitionsNames.add(dataPropertyDefinition.getName());
                }
                assertTrue("Tsn doesn`t have properties defined in appropriate table in excel",!tsn.hasPropertiesDefinedInTable());
                
                List<String> tsnPropNames = new ArrayList<String>();
                for (Map.Entry<String, Object> property : tsn.getTableProperties().getDefinedProperties().entrySet()) {
                    tsnPropNames.add(property.getKey());
                }
                assertTrue("Tsn contains all properties that must be set by default",
                        tsnPropNames.containsAll(defaultPropDefinitionsNames));
            }        
        }
       
    }    

}
