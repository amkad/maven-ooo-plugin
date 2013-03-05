package org.openoffice.maven.installer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openoffice.maven.AbstractOOoMojo;
import org.openoffice.maven.ConfigurationManager;

/**
 * @author Frederic Morin <frederic.morin.8@gmail.com>
 * @goal install
 * @phase install
 */
public class OOoInstalMojo extends AbstractOOoMojo {
    
    /**
     * The POM file.
     * 
     * @parameter default-value="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * The place where the plugin should be installed to.
     * 
     * @parameter
     */
    private String install;
    
    /**
     * The artifact factory.
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * The ArtifactInstaller for installation.
     * 
     * @component
     */
    private ArtifactInstaller installer;
    
    /**
     * The local Maven repository (normally ~/.m2/repository).
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * <p>
     * This method install an openoffice plugin package to the specified
     * openoffice installation
     * </p>
     * 
     * @throws MojoExecutionException
     *             if there is a problem during the packaging execution.
     * @throws MojoFailureException
     *             if the packaging can't be done.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (isRepositoryInstallationRequired()) {
            installToRepository();
        } else {
            installOOoPlugin();
        }
    }

    private boolean isRepositoryInstallationRequired() {
        String name = System.getProperty("org.openoffice.maven.install", install);
        return "repository".equalsIgnoreCase(name);
    }

    private void installToRepository() throws MojoExecutionException {
        File unoPluginFile = project.getArtifact().getFile();
        if (!unoPluginFile.exists()) {
            throw new MojoExecutionException("Could not find plugin artefact [" + unoPluginFile + "]");
        }
        try {
            installer.install(unoPluginFile, project.getArtifact(), localRepository);
//          installer.install(pomFile, project.getArtifact(), localRepository);
            installPOM();
            for (Object obj : project.getAttachedArtifacts()) {
                Artifact artifact = (Artifact) obj;
                installer.install(artifact.getFile(), artifact, localRepository);
            }
        } catch (ArtifactInstallationException e) {
            throw new MojoExecutionException("can't install " + unoPluginFile + " to " + localRepository, e);
        }
    }
    
    private void installPOM() throws ArtifactInstallationException {
        Artifact artifact = project.getArtifact();
        Artifact pomArtifact = artifactFactory.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getBaseVersion());
        pomArtifact.setFile(pomFile);
        installer.install(pomFile, pomArtifact, localRepository);
    }
    
    private void installOOoPlugin() throws MojoExecutionException {
        ooo = ConfigurationManager.initOOo(ooo);
        getLog().info("OpenOffice.org used: " + ooo.getAbsolutePath());

        sdk = ConfigurationManager.initSdk(sdk);
        getLog().info("OpenOffice.org SDK used: " + sdk.getAbsolutePath());

        File unoPluginFile = project.getArtifact().getFile();
        if (!unoPluginFile.exists()) {
            throw new MojoExecutionException("Could not find plugin artefact [" + unoPluginFile + "]");
        }

        try {
            String unopkg = getUnopkgName();

            getLog().info("Installing plugin to OOo... please wait");
            int returnCode = ConfigurationManager.runCommand(unopkg, "add", "-v", "-f",
                    unoPluginFile.getCanonicalPath());
            if (returnCode == 0) {
                getLog().info("Plugin installed successfully");
            } else {
                throw new MojoExecutionException("'unopkg add -v -f " + unoPluginFile + "' returned with " + returnCode);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while installing package to OOo.", e);
        }
    }

    /**
     * Gets the name of the unopkg command.
     * 
     * @return "unopkg" or "unopkg.com"
     */
    protected static String getUnopkgName() {
        String os = System.getProperty("os.name").toLowerCase();
        String unopkg = "unopkg";
        if (os.startsWith("windows")) {
            unopkg = "unopkg.com";
        }
        return unopkg;
    }

}
