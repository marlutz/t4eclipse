/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - activator for T4Eclipse plugin
 ******************************************************************************/
package ch.mlutz.plugins.t4e;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.handlers.CommandHandler;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.index.TapestryIndexer;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * the Log
	 */
	public static IEclipseLog log;

	// The shared instance
	private static Activator plugin;

	private static Map<ImageDescriptor, Image> imageCache= new HashMap<ImageDescriptor, Image>();

	private TapestryIndex tapestryIndex= null;

	private TapestryIndexer tapestryIndexer= null;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		plugin= this;

		EclipseLogFactory.setLog(getLog());

		log= EclipseLogFactory.create(Activator.class);

		// try to load tapestryIndex from plugin state
		IPath pluginStateLocation= getStateLocation();
		IPath pluginStateFilePath= pluginStateLocation.addTrailingSeparator()
				.append("tapestryindex.bin");
		File pluginStateFile= pluginStateFilePath.toFile();

		if (pluginStateFile.exists()) {
			try {
				InputStream fis= new FileInputStream(pluginStateFile);
				InputStream bis= new BufferedInputStream(fis);
				ObjectInput ois= new ObjectInputStream(bis);
				try {
					tapestryIndex= (TapestryIndex) ois.readObject();
				}
				finally {
					ois.close();
				}
			}
			catch (IOException ex) {
				getLog().log(new Status(Status.ERROR, Constants.PLUGIN_ID,
						Status.OK, "Cannot perform tapestryIndex read.", ex));
			}
		}

		/*
		// attach popup menu handler
		MenuManager manager = new MenuManager("#PopupMenu");
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				log.info("MENU ABOUT TO SHOW!");
			   /*
			   final IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			   boolean isEntityGroupSelected = OwlUI.isEntityGroupSelected(selection);
			   * /
			}
		});
		*/
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;

		TapestryIndex tapestryIndex= getTapestryIndex();

		if (tapestryIndex != null) {
			// save tapestryIndex to plugin state
			IPath pluginStateLocation= getStateLocation();
			IPath pluginStateFilePath= pluginStateLocation.addTrailingSeparator()
					.append("tapestryindex.bin");
			File pluginStateFile= pluginStateFilePath.toFile();

			try {
				OutputStream fos= new FileOutputStream(pluginStateFile);
				OutputStream bos= new BufferedOutputStream(fos);
				ObjectOutput oos= new ObjectOutputStream(bos);
				try {
					oos.writeObject(tapestryIndex);
				}
				finally {
					oos.close();
				}
			}
			catch (IOException ex) {
				getLog().log(new Status(Status.ERROR, Constants.PLUGIN_ID,
						Status.OK, "Cannot perform tapestryIndex write.", ex));
			}
		}

		/*
		 * try {

   java.io.File file =
	  new java.io.File(Activator.getDefault().getStateLocation().toFile(), filename);

} catch(Exception e) {
   e.printStackTrace();
}
		 */

		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(Constants.PLUGIN_ID, path);
	}

	public static Image getImage(String path) {
		ImageDescriptor imageDescriptor= getImageDescriptor(path);
		Image image;
		if ((image= imageCache.get(imageDescriptor)) == null) {
			image= imageDescriptor.createImage();
			imageCache.put(imageDescriptor, image);
		}
		return image;
	}

	/**
	 * @return the tapestryIndex
	 */
	public TapestryIndex getTapestryIndex() {
		if (tapestryIndex == null) {
			tapestryIndex= new TapestryIndex();
		}
		return tapestryIndex;
	}

	public TapestryIndexer getTapestryIndexer() {
		if (tapestryIndexer == null) {
			tapestryIndexer= new TapestryIndexer(getTapestryIndex());
		}
		return tapestryIndexer;
	}
}
