/*
 *
 * Copyright (c) 2019-2020 NasTel Technologies, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of NasTel
 * Technologies, Inc. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with NasTel
 * Technologies.
 *
 * NASTEL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. NASTEL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 */

package lt.slabs.com.jkoolcloud.remora;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.io.Files;
import com.jkoolcloud.remora.Remora;
import com.jkoolcloud.remora.core.output.SysOutOutput;

/**
 * This class intended to test loading premain and it's options.
 */
public class RemoraTest {
	public static void main(String[] args) throws Throwable {
		System.setProperty("probe.output", SysOutOutput.class.getName());
		System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		System.out.println( //
				"\n\tClass.loader=" + Remora.class.getClassLoader() //
						+ "\n\tJava.version=" + System.getProperty("java.version") //
						+ "\n\tJava.vendor=" + System.getProperty("java.vendor") //
						+ "\n\tJava.home=" + System.getProperty("java.home") //
						+ "\n\tJava.heap=" + Runtime.getRuntime().maxMemory() //
						+ "\n\tOS.name=" + System.getProperty("os.name") //
						+ "\n\tOS.version=" + System.getProperty("os.version") //
						+ "\n\tOS.arch=" + System.getProperty("os.arch") //
						+ "\n\tOS.cpus=" + Runtime.getRuntime().availableProcessors());

		new JustATest().instrumentedMethod("http://www.google.com", "Argument");

		Thread.sleep(3000);
	}

	@Test
	public void findJarsTest() throws IOException {
		File tempDir = Files.createTempDir();
		java.nio.file.Files.createTempFile(Paths.get(tempDir.getAbsolutePath()), "temp1", ".jar");
		java.nio.file.Files.createTempFile(Paths.get(tempDir.getAbsolutePath()), "temp2", ".jar");

		URL[] jars = Remora.findJars(tempDir.getAbsolutePath());
		assertEquals(2, jars.length);

		org.apache.commons.io.FileUtils.deleteDirectory(tempDir);

	}

}
