package org.openoffice.maven.packager;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.openoffice.maven.AbstractOOoMojo;
import org.openoffice.maven.utils.ClassReader;

/**
 * Base class for creating a jar from project classes.
 * 
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: AbstractJarMojo.java 611327 2008-01-11 23:15:17Z dennisl $
 */
public abstract class AbstractOxtMojo extends AbstractOOoMojo {
    
    private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html", "CVS", "**/CVS", ".cvsignore",
                    "**/.cvsignore" };

    private static final String[] DEFAULT_INCLUDES = new String[] { };

    /**
     * List of files to include. Specified as fileset patterns.
     * 
     * @parameter
     */
    private String[] includes;

    /**
     * List of files to exclude. Specified as fileset patterns.
     * 
     * @parameter
     */
    private String[] excludes;

    /**
     * Directory containing the generated JAR.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * Name of the generated JAR.
     * 
     * @parameter alias="jarName" expression="${jar.finalName}"
     *            default-value="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * The Jar archiver.
     * 
     * @parameter
     *            expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The archive configuration to use.
     * See <a
     * href="http://maven.apache.org/shared/maven-archiver/index.html">the
     * documentation for Maven Archiver</a>.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Path to the default MANIFEST file to use. It will be used if
     * <code>useDefaultManifestFile</code> is set to <code>true</code>.
     * 
     * @parameter
     *            expression="${project.build.outputDirectory}/META-INF/MANIFEST.MF"
     * @required
     * @readonly
     * @since 2.2
     */
    private File defaultManifestFile;

    /**
     * Set this to <code>true</code> to enable the use of the
     * <code>defaultManifestFile</code>.
     * 
     * @parameter expression="${jar.useDefaultManifestFile}"
     *            default-value="false"
     * @since 2.2
     */
    private boolean useDefaultManifestFile;

    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * Whether creating the archive should be forced.
     * 
     * @parameter expression="${jar.forceCreation}" default-value="false"
     */
    private boolean forceCreation;
    
    /**
     * Whether the dependent jars should be added to the plugin or not.
     * 
     * @parameter default-value="false"
     */
    protected boolean addDependencies;

    /**
     * Return the specific output directory to serve as the root for the
     * archive.
     */
    protected abstract File getClassesDirectory();

    /**
     * Overload this to produce a jar with another classifier, for example a
     * test-jar.
     */
    protected abstract String getClassifier();

    protected static File getJarFile(File basedir, String finalName, String classifier) {
        String cl = classifier;
        
        if (cl == null) {
            cl = "";
        } else if (cl.trim().length() > 0 && !cl.startsWith("-")) {
            cl = "-" + cl;
        }

        return new File(basedir, finalName + cl + ".jar");
    }

    /**
     * Default Manifest location. Can point to a non existing file.
     * Cannot return null.
     */
    protected File getDefaultManifestFile() {
        return defaultManifestFile;
    }

    /**
     * Generates the JAR.
     * 
     * @todo Add license files in META-INF directory.
     */
    public File createArchive() throws MojoExecutionException {
        File jarFile = getJarFile(outputDirectory, finalName, getClassifier());

        MavenArchiver archiver = new MavenArchiver();
        assert jarArchiver != null;
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(jarFile);
        archive.setForced(forceCreation);

        try {
            File contentDirectory = getClassesDirectory();
            assert contentDirectory != null;
            if (!contentDirectory.exists()) {
                getLog().warn("JAR will be empty - no content was marked for inclusion!");
            } else {
                archiver.getArchiver().addDirectory(contentDirectory, getIncludes(), getExcludes());
                handleManifest();
            }

            assert project.getArtifact() != null;
            getLog().debug("createArchive(..) with project " + project);
            archiver.createArchive(project, archive);

            return jarFile;
        } catch (ArchiverException ae) {
            throw new MojoExecutionException("can't create archive " + jarFile, ae);
        } catch (ManifestException me) {
            throw new MojoExecutionException("problem with manifest " + defaultManifestFile, me);
        } catch (IOException ioe) {
            throw new MojoExecutionException("can't create archive " + jarFile, ioe);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }
    }

    protected String getLibname(File file) {
        return "lib/" + file.getName();
    }

    private void handleManifest() {
        File existingManifest = getDefaultManifestFile();
        assert existingManifest != null;
        if (useDefaultManifestFile && existingManifest.exists() && archive.getManifestFile() == null) {
            getLog().info("Adding existing MANIFEST to archive. Found under: " + existingManifest.getPath());
            archive.setManifestFile(existingManifest);
        } else {
            archive.addManifestEntry("ManifestVersion", "1.0");
            addRegistrationClassNameToManifest();
            addClasspathToManifest();
        }
    }

    private void addRegistrationClassNameToManifest() {
        if (hasRegistrationClassNameInManifest()) {
            return;
        }
        try {
            String registrationClassName = findRegistrationClassName(getClassesDirectory());
            getLog().info("Adding RegistrationClassName=" + registrationClassName + " to MANIFEST");
            archive.addManifestEntry("RegistrationClassName", registrationClassName);
        } catch (FileNotFoundException fnfe) {
            getLog().warn(fnfe);
        }
    }
    
    private boolean hasRegistrationClassNameInManifest() {
        Map<?, ?> entries = archive.getManifestEntries();
        for (Object key : entries.keySet()) {
            if ("RegistrationClassName".equals(key)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Normally I would ask Maven to do the job to add a classpath entry to the
     * generated classpath. But when I tried it I with
     * <code>archive.getManifest().setAddClasspath(true);</code>
     * I got an error in 
     * MavenArchiver.getManifest(MavenProject, ManifestConfiguration, Map)
     * because no artifact is set.
     */
    @SuppressWarnings("unchecked")
    private void addClasspathToManifest() {
        StringBuilder classpath = new StringBuilder(".");
        if (this.oxtDir.exists()) {
            Collection<File> jarfiles = FileUtils.listFiles(this.oxtDir, new String[] { "jar" }, true);
            for (File file : jarfiles) {
                classpath.append(" " + basename(this.oxtDir, file));
            }
            if (this.addDependencies) {
                for (File file : getDependentJars()) {
                    classpath.append(" " + getLibname(file));
                }
            }
        } else {
            getLog().info(this.oxtDir + " does not exist - ignored for generated Class-Path entry");
        }
        archive.addManifestEntry("Class-Path", classpath.toString());
        getLog().debug("added to Class-Path: " + classpath);
    }
    
    
    /**
     * Gets the dependent jars. The code was inspired from
     * {@link "http://svn.supose.org/mlv/trunk/licenses-verifier-plugin/src/main/java/com/soebes/maven/plugins/mlv/AbstractLicenseVerifierPlugIn.java"}.
     *
     * @return the dependent jars
     */
    @SuppressWarnings("unchecked")
    protected Collection<File> getDependentJars() {
        Collection<File> jars = new ArrayList<File>();
        Set<Artifact> artifacts = this.getProject().getArtifacts();
        for (Artifact artifact : artifacts) {
            if (!("test".equalsIgnoreCase(artifact.getScope()))
                    && (!artifact.getGroupId().startsWith("org.openoffice"))) {
                jars.add(artifact.getFile());
            }
        }
        return jars;
    }

    private static String basename(final File dir, final File file) {
        String path = file.getPath();
        return path.substring(dir.getPath().length() + 1);
    }

    @SuppressWarnings("unchecked")
    private String findRegistrationClassName(File contentDirectory) throws FileNotFoundException {
        Collection<File> classfiles = FileUtils.listFiles(contentDirectory, new String[] { "class" }, true);
        for (File file : classfiles) {
            if (file.getName().equals("RegistrationHandler.class")) {
                return ClassReader.getAsClassname(contentDirectory, file);
            }
        }
        return findRegistrationClassNameByContent(contentDirectory);
    }

    @SuppressWarnings("unchecked")
    private String findRegistrationClassNameByContent(File contentDirectory) throws FileNotFoundException {
        Collection<File> classfiles = FileUtils.listFiles(contentDirectory, new String[] { "class" }, true);
        for (File file : classfiles) {
            ClassReader classReader = new ClassReader(contentDirectory, file);
            if (classReader.hasOOoRegistryMethods()) {
                return classReader.getClassname();
            }
        }
        throw new FileNotFoundException("no RegistrationHandler found in " + contentDirectory);
    }
    
    /**
     * Generates the JAR.
     * 
     * @todo Add license files in META-INF directory.
     */
    public void execute() throws MojoExecutionException {
        File jarFile = createArchive();

        String classifier = getClassifier();
        if (classifier != null) {
            projectHelper.attachArtifact(getProject(), "jar", classifier, jarFile);
        } else {
            projectHelper.attachArtifact(getProject(), "jar", null, jarFile);
        }
    }

    protected String[] getIncludes() {
        if (includes != null && includes.length > 0) {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    protected String[] getExcludes() {
        if (excludes != null && excludes.length > 0) {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }
}
