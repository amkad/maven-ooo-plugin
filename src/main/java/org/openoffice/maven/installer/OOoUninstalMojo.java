package org.openoffice.maven.installer;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openoffice.maven.AbstractOOoMojo;
import org.openoffice.maven.ConfigurationManager;

/**
 * 
 * @author Frederic Morin <frederic.morin.8@gmail.com>
 * 
 * @goal uninstall
 */
public class OOoUninstalMojo extends AbstractOOoMojo {

    /**
     * @parameter default-value="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List<Artifact> attachedArtifacts;

    /**
     * <p>This method uninstall an openoffice plugin package.</p>
     * 
     * @throws MojoExecutionException
     *             if there is a problem during the packaging execution.
     * @throws MojoFailureException
     *             if the packaging can't be done.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        ooo = ConfigurationManager.initOOo(ooo);
        getLog().info("OpenOffice.org used: " + ooo.getAbsolutePath());

        sdk = ConfigurationManager.initSdk(sdk);
        getLog().info("OpenOffice.org SDK used: " + sdk.getAbsolutePath());

        Artifact unoPlugin = null;
        for (Artifact attachedArtifact : attachedArtifacts) {
            String extension = FilenameUtils.getExtension(attachedArtifact.getFile().getPath());
            if ("zip".equals(extension) || "oxt".equals(extension)) {
                unoPlugin = attachedArtifact;
                break;
            }
        }

        if (unoPlugin == null) {
            throw new MojoExecutionException("Could not find plugin artefact (.zip)");
        }
        
        File unoPluginFile = unoPlugin.getFile();

        try {
            String unopkg = OOoInstalMojo.getUnopkgName();

            getLog().info("Uninstalling plugin to OOo... please wait");
            int returnCode = ConfigurationManager.runCommand(unopkg, "remove", unoPluginFile.getCanonicalPath());            
            if (returnCode == 0) {
                getLog().info("Plugin installed successfully");
            } else {
                throw new MojoExecutionException("'unopkg remove " + unoPluginFile + "' returned with " + returnCode);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while uninstalling package to OOo.", e);
        }
    }

}
