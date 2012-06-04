package org.cytoscape.app.internal.manager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.internal.exception.AppInstallException;
import org.cytoscape.app.internal.exception.AppInstanceException;
import org.cytoscape.app.internal.exception.AppUninstallException;

public class SimpleApp extends App {

	@Override
	public Object createAppInstance(CyAppAdapter appAdapter) throws AppInstanceException {
		
		File appFile = this.getAppFile();
		URL appURL = null;
		try {
			appURL = appFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new AppInstanceException("Unable to obtain URL for file: " 
					+ appFile + ". Reason: " + e.getMessage());
		}
		
		// TODO: Currently uses the CyAppAdapter's loader to load apps' classes. Is there reason to use a different one?
		ClassLoader appClassLoader = new URLClassLoader(
				new URL[]{appURL}, appAdapter.getClass().getClassLoader());
		
		// Attempt to load the class
		Class<?> appEntryClass = null;
		try {
			 appEntryClass = appClassLoader.loadClass(this.getEntryClassName());
		} catch (ClassNotFoundException e) {
			
			throw new AppInstanceException("Class " + this.getEntryClassName() + " not found in URL: " + appURL);
		}
		
		// Attempt to obtain the constructor
		Constructor<?> constructor = null;
		try {
			constructor = appEntryClass.getConstructor(CyAppAdapter.class);
		} catch (SecurityException e) {
			throw new AppInstanceException("Access to the constructor for " + appEntryClass + " denied.");
		} catch (NoSuchMethodException e) {
			throw new AppInstanceException("Unable to find a constructor for " + appEntryClass + " that takes a CyAppAdapter as its argument.");
		}
		
		// Attempt to instantiate the app's class that extends AbstractCyActivator.
		Object appInstance = null;
		try {
			appInstance = constructor.newInstance(appAdapter);
		} catch (IllegalArgumentException e) {
			throw new AppInstanceException("Illegal arguments passed to the constructor for the app's entry class: " + e.getMessage());
		} catch (InstantiationException e) {
			throw new AppInstanceException("Error instantiating the class " + appEntryClass + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new AppInstanceException("Access to constructor denied: " + e.getMessage());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new AppInstanceException("App constructor threw exception: " + e.toString());
		}
		
		return appInstance;
	}

	@Override
	public void install(AppManager appManager) throws AppInstallException {
		// Use the default installation method of copying over the file,
		// creating an instance, and registering with the app manager.
		defaultInstall(appManager);
	}

	@Override
	public void uninstall(AppManager appManager) throws AppUninstallException {
		
		// Use the default uninstallation procedure which is to move the app to
		// the uninstalled apps directory
		defaultUninstall(appManager);
				
		// Simple apps require a Cytoscape restart to be uninstalled
		setStatus(AppStatus.TO_BE_UNINSTALLED);
	}

}