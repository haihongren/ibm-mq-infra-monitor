package com.newrelic.infra.ibmmq;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class LogReaderTest {

	private String logPath = "LogReaderTest.txt";
	private String statePath = "LogReaderTestState.txt";
	private String searchPhrase = "special";
	private LogReader reader;

	@Before
	public void setup() throws Exception {
		reader = new LogReader(logPath, statePath, searchPhrase);
		createTestFile();
	}

	@After
	public void tearDown() throws IOException {
		removeFile(logPath);
		removeFile(statePath);
	}

	@Test
	public void testRead() throws Exception {
		assertNull("Shouldn't find the search value on an empty file", reader.findSearchValueLine());

		appendTestFile("Some text");
		appendTestFile("More text");

		assertNull("Don't find search value when it's not in the file", reader.findSearchValueLine());

		appendTestFile(searchPhrase + " text here");
		appendTestFile("more text");

		assertNotNull("Find search value when it's in file", reader.findSearchValueLine());
	}

	public void createTestFile() throws IOException {
		File f = new File(logPath);
		f.createNewFile();
	}

	public void appendTestFile(String txt) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(logPath, true))) {
			out.write(txt);
			out.newLine();
			out.flush();
		}
	}

	private void removeFile(String path) throws IOException {
		File f = new File(path);
		if(f.exists()) {
			if(!f.delete()) {
				throw new IOException("Couldn't delete file " + path);
			}
		}
	}
}
