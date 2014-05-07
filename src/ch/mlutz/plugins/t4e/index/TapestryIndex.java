/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 index
 ******************************************************************************/
package ch.mlutz.plugins.t4e.index;

/**
 * A class to that stores and serializes relations between different files
 * making up a Tapestry 4 web application. Any logic generating these relations
 * is the responsibility of different, more higher-level classes.
 *
 * @author Marcel Lutz
 * @version 1.0 27.01.2014
 */

import static ch.mlutz.plugins.t4e.tools.EclipseTools.extractFileBase;
import static ch.mlutz.plugins.t4e.tools.EclipseTools.getPackageFragmentRoots;
import static ch.mlutz.plugins.t4e.tools.StringTools.join;
import static ch.mlutz.plugins.t4e.tapestry.element.ParameterDirection.*;
import static ch.mlutz.plugins.t4e.tapestry.element.ParameterType.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;

import ch.mlutz.plugins.t4e.Activator;
import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.serializer.EclipseSerializer;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;
import ch.mlutz.plugins.t4e.tapestry.element.IComponent;
import ch.mlutz.plugins.t4e.tapestry.element.Parameter;
import ch.mlutz.plugins.t4e.tapestry.element.ParameterType;
import ch.mlutz.plugins.t4e.tapestry.element.StandardComponent;
import ch.mlutz.plugins.t4e.tapestry.parsers.AppSpecificationParser;
import ch.mlutz.plugins.t4e.tapestry.parsers.SpecificationParser;

import java.util.Collections;

/**
 * Stores the associations between Tapestry4 specification files and the
 * corresponding java files in maps. Offers in general the command
 * handle(file) and the query getCorrespondingFile(file)
 *
 * @author Marcel Lutz
 */
