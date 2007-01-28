/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.eclipseresourcestubs.FileResourceStub;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectStub;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.refactoring.TestDependentRefactoring;
import com.python.pydev.refactoring.refactorer.RefactorerFindReferences;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;
import com.python.pydev.refactoring.refactorer.refactorings.renamelocal.RefactoringLocalTestBase;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;

/**
 * A class used for the refactorings that need the rename project (in pysrcrefactoring)
 * 
 * @author Fabio
 */
public abstract class RefactoringRenameTestBase extends RefactoringLocalTestBase{
    /**
     * We want to keep it initialized among runs from the same class.
     * Check the restorePythonPath function.
     */
    public static PythonNature natureRefactoring;
    
    /**
     * This is the last rename processor used (so, we may query it about other things)
     */
    protected PyRenameEntryPoint lastProcessorUsed;
    
    /**
     * Backwards-compatibility interface
     */
    protected boolean restoreProjectPythonPathRefactoring(boolean force, String path){
        return restoreProjectPythonPathRefactoring(force, path, "testProjectStubRefactoring");
    }

    /**
     * The list of python files contained in the refactoring pysrc project
     */
    protected static List<File> filesInRefactoringProject;
    
    public static final String CURRENT_MODULE_IN_REFERENCES = "__current_module__";

    protected static boolean DEBUG_REFERENCES = false;
    
    /**
     * In the setUp, it initializes the files in the refactoring project
     * @see com.python.pydev.refactoring.refactorer.refactorings.renamelocal.RefactoringLocalTestBase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if (filesInRefactoringProject == null){
            filesInRefactoringProject = PydevPlugin.getPyFilesBelow(new File(TestDependentRefactoring.TEST_PYSRC_LOC_REFACTORING), 
                    new NullProgressMonitor(), true, false)[0];
            
            ArrayList<IFile> iFiles = new ArrayList<IFile>();
            for (File f : filesInRefactoringProject) {
                iFiles.add(new FileResourceStub(f, natureRefactoring.getProject()));
            }
            
            RefactorerFindReferences.FORCED_RETURN = iFiles;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Checks if the refactor processes are all of the type we're testing here.
     */
    protected void checkProcessors() {
        if(lastProcessorUsed != null){
            List<IRefactorRenameProcess> processes = lastProcessorUsed.process;
            assertEquals(1, processes.size());
            
            for(IRefactorRenameProcess p:processes){
                assertTrue( StringUtils.format("Expected %s. Received:%s", getProcessUnderTest(), p.getClass()),
                			getProcessUnderTest().isInstance(p)); //we should only activate the rename class process in this test case
            }
        }
    }

    /**
     * @return the process class that we're testing.
     */
    protected abstract Class getProcessUnderTest();

    /**
     * A method that creates a project that references no other project 
     * 
     * @param force whether the creation of the new nature should be forced
     * @param path the pythonpath for the new nature
     * @param name the name for the project
     * @return true if the creation was needed and false if it wasn't
     */
    protected boolean restoreProjectPythonPathRefactoring(boolean force, String path, String name){
        PythonNature n = checkNewNature(name, force);
        if(n != null){
            natureRefactoring = n;
            
            ProjectStub projectFromNatureRefactoring = new ProjectStub(name, path, new IProject[0], new IProject[0]);
            setAstManager(path, projectFromNatureRefactoring, natureRefactoring);
            return true;
        }
        return false;
    }

