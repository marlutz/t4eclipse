/*******************************************************************************
 * Copyright (c) 2014 Made in Switzerland, Marcel Lutz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of The MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     Marcel Lutz - serializers for Eclipse entities
 ******************************************************************************/
package ch.mlutz.plugins.t4e.serializer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;

/**
 * @author Marcel Lutz
 * @version 1.0 16.02.2014
 *
 */
public class EclipseSerializer {

	/**
	 * the Log
	 */
	public static final IEclipseLog log= EclipseLogFactory.create(
			EclipseSerializer.class);

	public static <E extends IResource> void serializeResource(
			ObjectOutputStream outputStream,
			E element)
					throws IOException {
		if (element != null) {
			outputStream.writeObject(element.getLocationURI());
		} else {
			outputStream.writeObject(null);
		}
	}

	public static <E extends IResource> E deserializeResource(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<E> elementClass)
					throws IOException, ClassNotFoundException {

		Object uri;
		if ((uri= inputStream.readObject()) instanceof URI) {
			IResource[] resources= getIResourceFromURI((URI) uri, elementClass);

			if (resources.length == 0) {
				log.warn("No resource found for URI: " + uri);
				return null;
			} else if (resources.length > 1) {
				log.warn("More than one resource found for URI: " + uri);
			}

			@SuppressWarnings("unchecked")
			E adapter = (E) resources[0].getAdapter(elementClass);
			return adapter;
		}
		return null;
	}

	public static <E extends IResource> void serializeResourceSet(
			ObjectOutputStream outputStream,
			Set<E> entitySet)
					throws IOException {
		outputStream.writeInt(entitySet.size());
		for (E element: entitySet) {
			serializeResource(outputStream, element);
		}
	}

	public static <E extends IResource> Set<E> deserializeResourceSet(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<E> elementClass)
					throws IOException, ClassNotFoundException {
		Set<E> result= new HashSet<E>();
		int size= inputStream.readInt();
		E element;

		for (int i= 0; i < size; i++) {
			element= deserializeResource(inputStream, workspaceRoot,
					elementClass);
			result.add(element);
		}
		return result;
	}

	public static <K extends IResource, V extends IResource>
	void serializeMapOfResources(
			ObjectOutputStream outputStream,
			Map<K, V> entitiesMap)
					throws IOException {
		Set<K> keySet= entitiesMap.keySet();
		outputStream.writeInt(keySet.size());
		for (K key: keySet) {
			serializeResource(outputStream, key);
			serializeResource(outputStream, entitiesMap.get(key));
		}
	}

	public static <K extends IResource, V extends IResource> Map<K, V>
	deserializeMapOfResources(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<K> keyClass,
			Class<V> valueClass)
					throws IOException, ClassNotFoundException {

		Map<K, V> result= new HashMap<K, V>();
		int size= inputStream.readInt();
		K key;
		V value;

		for (int i= 0; i < size; i++) {
			key= deserializeResource(inputStream, workspaceRoot, keyClass);
			value= deserializeResource(inputStream, workspaceRoot, valueClass);
			result.put(key, value);
		}
		return result;
	}

	public static <K extends IResource, V extends Serializable>
	void serializeMapFromResource(
			ObjectOutputStream outputStream,
			Map<K, V> entitiesMap)
					throws IOException {
		Set<K> keySet= entitiesMap.keySet();
		outputStream.writeInt(keySet.size());
		for (K key: keySet) {
			serializeResource(outputStream, key);
			outputStream.writeObject(entitiesMap.get(key));
		}
	}

	public static <K extends IResource, V extends Serializable> Map<K, V>
	deserializeMapFromResource(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<K> keyClass,
			Class<V> valueClass)
					throws IOException, ClassNotFoundException {

		Map<K, V> result= new HashMap<K, V>();
		int size= inputStream.readInt();

		for (int i= 0; i < size; i++) {
			K key= deserializeResource(inputStream, workspaceRoot, keyClass);
			@SuppressWarnings("unchecked")
			V value = (V) inputStream.readObject();
			result.put(key, value);
		}
		return result;
	}