public class TapestryIndex implements Serializable {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			TapestryIndex.class);

	// TODO: move index functionality down to class TapestryModule

	// TODO: update project should scan through resources and identify
	// app specifications, parse them and the hivemodule.xmls

	// TODO: make Map from IContainer to TapestryModule for fast lookup

	// TODO: Serialize should serialize all modules

	public TapestryModule getModuleForResource(IResource resource) {
		// TODO: loop through ancestors of resource until one of them
		// is a module root
		//
		IContainer currentContainer= resource.getParent();

		while (currentContainer != null) {
			TapestryModule module= webappFolderMap.get(currentContainer);
			if (module != null) {
				return module;
			}
			currentContainer= currentContainer.getParent();
		}
		return null;
	}

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4467549554390808167L;

	private static final String WEBAPP_FOLDER_NAME= "webapp";

	// Debug
	private static final boolean UPDATE_VERBOSE= false;

	// transient member variables
	private transient AppSpecificationParser appSpecificationParser= new AppSpecificationParser();

	/*
	private Set<IFile> webDeploymentDescriptors= new HashSet<IFile>();
	private IFile mainWebDeploymentDescriptor= null;
	*/

	// maps saving data
	private Map<IProject, Set<IFile>> appSpecificationMap= new HashMap<IProject, Set<IFile>>();
	private Map<IProject, Set<String>> componentClassPackageMap= new HashMap<IProject, Set<String>>();
	private Map<IProject, Set<String>> pageClassPackageMap= new HashMap<IProject, Set<String>>();

	/*
	 *  these map makes the connection spec ==> java; the connection
	 *  html ==> spec is done by TapestryTools.findXYSpecificationforHtmlFile
	 *  this is the first priority in lookup
	 */
	private Map<IFile, IFile> specificationToJavaMap= new HashMap<IFile, IFile>();
	private Map<IFile, Set<IFile>> javaToSpecificationMap= new HashMap<IFile, Set<IFile>>();

	/*
	 * this map is used for html files that have no specification but are
	 * connected to java file by name; this has second priority in lookup;
	 * this applies to both pages and components
	 */
	private Map<IFile, IFile> htmlToJavaMap= new HashMap<IFile, IFile>();

	/*
	 * this map is used for java files that are connected to their html files;
	 * by name, without specification
	 */
	private Map<IFile, IFile> javaToHtmlMap= new HashMap<IFile, IFile>();

	// logProvider will be set by owning handler
	// protected transient LogProvider logProvider= null;

	public IContentType getHtmlContentType() {
		return Platform.getContentTypeManager().getContentType("org.eclipse.wst.html.core.htmlsource");
	}

	// ctors
	public TapestryIndex() {}

	/*
	public TapestryIndex(LogProvider logProvider) {
		this.logProvider= logProvider;
	}
	*/

	public void addAppSpecification(IFile file) {
		IProject project= file.getProject();
		addValueToMapListByKey(appSpecificationMap, project, file);
	}

	public void addComponentClassPackages(IProject project, String[] packages) {
		addValuesToMapListByKey(componentClassPackageMap, project, packages);
	}

	public void addPageClassPackages(IProject project, String[] packages) {
		addValuesToMapListByKey(pageClassPackageMap, project, packages);
	}

	private <K, V> void addValueToMapListByKey(Map<K, Set<V>> map, K key, V value) {
		Set<V> set= map.get(key);
		if (set != null) {
			set.add(value);
		} else {
			set= new HashSet<V>();
			set.add(value);
			map.put(key, set);
		}
	}

	private <K, V> void addValuesToMapListByKey(Map<K, Set<V>> map, K key, V[] values) {
		Set<V> list= map.get(key);
		if (list != null) {
			list.addAll(Arrays.asList(values));
		} else {
			list= new HashSet<V>(Arrays.asList(values));
			map.put(key, list);
		}
	}

	public Set<String> getComponentClassPackages(IProject project) {
		return componentClassPackageMap.get(project);
	}

	public Set<String> getPageClassPackages(IProject project) {
		return pageClassPackageMap.get(project);
	}

	public void logMessage(String s) {
		System.out.println(s);
	}


	public void handleHtmlFile(IFile file) {
		/* DEBUG
		if ("Home.html".equals(file.getName())) {
			EclipseTools.logMessage("Home.html found.");
			try
			{
				file.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
				createMarkerForResource(file, IMarker.TASK, 20, "TODO fix this problem.");
				createMarkerForResource(file, IMarker.PROBLEM, 21, "There is a PROBLEM here.", IMarker.SEVERITY_ERROR);
				createMarkerForResource(file, IMarker.PROBLEM, 22, "There is a WARNING here.", IMarker.SEVERITY_WARNING);
				createMarkerForResource(file, IMarker.PROBLEM, 23, "INFO for source here.", IMarker.SEVERITY_INFO);
				createMarkerForResource(file, IMarker.BOOKMARK, 24, "Just a random BOOKMARK");
			}
			catch(CoreException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		*/

		// TODO: search for html file by package
	}

	public IMarker createMarkerForResource(IResource resource, String type,
			int lineNumber, String message) throws CoreException {
		IMarker marker = resource.createMarker(type);
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		marker.setAttribute(IMarker.MESSAGE, message);
		// marker.setAttribute(IMarker.TRANSIENT, true);
		return marker;

		/*
		if (marker.exists()) {
			EclipseTools.logMessage("Marker exists.");
			//Once we have a marker object, we can set its attributes
			// marker.setAttribute("coolFactor", "ULTRA");
			// marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, "Variable 'i' has not been defined.");
			marker.setAttribute(IMarker.LINE_NUMBER, 20);
		} else {
			EclipseTools.logMessage("Marker doesn't exist!");
		}
		*/
	}

	public IMarker createMarkerForResource(IResource resource, String type,
			int lineNumber, String message, int severity) throws CoreException {
		IMarker marker= createMarkerForResource(resource, type, lineNumber, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		return marker;
	}

	public void printStatistic() {
		// TODO: implement new one
	}

	public void addHtmlToJavaEntry(IFile htmlFile, IFile javaFile) {
		htmlToJavaMap.put(htmlFile, javaFile);
		javaToHtmlMap.put(javaFile, htmlFile);
	}

	public void addSpecificationToJavaEntry(IFile specificationFile, IFile javaFile) {
		specificationToJavaMap.put(specificationFile, javaFile);
		addValueToMapListByKey(javaToSpecificationMap, javaFile, specificationFile);
	}

	public IFile getJavaForSpecificationFile(IFile specificationFile) {
		return specificationToJavaMap.get(specificationFile);
	}

	public Set<String> getComponentClassPackagesByProject(IProject project)
	{
		return componentClassPackageMap.get(project);
	}

	public Set<String> getPageClassPackagesByProject(IProject project)
	{
		return pageClassPackageMap.get(project);
	}

	// modifiers
	public void clear() {
		appSpecificationMap.clear();
		componentClassPackageMap.clear();
		pageClassPackageMap.clear();

		// NEW INFRASTRUCTURE
		projects.clear();
		relationMap.clear();
		// relationToCompilationUnit.clear();
		webappFolderMap.clear();
		modules.clear();
		documentToFileMap.clear();
		if (standardComponents != null) {
			standardComponents.clear();
		}
	}

	public void clear(IProject project) {
		if (project == null) {
			// logProvider.logWarning("Parameter project is null in tapestryIndex.clear(IProject project)");
			return;
		}

		appSpecificationMap.remove(project);
		componentClassPackageMap.remove(project);
		pageClassPackageMap.remove(project);
	}

	// serialization
	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException {
		EclipseSerializer.serializeMapResourceSet(stream, appSpecificationMap);
		EclipseSerializer.serializeMapStringSet(stream, componentClassPackageMap);
		EclipseSerializer.serializeMapStringSet(stream, pageClassPackageMap);

		EclipseSerializer.serializeMapOfResources(stream, specificationToJavaMap);
		EclipseSerializer.serializeMapResourceSet(stream, javaToSpecificationMap);

		EclipseSerializer.serializeMapOfResources(stream, htmlToJavaMap);
		EclipseSerializer.serializeMapOfResources(stream, javaToHtmlMap);

		EclipseSerializer.serializeResourceSet(stream, projects);

		stream.writeObject(modules);

		/*

		private Map<IContainer, TapestryModule> webappFolderMap=
				new HashMap<IContainer, TapestryModule>();

		public Set<IProject> getProjects() {
			return Collections.unmodifiableSet(projects);
		}
		*/
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {

	    // IMPORTANT: set reference in Activator first; otherwise, we might get
	    // multiple instances of TapestryIndex
	    Activator.getDefault().setTapestryIndex(this);

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		appSpecificationMap= EclipseSerializer.deserializeMapResourceSet(stream,
				workspaceRoot,
				IProject.class, IFile.class);
		componentClassPackageMap= EclipseSerializer.deserializeMapStringSet(
				stream, workspaceRoot,
				IProject.class);
		pageClassPackageMap= EclipseSerializer.deserializeMapStringSet(
				stream, workspaceRoot,
				IProject.class);

		specificationToJavaMap= EclipseSerializer.deserializeMapOfResources(stream,
				workspaceRoot,
				IFile.class, IFile.class);
		javaToSpecificationMap= EclipseSerializer.deserializeMapResourceSet(
				stream, workspaceRoot,
				IFile.class, IFile.class);

		htmlToJavaMap= EclipseSerializer.deserializeMapOfResources(stream,
				workspaceRoot,
				IFile.class, IFile.class);
		javaToHtmlMap= EclipseSerializer.deserializeMapOfResources(stream,
				workspaceRoot,
				IFile.class, IFile.class);

		projects= Collections.synchronizedSet(
				EclipseSerializer.deserializeResourceSet(stream,
				workspaceRoot, IProject.class));

		// initialize transient variables
        appSpecificationParser= new AppSpecificationParser();

        relationMap= new HashMap<IFile, Object>();

        webappFolderMap= new HashMap<IContainer, TapestryModule>();

        documentToFileMap= new HashMap<IDocument, IFile>();

        /*
        relationToCompilationUnit=
            new HashMap<IFile, ICompilationUnit>();
        */

		@SuppressWarnings("unchecked")
		Set<TapestryModule> readObject = (Set<TapestryModule>) stream.readObject();
		modules= readObject;

		// postprocessing
		for (TapestryModule module: modules) {
			webappFolderMap.put(module.getWebappFolder(), module);
		}
	}

	/*
	 * NEW INFRASTRUCTURE
	 *
	 */

	private Set<IProject> projects= Collections.synchronizedSet(new HashSet<IProject>());

	private transient Map<IFile, Object> relationMap= new HashMap<IFile, Object>();

	/*
	private transient Map<IFile, ICompilationUnit> relationToCompilationUnit=
		new HashMap<IFile, ICompilationUnit>();
	*/

	private transient Map<IContainer, TapestryModule> webappFolderMap=
			new HashMap<IContainer, TapestryModule>();

	private Set<TapestryModule> modules= new HashSet<TapestryModule>();

	private transient Map<IDocument, IFile> documentToFileMap= new HashMap<IDocument, IFile>();

	private transient List<IComponent> standardComponents= null;

	public Set<IProject> getProjects() {
		return Collections.unmodifiableSet(projects);
	}

	/**
	 * Checks if the Tapestry Index contains the given project. When the index
	 * contains a project, this means that either its modules have been added
	 * to the index as well or that a job to add the modules is pending.
	 *
	 * @param project the project to check if it is in the index
	 * @return true iff the index contains project
	 */
	public boolean contains(IProject project) {
		return projects.contains(project);
	}

	/**
	 * Adds a project to the TapestryIndex.
	 *
	 * @param project the project to add to the index.
	 * @return
	 */
	public void add(IProject project) {
		projects.add(project);
	}

	public void remove(IProject project) {
		projects.remove(project);
	}

	public Set<TapestryModule> getModules() {
		return Collections.unmodifiableSet(modules);
	}

	public boolean contains(TapestryModule module) {
		return projects.contains(module);
	}

	public void add(TapestryModule module) {
		modules.add(module);
		webappFolderMap.put(module.getWebappFolder(), module);
	}

	public void remove(TapestryModule module) {
		webappFolderMap.remove(module.getWebappFolder());
		modules.remove(module);
	}

	public List<TapestryModule> getModulesForProject(IProject project) {
		List<TapestryModule> result= new ArrayList<TapestryModule>();
		for (TapestryModule module: modules) {
			if  (module.getProject().equals(project)) {
				result.add(module);
			}
		}
		return result;
	}

	public TapestryModule getModuleForWebappFolder(IContainer container) {
		return webappFolderMap.get(container);
	}

	public synchronized void addBidiRelation(IFile file1, Object object) {
		if (object instanceof IFile) {
			addRelation(file1, object);
			addRelation((IFile) object, file1);
		} else if (object instanceof ICompilationUnit) {
			addRelation(file1, object);
			try {
				addRelation((IFile) ((ICompilationUnit) object)
						.getCorrespondingResource().getAdapter(IFile.class),
						file1);
			} catch (JavaModelException e) {
				log.warn("Couldn't get corresponding resource of compilation "
						+ "unit " + ((ICompilationUnit) object).getElementName(), e);
			}
		}
	}

	public synchronized void addRelation(IFile from, Object to) {
		Object existingTo= relationMap.get(from);
		if (existingTo == null) {
			relationMap.put(from, to);
		} else {
			if (!(existingTo instanceof Set)) {
				Set<Object> toSet= new HashSet<Object>();
				if (existingTo instanceof IFile
						|| existingTo instanceof ICompilationUnit) {
					toSet.add(existingTo);
				}
				existingTo= toSet;
				relationMap.put(from, existingTo);
			}

			// existingTo is definitely Set<IFile>
			@SuppressWarnings("unchecked")
			Set<Object> toSet= (Set<Object>) existingTo;
			toSet.add(to);
		}
	}

	/*
	public synchronized void addRelationToCompilationUnit(IFile from,
			ICompilationUnit compilationUnit) {
		relationToCompilationUnit.put(from, compilationUnit);
	}
	*/

	public synchronized ICompilationUnit getRelatedCompilationUnit(IFile file) {
		Set<Object> relatedObjects= getRelatedObjects(file);
		if (relatedObjects == null) {
		    return null;
		}

		for (Object relatedObject: relatedObjects) {
		    if (relatedObject instanceof ICompilationUnit) {
		        return (ICompilationUnit) relatedObject;
		    }
		}
		return null;
	}


	/**
	 * Always returns a set (possibly empty)
	 *
	 * @param file
	 * @return
	 */
	public synchronized Set<Object> getRelatedObjects(IFile file) {
		Object to= relationMap.get(file);
		Set<Object> result= new HashSet<Object>();
		if (to instanceof IFile || to instanceof ICompilationUnit) {
			result.add(to);
			return result;
		} else if (to instanceof Set) {
			@SuppressWarnings("unchecked")
			Set<Object> toSet= (Set<Object>) to;
			return toSet;
		} else {
			return result;
		}
	}

	/*
	 * ... If multiple targets for file exist, returns just the first one
	 *
	 * @param file
	 * @return
	 *
	public synchronized IFile getRelatedFile(IFile file) {
		Object to= relationMap.get(file);
		if (to instanceof IFile) {
			return (IFile) to;
		} else if (to instanceof Set) {
			@SuppressWarnings("unchecked")
			Set<IFile> toSet= (Set<IFile>) to;
			if (toSet.size() > 0) {
				List<IFile> fileList= new ArrayList<IFile>();
				fileList.addAll(toSet);
				return fileList.get(0);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	*/

	public synchronized void removeAllRelations(Object o) {
	    // NOTE: always first use remove To, because it needs
	    // existing From relations to do it's job
	    removeRelationsTo(o);
	    removeRelationsFrom(o);
	}

	public synchronized void removeRelationsFrom(Object from) {
		if (from instanceof IFile) {
		    relationMap.remove(from);
		} else if (from instanceof ICompilationUnit) {
		    try {
	            IFile javaFile= (IFile) ((ICompilationUnit) from)
	                .getCorrespondingResource().getAdapter(IFile.class);
	            relationMap.remove(javaFile);
	        } catch(JavaModelException e) {
	            log.warn("Could not get corresponding resource from compilation"
	                + " unit " + ((ICompilationUnit) from).getElementName(), e);
	        }

		}
	}

	public synchronized void removeRelationsTo(Object to) {
		IFile toFile= null;
		if (to instanceof IFile) {
			toFile= (IFile) to;
		} else if (to instanceof ICompilationUnit) {
			toFile= TapestryIndexer.getCorrespondingFileForCompilationUnit(
					(ICompilationUnit) to);
		}

		if (to != null) {
			Set<Object> fromSet= getRelatedObjects(toFile);

			for (Object from: fromSet) {
				IFile fromFile= null;

				if (from instanceof IFile) {
					fromFile= (IFile) from;
				} else if (from instanceof ICompilationUnit) {
					fromFile= TapestryIndexer
							.getCorrespondingFileForCompilationUnit(
									(ICompilationUnit) from);
			    }

				if (fromFile != null) {
					Set<Object> toSet= getRelatedObjects(fromFile);

					if (toSet.size() == 1 && toSet.contains(to)) {
						relationMap.remove(fromFile);
					} else {
						toSet.remove(to);
					}
				}
			}
		}
	}

	// the following three methods are used by TapestryDocumentProvider and
	// TapestryEditor
	public synchronized void addDocumentToFileMapping(IDocument document, IFile file) {
		documentToFileMap.put(document, file);
	}

	public synchronized IFile getDocumentToFileMapping(IDocument document) {
		return documentToFileMap.get(document);
	}

	public synchronized void removeDocumentToFileMapping(IDocument document) {
		documentToFileMap.remove(document);
	}

	public synchronized List<IComponent> getStandardComponents() {
		if (standardComponents == null) {
			String[] componentJwcIds= new String[] {
				"Else", "Image", "InsertText", "Script",
				"ScriptIncludes", "Style", "Frame", "Shell",
				"Block", "Body", "Delegator", "InvokeListener",
				"Describe", "ExceptionDisplay", "Relation", "RenderBlock", "RenderBody"
			};

			standardComponents= new ArrayList<IComponent>();

			standardComponents.add(
					new StandardComponent("Insert",
							new Parameter("value", OBJECT, IN, false, null),
							new Parameter("format", FORMAT, IN, false, null),
							new Parameter("class", STRING, IN, false, null),
							new Parameter("raw", BOOLEAN, IN, false, Boolean.FALSE)
					)
			);

			standardComponents.add(
					new StandardComponent("Any",
							new Parameter("element", STRING, IN, false, null)
					)
			);

			standardComponents.add(
					new StandardComponent("If",
							new Parameter("condition", BOOLEAN, IN, true, null),
							new Parameter("conditionvalue", BOOLEAN, IN, false, null),
							new Parameter("listener", LISTENER, IN, false, null),
							new Parameter("renderTag", BOOLEAN, IN, false, Boolean.TRUE),
							new Parameter("element", STRING, IN, false, null),
							new Parameter("volatile", BOOLEAN, IN, false, Boolean.FALSE)
					)
			);

			standardComponents.add(
					new StandardComponent("For",
							new Parameter("source", COLLECTION, IN, true, null),
							new Parameter("value", OBJECT, OUT, false, null),
							new Parameter("index", INTEGER, OUT, false, null),
							new Parameter("renderTag", BOOLEAN, IN, false, Boolean.TRUE),
							new Parameter("element", STRING, IN, false, null),
							new Parameter("keyExpression", STRING, IN, false, null),
							new Parameter("fullSource", COLLECTION, IN, false, null),
							new Parameter("defaultValue", OBJECT, IN, false, null),
							new Parameter("converter", PRIMARYKEYCONVERTER, IN, false, null),
							new Parameter("primaryKeys", COLLECTION, OUT, false, null),
							new Parameter("match", BOOLEAN, IN, false, Boolean.TRUE),
							new Parameter("volatile", BOOLEAN, IN, false, Boolean.FALSE)
					)
			);

			for (String jwcId: componentJwcIds) {
				StandardComponent component= new StandardComponent(jwcId);
				standardComponents.add(component);
			}
		}
		return standardComponents;
	}

	public Map<String, IComponent> getStandardComponentMap() {
		Map<String, IComponent> result= new HashMap<String, IComponent>();
		List<IComponent> standardComponents= getStandardComponents();
		for (IComponent c: standardComponents) {
			result.put(c.getName(), c);
		}
		return result;
	}
}
