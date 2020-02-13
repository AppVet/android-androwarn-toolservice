package gov.nist.appvet.tool.androwarn.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

public class Native {

	private Logger log = null;

	public Native(Logger log) {
		this.log = log;
	}

	/** If exit value != 0, error stream is returned in resultBuf. */
	public int exec(ProcessBuilder pb, int waitSeconds, StringBuffer resultBuf) {
		log.debug("Executing " + pb.command());
		Process proc = null;
		InputThreadHandler inputHandler = null;
		InputThreadHandler errorHandler = null;
		InputStream inputStream = null;
		InputStream errorStream = null;

		try {
			proc = pb.start();

			inputStream = proc.getInputStream();
			inputHandler = new InputThreadHandler(inputStream);
			inputHandler.start();
			
			errorStream = proc.getErrorStream();
			errorHandler = new InputThreadHandler(errorStream);
			errorHandler.start();
			
			// Wait for process to complete
			proc.waitFor(waitSeconds, TimeUnit.SECONDS);
			
			// Return result
			int exitCode = proc.exitValue();
			
			log.debug("Waiting for stream threads to stop");
			inputHandler.join();
			errorHandler.join();
			log.debug("Stream threads stopped");

			if (exitCode != 0) {
				log.error("Exit code " + exitCode + " executing: " + pb.command());
				resultBuf.append(errorHandler.getResult());
			} else {
				resultBuf.append(inputHandler.getResult());
			}

			return exitCode;

		} catch (IOException e) {
			log.error("Process had I/O exception executing command");
			resultBuf.append(e.getMessage());
			e.printStackTrace();
			return 1;
		} catch (InterruptedException e) {
			log.error("Process timed-out executing command");
			resultBuf.append(e.getMessage());
			e.printStackTrace();
			return 2;
		} finally {
			// Close streams
			try {
				inputStream.close();
				errorStream.close();
				log.debug("Closed input and error streams");
			} catch (IOException e) {
				log.error("Could not close input and error streams");
				resultBuf.append(e.getMessage());
				e.printStackTrace();
			}
			proc.destroy();
		}
	}

	/** If exit value != 0, error stream is returned in resultBuf. */
	public int writeReceivedFile(ProcessBuilder pb, int waitSeconds, File pdfFile) {
		log.debug("Executing " + pb.command());
		Process proc = null;

		try {
			proc = pb.start();

			InputStream inputStream = proc.getInputStream();

			java.nio.file.Files.copy(
					inputStream, 
					pdfFile.toPath(), 
					StandardCopyOption.REPLACE_EXISTING);

			// Wait for process to complete
			proc.waitFor(waitSeconds, TimeUnit.SECONDS);

			// Return result
			int exitCode = proc.exitValue();

			log.debug("Exit code " + exitCode + " executing: " + pb.command());

			// Close streams
			IOUtils.closeQuietly(inputStream);

			proc.destroy();

			return exitCode;

		} catch (IOException e) {
			log.error("Process had I/O exception executing command");
			e.printStackTrace();
			return 1;
		} catch (InterruptedException e) {
			log.error("Process timed-out executing command");
			e.printStackTrace();
			return 2;
		}
	}

	private class InputThreadHandler extends Thread {
		public InputStream inputStream;
		public StringBuffer resultBuffer = new StringBuffer();
		private final String lineSeparator = System
				.getProperty("line.separator");

		InputThreadHandler(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		public void run() {
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(inputStream));
			try {
				for (String line; (line = br.readLine()) != null; ) {
					//log.debug("line: " + line);
					resultBuffer.append(line + lineSeparator);
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getResult() {
			return resultBuffer.toString();
		}
	}

}
