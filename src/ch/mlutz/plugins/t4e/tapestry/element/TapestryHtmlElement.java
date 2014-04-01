/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - Tapestry 4 element
 ******************************************************************************/
package ch.mlutz.plugins.t4e.tapestry.element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.serializer.EclipseSerializer;
import ch.mlutz.plugins.t4e.tapestry.TapestryModule;

/**
 * @author Marcel Lutz
 * @version 1.0 23.03.2014
 */

public class TapestryHtmlElement extends TapestryElement
		implements IComponent, Comparable<TapestryHtmlElement> {

	/**
	 *
	 */
	private static final long serialVersionUID = -29507081696210709L;

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			TapestryHtmlElement.class);

	TapestryModule parent;
	ElementType type;
	IFile htmlFile;
	IFile specification;
	ICompilationUnit javaCompilationUnit;

	public TapestryHtmlElement(TapestryModule parent, ElementType type,
			IFile htmlFile) {
		super();

		if (htmlFile == null) {
			throw new IllegalArgumentException("Parameter htmlFile for "
					+ "TapestryHtmlElement can't be null.");
		}

		this.parent= parent;
		this.type= type;
		this.htmlFile= htmlFile;
	}

	/**
	 *
	 * @param parent
	 * @param type
	 * @param htmlFile the underlying html file. Can't be null.
	 * @param specification
	 * @param javaCompilationUnit
	 */
	public TapestryHtmlElement(TapestryModule parent, ElementType type,
			IFile htmlFile, IFile specification,
			ICompilationUnit javaCompilationUnit) {
		this(parent, type, htmlFile);

		this.specification=			specification;
		this.javaCompilationUnit=	javaCompilationUnit;
	}

	@Override
	public String getName() {
		return FilenameUtils.removeExtension(htmlFile.getName());
	}

	public String getPath() {
		StringBuilder sb= new StringBuilder();
		IContainer webAppFolder= parent.getWebappFolder();
		IContainer currentContainer= htmlFile.getParent();
		while (!currentContainer.equals(webAppFolder)) {
			if (sb.length() > 0) {
				sb.insert(0, "/");
			}
			sb.insert(0, currentContainer.getName());
			currentContainer= currentContainer.getParent();
		}
		if (sb.length() > 0) {
			sb.append("/");
		}
		sb.append(FilenameUtils.getBaseName(htmlFile.getName()));
		return sb.toString();
	}

	@Override
	public ElementType getType() {
		return type;
	}

	public List<Pair<IFile, IFile>> getRelations() {
		List<Pair<IFile, IFile>> result= new ArrayList<Pair<IFile, IFile>>();
		IFile javaFile= null;

		if (javaCompilationUnit != null) {
			try {
				IResource javaResource = javaCompilationUnit.getCorrespondingResource();
				if (javaResource != null) {
					javaFile= (IFile) javaResource.getAdapter(IFile.class);
				}
			} catch (JavaModelException e) {
				log.warn("Can't get correspondingResource for "
						+ javaCompilationUnit.getElementName());
			}
		}

		if (javaFile != null) {
			result.add(new ImmutablePair<IFile, IFile>(htmlFile,
					javaFile));
			if (specification != null) {
				result.add(new ImmutablePair<IFile, IFile>(specification,
						javaFile));
			}
		}

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((htmlFile == null) ? 0 : htmlFile.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (!(obj instanceof TapestryHtmlElement)) {
			return false;
		}
		TapestryHtmlElement other = (TapestryHtmlElement) obj;
		if (htmlFile == null) {
			if (other.htmlFile != null) {
				return false;
			}
		} else if (!htmlFile.equals(other.htmlFile)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	// serialization
	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException, JavaModelException {
		stream.writeObject(type);
		EclipseSerializer.serializeResource(stream, htmlFile);
		EclipseSerializer.serializeResource(stream, specification);
		EclipseSerializer.serializeJavaElement(stream, javaCompilationUnit);
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		type= (ElementType) stream.readObject();
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		htmlFile= EclipseSerializer.deserializeResource(stream,
				workspaceRoot, IFile.class);
		specification= EclipseSerializer.deserializeResource(stream,
				workspaceRoot, IFile.class);
		javaCompilationUnit= EclipseSerializer.deserializeJavaElement(
				stream, workspaceRoot, ICompilationUnit.class);
	}

	/**
	 * @return the parent
	 */
	public TapestryModule getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(TapestryModule parent) {
		this.parent = parent;
	}

	public IFile getHtmlFile()
	{
		return htmlFile;
	}

	public ICompilationUnit getJavaCompilationUnit()
	{
		return javaCompilationUnit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TapestryHtmlElement o)
	{
		String thisName = getPath();
		String anotherString = o.getPath();
		return thisName.compareTo(anotherString);
	}

	/* (non-Javadoc)
	 * @see ch.mlutz.plugins.t4e.tapestry.element.IComponent#getParameters()
	 */
	@Override
	public List<Parameter> getParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public IFile getSpecification()
	{
		return specification;
	}
}
