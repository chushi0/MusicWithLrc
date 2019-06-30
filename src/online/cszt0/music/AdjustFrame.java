package online.cszt0.music;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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

	JCheckBoxMenuItem repeat;

	volatile boolean lockListSelection;

	MediaPlayer mediaPlayer;
	Lrc lrc;

	Thread thread;

	private AdjustFrame() {
		super("调整歌词");
		JSplitPane contentPanel = new JSplitPane();
		contentPanel.setDividerLocation(270);
		lrcList = new JList<>();
		lrcList.setFixedCellHeight(20);
		lrcListScrollPane = new JScrollPane(lrcList);
		contentPanel.setLeftComponent(lrcListScrollPane);
		JPanel rightContentPanel = new JPanel(new BorderLayout());
		JPanel topPanel = new JPanel(new BorderLayout());
		statusLabel = new JLabel("已停止");
		timeLabel = new JLabel();
		timeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
		mainLabel.setCursor(new Cursor(Cursor.TEXT_CURSOR));
		translateLabel = new ProgressTextLabel() {
			@Override
			protected int getLeft() {
				return 0;
			}
		};
		translateLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
		translateLabel.setUnreachedColor(0);
		translateLabel.setCursor(new Cursor(Cursor.TEXT_CURSOR));
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
				int button = e.getButton();
				if (button == MouseEvent.BUTTON1) {
					long time = lrc.getStartTimeByIndex(index);
					ensurePlayMusic();
					mediaPlayer.seek(new Duration(time));
				}
				if (button == MouseEvent.BUTTON3) {
					//noinspection SynchronizeOnNonFinalField
					synchronized (lrcList) {
						lockListSelection = true;
					}
					lrcList.setSelectedIndex(index);
					JMenu menu = new JMenu();
					JMenuItem editMainText = new JMenuItem("编辑歌词");
					editMainText.addActionListener(l -> editMainText(index));
					JMenuItem editTranslate = new JMenuItem("编辑翻译");
					editTranslate.addActionListener(l -> editTranslate(index));
					JMenuItem editLrcText = new JMenuItem("直接编辑歌词文件");
					editLrcText.addActionListener(l -> editLrcText(index));
					JMenuItem addBefore = new JMenuItem("前面插入");
					addBefore.addActionListener(l -> {
						lrc.addBefore(index);
						updateLrcList();
					});
					JMenuItem addAfter = new JMenuItem("后面插入");
					addAfter.addActionListener(l -> {
						lrc.addAfter(index);
						updateLrcList();
					});
					JMenuItem clearTime = new JMenuItem("清除开始时间");
					clearTime.addActionListener(l -> lrc.clearTime(index));
					JMenuItem deleteStatement = new JMenuItem("删除此句");
					deleteStatement.addActionListener(l -> {
						lrc.deleteStatement(index);
						updateLrcList();
					});
					JMenuItem clearAllTime = new JMenuItem("清除全部歌词时间");
					clearAllTime.addActionListener(l -> lrc.clearTime());
					menu.add(editMainText);
					menu.add(editTranslate);
					menu.add(editLrcText);
					menu.addSeparator();
					menu.add(addBefore);
					menu.add(addAfter);
					menu.addSeparator();
					menu.add(clearTime);
					menu.add(deleteStatement);
					menu.add(clearAllTime);
					JPopupMenu popupMenu = menu.getPopupMenu();
					popupMenu.addPopupMenuListener(new PopupMenuListener() {
						@Override
						public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						}

						@Override
						public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
							//noinspection SynchronizeOnNonFinalField
							synchronized (lrcList) {
								lockListSelection = false;
							}
						}

						@Override
						public void popupMenuCanceled(PopupMenuEvent e) {
						}
					});
					popupMenu.show(lrcList, e.getX(), e.getY());
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

		// 菜单
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("文件");
		JMenuItem reloadLrc = new JMenuItem("重新加载歌词");
		reloadLrc.addActionListener(e -> {
			int res = JOptionPane.showConfirmDialog(this, "将会丢失当前工作进度，是否继续？", "重新加载歌词", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (res == JOptionPane.YES_OPTION) {
				loadLrcFile(true);
			}
		});
		JMenuItem saveLrc = new JMenuItem("保存歌词");
		saveLrc.addActionListener(e -> saveLrc());
		JMenuItem editByTextEditor = new JMenuItem("使用文本编辑器编辑歌词");
		editByTextEditor.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(lrcFile);
			} catch (IOException err) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(this, "启动失败：" + err.getLocalizedMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
			}
		});
		JMenuItem editByNotepad = new JMenuItem("使用 记事本 编辑歌词 (Windows)");
		editByNotepad.addActionListener(e -> {
			try {
				Runtime.getRuntime().exec("notepad " + lrcFile.getAbsolutePath());
			} catch (IOException err) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(this, "启动失败：" + err.getLocalizedMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
			}
		});
		fileMenu.add(reloadLrc);
		fileMenu.add(saveLrc);
		fileMenu.addSeparator();
		fileMenu.add(editByTextEditor);
		fileMenu.add(editByNotepad);
		JMenu controlMenu = new JMenu("控制");
		repeat = new JCheckBoxMenuItem("循环播放");
		JMenu rateMenu = new JMenu("播放速度");
		JCheckBoxMenuItem normalRate = new JCheckBoxMenuItem("正常");
		JCheckBoxMenuItem halfRate = new JCheckBoxMenuItem("x1/2");
		JCheckBoxMenuItem quarterRate = new JCheckBoxMenuItem("x1/4");
		JCheckBoxMenuItem doubleRate = new JCheckBoxMenuItem("x2");
		JCheckBoxMenuItem otherRate = new JCheckBoxMenuItem("自定义");
		normalRate.addActionListener(e -> {
			normalRate.setSelected(true);
			halfRate.setSelected(false);
			quarterRate.setSelected(false);
			doubleRate.setSelected(false);
			otherRate.setSelected(false);
			mediaPlayer.setRate(1);
		});
		halfRate.addActionListener(e -> {
			normalRate.setSelected(false);
			halfRate.setSelected(true);
			quarterRate.setSelected(false);
			doubleRate.setSelected(false);
			otherRate.setSelected(false);
			mediaPlayer.setRate(0.5);
		});
		quarterRate.addActionListener(e -> {
			normalRate.setSelected(false);
			halfRate.setSelected(false);
			quarterRate.setSelected(true);
			doubleRate.setSelected(false);
			otherRate.setSelected(false);
			mediaPlayer.setRate(0.25);
		});
		doubleRate.addActionListener(e -> {
			normalRate.setSelected(false);
			halfRate.setSelected(false);
			quarterRate.setSelected(false);
			doubleRate.setSelected(true);
			otherRate.setSelected(false);
			mediaPlayer.setRate(2);
		});
		otherRate.addActionListener(e -> {
			String speed = JOptionPane.showInputDialog("请输入播放速度，范围：0.0~8.0", Double.toString(mediaPlayer.getRate()));
			if (speed == null) {
				otherRate.setSelected(!otherRate.isSelected());
				return;
			}
			try {
				double rate = Double.parseDouble(speed);
				rate = Math.min(Math.max(rate, 0.0), 8.0);
				normalRate.setSelected(false);
				halfRate.setSelected(false);
				quarterRate.setSelected(false);
				doubleRate.setSelected(false);
				otherRate.setSelected(true);
				mediaPlayer.setRate(rate);
			} catch (NumberFormatException err) {
				otherRate.setSelected(!otherRate.isSelected());
			}
		});
		normalRate.setSelected(true);
		rateMenu.add(normalRate);
		rateMenu.add(halfRate);
		rateMenu.add(quarterRate);
		rateMenu.add(doubleRate);
		rateMenu.addSeparator();
		rateMenu.add(otherRate);
		controlMenu.add(repeat);
		controlMenu.add(rateMenu);
		menuBar.add(fileMenu);
		menuBar.add(controlMenu);
		setJMenuBar(menuBar);

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
		}
	}

	private void editTranslate(int index) {
		Lrc.Info info = lrc.getInfoByIndex(index);
		String mainText = info.text;
		String translate = info.subText;
		translate = JOptionPane.showInputDialog("请输入歌词翻译：", translate);
		if (translate != null) {
			lrc.setStatementByIndex(index, mainText, translate);
		}
	}

	private void updateLrcList() {
		//noinspection SynchronizeOnNonFinalField
		synchronized (lrcList) {
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
			updateLrcList();
		}
	}

	private void editMainText(int index) {
		Lrc.Info info = lrc.getInfoByIndex(index);
		String mainText = info.text;
		String translate = info.subText;
		mainText = JOptionPane.showInputDialog("请输入歌词：", mainText);
		if (mainText != null) {
			lrc.setStatementByIndex(index, mainText, translate);
			updateLrcList();
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
		editRawText.addActionListener(l -> {
			long time = getPlayTime();
			int index = lrc.getIndexByTime(time);
			editLrcText(index);
		});
		menu.add(editMainText);
		menu.add(editTranslate);
		menu.addSeparator();
		menu.add(editRawText);
		JPopupMenu popupMenu = menu.getPopupMenu();
		popupMenu.show(label, e.getX(), e.getY());
	}

	private void editLrcText(int index) {
		JDialog dialog = new JDialog(this, true);
		dialog.setTitle("编辑歌词");
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
		JTextArea textArea = new JTextArea();
		textArea.setText(lrc.getStatementText(index));
		textArea.setBorder(new LineBorder(Color.BLACK));
		textArea.setRows(3);
		textArea.setColumns(25);
		textArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
		JPanel bottomPanel = new JPanel();
		JButton okButton = new JButton("确定");
		okButton.addActionListener(a -> {
			if (lrc.replaceStatement(textArea.getText(), index)) {
				dialog.dispose();
				updateLrcList();
			} else {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(dialog, "无法替换对应歌词信息", "操作失败", JOptionPane.ERROR_MESSAGE);
			}
		});
		JButton cancelButton = new JButton("取消");
		cancelButton.addActionListener(a -> dialog.dispose());
		bottomPanel.add(okButton);
		bottomPanel.add(cancelButton);
		contentPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		contentPanel.add(bottomPanel, BorderLayout.SOUTH);
		dialog.setContentPane(contentPanel);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
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
		if (!adjustFrame.loadLrc(music)) {
			adjustFrame.dispose();
			return;
		}
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

	private boolean loadLrc(String musicName) {
		musicName = musicName.substring(0, musicName.lastIndexOf('.')) + ".lrc";
		lrcFile = new File(MainFrame.lrcDir, musicName);
		return loadLrcFile(false);
	}

	/**
	 * 加载歌词文件
	 *
	 * @param warnLastModify
	 * 		是否提醒用户修改时间
	 * @return 是否正常运行
	 */
	private boolean loadLrcFile(boolean warnLastModify) {
		long lastModify = lrcFile.lastModified();
		if (warnLastModify && lastModify != lrcFileLastModify) {
			int res = JOptionPane.showConfirmDialog(this, "检测到歌词文件已被其他程序修改，要继续加载吗？", "继续操作", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (res == JOptionPane.NO_OPTION) {
				return true;
			}
		}
		lrcFileLastModify = lastModify;
		lrc = Lrc.fromFile(lrcFile);
		if (lrc == null) {
			JOptionPane.showMessageDialog(this, "读取歌词失败");
			return false;
		}
		updateLrcList();
		return true;
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
			JOptionPane.showMessageDialog(this, "保存失败：" + e.getLocalizedMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		lrcFileLastModify = lrcFile.lastModified();
		return true;
	}

	private void onMusicStop() {
		mediaPlayer.stop();
		mediaPlayer.seek(new Duration(0));
		statusLabel.setText("已停止");
		playButton.setText("播放");
		if (repeat.isSelected()) {
			ensurePlayMusic();
		}
	}

	private void run() {
		while (true) {
			if (lrc != null && mediaPlayer != null) {
				long playTime = getPlayTime();
				timeLabel.setText(String.format("%d:%02d.%03d(%,03d)", playTime / 1000 / 60, playTime / 1000 % 60, playTime % 1000, playTime));
				// 更新歌词
				Lrc.Info info = lrc.getInfoByTime(playTime);
				mainLabel.setText(info.text);
				mainLabel.setProgress(info.textProgress, info.textStart, info.textEnd);
				translateLabel.setText(info.subText);
				translateLabel.setProgress(info.subTextProgress, info.subTextStart, info.subTextEnd);
				// 在列表中选中当前歌词
				if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
					int index = lrc.getIndexByTime(playTime);
					int lastIndex = lrcList.getSelectedIndex();
					// 仅在需要更新时变化
					//noinspection SynchronizeOnNonFinalField
					synchronized (lrcList) {
						if (lastIndex != index && !lockListSelection) {
							lrcList.setSelectedIndex(index);
							boolean canScroll = false;
							// 确保上次选中项在可视范围
							if (lastIndex != -1) {
								canScroll = lrcList.getFirstVisibleIndex() <= lastIndex && lrcList.getLastVisibleIndex() >= lastIndex;
							}
							if (canScroll) {
								lrcList.ensureIndexIsVisible(index);
								repaint();
							}
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
