package org.openl.rules.extension.load;

import org.openl.rules.lang.xls.XlsLoader;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;

/**
 * @deprecated Will be deleted soon. Now extension is declared in rules.xml.
 */
@Deprecated
public interface IExtensionLoader {

    String getModuleName();

    void process(XlsLoader xlsLoader, TableSyntaxNode tsn, IGridTable table, XlsSheetSourceCodeModule sheetSource);

}