/*************************************************************************
 * IdlBuilderMojoTest.java
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

package org.openoffice.maven.idl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.openoffice.maven.AbstractTest;
import org.openoffice.maven.Environment;
import org.openoffice.maven.idl.IdlBuilderMojo.PackageNameFilter;

/**
 * JUnit test for IdlBuilderMojo.
 * 
 * @author oliver
 * @since 1.2 (02.08.2010)
 */
public final class IdlBuilderMojoTest extends AbstractMojoTestCase {
    
    /**
     * Set up the mojo.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        AbstractTest.setUpEnvironment();
    }
    
    /**
     * This unit test was copied from
     * {@link "http://maven.apache.org/plugin-developers/plugin-testing.html"}.
     * But it does not work. So it is now marked as "broken".
     *
     * @throws Exception in case of error
     */
    public void brokentestMojoGoal() throws Exception {
        File testPom = new File(getBasedir(), "src/main/resources/archetype-resources/pom.xml");
        IdlBuilderMojo mojo = (IdlBuilderMojo) lookupMojo("build-idl", testPom);
        assertNotNull(mojo);
    }

    /**
     * This unit test was copied from
     * {@link "https://cwiki.apache.org/confluence/display/MAVENOLD/Maven+Plugin+Harness"}.
     *
     * @throws Exception in case of error
     */
    public void testSettingMojoVariables() throws Exception {
        IdlBuilderMojo mojo = new IdlBuilderMojo();
        File value = new File("/opt/");
        setVariableValueToObject(mojo, "ooo", value);
        assertEquals(value, (File) getVariableValueFromObject(mojo, "ooo"));
    }

    /**
     * Test method for {@link org.openoffice.maven.idl.IdlBuilderMojo#execute()}.
     *
     * @throws IllegalAccessException the illegal access exception
     * @throws MojoExecutionException the mojo execution exception
     * @throws MojoFailureException the mojo failure exception
     * @throws IOException if "types.rdb" can't be copied
     */
    public void testExecute() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException {
        IdlBuilderMojo mojo = new IdlBuilderMojo();
        setVariableValueToObject(mojo, "ooo", Environment.getOfficeHome());
        setVariableValueToObject(mojo, "sdk", Environment.getOoSdkHome());
        initIdlDir(mojo);
        initResources(mojo);
        File buildDir = new File(getBasedir(), "target");
        setVariableValueToObject(mojo, "directory", buildDir);
        setVariableValueToObject(mojo, "outputDirectory", new File(buildDir, "test-classes"));
        FileUtils.copyFile(new File("src/test/resources/types.rdb"), new File(buildDir, "types.rdb"));
        mojo.execute();
    }

    private void initIdlDir(IdlBuilderMojo mojo) throws IllegalAccessException {
        File idlDir = new File(getBasedir(), "src/main/resources/archetype-resources/src/main/resources/idl");
        setVariableValueToObject(mojo, "idlDir", idlDir);
    }

    private void initResources(IdlBuilderMojo mojo) throws IllegalAccessException {
        List<Resource> resources = new ArrayList<Resource>();
        Resource rsc = new Resource();
        rsc.setDirectory("src/test/resources");
        resources.add(rsc);
        setVariableValueToObject(mojo, "resources", resources);
    }
    
    public void testPackageNameFilter() {
        IdlBuilderMojo.PackageNameFilter filter = new PackageNameFilter();
        File dir = new File(getBasedir());
        assertTrue("'de' is a valid package name", filter.accept(dir, "de"));
        assertFalse("'CVS' should be considered as invalid", filter.accept(dir, "CVS"));
    }

}