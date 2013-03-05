/*************************************************************************
 *
 * $RCSfile: IdlcVisitor.java,v $
 *
 * $Revision: 1.1 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2007/10/08 18:35:15 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.openoffice.maven.idl;

import java.io.File;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.openoffice.maven.ConfigurationManager;
import org.openoffice.maven.Environment;
import org.openoffice.maven.utils.IVisitable;
import org.openoffice.maven.utils.VisitableFile;

/**
 * Visits all the IDL files and build them.
 * 
 * @author Cedric Bosdonnat
 */
public class IdlcVisitor extends AbstractVisitor {

    private boolean mFoundIdlFile = false;
    
    /**
     * @return <code>true</code> if one IDL file have been build,
     *         <code>false</code> otherwise
     */
    public boolean hasBuildIdlFile() {
        return mFoundIdlFile;
    }

    /**
     * {@inheritDoc}
     */
    public boolean visit(IVisitable pNode) throws Exception {

        boolean visitChildren = false;
        VisitableFile file = (VisitableFile) pNode;

        if (file.isFile() && file.canRead()) {
            // Try to compile the file if it is an IDL file
            if (file.getName().endsWith(".idl")) {
                runIdlcOnFile(file);
                mFoundIdlFile = true;
            }
        } else if (file.isDirectory()) {
            visitChildren = true;
        }
        return visitChildren;
    }

    /**
     * Executes the <code>idlc</code> tool on the provided IDL file.
     * 
     * @param pFile
     *            the IDL file to compile
     * @throws Exception
     *             if the idl file compilation fails
     */
    private static void runIdlcOnFile(final VisitableFile pFile) throws Exception {

        getLog().info("Building file: " + pFile.getPath());

        String idlPath = ConfigurationManager.getIdlDir().getAbsolutePath();
        String idlRelativePath = pFile.getParentFile().getAbsolutePath().substring(idlPath.length());

        File outDir = new File(ConfigurationManager.getUrdDir(), idlRelativePath);
        outDir.mkdirs();
        File sdkIdl = new File(ConfigurationManager.getSdk(), "idl");
        File prjIdl = ConfigurationManager.getIdlDir();

        getLog().debug("output dir: " + outDir);

//        int n = ConfigurationManager.runCommand("idlc", "-O", outDir.getPath(), "-I", sdkIdl.getPath(), "-I",
//                prjIdl.getPath(), pFile.getPath());
        int n;
        String idlc = "idlc";
        try {
            n = runIdlc(idlc, outDir, sdkIdl, prjIdl, pFile);
        } catch (CommandLineException cle) {
            idlc = new File(Environment.getSdkBinPath(), "idlc").getPath();
            getLog().warn("'idlc' failed - trying now '" + idlc + "'...", cle);
            n = runIdlc(idlc, outDir, sdkIdl, prjIdl, pFile);
        }
        if (n != 0) {
            throw new CommandLineException(idlc + " exits with " + n);
        }
    }

    private static int runIdlc(final String idlc, final File outDir, final File sdkIdl, final File prjIdl,
            final VisitableFile pFile) throws CommandLineException {
        return ConfigurationManager.runCommand(idlc, "-O", outDir.getPath(), "-I", sdkIdl.getPath(), "-I",
                prjIdl.getPath(), pFile.getPath());
    }
    
}
