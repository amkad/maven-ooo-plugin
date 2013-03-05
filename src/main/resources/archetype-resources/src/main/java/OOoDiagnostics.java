/*************************************************************************
 * OOoDiagnostics.java
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

package ${package};

import java.lang.reflect.Method;

import com.sun.star.registry.XRegistryKey;

/**
 * Diagnostic class to detect some OOo pitfalls. These can be:
 * <ul>
 * 	<li>incomplete classpath entry in the generated manifest file,</li>
 * 	<li>a missing __writeRegistryServiceInfo or</li>
 * 	<li>a missing __getComponentFactory method in a Java component</li>
 * </ul>
 * (see also {@link "http://oli.blogger.de/stories/1717382/"}).
 * 
 * @author oliver (oliver.boehm@agentes.de)
 * @since 1.0 (15.12.2010)
 */
public class OOoDiagnostics {
	
	/** The Constant regHandlerClass. */
	private static final Class<?> regHandlerClass = RegistrationHandler.class;
	
	/**
	 * The main method to start the diagnostics.
	 *
	 * @param args the arguments
	 */
	public static void main(final String[] args) {
		try {
			start();
		} catch (Exception e) {
			System.err.println("\nDIAGNOSTIC FAILED WITH " + e);
			System.exit(1);
		}
	}

	/**
	 * This is the entry point to start the different checks. It can be used
	 * e.g. from a JUnit test.
	 *
	 * @throws ClassNotFoundException the class not found exception
	 * @throws NoSuchMethodException the no such method exception
	 */
	public static void start() throws ClassNotFoundException,
			NoSuchMethodException {
		System.out.println("=============================");
		System.out.println("Starting some diagnostics ...");
		System.out.println("=============================");
		OOoDiagnostics diagnostics = new OOoDiagnostics();
		diagnostics.testRegistrationHandler();
		diagnostics.testRegistryMethods();
		System.out.println("=============================");
		System.out.println("Diagnostic succesful finished");
		System.out.println("=============================");
	}
	
	/**
	 * Tests if the RegistrationHandler class can be loaded and if the
	 * classpath seems to be correct.
	 *
	 * @throws ClassNotFoundException the class not found exception
	 */
	public void testRegistrationHandler() throws ClassNotFoundException {
		Class<?> cl = Class.forName(regHandlerClass.getName());
		System.out.println(cl + " found");
	}
	
	/**
	 * Tests if the registration handler has the required (static) methods.
	 * The required methods are:
	 * <ul>
	 * 	<li>a missing __writeRegistryServiceInfo,</li>
	 * 	<li>a missing __getComponentFactory method in a Java component</li>
	 * </ul>
	 *
	 * @throws SecurityException the security exception
	 * @throws NoSuchMethodException one of the required methods were not found
	 */
	public void testRegistryMethods() throws SecurityException,
			NoSuchMethodException {
        try {
            Method getComponentFactory = regHandlerClass.getMethod(
                    "__getComponentFactory", String.class);
            System.out.println(getComponentFactory + " found");
            Method writeRegistryServiceInfo = regHandlerClass.getMethod(
                    "__writeRegistryServiceInfo", XRegistryKey.class);
            System.out.println(writeRegistryServiceInfo + " found");
        } catch (NoClassDefFoundError e) {
            System.out.println(e + " - required methods are not checked");
        }
	}

}
