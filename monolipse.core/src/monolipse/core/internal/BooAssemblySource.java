package monolipse.core.internal;

import java.io.*;
import java.util.*;

import monolipse.core.*;
import monolipse.core.foundation.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.io.xml.*;


public class BooAssemblySource implements IAssemblySource {
	
	private static final String SETTINGS_CHARSET = "utf-8";

	private static final String SETTINGS_FILE = ".monolipse";
	
	private static final QualifiedName ASSEMBLY_SOURCE_SESSION_KEY = new QualifiedName("monolipse.core.resources", "BooAssemblySourceSession");
	
	public static IAssemblySource create(IFolder folder, AssemblySourceLanguage language) throws CoreException {
		synchronized (folder) {
			IAssemblySource source = BooAssemblySource.get(folder);
			if (null != source)
				return source;
			source = internalCreate(folder, language);
			source.save(null);
			return source;
		}
	}
	
	public static BooAssemblySource get(IFolder folder) throws CoreException {
		synchronized (folder) {
			BooAssemblySource source = (BooAssemblySource) folder.getSessionProperty(ASSEMBLY_SOURCE_SESSION_KEY);
			if (source == null) {
				if (isAssemblySource(folder)) {
					source = internalCreate(folder, null);
				}
			}
			return source;
		}
	}

	private static BooAssemblySource internalCreate(IFolder folder, AssemblySourceLanguage defaultLanguage) throws CoreException {
		BooAssemblySource source = new BooAssemblySource(folder, defaultLanguage);
		folder.setSessionProperty(ASSEMBLY_SOURCE_SESSION_KEY, source);
		return source;
	}
	
	public static boolean isAssemblySource(Object element) {
		try {
			return element instanceof IFolder
				&& BooAssemblySource.isAssemblySource((IFolder)element);
		} catch (CoreException x) {
			x.printStackTrace();
		}
		return false;
	}

	public static boolean isAssemblySource(IFolder folder) throws CoreException {
		return folder.getFile(SETTINGS_FILE).exists();
	}
	
	private IFolder _folder;

	private IAssemblyReference[] _references;
	
	private String _outputType;

	private AssemblySourceLanguage _language;

	private IFolder _outputFolder;

	private String _additionalOptions;

	BooAssemblySource(IFolder folder, AssemblySourceLanguage defaultLanguage) throws CoreException {
		if (null == folder || !folder.exists()) throw new IllegalArgumentException();
		_folder = folder;
		_language = defaultLanguage;
		_outputFolder = null;
		refresh(null);
	}

	public void setLanguage(AssemblySourceLanguage language) {
		_language = language;
	}
	
	public AssemblySourceLanguage getLanguage() {
		return _language == null
			? AssemblySourceLanguage.BOO
			: _language;
	}
	
	public void setOutputFolder(IFolder folder) {
		_outputFolder = folder;
	}
	
