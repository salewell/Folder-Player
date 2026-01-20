# How to Add Lyrics to Your Music

This guide explains how to add synchronized lyrics (LRC format) to your music for display in Folder Player.

## ⚠️ Important: Recommended Method

**We strongly recommend using external `.lrc` files** instead of embedded lyrics for the following reasons:

- ✅ **100% reliable** - works on all devices and formats
- ✅ **Easy to edit** - just edit the text file
- ✅ **Works with WebDAV** - can be stored on your NAS
- ✅ **No compatibility issues** - guaranteed to work

**Embedded lyrics are experimental** and have significant limitations (see Method 2 below).

---

## Method 1: External .lrc Files (✅ Recommended)

### Step 1: Create the LRC File

1. Create a text file with the same name as your music file
2. Change the extension to `.lrc`
3. Place it in the same folder as the music file

**Example:**
```
Music/
  ├── song.mp3
  └── song.lrc  ← Create this file
```

### Step 2: Add Lyrics in LRC Format

Open the `.lrc` file in any text editor and add lyrics with timestamps:

```
[00:12.00]First line of lyrics
[00:17.20]Second line of lyrics
[00:21.10]Third line of lyrics
[00:25.50]And so on...
```

**Time Format:** `[mm:ss.xx]` where:
- `mm` = minutes
- `ss` = seconds  
- `xx` = hundredths of a second

### Online Tools for Creating LRC Files

- **LRC Maker**: https://lrc-maker.github.io/ (plays music and helps you time lyrics)
- **LRC Editor**: Various online editors available
- Or use any text editor!

### For WebDAV/NAS Users

Simply upload both the music file and `.lrc` file to your WebDAV server:

```
NAS/Music/
  ├── song.mp3
  └── song.lrc
```

The app will automatically find and display the lyrics!

---

## Method 2: Embedded Lyrics (⚠️ Experimental)

> **Warning**: This method is experimental and has limited support. It only works for:
> - Local MP3/M4A files (not WebDAV)
> - Devices with compatible Android versions
> - Files with properly embedded USLT tags
>
> **Many files may not work!** Use Method 1 (external .lrc) for reliability.

### Supported Formats
- **MP3** (ID3v2 tags) - Best support
- **M4A/MP4** (iTunes tags) - Limited support
- FLAC, OGG, OPUS - Not supported

### Using Mp3tag (Windows/Mac)

[Mp3tag](https://www.mp3tag.de/) is a free metadata editor.

**Steps:**
1. Download and install Mp3tag
2. Open your MP3 file in Mp3tag
3. Right-click the file → "Extended Tags" (or press `Alt+T`)
4. Click "Add field"
5. Select "UNSYNCEDLYRICS" from the dropdown
6. Paste your LRC formatted lyrics:
   ```
   [00:12.00]First line
   [00:17.20]Second line
   ```
7. Click OK and save (`Ctrl+S`)

### Using Kid3 (Linux/Mac/Windows)

[Kid3](https://kid3.kde.org/) is an open-source tag editor.

**Steps:**
1. Install Kid3
2. Open your music file
3. Find the "Unsynchronized Lyrics" field in the tag editor
4. Paste your LRC lyrics
5. Save the file

### Using MusicBrainz Picard (Cross-platform)

[Picard](https://picard.musicbrainz.org/) is an open-source music tagger.

**Steps:**
1. Install Picard
2. Load your music file
3. Add a new tag: `UNSYNCEDLYRICS`
4. Paste your LRC lyrics
5. Save the file

---

## Verification & Troubleshooting

### Verifying External .lrc Files

1. Make sure the `.lrc` file has **exactly the same name** as the music file
2. Both files must be in the **same folder**
3. Play the song in Folder Player
4. Lyrics should auto-scroll!

### If Lyrics Don't Show

**For External .lrc files:**
- ✅ Check the file name matches exactly (case-sensitive on some systems)
- ✅ Verify the `.lrc` file is in the same directory
- ✅ Open the `.lrc` file and check the time format `[mm:ss.xx]`
- ✅ Make sure the file is UTF-8 encoded (not UTF-16)

**For Embedded lyrics:**
- ⚠️ **This is expected!** Embedded lyrics have very limited support
- ⚠️ Only works for local MP3/M4A files
- ⚠️ Support varies by device and Android version
- ✅ **Solution**: Use external `.lrc` files instead (100% reliable)

### Common LRC Format Mistakes

❌ Wrong:
```
0:12 First line
[0:12.0] Second line
00:17.20 Third line
```

✅ Correct:
```
[00:12.00]First line
[00:12.00]Second line
[00:17.20]Third line
```

---

## Tips & Best Practices

1. **Always use Method 1** (external .lrc files) for reliability
2. **Batch Processing**: Use Mp3tag to process multiple files at once
3. **Backup**: Keep backup copies before modifying files
4. **UTF-8 Encoding**: Save `.lrc` files as UTF-8 (not UTF-16 or ANSI)
5. **Test First**: Test with one song before processing your entire library
6. **WebDAV Users**: Always use`.lrc` files (embedded lyrics don't work over WebDAV)

---

## Quick Reference

| Method | Reliability | Works with WebDAV | Difficulty | Recommendation |
|--------|-------------|-------------------|------------|----------------|
| External .lrc | ✅ 100% | ✅ Yes | ⭐ Easy | **Use This!** |
| Embedded | ⚠️ ~20% | ❌ No | ⭐⭐⭐ Hard | Experimental only |

---

## Resources

- **LRC Maker**: https://lrc-maker.github.io/
- **LRC Format Spec**: [Wikipedia](https://en.wikipedia.org/wiki/LRC_(file_format))
- **Mp3tag**: https://www.mp3tag.de/
- **Kid3**: https://kid3.kde.org/
- **Picard**: https://picard.musicbrainz.org/
