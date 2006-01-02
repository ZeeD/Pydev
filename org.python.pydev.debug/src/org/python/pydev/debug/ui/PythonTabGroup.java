/*
 * Author: atotic
 * Created: Aug 20, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Create tabs for the debugger setup.
 * 
 * <p>Creates the tabs in the Debug setup window</p>
 * 
 * TODO: Fix tabs so that invalid configuration disables Apply and Run buttons always
 */
public class PythonTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new MainModuleTab(PydevPlugin.getPythonInterpreterManager()),
			new ArgumentsTab(PydevPlugin.getPythonInterpreterManager()),          
			new RefreshTab(),
			new EnvironmentTab(),
			new CommonTab()	};
		setTabs(tabs);
	}
}