	public IFolder getOutputFolder() throws CoreException {
		return _outputFolder != null ? _outputFolder : defaultOutputFolder();
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#getFolder()
	 */
	public IFolder getFolder() {
		return _folder;
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#getSourceFiles()
	 */
	public IFile[] getSourceFiles() throws CoreException {
		final List<IResource> files = new ArrayList<IResource>();
		IResourceVisitor visitor = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (isBooFile(resource) && resource.exists()) {
					files.add(resource);
				}
				return true;
			}
		};
		_folder.accept(visitor, IResource.DEPTH_INFINITE, IResource.FILE);
		return files.toArray(new IFile[files.size()]);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(IFolder.class)) {
			return _folder;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#setReferences(monolipse.core.IBooAssemblyReference[])
	 */
	public void setReferences(IAssemblyReference... references) {
		if (null == references) throw new IllegalArgumentException("references");
		_references = references;
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#getReferences()
	 */
	public IAssemblyReference[] getReferences() {
		return _references;
	}

	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#getOutputType()
	 */
	public String getOutputType() {
		return _outputType;
	}
	
	public void setOutputType(String outputType) {
		if (null == outputType) throw new IllegalArgumentException();
		if (!outputType.equals(OutputType.CONSOLE_APPLICATION)
			&& !outputType.equals(OutputType.WINDOWS_APPLICATION)
			&& !outputType.equals(OutputType.LIBRARY)) {
			throw new IllegalArgumentException("outputType");
		}
		_outputType = outputType;
	}

	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#getOutputFile()
	 */
	public IFile getOutputFile() throws CoreException {
		return getOutputFolder().getFile(assemblyFileName());
	}

	private String assemblyFileName() {
		return assemblyName() + getOutputAssemblyExtension();
	}

	private String assemblyName() {
		return _folder.getName();
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#refresh(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile file = getSettingsFile();
				file.refreshLocal(IResource.DEPTH_ZERO, monitor);
				if (!file.exists()) {
					useDefaultSettings();
					save(monitor); 
				} else {
					loadSettingsFrom(file);
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}
	
	static public class AssemblySourceRemembrance {
		public String language;
		public String outputType;
		public IRemembrance[] references;
		public String outputFolder;
		public String additionalOptions;
		public AssemblySourceRemembrance(BooAssemblySource source) throws CoreException {
			language = source.getLanguage().id();
			outputType = source.getOutputType();
			references = new IRemembrance[source._references.length];
			outputFolder = source.hasOutputFolder() ? source.getOutputFolder().getFullPath().toPortableString() : null;
			additionalOptions = source.getAdditionalOptions();
			for (int i=0; i<references.length; ++i) {
				references[i] = source._references[i].getRemembrance();
			}
		}
		
		/**
		 * public no arg constructor for xstream deserialization
		 * on less capable virtual machines.
		 */
		public AssemblySourceRemembrance() {
		}
		
		public IAssemblyReference[] activateReferences() throws CoreException {
			IAssemblyReference[] asmReferences = new IAssemblyReference[references.length];
			for (int i=0; i<asmReferences.length; ++i) {
				asmReferences[i] = activateReference(references[i]);
			}
			return asmReferences;
			                                                                
		}

		private IAssemblyReference activateReference(IRemembrance ref) {
			try {
				return (IAssemblyReference) ref.activate();
			} catch (CoreException e) {
				return new AssemblyReferenceError(e, ref);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see monolipse.core.IBooAssemblySource#save()
	 */
	public void save(IProgressMonitor monitor) throws CoreException {
		XStream stream = createXStream();
		String xml = stream.toXML(new AssemblySourceRemembrance(this));
		IFile file = getSettingsFile();
		if (!file.exists()) {
			file.create(encode(xml), true, monitor);
			file.setCharset(SETTINGS_CHARSET, monitor);
		} else {
			file.setContents(encode(xml), true, true, monitor);
		}
	}

	public boolean hasOutputFolder() {
		return _outputFolder != null;
	}

	private void loadSettingsFrom(IFile file) throws CoreException {
		AssemblySourceRemembrance remembrance = (AssemblySourceRemembrance) createXStream().fromXML(decode(file));
		String language = remembrance.language;
		_language = isEmptyOrNull(language)
			? AssemblySourceLanguage.BOO
			: AssemblySourceLanguage.forId(language);
		
		_outputType = remembrance.outputType;
		_references = remembrance.activateReferences();
		_additionalOptions = remembrance.additionalOptions;
		
		String path = remembrance.outputFolder;
		_outputFolder = isEmptyOrNull(path)
			? null
			: WorkspaceUtilities.getFolder(path);
	}
	
	private boolean isEmptyOrNull(String language) {
		return language == null || language.length() == 0;
	}

	private IFile getSettingsFile() {
		return _folder.getFile(SETTINGS_FILE);
	}
	
	private XStream createXStream() {
		XStream stream = new XStream(new DomDriver());
		stream.alias("settings", AssemblySourceRemembrance.class);
		return stream;
	}

	private InputStream encode(String xml) throws CoreException {
		try {
			return new ByteArrayInputStream(xml.getBytes(SETTINGS_CHARSET));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			WorkspaceUtilities.throwCoreException(e);
		}
		return null;
	}

	private Reader decode(IFile file) throws CoreException {
		try {
			return new InputStreamReader(file.getContents(), file.getCharset());
		} catch (IOException e) {
			e.printStackTrace();
			WorkspaceUtilities.throwCoreException(e);
		}
		return null;
	}

	private void useDefaultSettings() {
		_references = getLanguage() == AssemblySourceLanguage.BOO
			? defaultBooAssemblyReferences()
			: new IAssemblyReference[0];
		_outputType = OutputType.CONSOLE_APPLICATION;
	}

	private IAssemblyReference[] defaultBooAssemblyReferences() {
		return new IAssemblyReference[] {
			BooCore.createBooAssemblyReference("Boo.Lang"),
			BooCore.createBooAssemblyReference("Boo.Lang.PatternMatching")
		};
	}

	private String getOutputAssemblyExtension() {
		if (getLanguage() == AssemblySourceLanguage.BOOJAY)
			return ".jar";
		return OutputType.LIBRARY.equals(getOutputType()) ? ".dll" : ".exe";
	}
	
	boolean isBooFile(IResource resource) {
		if (IResource.FILE != resource.getType()) return false;
		final String extension = resource.getFileExtension();
		if (extension == null) return false;
		return extension.equalsIgnoreCase(expectedSourceFileExtension());
	}

	private String expectedSourceFileExtension() {
		return getLanguage().fileExtension();
	}

	public static IAssemblySource getContainer(IResource resource) throws CoreException {
		IContainer parent = resource instanceof IContainer
						? (IContainer)resource
						: resource.getParent();
		while (null != parent && IResource.FOLDER == parent.getType()) {
			BooAssemblySource source = get((IFolder)parent);
			if (null != source) return source;
			parent = parent.getParent();
		}
		return null;
	}

	public boolean visitReferences(IAssemblyReferenceVisitor visitor) throws CoreException {
		for (IAssemblyReference r : _references)
			if (!r.accept(visitor)) return false;
		return true;
	}
	
	public String toString() {
		return _folder.getFullPath().toString();
	}

	public static boolean references(IAssemblySource l, final IAssemblySource r) {
		IAssemblyReference[] references = l.getReferences();
		for (int i = 0; i < references.length; ++i) {
			IAssemblyReference reference = references[i];
			if (reference instanceof IAssemblySourceReference) {
				if (r == ((IAssemblySourceReference) reference).getAssemblySource()) {
					return true;
				}
			}
		}
		return false;
	}

	public String getAdditionalOptions() {
		return _additionalOptions == null ? "" : _additionalOptions;
	}

	public void setAdditionalOptions(String additionalOptions) {
		_additionalOptions = additionalOptions;
	}
	
	private IFolder defaultOutputFolder() throws CoreException {
		return _language == AssemblySourceLanguage.BOOJAY
			? javaOutputFolder()
			: projectFolder("bin");
	}

	private IFolder projectFolder(String path) {
		return project().getFolder(path);
	}

	private IFolder javaOutputFolder() throws JavaModelException {
		return workspaceFolder(javaProject().getOutputLocation());
	}

	private IFolder workspaceFolder(IPath location) {
		return workspaceRoot().getFolder(location);
	}

	private IWorkspaceRoot workspaceRoot() {
		return workspace().getRoot();
	}

	private IWorkspace workspace() {
		return project().getWorkspace();
	}

	private IJavaProject javaProject() {
		return JavaCore.create(project());
	}

	private IProject project() {
		return _folder.getProject();
	}

}
