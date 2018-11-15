package com.newrelic.infra.ibmmq;

import java.io.*;
import java.nio.channels.Channels;

/**
 * A class for monitoring the tail of some log for some value.  This class keeps it's state in a state file so it can
 * continue searching the log file starting at the point it left off in a prior run.
 */
public class LogReader {

	private String logPath;
	private String searchValue;
	private String statePath;

	LogReader(String logPath, String statePath, String searchValue) {
		this.logPath = logPath;
		this.searchValue = searchValue;
		this.statePath = statePath;
	}

	public String findSearchValueLine() throws IOException {
		String foundLine = null;
		long start = loadState();

		File f = new File(logPath);
		long end = f.length();

		if(start > end) {
			start = 0;
		}

		if(start < end) {
			try (RandomAccessFile raf = new RandomAccessFile(logPath, "r")) {
				raf.seek(start);

				BufferedReader in = new BufferedReader(new InputStreamReader(Channels.newInputStream(raf.getChannel())));

				String line;
				while ((line = in.readLine()) != null) {
					if(line.contains(searchValue)) {
						foundLine = line;
						break;
					}
				}

				persistState(end);
			}
		}

		return foundLine;
	}

	private long loadState() throws IOException {
		File f = new File(statePath);
		if(!f.exists()) {
			return 0;
		} else {
			try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
				return in.readLong();
			}
		}
	}

	private void persistState(long start) throws IOException {
		File f = new File(statePath);
		if(!f.exists()) {
			f.createNewFile();
		}
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
			out.writeLong(start);
			out.flush();
		}
	}

}
