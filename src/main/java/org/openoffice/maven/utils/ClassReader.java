/*************************************************************************
 * ClassReader.java
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

package org.openoffice.maven.utils;

import java.io.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.sun.star.registry.XRegistryKey;

/**
 * This is a simple reader for classes to be able to look if it there is a
 * wanted method inside.
 * Perhaps it would be better to use a disassembler and not the classloader
 * to look for certain methods. But it is only a first draft for the moment...
 * 
 * @author oliver (oliver.boehm@agentes.de)
 * @since 1.1.1 (25.10.2010)
 */
public class ClassReader extends ClassLoader {
    
    private final File baseDir;
    private String classname;
    private Class<?> clazz;
    private byte[] bytecode;

    /**
     * Instantiates a new class reader.
     *
     * @param classesDir the classes dir (base directory)
     */
    public ClassReader(final File classesDir) {
        this.baseDir = classesDir;
    }
    
    /**
     * Instantiates a new class reader.
     *
     * @param classesDir the classes dir
     * @param classFile the class file
     */
    public ClassReader(final File classesDir, final File classFile) {
        this(classesDir);
        this.classname = this.getAsClassname(classFile);
        try {
            this.clazz = this.loadClass(classFile);
        } catch (ClassNotFoundException e) {
            this.clazz = null;
        } catch (NoClassDefFoundError e) {
            this.clazz = null;
        }
    }
    
    /**
     * Gets the classname.
     *
     * @return the classname
     */
    public String getClassname() {
        return this.classname;
    }

    /**
     * Load class.
     *
     * @param name the name
     * @return the class
     * @throws ClassNotFoundException the class not found exception
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if ((this.classname != null) && name.equals(this.classname)) {
            if (this.clazz == null) {
                this.clazz = super.loadClass(name);
            }
            return this.clazz;
        }
        return super.loadClass(name);
    }

    /**
     * Load class.
     *
     * @param classfile the classfile
     * @return the class
     * @throws ClassNotFoundException the class not found exception
     */
    public Class<?> loadClass(final File classfile) throws ClassNotFoundException {
        if (this.clazz == null) {
            this.classname = this.getAsClassname(classfile);
            this.clazz = loadClass();
        }
        return this.clazz;
    }
    
    /**
     * Load class.
     *
     * @return the class
     * @throws ClassNotFoundException the class not found exception
     */
    public Class<?> loadClass() throws ClassNotFoundException {
        if (this.clazz == null) {
            this.clazz = findLoadedClass(this.classname);
            if (this.clazz == null) {
                this.clazz = findClass(this.classname);
            }
        }
        return this.clazz;
    }

    /**
     * Loads the given classname.
     *
     * @param name the name
     * @return the class
     * @throws ClassNotFoundException the class not found exception
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            if (name.equals(this.classname)) {
                if (this.clazz == null) {
                    this.bytecode = loadClassData(name);
                    this.clazz = defineClass(name, this.bytecode, 0, this.bytecode.length);
                }
                return this.clazz;
            }
            byte[] b = loadClassData(name);
            return defineClass(name, b, 0, b.length);
        } catch (ClassFormatError e) {
            throw new ClassNotFoundException("wrong format for " + name, e);
        } catch (IOException ioe) {
            throw new ClassNotFoundException("can't read class for " + name, ioe);
        }
    }
    
    /**
     * Load class data.
     *
     * @param name the name
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private byte[] loadClassData(String name) throws IOException {
        String filename = name.replace('.', '/') + ".class";
        File classfile = new File(baseDir, filename);
        InputStream istream = new FileInputStream(classfile);
        try {
            return IOUtils.toByteArray(istream);
        } finally {
            istream.close();
        }
    }
    
    /**
     * Checks if there is a __writeRegistryServiceInfo and
     * __getComponentFactory method inside.
     *
     * @return true, if successful
     */
    public boolean hasOOoRegistryMethods() {
        if (this.clazz != null) {
            try {
                clazz.getMethod("__writeRegistryServiceInfo", new Class[] { XRegistryKey.class });
                clazz.getMethod("__getComponentFactory", new Class[] { String.class });
            } catch (NoSuchMethodException e) {
                return false;
            } catch (NoClassDefFoundError e) {
                return hasOOoRegistryMethodsInBytecode();
            }
            return true;
        } else {
            return hasOOoRegistryMethodsInBytecode();
        }
    }

    /**
     * We scan here the bytecode for the OOo specific
     * registry methods.
     * 
     * TODO: disassemble the bytecode
     *
     * @return true, if successful
     */
    boolean hasOOoRegistryMethodsInBytecode() {
        String bytes = new String(bytecode);
        return (bytes.contains("__writeRegistryServiceInfo") && bytes.contains("__getComponentFactory"));
    }
    
    private String getAsClassname(final File classFile) {
        return getAsClassname(this.baseDir, classFile);
    }
    
    /**
     * Gets the file as classname.
     *
     * @param baseDir the base dir
     * @param classFile the class file
     * @return the as classname
     */
    public static String getAsClassname(final File baseDir, final File classFile) {
        String classFilename = classFile.getAbsolutePath();
        String baseDirname = baseDir.getAbsolutePath();
        if (classFilename.startsWith(baseDirname)) {
            classFilename = classFilename.substring(baseDirname.length() + 1);
        } else {
            classFilename = classFile.getPath();
        }
        classFilename = FilenameUtils.removeExtension(classFilename);
        return FilenameUtils.separatorsToUnix(classFilename).replace('/', '.');
    }

}
