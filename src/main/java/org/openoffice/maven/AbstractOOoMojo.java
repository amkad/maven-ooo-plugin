/*************************************************************************
 * AbstractOOoMojo.java
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
 * 
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 *
 * Contributor(s): oliver.boehm@agentes.de
 ************************************************************************/

package org.openoffice.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * Abstract superclass for all mojos to be able to store the configured
 * parameters in one place.
 * 
 * @author oliver (oliver.boehm@agentes.de)
 * @since 1.1.1 (06.11.2010)
 */
public abstract class AbstractOOoMojo extends AbstractMojo {
    
    /**
     * OOo instance to build the extension against.
     * 
     * @parameter
     */
    protected File ooo;

    /**
     * OOo SDK installation where the build tools are located.
     * 
     * @parameter
     */
    protected File sdk;
    
    /**
     * IDL directory where the IDL sources can be found
     * 
     * @parameter expression="src/main/idl"
     */
    protected File idlDir;

    /**
     * OXT directory where the OXT sources can be found
     * 
     * @parameter expression="src/main/oxt"
     */
    protected File oxtDir;

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Gets the project.
     *
     * @return the project
     */
    public final MavenProject getProject() {
        return project;
    }

}
