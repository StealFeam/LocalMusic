# 安装体验(Download)
[Love Music](https://github.com/StealFeam/LocalMusic/raw/master/app/release/app-release.apk)

### 下一步计划
- 使用JetPack Compose替换掉xml布局
- 增加Material Design的动态效果
- ...

**新版说明**
- ViewPager2 + DiffUtil + ViewBinding
- MediaBrowserService + MediaBrowserServiceCompat实现的后台播放
- 由于Android 11的限制，无法全盘扫描，所以通过系统的媒体库进行获取数据。
- Android 10以下的版本还是通过全盘扫描，Fork/Join方式

如果出现问题请提交issue或者联系我 2295573743@qq.com
出现闪退可查看本机中Download文件夹有个music_error_log.txt中有异常的详细信息
如果能提供这个文件最好不过了

<img src="https://raw.githubusercontent.com/Sole2016/LocalMusic/master/screenshots/home5.png" width="50%" height="50%" />