	public static <K extends IResource, E extends IResource>
	void serializeMapResourceSet(
			ObjectOutputStream outputStream,
			Map<K, Set<E>> entitiesMapSet)
					throws IOException {
		Set<K> keySet= entitiesMapSet.keySet();
		outputStream.writeInt(keySet.size());
		for (K key: keySet) {
			serializeResource(outputStream, key);
			serializeResourceSet(outputStream, entitiesMapSet.get(key));
		}
	}

	public static <K extends IResource, E extends IResource> Map<K, Set<E>>
	deserializeMapResourceSet(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<K> keyClass,
			Class<E> elementClass)
					throws IOException, ClassNotFoundException {

		Map<K, Set<E>> result= new HashMap<K, Set<E>>();
		int size= inputStream.readInt();
		K key;
		Set<E> value;

		for (int i= 0; i < size; i++) {
			key= deserializeResource(inputStream, workspaceRoot, keyClass);
			value= deserializeResourceSet(inputStream, workspaceRoot, elementClass);
			result.put(key, value);
		}
		return result;
	}

	public static <K extends IResource> void
			serializeMapStringSet(
			ObjectOutputStream outputStream,
			Map<K, Set<String>> entitiesMapSet)
					throws IOException {
		Set<K> keySet= entitiesMapSet.keySet();
		outputStream.writeInt(keySet.size());
		for (K key: keySet) {
			serializeResource(outputStream, key);
			outputStream.writeObject(entitiesMapSet.get(key));
		}
	}

	public static <K extends IResource> Map<K, Set<String>>
			deserializeMapStringSet(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<K> keyClass)
					throws IOException, ClassNotFoundException {
		Map<K, Set<String>> result= new HashMap<K, Set<String>>();
		int size= inputStream.readInt();

		for (int i= 0; i < size; i++) {
			K key= deserializeResource(inputStream, workspaceRoot, keyClass);
			@SuppressWarnings("unchecked")
			Set<String> value= (Set<String>) inputStream.readObject();
			result.put(key, value);
		}
		return result;
	}

	public static <E extends IJavaElement>
	void serializeJavaElement(
			ObjectOutputStream outputStream,
			E javaElement)
					throws IOException {

		IResource resource= null;
		try {
			if (javaElement != null) {
				resource= javaElement.getCorrespondingResource();
			}
		} catch(JavaModelException e) {
			log.warn("Couldn't serialize JavaElement: ", e);
		}
		serializeResource(outputStream, resource);
	}

	public static <E extends IJavaElement>
	E deserializeJavaElement(
			ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot,
			Class<E> keyClass)
					throws IOException, ClassNotFoundException {
		IResource resource= deserializeResource(inputStream,
				workspaceRoot, IResource.class);
		if (resource instanceof IFile) {
			IFile file= (IFile) resource;
			@SuppressWarnings("unchecked")
			E javaElement= (E) JavaCore.createCompilationUnitFrom(file)
					.getAdapter(keyClass);
			return javaElement;
		}
		return null;
	}

	public static <E> IResource[] getIResourceFromURI(URI uri,
			Class<E> elementClass) {
		// the getRoot().find... methods return not null but empty arrays
		IResource[] result= new IResource[0];
		if (IContainer.class.isAssignableFrom(elementClass)) {
			result= ResourcesPlugin.getWorkspace().getRoot()
				.findContainersForLocationURI(uri);
		} else {
			result= ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocationURI(uri);
		}
		return result;
	}

	/*
	public static void serializeIFileSet(ObjectOutputStream outputStream,
			Set<IFile> iFileSet) throws IOException {
		outputStream.writeInt(iFileSet.size());
		for (IFile f: iFileSet) {
			outputStream.writeObject(f.getLocationURI());
		}
	}

	public static Set<IFile> deserializeIFileSet(ObjectInputStream inputStream,
			IWorkspaceRoot workspaceRoot)
			throws IOException, ClassNotFoundException {
		Set<IFile> result= new HashSet<IFile>();
		int size= inputStream.readInt();
		Object path;
		IResource resource;
		IFile file;

		for (int i= 0; i < size; i++) {
			if ((path= inputStream.readObject()) instanceof String) {
				resource= workspaceRoot.findMember((String) path);
				if ((file= (IFile) resource.getAdapter(IFile.class)) != null) {
					result.add(file);
				}
			}
		}
		return result;
	}
	 */
}


