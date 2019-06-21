package online.cszt0.music;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 初始状态0
 * @date 2019/5/22 20:56
 */
@SuppressWarnings({"WeakerAccess","unused"})
public final class Lrc {
	private static final String OFFSET_LABEL = "offset";

	private final String filename;
	private LinkedList<Statement> statements;
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private Map<String, String> infoMap;

	public Lrc(String filename) {
		statements = new LinkedList<>();
		infoMap = new HashMap<>();
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			this.filename = filename.substring(0, dotIndex);
		} else {
			this.filename = filename;
		}
	}

	public Info getInfoByTime(long time) {
		Statement res = new Statement();
		res.text = filename;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
			} else {
				return new Info(res.text, res.subText, (float) (time - res.time) / (float) (statement.time - res.time));
			}
		}
		return new Info(res.text, res.subText, Math.min((time - res.time) / 5000f, 1f));
	}

	public Info getInfoByIndex(int index) {
		Statement statement = statements.get(index);
		return new Info(statement.text, statement.subText, Float.NaN);
	}

	public synchronized void endStatementByTime(long time) {
		for (Statement statement : statements) {
			if (statement.time > time) {
				statement.time = time;
				return;
			}
		}
	}

	public static Lrc fromFile(File file) {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".txt")) {
			return parseFileAsRawText(file);
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			Pattern labelPattern = Pattern.compile("\\[\\s*(\\w+)\\s*:\\s*([^]]+)\\s*]");
			Pattern timePattern = Pattern.compile("\\[\\s*(\\d+)\\s*:\\s*(\\d+)\\s*\\.\\s*(\\d+)\\s*](.*+)");
			Lrc lrcInfo = new Lrc(file.getName());
			String line;
			long offset = 0;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				// 元信息
				Matcher matcher = labelPattern.matcher(line);
				if (matcher.matches()) {
					String key = matcher.group(1);
					String value = matcher.group(2);
					if (key.equalsIgnoreCase(OFFSET_LABEL)) {
						try {
							offset = Long.parseLong(value);
						} catch (NumberFormatException ignored) {
						}
					} else {
						lrcInfo.infoMap.put(key, value);
					}
					continue;
				}
				// 歌词
				matcher = timePattern.matcher(line);
				if (matcher.find() && matcher.start() == 0) {
					long time = Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
							+ Long.parseLong(matcher.group(3));
					String text = matcher.group(4);
					Statement statement = new Statement();
					statement.time = time;
					statement.text = text;
					lrcInfo.statements.add(statement);
					continue;
				}
				// 译文
				lrcInfo.statements.getLast().subText = line;
			}
			// 重新更新时间
			if(offset!=0) {
				for (Statement statement : lrcInfo.statements) {
					statement.time += offset;
				}
			}
			return lrcInfo;
		} catch (IOException e) {
			return null;
		}
	}

	private static Lrc parseFileAsRawText(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			Lrc lrcInfo = new Lrc(file.getName());
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				Statement statement = new Statement();
				statement.time = Long.MAX_VALUE;
				statement.text = line;
				lrcInfo.statements.add(statement);
			}
			return lrcInfo;
		} catch (IOException e) {
			return null;
		}
	}

	public boolean isStatementEditableByTime(long time) {
		if (statements.isEmpty()) {
			return false;
		}
		return statements.getFirst().time <= time;
	}

	public synchronized void setStatementByTime(long time, String main, String sub) {
		Statement res = null;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
			} else {
				assert res != null;
				res.text = main;
				res.subText = sub;
			}
		}
		assert res != null;
		res.text = main;
		res.subText = sub;
	}

	public String[] toStringArray() {
		int size = statements.size();
		String[] arr = new String[size];
		int index = 0;
		for (Statement statement : statements) {
			arr[index++] = statement.text;
		}
		return arr;
	}

	public synchronized void setStatementByIndex(int position, String main, String sub) {
		Statement statement = statements.get(position);
		statement.text = main;
		statement.subText = sub;
	}

	public synchronized void clearTime() {
		for (Statement statement : statements) {
			statement.time = Long.MAX_VALUE;
		}
	}

	public synchronized void deleteStatement(int position) {
		statements.remove(position);
	}

	public synchronized void clearTime(int position) {
		int size = statements.size();
		if (position == size - 1) {
			statements.getLast().time = Long.MAX_VALUE;
		} else {
			statements.get(position).time = statements.get(position + 1).time;
		}
	}

	public synchronized void addAfter(int position) {
		Statement statement = new Statement();
		int size = statements.size();
		if (position == size - 1) {
			statement.time = Long.MAX_VALUE;
		} else {
			statement.time = statements.get(position + 1).time;
		}
		statements.add(position + 1, statement);
	}

	public void addBefore(int position) {
		Statement statement = new Statement();
		Statement curStatement = statements.get(position);
		statement.time = curStatement.time;
		if (position == 0 && position != statements.size() - 1) {
			curStatement.time = statements.get(position + 1).time;
		}
		statements.add(position, statement);
	}

	public void writeToStream(OutputStream outputStream) {
		PrintStream printStream = new PrintStream(outputStream);
		// 元信息
		Set<HashMap.Entry<String, String>> entries = infoMap.entrySet();
		for (HashMap.Entry<String, String> entry : entries) {
			printStream.printf("[%s:%s]%n", entry.getKey(), entry.getValue());
		}
		// 歌词信息
		for (Statement statement : statements) {
			long time = statement.time;
			printStream.printf("[%d:%d.%d]%s%n", time / 60 / 1000, time / 1000 % 60, time % 1000, statement.text);
			if (!statement.subText.isEmpty()) {
				printStream.println(statement.subText);
			}
		}
	}

	public static class Info {
		public final String text;
		public final String subText;
		public final float progress;

		public Info(String text, String subText, float progress) {
			this.text = text;
			this.subText = subText;
			this.progress = progress;
		}
	}

	private static class Statement {
		private long time;
		private String text;
		private String subText;
	}
}