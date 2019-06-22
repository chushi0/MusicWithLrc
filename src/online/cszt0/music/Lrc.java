package online.cszt0.music;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
@SuppressWarnings({"WeakerAccess", "unused"})
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
		Statement res = null;
		long endTime = 5000;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
				endTime = statement.time + 5000;
			} else {
				endTime = statement.time;
				break;
			}
		}
		if (res == null) {
			return new Info(filename, null, Math.min((float) time / (float) endTime, 1f));
		}
		// 歌词的中间关键点
		int textStart = 0;
		int textEnd = res.text.length();
		long textStartTime = res.time;
		long textEndTime = endTime;
		int textBreakSize = res.textBreakPoints.length;
		for (int i = 0; i < textBreakSize; i++) {
			long breakTime = res.textBreakTimes[i];
			if (time >= breakTime) {
				textStart = res.textBreakPoints[i];
				textStartTime = breakTime;
			} else {
				textEnd = res.textBreakPoints[i];
				textEndTime = breakTime;
				break;
			}
		}
		float textProgress = Math.min((float) (time - textStartTime) / (float) (textEndTime - textStartTime), 1f);
		// 翻译的中间关键点
		int subTextStart = 0;
		int subTextEnd = res.subText.length();
		long subTextStartTime = res.time;
		long subTextEndTime = endTime;
		int subTextBreakSize = res.subTextBreakPoints.length;
		for (int i = 0; i < subTextBreakSize; i++) {
			long breakTime = res.subTextBreakTimes[i];
			if (time >= breakTime) {
				subTextStart = res.subTextBreakPoints[i];
				subTextStartTime = breakTime;
			} else {
				subTextEnd = res.subTextBreakPoints[i];
				subTextEndTime = breakTime;
				break;
			}
		}
		float subTextProgress = Math.min((float) (time - subTextStartTime) / (float) (subTextEndTime - subTextStartTime), 1f);
		// 返回
		return new Info(res.text, res.subText, textProgress, textStart, textEnd, subTextProgress, subTextStart, subTextEnd);
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
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			Pattern labelPattern = Pattern.compile("\\[\\s*(\\w+)\\s*:\\s*([^]]+)\\s*]");
			Pattern timePattern = Pattern.compile("\\[\\s*(\\d+)\\s*:\\s*(\\d+)\\s*\\.\\s*(\\d+)\\s*]([^\\[]*)");
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
				boolean find = matcher.find();
				if (find && matcher.start() == 0) {
					long time = Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
							+ Long.parseLong(matcher.group(3));
					String text = matcher.group(4);
					Statement statement = new Statement();
					statement.time = time;
					StringBuilder statementBuilder = new StringBuilder(text);
					ArrayList<Integer> breakPoints = new ArrayList<>();
					ArrayList<Long> breakTimes = new ArrayList<>();
					int index = text.length();
					while (matcher.find()) {
						breakPoints.add(index);
						breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
								+ Long.parseLong(matcher.group(3)));
						text = matcher.group(4);
						statementBuilder.append(text);
						index += text.length();
					}
					statement.text = statementBuilder.toString();
					statement.textBreakPoints = list2intArray(breakPoints);
					statement.textBreakTimes = list2LongArray(breakTimes);
					lrcInfo.statements.add(statement);
					continue;
				}
				// 译文
				if (!find) {
					Statement statement = lrcInfo.statements.getLast();
					statement.subText = line;
					statement.subTextBreakPoints = new int[0];
					statement.subTextBreakTimes = new long[0];
					continue;
				}
				int index = matcher.start();
				StringBuilder subTextBuilder = new StringBuilder(line.substring(0, index));
				ArrayList<Integer> breakPoints = new ArrayList<>();
				ArrayList<Long> breakTimes = new ArrayList<>();
				String text;
				do {
					breakPoints.add(index);
					breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
							+ Long.parseLong(matcher.group(3)));
					text = matcher.group(4);
					subTextBuilder.append(text);
					index += text.length();
				} while (matcher.find());
				Statement statement = lrcInfo.statements.getLast();
				statement.subText = subTextBuilder.toString();
				statement.subTextBreakPoints = list2intArray(breakPoints);
				statement.subTextBreakTimes = list2LongArray(breakTimes);
			}
			// 重新更新时间
			if (offset != 0) {
				for (Statement statement : lrcInfo.statements) {
					statement.time += offset;
				}
			}
			return lrcInfo;
		} catch (IOException e) {
			return null;
		}
	}

	private static long[] list2LongArray(ArrayList<Long> list) {
		int size = list.size();
		long[] res = new long[size];
		for (int i = 0; i < size; i++) {
			res[i] = list.get(i);
		}
		return res;
	}

	private static int[] list2intArray(ArrayList<Integer> list) {
		int size = list.size();
		int[] res = new int[size];
		for (int i = 0; i < size; i++) {
			res[i] = list.get(i);
		}
		return res;
	}

	private static Lrc parseFileAsRawText(File file) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
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
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		}
	}

	public synchronized void deleteStatement(int position) {
		statements.remove(position);
	}

	public synchronized void clearTime(int position) {
		int size = statements.size();
		if (position == size - 1) {
			Statement statement = statements.getLast();
			statement.time = Long.MAX_VALUE;
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		} else {
			Statement statement = statements.get(position);
			statement.time = statements.get(position + 1).time;
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
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
		public final float textProgress;
		public final int textStart;
		public final int textEnd;
		public final float subTextProgress;
		public final int subTextStart;
		public final int subTextEnd;

		public Info(String text, String subText, float progress) {
			this.text = text;
			this.subText = subText == null ? "" : subText;
			this.textProgress = progress;
			this.subTextProgress = progress;
			this.textStart = 0;
			this.textEnd = text.length();
			this.subTextStart = 0;
			this.subTextEnd = this.subText.length();
		}

		public Info(String text, String subText, float textProgress, int textStart, int textEnd, float subTextProgress, int subTextStart, int subTextEnd) {
			this.text = text;
			this.subText = subText;
			this.textProgress = textProgress;
			this.textStart = textStart;
			this.textEnd = textEnd;
			this.subTextProgress = subTextProgress;
			this.subTextStart = subTextStart;
			this.subTextEnd = subTextEnd;
		}
	}

	private static class Statement {
		private long time;
		private String text;
		private String subText;
		private int[] textBreakPoints;
		private long[] textBreakTimes;
		private int[] subTextBreakPoints;
		private long[] subTextBreakTimes;
	}
}