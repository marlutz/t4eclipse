/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 module logic
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry;

/**
 * @author Marcel Lutz
 * @version 1.0 12.02.2014
 */

import static ch.mlutz.plugins.t4e.tools.EclipseTools.extractFileBase;
import static ch.mlutz.plugins.t4e.tools.EclipseTools.getPackageFragmentRoots;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.omg.CORBA.Request;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.index.TapestryIndex;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.serializer.EclipseSerializer;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryElement;
import ch.mlutz.plugins.t4e.tapestry.element.ElementType;
import ch.mlutz.plugins.t4e.tapestry.element.TapestryHtmlElement;
import ch.mlutz.plugins.t4e.tapestry.element.Service;
import ch.mlutz.plugins.t4e.tapestry.parsers.SpecificationParser;
import ch.mlutz.plugins.t4e.tools.TapestryTools;

public class TapestryModule implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4604095727846982073L;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			TapestryModule.class);

	public static final String WEB_INF_FOLDER_NAME= "WEB-INF";

	public static final String WEBAPP_FOLDER_NAME= "webapp";

	// application specification
	private AppSpecification appSpecification;

	// HiveMind Module Descriptor
	private transient HiveModuleDescriptor hiveModuleDescriptor;

	// this could be something like "app"
	private String moduleId;

	/*
	 * the folder containing the webapp folder which in turn contains the
	 * webapp files (page and component templates) and possibly the resources
	 * folder containing the hivemodule.xml
	 * the folder might typically be named 'main'
	 */
	private transient IContainer webappFolder;

	private Set<TapestryHtmlElement> components= new HashSet<TapestryHtmlElement>();

	public Set<TapestryHtmlElement> getComponents() {
		return Collections.unmodifiableSet(components);
	}

	private List<TapestryElement> elements= new ArrayList<TapestryElement>();

	private Set<TapestryHtmlElement> pages= new HashSet<TapestryHtmlElement>();

	private transient Set<Service> services= new HashSet<Service>();

	private transient TapestryIndex tapestryIndexStore;

	/**
	 * Constructor
	 *
	 * @param appSpecificationFile the modules app specification file
	 * @throws TapestryException if the project structure is not correct
	 */
	public TapestryModule(IFile appSpecificationFile) throws TapestryException {

		validateAppSpecificationFile(appSpecificationFile);

		appSpecification= new AppSpecification(appSpecificationFile);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result
				+ ((appSpecification == null) ? 0 :
					appSpecification.hashCode());

		/*
		result = prime * result
				+ ((moduleId == null) ? 0 : moduleId.hashCode());
		result = prime * result
				+ ((webappFolder == null) ? 0 : webappFolder.hashCode());
		*/
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TapestryModule)) {
			return false;
		}
		TapestryModule other = (TapestryModule) obj;
		if (appSpecification == null) {
			if (other.appSpecification != null) {
				return false;
			}
		} else if (!appSpecification.equals(other.appSpecification)) {
			return false;
		}
		return true;
	}

	public AppSpecification getAppSpecification() {
		return appSpecification;
	}

	public void setAppSpecification(AppSpecification appSpecification) {
		this.appSpecification = appSpecification;
	}

	public HiveModuleDescriptor getHiveModule() {
		return hiveModuleDescriptor;
	}

	public void addElementsToTapestryIndex() {
		for (TapestryHtmlElement htmlElement: components) {
			for (Pair<IFile, IFile> relation: htmlElement.getRelations()) {
				getTapestryIndex().addBidiRelation(relation.getLeft(),
					relation.getRight());
				getTapestryIndex().addRelationToCompilationUnit(
					htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
			}
		}

		for (TapestryHtmlElement htmlElement: pages) {
			for (Pair<IFile, IFile> relation: htmlElement.getRelations()) {
				getTapestryIndex().addBidiRelation(relation.getLeft(),
					relation.getRight());
				getTapestryIndex().addRelationToCompilationUnit(
					htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
			}
		}
	}

	public void add(TapestryElement element) {
		TapestryHtmlElement htmlElement;
		switch (element.getType()) {
			case COMPONENT:
				htmlElement= (TapestryHtmlElement) element;
				components.add(htmlElement);
				for (Pair<IFile, IFile> relation: htmlElement.getRelations()) {
					getTapestryIndex().addBidiRelation(relation.getLeft(),
						relation.getRight());
				}
				getTapestryIndex().addRelationToCompilationUnit(
					htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
				break;
			case PAGE:
				htmlElement= (TapestryHtmlElement) element;
				pages.add(htmlElement);
				for (Pair<IFile, IFile> relation: htmlElement.getRelations()) {
					getTapestryIndex().addBidiRelation(relation.getLeft(),
						relation.getRight());
				}
				getTapestryIndex().addRelationToCompilationUnit(
					htmlElement.getHtmlFile(), htmlElement.getJavaCompilationUnit());
				break;
			case SERVICE:
				services.add((Service) element);
				break;
			default:
				elements.add(element);
		}
	}

	public void remove(TapestryElement element) {
		switch (element.getType()) {
			case COMPONENT:
				components.remove(element);
				break;
			case PAGE:
				pages.remove(element);
				break;
			case SERVICE:
				services.remove(element);
				break;
			default:
				elements.remove(element);
		}
	}

	/**
	 * Tries to retrieve the hivemodule.xml file.
	 *
	 * @return the IFile handle to hivemodule.xml or null if no suitable file
	 * 		at the expected location can be found.
	 */
	public IFile getHiveModuleFile() {
		/*
		 * go from webapp folder up to resources/META-inf/hivemodule.xml
		 */
		IContainer tapestryRoot= webappFolder.getParent();

		assert tapestryRoot != null : "TapestryRoot can't be null.";

		return tapestryRoot
				.getFile(new Path("resources/META-INF/hivemodule.xml"));
	}

	/**
	 * Retrieves the project this TapestryModule is contained in
	 *
	 * @return the project of this TapestryModule, actually the app
	 * 		specification's project
	 */
	public IProject getProject() {
		return appSpecification.getAppSpecificationFile().getProject();
	}

	public int getScanAndUpdateWork() {
		return 100;
	}

	/**
	 * Scans app specification for module metadata (mainly page and component
	 * class packages); then scans this module (html and java files) and updates
	 * the central tapestryIndex with file relations; finally scans
	 * hivemodule.xml and collects and stores services in this module
	 * @param monitor
	 *
	 * @throws TapestryException
	 */
	public void scanAndUpdateIndex(IProgressMonitor monitor) throws TapestryException {
		try {
			// validateAppSpecificationFile(appSpecification.getAppSpecificationFile());
			if (!getAppSpecification().update()) {
				return;
			}

			Queue<IResource> resourceQueue= new LinkedList<IResource>();
			resourceQueue.add(webappFolder);

			IResource current= resourceQueue.poll();
			while (current != null) {
				if (current.getType() == IResource.FILE) {
					if ("html".equals(FilenameUtils.getExtension(current.getName()))) {
						try {
							findRelatedUnit((IFile) current);
						} catch(CoreException e) {
							log.warn("Couldn't find related unit for " + current.getName(), e);
						}
					}
				} else if (current.getType() == IResource.FOLDER) {
					// add all members in this folder to queue
					IResource[] members;
					try {
						members = ((IFolder) current).members();
						for (IResource member: members) {
							resourceQueue.add(member);
						}
					} catch(CoreException e) {
						log.warn("Couldn't get members of folder " + current.getName(), e);
					}
				}
				current= resourceQueue.poll();
			}
		} finally {
			if (monitor != null) {
				monitor.worked(getScanAndUpdateWork());
			}
		}
	}

	protected void validateAppSpecificationFile(IFile appSpecificationFile) throws TapestryException {
		IContainer webInfFolder= appSpecificationFile.getParent();

		if (webInfFolder == null) {
			throw new TapestryException("Expected " + WEB_INF_FOLDER_NAME
					+ " folder is null");
		}

		if (!WEB_INF_FOLDER_NAME.equals(webInfFolder.getName())) {
			throw new TapestryException("Expected " + WEB_INF_FOLDER_NAME
					+ " folder has unexpected name '" + webInfFolder.getName()
					+ "'");
		}

		webappFolder= webInfFolder.getParent();

		if (webappFolder == null) {
			throw new TapestryException("Expected " + WEBAPP_FOLDER_NAME
					+ " folder is null");
		}

		if (!WEBAPP_FOLDER_NAME.equals(webappFolder.getName())) {
			throw new TapestryException("Expected " + WEBAPP_FOLDER_NAME
					+ " folder has unexpected name '"
					+ webappFolder.getName() + "'");
		}
	}

	public void setHiveModule(HiveModuleDescriptor hiveModule) {
		this.hiveModuleDescriptor = hiveModule;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public IContainer getWebappFolder() {
		return webappFolder;
	}

	public void setWebappFolder(IFolder webappFolder) {
		this.webappFolder = webappFolder;
	}

	@Override
	public String toString() {
		return "TapestryModule [appSpecification=" + appSpecification.getAppSpecificationFile()
				+ ", moduleId=" + moduleId + ", webappFolder=" + webappFolder
				+ "]";
	}

	public IFile findRelatedFile(IFile currentFile) throws CoreException {
		ICompilationUnit compilationUnit= findRelatedUnit(currentFile);

		if (compilationUnit != null) {
			try {
				return (IFile) compilationUnit.getCorrespondingResource()
						.getAdapter(IFile.class);
			} catch (JavaModelException e) {
				log.warn("Couldn't get corresponding resource for compilation "
						+ "unit " + compilationUnit.getElementName(), e);
			}
		}

		return null;
	}

	private ICompilationUnit findRelatedUnit(IFile currentFile) throws
			CoreException {
		if (TapestryTools.isHtmlFile(currentFile)) {
			return findRelatedUnitForHtml(currentFile);
		} else if (TapestryTools.isPageSpecification(currentFile)) {
			return findRelatedUnitForSpecification(currentFile,
					"page-specification");
		} else if (TapestryTools.isComponentSpecification(currentFile)) {
			return findRelatedUnitForSpecification(currentFile,
					"component-specification");
		}

		return null;
	}

	private ICompilationUnit findRelatedUnitForHtml(IFile currentFile)
			throws CoreException {

		// try to find by component specification
		IFile specificationFile= TapestryTools.
				findComponentSpecificationforHtmlFile(currentFile);

		ICompilationUnit compilationUnit;

		if (specificationFile != null) {
			compilationUnit= findRelatedUnit(specificationFile);
			if (compilationUnit != null) {

				// create component and add to index
				TapestryHtmlElement component= new TapestryHtmlElement(
						this, ElementType.COMPONENT, currentFile,
						specificationFile, compilationUnit);
				add(component);

				return compilationUnit;
			}
		}

		// then try to find by page specification
		specificationFile= TapestryTools.
				findPageSpecificationforHtmlFile(currentFile);

		if (specificationFile != null) {
			compilationUnit=
					findRelatedUnit(specificationFile);
			if (compilationUnit != null) {

				// create page and add to index
				TapestryHtmlElement page= new TapestryHtmlElement(
						this, ElementType.PAGE, currentFile,
						specificationFile, compilationUnit);
				add(page);

				return compilationUnit;
			}
		}

		// at last, try directly and assemble fully qualified class name
		String javaFileName= extractFileBase(currentFile.getName()) + ".java";

		// go up until we find a folder named webapp or the project; every folder
		// on the way up contributes to the packageSuffix

		StringBuilder sb= new StringBuilder();
		IContainer parent= currentFile.getParent();
		while (!WEBAPP_FOLDER_NAME.equals(parent.getName().toLowerCase()) &&
				parent.getType() != IResource.PROJECT)  {
			sb.insert(0, parent.getName());
			sb.insert(0, '.');
			parent= parent.getParent();
		}

		String packageSuffix= sb.toString();

		// loop through all page packages
		Set<String> pageClassPackages=
				appSpecification.getPageClassPackages();
		if (pageClassPackages != null) {
			compilationUnit= findCompilationUnitInClassPackages(
					getProject(),
					pageClassPackages,
					packageSuffix,
					javaFileName);

			// create page and add to index
			TapestryHtmlElement page= new TapestryHtmlElement(
					this, ElementType.PAGE, currentFile,
					specificationFile, compilationUnit);
			add(page);

			return compilationUnit;
		}

		// resource is null here
		Set<String> componentClassPackages=
				appSpecification.getComponentClassPackages();
		if (componentClassPackages != null) {
			compilationUnit= findCompilationUnitInClassPackages(
					getProject(),
					componentClassPackages,
					packageSuffix,
					javaFileName);

			// create component and add to index
			TapestryHtmlElement component= new TapestryHtmlElement(
					this, ElementType.COMPONENT, currentFile,
					specificationFile, compilationUnit);
			add(component);

			return compilationUnit;
		}

		return null;
	}

	public ICompilationUnit findRelatedUnitForSpecification(IFile file,
			String specificationTagName) {
		// parse specification and extract data
		SpecificationParser sp;
		try
		{
			sp= new SpecificationParser(specificationTagName);
			sp.parse(file.getContents());
		}
		catch(UnsupportedEncodingException e)
		{
			log.warn("Unsupported encoding while trying to parse file " +
					file.getName(), e);
			return null;
		}
		catch(CoreException e)
		{
			log.warn("CoreException while trying to parse file " +
					file.getName(), e);
			return null;
		}

		String targetQualifiedClassName= sp.getTargetClass();

		if (targetQualifiedClassName == null
				|| "".equals(targetQualifiedClassName)) {
			log.warn("targetQualifiedClassName in specification is null or empty.");
			return null;
		}

		ICompilationUnit compilationUnit= findCompilationUnit(file.getProject(),
				targetQualifiedClassName + ".java");

		return compilationUnit;
	}

	public static ICompilationUnit findCompilationUnit(IProject project,
			String fullyQualifiedName) {

		IPackageFragment fragment;
		ICompilationUnit compilationUnit;
		IPackageFragmentRoot[] fragmentRoots;
		try {
			fragmentRoots = getPackageFragmentRoots(project);
		} catch (JavaModelException e) {
			log.warn("Could not get packageFragmentRoots of project "
					+ project.getName(), e);
			return null;
		} catch (CoreException e) {
			log.warn("Could not get packageFragmentRoots of project "
					+ project.getName(), e);
			return null;
		}

		// split fullyQualifiedName into package and simple name
		// int lastDotIndex= fullyQualifiedName.lastIndexOf('.');
		String packageName;
		String compilationUnitName;

		Pattern pattern= Pattern.compile("(?:(.*)\\.)?([^.]+\\.[^.]+)");
		Matcher matcher= pattern.matcher(fullyQualifiedName);
		if (matcher.matches()) {
			packageName= matcher.group(1);
			if (packageName == null) {
				packageName= "";	// default package
			}
			compilationUnitName= matcher.group(2);
		} else {
			return null;
		}


		/*
		if (lastDotIndex != -1) {
			packageName= fullyQualifiedName.substring(0, lastDotIndex);
			compilationUnitName= fullyQualifiedName.substring(lastDotIndex + 1,
					fullyQualifiedName.length());
		} else {
			// no dot found ==> try to retrieve class in default package
			packageName= "";
			compilationUnitName= fullyQualifiedName;
		}
		*/


		for (IPackageFragmentRoot root: fragmentRoots) {
			try {
				// only take into account source roots
				if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
					continue;
				}
			} catch (JavaModelException e) {
				log.warn("Could not get kind of packageFragmentRoot "
						+ project.getName(), e);
				continue;
			}

			fragment= root.getPackageFragment(packageName);
			if (fragment == null) {
				continue;
			}

			/*
			log.info("Trying package fragment: "
					+ fragment.getPath());
			*/

			compilationUnit= fragment.getCompilationUnit(compilationUnitName);
			if (compilationUnit != null && compilationUnit.exists()) {
				return compilationUnit;
			}
		}
		return null;
	}

	public static ICompilationUnit findCompilationUnitInClassPackages(IProject project,
			Iterable<String> classPackages, String packageSuffix,
			String resourceName) {

		IPackageFragment fragment;
		ICompilationUnit compilationUnit;
		IPackageFragmentRoot[] fragmentRoots;
		try {
			fragmentRoots = getPackageFragmentRoots(project);
		} catch (JavaModelException e) {
			log.warn("Could not get packageFragmentRoots of project "
					+ project.getName(), e);
			return null;
		} catch (CoreException e) {
			log.warn("Could not get packageFragmentRoots of project "
					+ project.getName(), e);
			return null;
		}

		if (classPackages == null) {
			// add empty string as classPackage if none supplied
			classPackages= Arrays.asList(new String[] {""});
		}

		// loop through class package names
		for (String packageName: classPackages) {
			for (IPackageFragmentRoot root: fragmentRoots) {
				try {
					// only take into account source roots
					if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
						continue;
					}
				} catch (JavaModelException e) {
					log.warn("Could not get kind of packageFragmentRoot "
							+ project.getName(), e);
					continue;
				}

				fragment= root.getPackageFragment(packageName + packageSuffix);
				if (fragment == null) {
					continue;
				}

				/*
				log.info("Trying package fragment: "
						+ fragment.getPath());
				*/

				compilationUnit= fragment.getCompilationUnit(resourceName);
				if (compilationUnit != null && compilationUnit.exists()) {
					return compilationUnit;
				}
			}
		}
		return null;
	}

	public void setTapestryIndex(TapestryIndex tapestryIndex) {
		tapestryIndexStore= tapestryIndex;
	}

	public TapestryIndex getTapestryIndex() {
		if (tapestryIndexStore == null) {
			tapestryIndexStore= Activator.getDefault().getTapestryIndex();
		}
		return tapestryIndexStore;
	}

	// serialization
	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException {
		stream.defaultWriteObject();
		EclipseSerializer.serializeResource(stream, webappFolder);
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		webappFolder= EclipseSerializer.deserializeResource(stream, workspaceRoot, IContainer.class);

		for (TapestryHtmlElement component: components) {
			component.setParent(this);
		}

		for (TapestryHtmlElement page: pages) {
			page.setParent(this);
		}
	}
}
