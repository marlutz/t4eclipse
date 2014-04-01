/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - T4E Classpath container initializer
 ******************************************************************************/
package ch.mlutz.plugins.t4e.container;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.constants.Constants;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.pom.Dependency;
import ch.mlutz.plugins.t4e.pom.parsers.PomParser;
import ch.mlutz.plugins.t4e.tools.MavenTools;

/**
 * T4eClasspathContainerInitializer
 *
 * @author Marcel Lutz
 */
public class T4eClasspathContainerInitializer extends ClasspathContainerInitializer {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			T4eClasspathContainerInitializer.class);

	public static boolean isT4eClasspathContainer(IPath containerPath) {
		return containerPath != null && containerPath.segmentCount() > 0
				&& Constants.CONTAINER_ID.equals(containerPath.segment(0));
	}

	public void initialize(IPath containerPath, final IJavaProject project) {
		if(isT4eClasspathContainer(containerPath)) {
			IClasspathContainer container;
			final Activator plugin = Activator.getDefault();
			try {
				container = JavaCore.getClasspathContainer(containerPath, project);
			} catch(JavaModelException e) {
				log.error("Unable to get container for " + containerPath.toString(), e);
				return;
			}


			// plugin.getMavenModelManager().initModels(new NullProgressMonitor());

			T4eClasspathContainer t4eContainer;
			if(container == null) {

				// parse the pom.xml
				IProject resourceProject= project.getProject();

				PomParser pomParser= new PomParser();

				IFile pomFile= resourceProject.getFile("pom.xml");
				List<Dependency> dependencies;
				if (pomFile.exists()) {

					try {
						pomParser.parse(pomFile.getContents());
					} catch(UnsupportedEncodingException e) {
						log.warn("Unsupported encoding in pom.xml: " + e);
					} catch(CoreException e) {
						log.warn("CoreException when getting contents of pom.xml: " + e);
					}

					dependencies= pomParser.getDependencyManagement();
					log.info("DependencyManagement dependencies count: " + String.valueOf(dependencies.size()));
					dependencies= pomParser.getDependencies();
					log.info("Dependencies count: " + String.valueOf(dependencies.size()));
				} else {
					log.warn("pom.xml doesn't seem to exist.");
				}

				// add dependency management entries to T4eClasspathContainer7
				List<IClasspathEntry> entryList= new ArrayList<IClasspathEntry>();

				String mavenRepositoryBasePath= MavenTools.getMavenLocalRepoPath()
						.replaceAll("\\\\", "/");
				dependencies= pomParser.getDependencyManagement();
				for (Dependency dependency : dependencies) {
					Path path= new Path(mavenRepositoryBasePath + "/"
							 + dependency.getGroupId().replaceAll("\\.", "/")
							 + "/" + dependency.getArtifactId() + "/"
							 + dependency.getVersion() + "/"
							 + dependency.getArtifactId() + "-"
							 + dependency.getVersion() + ".jar");
					entryList.add(JavaCore.newLibraryEntry(
						 path, null, null, true));
				}

				t4eContainer = new T4eClasspathContainer(new Path(Constants.CONTAINER_ID),
						(IClasspathEntry[]) entryList.toArray(new IClasspathEntry[0]));
			} else {
				t4eContainer = new T4eClasspathContainer(containerPath, container.getClasspathEntries());
			}

			try {
				JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
						new IClasspathContainer[] {t4eContainer}, new NullProgressMonitor());
			} catch(JavaModelException e) {
				log.warn("Unable to set container for " + containerPath.toString(), e);
				return;
			}

			if(container != null) {
				return;
			}

			/*
			plugin.getBuildpathManager().scheduleUpdateClasspathContainer(project.getProject());
			*/
		}
	}
}
