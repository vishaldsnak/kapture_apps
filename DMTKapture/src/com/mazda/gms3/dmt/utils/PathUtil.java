package com.mazda.gms3.dmt.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * ONE PLACE WHERE A PATH CROSSES BETWEEN THE APPLICATION AND THE FILE SYSTEM.
 *
 * The application runs on Windows and on Linux. Inside the application a path is ALWAYS held
 * in its backslash form: the documents are keyed in the database on backslash paths
 * (DC_SOURCE_NETWORK_LOC and others), and the conversion code looks for folders with
 * "\folder\" in a path. Those must give the same answer on both systems.
 *
 *   file system -> application : winPath(file)       backslash form on every system
 *   application -> file system : file(path) and the stream methods below
 *
 * On Windows every method here leaves the path exactly as it is.
 * DO NOT call getAbsolutePath() or new File(String) directly in the application code.
 */
public class PathUtil {

	private static final boolean WINDOWS = File.separatorChar == '\\';

	/** The absolute path of a file in the form the application works with (backslashes). */
	public static String winPath(File file) {
		String path = file.getAbsolutePath();
		return WINDOWS ? path : path.replace('/', '\\');
	}

	/** A path of the application in the form the file system of this server takes. */
	public static String fs(String path) {
		if (WINDOWS || null == path) {
			return path;
		}
		return path.replace('\\', '/');
	}

	public static File file(String path) {
		return new File(fs(path));
	}

	public static File file(String parent, String child) {
		return new File(fs(parent), fs(child));
	}

	public static File file(File parent, String child) {
		return new File(parent, fs(child));
	}

	public static FileInputStream fileInputStream(String path) throws FileNotFoundException {
		return new FileInputStream(fs(path));
	}

	public static FileInputStream fileInputStream(File file) throws FileNotFoundException {
		return new FileInputStream(file);
	}

	public static FileOutputStream fileOutputStream(String path) throws FileNotFoundException {
		return new FileOutputStream(fs(path));
	}

	public static FileOutputStream fileOutputStream(String path, boolean append) throws FileNotFoundException {
		return new FileOutputStream(fs(path), append);
	}

	public static FileOutputStream fileOutputStream(File file) throws FileNotFoundException {
		return new FileOutputStream(file);
	}

	public static FileOutputStream fileOutputStream(File file, boolean append) throws FileNotFoundException {
		return new FileOutputStream(file, append);
	}

	public static FileReader fileReader(String path) throws FileNotFoundException {
		return new FileReader(fs(path));
	}

	public static FileReader fileReader(File file) throws FileNotFoundException {
		return new FileReader(file);
	}

	public static FileWriter fileWriter(String path) throws IOException {
		return new FileWriter(fs(path));
	}

	public static FileWriter fileWriter(String path, boolean append) throws IOException {
		return new FileWriter(fs(path), append);
	}

	public static FileWriter fileWriter(File file) throws IOException {
		return new FileWriter(file);
	}

	public static FileWriter fileWriter(File file, boolean append) throws IOException {
		return new FileWriter(file, append);
	}

	/**
	 * Copies one file into a directory, replacing a file of the same name; the directory is
	 * created when it is missing. A failure is logged and NOT thrown: every caller checks
	 * afterwards whether the file has arrived and records the failure itself.
	 *
	 * @param source    the file (File) or its path (String)
	 * @param directory the target directory (File) or its path (String)
	 */
	public static void copyFileToDirectory(Object source, Object directory) {
		try {
			File sourceFile = source instanceof File ? (File) source : file(String.valueOf(source));
			File targetDirectory = directory instanceof File ? (File) directory : file(String.valueOf(directory));
			if (!targetDirectory.isDirectory()) {
				targetDirectory.mkdirs();
			}
			Files.copy(sourceFile.toPath(), new File(targetDirectory, sourceFile.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(PathUtil.class.getName(), "copyFileToDirectory()", e);
		}
	}
}
