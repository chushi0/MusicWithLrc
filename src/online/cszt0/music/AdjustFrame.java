package online.cszt0.music;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import jdk.nashorn.internal.scripts.JO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 调整歌词的界面
 *
 * @author 初始状态0
 * @date 2019/6/29 15:47
 */
@SuppressWarnings("WeakerAccess")
public class AdjustFrame extends JFrame {

	File lrcFile;
	long lrcFileLastModify;

	JList<String> lrcList;
	JScrollPane lrcListScrollPane;
	JLabel statusLabel;
	JLabel timeLabel;
	ProgressTextLabel mainLabel;
	ProgressTextLabel translateLabel;

	JButton playButton;
	JButton commitButton;

	MediaPlayer mediaPlayer;
	Lrc lrc;

	Thread thread;

	private AdjustFrame() {
		super("调整歌词");
		JSplitPane contentPanel = new JSplitPane();
		contentPanel.setDividerLocation(270);
		lrcList = new JList<>();
		lrcListScrollPane = new JScrollPane(lrcList);
		contentPanel.setLeftComponent(lrcListScrollPane);
		JPanel rightContentPanel = new JPanel(new BorderLayout());
		JPanel topPanel = new JPanel(new BorderLayout());
		statusLabel = new JLabel("已停止");
		timeLabel = new JLabel();
		topPanel.add(statusLabel, BorderLayout.WEST);
		topPanel.add(timeLabel, BorderLayout.EAST);
		rightContentPanel.add(topPanel, BorderLayout.NORTH);
		JPanel centerPanel = new JPanel(new GridBagLayout());
		mainLabel = new ProgressTextLabel() {
			@Override
			protected int getLeft() {
				return 0;
			}
		};
		mainLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
		mainLabel.setUnreachedColor(0);
		translateLabel = new ProgressTextLabel() {
			@Override
			protected int getLeft() {
				return 0;
			}
		};
		translateLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
		translateLabel.setUnreachedColor(0);
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.CENTER;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridx = 0;
		centerPanel.add(mainLabel, gridBagConstraints);
		gridBagConstraints.gridy = 1;
		centerPanel.add(translateLabel, gridBagConstraints);
		rightContentPanel.add(centerPanel, BorderLayout.CENTER);
		JPanel bottomPanel = new JPanel();
		playButton = new JButton("播放");
		commitButton = new JButton("这一句唱完了");
		bottomPanel.add(playButton);
		bottomPanel.add(commitButton);
		rightContentPanel.add(bottomPanel, BorderLayout.SOUTH);
		contentPanel.setRightComponent(rightContentPanel);
		setContentPane(contentPanel);

		lrcList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				int index = lrcList.locationToIndex(new Point(e.getX(), e.getY()));
				if (e.getButton() == MouseEvent.BUTTON1) {
					long time = lrc.getStartTimeByIndex(index);
					ensurePlayMusic();
					mediaPlayer.seek(new Duration(time));
				}
			}
		});

		timeLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				JOptionPane.showInputDialog(null, "当前播放进度：", "信息", JOptionPane.INFORMATION_MESSAGE, null, null, timeLabel.getText());
			}
		});

		mainLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getButton() == MouseEvent.BUTTON1) {
					editMainText();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					lrcLabelRightClick(mainLabel, e);
				}
			}
		});

		translateLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getButton() == MouseEvent.BUTTON1) {
					editTranslate();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					lrcLabelRightClick(translateLabel, e);
				}
			}
		});

		playButton.addActionListener(e -> {
			if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
				mediaPlayer.pause();
				playButton.setText("播放");
				statusLabel.setText("已暂停");
			} else {
				ensurePlayMusic();
			}
		});

		commitButton.addActionListener(e -> lrc.endStatementByTime(getPlayTime()));

		thread = new Thread(this::run);
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 编辑翻译
	 */
	private void editTranslate() {
		long currentTime = getPlayTime();
		String mainText = mainLabel.getText();
		String translateText = translateLabel.getText();
		if (!lrc.isStatementEditableByTime(currentTime)) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(AdjustFrame.this, "这句歌词根据音乐名自动生成。若要修改，请修改对应音乐的文件名。", "当前歌词不可编辑", JOptionPane.ERROR_MESSAGE);
			return;
		}
		translateText = JOptionPane.showInputDialog("请输入歌词翻译：", translateText);
		if (translateText != null) {
			lrc.setStatementByTime(currentTime, mainText, translateText);
			lrcList.setListData(lrc.toStringArray());
		}
	}

	/**
	 * 编辑歌词
	 */
	private void editMainText() {
		long currentTime = getPlayTime();
		String mainText = mainLabel.getText();
		String translateText = translateLabel.getText();
		if (!lrc.isStatementEditableByTime(currentTime)) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(AdjustFrame.this, "这句歌词根据音乐名自动生成。若要修改，请修改对应音乐的文件名。", "当前歌词不可编辑", JOptionPane.ERROR_MESSAGE);
			return;
		}
		mainText = JOptionPane.showInputDialog("请输入歌词：", mainText);
		if (mainText != null) {
			lrc.setStatementByTime(currentTime, mainText, translateText);
			lrcList.setListData(lrc.toStringArray());
		}
	}

	/**
	 * 鼠标右键点击（滚动的）歌词
	 *
	 * @param label
	 * 		对应的控件
	 * @param e
	 * 		右键点击事件
	 */
	private void lrcLabelRightClick(ProgressTextLabel label, MouseEvent e) {
		JMenu menu = new JMenu();
		JMenuItem editMainText = new JMenuItem("编辑歌词");
		editMainText.addActionListener(l -> editMainText());
		JMenuItem editTranslate = new JMenuItem("编辑翻译");
		editTranslate.addActionListener(l -> editTranslate());
		JMenuItem editRawText = new JMenuItem("直接编辑歌词文件");
		menu.add(editMainText);
		menu.add(editTranslate);
		menu.addSeparator();
		menu.add(editRawText);
		JPopupMenu popupMenu = menu.getPopupMenu();
		popupMenu.show(label, e.getX(), e.getY());
	}

	private void ensurePlayMusic() {
		mediaPlayer.play();
		playButton.setText("暂停");
		statusLabel.setText("正在播放");
	}

	private long getPlayTime() {
		return ((long) mediaPlayer.getCurrentTime().toMillis());
	}

	@Override
	public void dispose() {
		super.dispose();
		thread.interrupt();
		if (mediaPlayer != null) {
			mediaPlayer.dispose();
		}
	}

	static void showFrame(String music) {
		AdjustFrame adjustFrame = new AdjustFrame();
		adjustFrame.loadMusic(music);
		adjustFrame.loadLrc(music);
		adjustFrame.setIconImage(new ImageIcon("icon.png").getImage());
		adjustFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		adjustFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				int res = JOptionPane.showConfirmDialog(adjustFrame, "关闭之前是否保存歌词？", "是否保存？", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (res == JOptionPane.YES_OPTION) {
					if (adjustFrame.saveLrc()) {
						adjustFrame.dispose();
					}
				} else if (res == JOptionPane.NO_OPTION) {
					adjustFrame.dispose();
				}

			}
		});
		adjustFrame.setMinimumSize(new Dimension(700, 450));
		adjustFrame.setLocationRelativeTo(null);
		adjustFrame.setVisible(true);
	}

	private void loadMusic(String music) {
		File file = new File(MainFrame.musicDir, music);
		Media media = new Media(file.toURI().toString());
		mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setOnEndOfMedia(this::onMusicStop);
		mediaPlayer.setOnError(this::dispose);
	}

	private void loadLrc(String musicName) {
		musicName = musicName.substring(0, musicName.lastIndexOf('.')) + ".lrc";
		lrcFile = new File(MainFrame.lrcDir, musicName);
		lrcFileLastModify = lrcFile.lastModified();
		lrc = Lrc.fromFile(lrcFile);
		if (lrc == null) {
			JOptionPane.showMessageDialog(this, "读取歌词失败");
			dispose();
			return;
		}
		lrcList.setListData(lrc.toStringArray());
	}

	private boolean saveLrc() {
		long lastModify = lrcFile.lastModified();
		if (lastModify != lrcFileLastModify) {
			int res = JOptionPane.showConfirmDialog(this, "检测到歌词文件已被其他程序修改，要忽略其他程序的修改而继续保存吗？", "继续操作", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (res == JOptionPane.NO_OPTION) {
				return true;
			} else if (res == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(lrcFile)) {
			lrc.writeToStream(fileOutputStream);
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(this, "保存失败：" + e.getMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private void onMusicStop() {
		mediaPlayer.stop();
		mediaPlayer.seek(new Duration(0));
		statusLabel.setText("已停止");
		playButton.setText("播放");
	}

	private void run() {
		while (true) {
			if (lrc != null && mediaPlayer != null) {
				long playTime = getPlayTime();
				timeLabel.setText(String.format("%d:%02d.%03d(%,03d)", playTime / 1000 / 60, playTime / 1000 % 60, playTime % 1000, playTime));
				Lrc.Info info = lrc.getInfoByTime(playTime);
				mainLabel.setText(info.text);
				mainLabel.setProgress(info.textProgress, info.textStart, info.textEnd);
				translateLabel.setText(info.subText);
				translateLabel.setProgress(info.subTextProgress, info.subTextStart, info.subTextEnd);
				if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
					int index = lrc.getIndexByTime(playTime);
					if (lrcList.getSelectedIndex() != index) {
						lrcList.setSelectedIndex(index);
						if (index != -1) {
							Point point = lrcList.indexToLocation(index);
							JScrollBar verticalScrollBar = lrcListScrollPane.getVerticalScrollBar();
							int value = verticalScrollBar.getValue();
							int bottomValue = point.y - (lrcListScrollPane.getViewport().getHeight() - lrcList.getCellBounds(index, index).height);
							int topValue = point.y;
							if (value < bottomValue) {
								verticalScrollBar.setValue(bottomValue);
							} else if (value > topValue) {
								verticalScrollBar.setValue(topValue);
							}
							repaint();
						}
					}
				}
			} else {
				mainLabel.setText(null);
				translateLabel.setText(null);
			}
		}
	}
}
