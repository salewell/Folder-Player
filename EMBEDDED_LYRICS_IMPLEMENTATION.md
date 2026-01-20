# 内嵌歌词功能 - 新实现方案

## ✅ 已完成 (使用 ExoPlayer MetadataRetriever)

### 优点
- ✅ **无需第三方依赖**：完全使用 Media3 (ExoPlayer) 内置功能
- ✅ **轻量级**：不会增加 APK 大小
- ✅ **兼容性好**：与现有 ExoPlayer 完美集成
- ✅ **性能优化**：只针对本地文件提取，不影响 WebDAV 播放

### 实现细节

#### 1. 新建 `EmbeddedLyricsExtractor.kt`
- 使用 `MetadataRetriever.retrieveMetadata()` 异步提取元数据
- 支持 ID3v2 标签（MP3）
- 查找 `USLT` (Unsynchronized Lyrics) 帧
- 使用 Kotlin Coroutines 异步处理

#### 2. 支持的格式
- MP3 (ID3v2 tags) ✅
- FLAC (Vorbis Comments) - 部分支持
- M4A/AAC - 部分支持  
- OGG/OPUS - 部分支持

主要支持 MP3 格式的 USLT 标签。

#### 3. 工作流程
```kotlin
1. 检查文件是否为本地文件
2. 检查文件扩展名是否支持
3. 使用 MetadataRetriever 提取元数据
4. 遍历元数据查找 USLT 歌词帧
5. 解析 LRC 格式并显示
6. 如果没有内嵌歌词，回退到外部 .lrc 文件
```

### 依赖更新
```kotlin
// gradle/libs.versions.toml
androidx-media3-extractor = { ... }

// app/build.gradle.kts
implementation(libs.androidx.media3.extractor)
```

### 测试方法

#### 准备测试文件
使用 Mp3tag 在 MP3 文件中添加 USLT 标签：
1. 打开 Mp3tag
2. 选择 MP3 文件
3. 右键 → Extended Tags (Alt+T)
4. Add field → "UNSYNCEDLYRICS"
5. 粘贴 LRC 格式歌词
6. 保存

#### LRC 格式示例
```
[00:12.00]第一句歌词
[00:17.20]第二句歌词
[00:21.10]第三句歌词
```

### 已知限制
- ✅ 仅支持本地文件（WebDAV 使用外部 .lrc）
- ⚠️ ExoPlayer 对某些格式的元数据支持有限
- ⚠️ 主要针对 MP3 ID3v2 标签优化

### 故障排除

如果内嵌歌词不显示：
1. 确认文件是 MP3 格式
2. 确认使用的是 USLT 或 UNSYNCEDLYRICS 标签
3. 检查 Logcat 查看 "EmbeddedLyricsExtractor" 日志
4. 尝试使用外部 .lrc 文件作为备用

## 下一步

如果 ExoPlayer 的元数据提取功能不够强大，可以考虑：
1. 针对 MP3 使用更专门的 ID3 库
2. 或者主推外部 .lrc 文件方案
