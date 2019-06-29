# MusicWithLrc
带有歌词的音乐播放器

音乐路径：程序当前目录/music
歌词路径：程序当前目录/lrc

注意：必须有歌词文件才能播放音乐。

## 歌词格式
* lrc扩展名，文件名与音乐名相同；
* 中括号标记时间，格式为：`[m:s.ms]`，位数不做要求；
* 行首有标记时间，则该行为歌词行。歌词行中任意位置均可插入时间，来控制速度；
* 歌词行后可以添加翻译行，翻译行也可以标记时间，但不能在行首标记；
* 如果该行格式为`[key:value]`，则为标签行。`key`表示键，`value`表示值；
* 空行自动忽略，行首行尾空格自动忽略

## 支持的键
键名|说明|默认值
:---:|:---:|:---:
offset|整体偏移时间，文件中出现的所有时间将会与该时间相加，得出实际时间。|0