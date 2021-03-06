package monolipse.core.foundation;

import java.io.*;
import java.net.URL;

import monolipse.core.BooCore;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

public class WorkspaceUtilities {
	public static void createTree(IFolder folder) throws CoreException {
		IContainer parent = folder.getParent();
		if (isFolder(parent) && !parent.exists())
			createTree((IFolder)parent);
		if (!folder.exists())
			folder.create(true, true, null);
	}

	public static void ensureDerivedParentExists(IFile file, IProgressMonitor monitor) throws CoreException {
		IContainer parent = file.getParent();
		if (isFolder(parent)) {
			createTree((IFolder)parent);
			parent.setDerived(true, monitor);
		}
	}

	private static boolean isFolder(IContainer parent) {
		return IResource.FOLDER == parent.getType();
	}
	
	public static String getLocation(IResource resource) {
		return resource.getLocation().toOSString();
	}
	
	public static IFolder getFolder(String path) {
		return getWorkspaceRoot().getFolder(new Path(path));
	}
	
	public static IFile getFile(String path) {
		return getWorkspaceRoot().getFile(new Path(path));
	}
	
	public static IFile getFileForLocation(String path) {
		return getWorkspaceRoot().getFileForLocation(new Path(path));
	}

	public static IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public static void throwCoreException(IOException e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, BooCore.ID_PLUGIN, -1, e.getLocalizedMessage(), e));
	}

	public static String getPortablePath(IResource resource) {
		return resource.getFullPath().toPortableString();
	}

	public static String getResourceLocalPath(Bundle bundle, String resourcePath) throws IOException {
		URL url = FileLocator.find(bundle, new Path(resourcePath), null);
		if (url == null)
			throw new FileNotFoundException(resourcePath);
		return new File(FileLocator.toFileURL(url).getFile()).getCanonicalPath();
	}
}