    /**
     * Overriden so that the pythonpath is only restored for the system and the refactoring nature
     * 
     * @param force whether this should be forced, even if it was previously created for this class
     */
    @Override
    public void restorePythonPath(boolean force){
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Restoring system pythonpath");
        }
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB);
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Restoring project pythonpath for refactoring nature");
        }
        restoreProjectPythonPathRefactoring(force, TestDependentRefactoring.TEST_PYSRC_LOC_REFACTORING);
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Checking size (for projrefactoring)");
        }
        
        checkSize();
    }

    
    /**
     * checks if the size of the system modules manager and the project moule manager are coherent
     * (we must have more modules in the system than in the project)
     */
    protected void checkSize() {
        IInterpreterManager iMan = getInterpreterManager();
        InterpreterInfo info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(getProgressMonitor());
        assertTrue(info.modulesManager.getSize() > 0);
        
        int size = ((ASTManager)natureRefactoring.getAstManager()).getSize();
        assertTrue("Interpreter size:"+info.modulesManager.getSize()+" should be smaller than project size:"+size+" " +
                "(because it contains system+project info)" , info.modulesManager.getSize() < size );
        
    }

    /**
     * Gets the references for the rename without expecting any error.
     * @param line: starts at 0
     * @param col: starts at 0
     */
    protected Map<String, List<ASTEntry>> getReferencesForRenameSimple(String moduleName, int line, int col) {
        Map<String, List<ASTEntry>> referencesForRename = getReferencesForRenameSimple(moduleName, line, col, false);
        if(DEBUG_REFERENCES){
            for (Map.Entry<String, List<ASTEntry>> entry : referencesForRename.entrySet()) {
                System.out.println(entry.getKey());
                for(ASTEntry e :entry.getValue()){
                    System.out.println(e);
                }
            }
        }
        return referencesForRename;
    }
    
    /**
     * Same as {@link #getReferencesForRename(String, int, int, boolean)} but returning
     * the key for the map as a string with the module name.
     */
    protected Map<String, List<ASTEntry>> getReferencesForRenameSimple(String moduleName, int line, int col, boolean expectError) {
        Map<String, List<ASTEntry>> occurrencesToReturn=new HashMap<String, List<ASTEntry>>();
        
        Map<Tuple<String, IFile>, List<ASTEntry>> referencesForRename = getReferencesForRename(moduleName, line, col, expectError);
        for (Map.Entry<Tuple<String, IFile>, List<ASTEntry>> entry : referencesForRename.entrySet()) {
            if(occurrencesToReturn.get(entry.getKey()) != null){
                throw new RuntimeException("Error. Module: "+entry.getKey()+" already exists.");
            }
            occurrencesToReturn.put(entry.getKey().o1, entry.getValue());
        }        
        return occurrencesToReturn;
    }
    
    /**
     * Goes through all the workspace (in this case the refactoring project) and gathers the references
     * for the current selection.
     * 
     * @param moduleName the name of the module we're currently in
     * @param line the line we're in
     * @param col the col we're in
     * @param expectError whether we are expecting some error or not
     * @return a map with the name of the module and the file representing it pointing to the
     * references found in that module.
     */
    protected Map<Tuple<String, IFile>, List<ASTEntry>> getReferencesForRename(String moduleName, int line, int col, boolean expectError) {
        Map<Tuple<String, IFile>, List<ASTEntry>> occurrencesToReturn=null;
        try {
            ProjectModulesManager modulesManager = (ProjectModulesManager) natureRefactoring.getAstManager().getModulesManager();
            IModule module = modulesManager.getModuleInDirectManager(moduleName, natureRefactoring, true);
            if(module == null){
                throw new RuntimeException("Unable to get source module for module:"+moduleName);
            }
            String strDoc = REF.getFileContents(module.getFile());
            
            Document doc = new Document(strDoc);
            PySelection ps = new PySelection(doc, line, col);
            
            RefactoringRequest request = new RefactoringRequest(null, ps, natureRefactoring);
            request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false);
            request.moduleName = moduleName;
            request.fillInitialNameAndOffset();
    
            PyRenameEntryPoint processor = new PyRenameEntryPoint(request);
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
			checkStatus(processor.checkInitialConditions(nullProgressMonitor), expectError);
			lastProcessorUsed = processor;
	        checkProcessors();

            checkStatus(processor.checkFinalConditions(nullProgressMonitor, null, false), expectError);
            occurrencesToReturn = processor.getOccurrencesInOtherFiles();
            occurrencesToReturn.put(new Tuple<String, IFile>(CURRENT_MODULE_IN_REFERENCES, null), processor.getOcurrences());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return occurrencesToReturn;
    }
   
    /**
     * Used to see if some line/col is available in a list of entries.
     */
    protected void assertContains(int line, int col, List<ASTEntry> names) {
        for (ASTEntry name : names) {
            if(name.node.beginLine == line && name.node.beginColumn == col){
                return;
            }
        }
        fail(StringUtils.format("Unable to find line:%s col:%s in %s", line, col, names));
        
    }


}
