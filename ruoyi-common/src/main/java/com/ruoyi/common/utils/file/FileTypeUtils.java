package com.ruoyi.common.utils.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.common.core.domain.FileTypeInfo;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件类型工具类
 *
 * @author ruoyi
 */
public class FileTypeUtils
{
    /**
     * 获取文件类型
     * <p>
     * 例如: ruoyi.txt, 返回: txt
     * 
     * @param file 文件名
     * @return 后缀（不含".")
     */
    public static String getFileType(File file)
    {
        if (null == file)
        {
            return StringUtils.EMPTY;
        }
        return getFileType(file.getName());
    }

    /**
     * 获取文件类型
     * <p>
     * 例如: ruoyi.txt, 返回: txt
     *
     * @param fileName 文件名
     * @return 后缀（不含".")
     */
    public static String getFileType(String fileName)
    {
        int separatorIndex = fileName.lastIndexOf(".");
        if (separatorIndex < 0)
        {
            return "";
        }
        return fileName.substring(separatorIndex + 1).toLowerCase();
    }

    /**
     * 获取文件类型
     * 
     * @param photoByte 文件字节码
     * @return 后缀（不含".")
     */
    public static String getFileExtendName(byte[] photoByte)
    {
        String strFileExtendName = "JPG";
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70) && (photoByte[3] == 56)
                && ((photoByte[4] == 55) || (photoByte[4] == 57)) && (photoByte[5] == 97))
        {
            strFileExtendName = "GIF";
        }
        else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73) && (photoByte[9] == 70))
        {
            strFileExtendName = "JPG";
        }
        else if ((photoByte[0] == 66) && (photoByte[1] == 77))
        {
            strFileExtendName = "BMP";
        }
        else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71))
        {
            strFileExtendName = "PNG";
        }

        // ZIP (PKxx)
        else if (photoByte[0] == 80 && photoByte[1] == 75 &&
                (photoByte[2] == 3 || photoByte[2] == 5 || photoByte[2] == 7) &&
                (photoByte[3] == 4 || photoByte[3] == 6 || photoByte[3] == 8))
        {
            strFileExtendName = "ZIP";
        }
        // GZ/GZIP
        else if ((photoByte[0] & 0xFF) == 0x1F && (photoByte[1] & 0xFF) == 0x8B)
        {
            strFileExtendName = "GZ";
        }
        // RAR 4.x
        else if ((photoByte[0] & 0xFF) == 0x52 && (photoByte[1] & 0xFF) == 0x61 && (photoByte[2] & 0xFF) == 0x72
                && (photoByte[3] & 0xFF) == 0x21 && (photoByte[4] & 0xFF) == 0x1A && (photoByte[5] & 0xFF) == 0x07
                && (photoByte[6] & 0xFF) == 0x00)
        {
            strFileExtendName = "RAR";
        }
        // RAR 5.x
        else if ((photoByte[0] & 0xFF) == 0x52 && (photoByte[1] & 0xFF) == 0x61 && (photoByte[2] & 0xFF) == 0x72
                && (photoByte[3] & 0xFF) == 0x21 && (photoByte[4] & 0xFF) == 0x1A && (photoByte[5] & 0xFF) == 0x07
                && (photoByte[6] & 0xFF) == 0x01 && (photoByte[7] & 0xFF) == 0x00)
        {
            strFileExtendName = "RAR";
        }
        // 7Z
        else if ((photoByte[0] & 0xFF) == 0x37 && (photoByte[1] & 0xFF) == 0x7A && (photoByte[2] & 0xFF) == 0xBC
                && (photoByte[3] & 0xFF) == 0xAF && (photoByte[4] & 0xFF) == 0x27 && (photoByte[5] & 0xFF) == 0x1C)
        {
            strFileExtendName = "7Z";
        }
        // PDF
        else if (photoByte[0] == 0x25 && photoByte[1] == 0x50 && photoByte[2] == 0x44 && photoByte[3] == 0x46)
        {
            strFileExtendName = "PDF";
        }
        // DOC（D0CF11E0老版Office文档）
        else if ((photoByte[0] & 0xFF) == 0xD0 && (photoByte[1] & 0xFF) == 0xCF && (photoByte[2] & 0xFF) == 0x11 && (photoByte[3] & 0xFF) == 0xE0)
        {
            strFileExtendName = "DOC";
        }
        // EXE (MZ)
        else if (photoByte[0] == 0x4D && photoByte[1] == 0x5A)
        {
            strFileExtendName = "EXE";
        }
        // XML（以<开头且前几个字母出现"?xml"）
        else if (photoByte[0] == 0x3C && photoByte[1] == 0x3F && photoByte[2] == 0x78 && photoByte[3] == 0x6D && photoByte[4] == 0x6C)
        {
            strFileExtendName = "XML";
        }
        // YML/YAML（以'-'或'---'开头）
        else if (photoByte[0] == 0x2D && photoByte[1] == 0x2D && photoByte[2] == 0x2D)
        {
            strFileExtendName = "YML";
        }
        // WAV
        else if (photoByte[0] == 0x52 && photoByte[1] == 0x49 && photoByte[2] == 0x46 && photoByte[3] == 0x46 &&
                photoByte[8] == 0x57 && photoByte[9] == 0x41 && photoByte[10] == 0x56 && photoByte[11] == 0x45)
        {
            strFileExtendName = "WAV";
        }
        // FLAC
        else if (photoByte[0] == 0x66 && photoByte[1] == 0x4C && photoByte[2] == 0x61 && photoByte[3] == 0x43)
        {
            strFileExtendName = "FLAC";
        }
        // OGG
        else if (photoByte[0] == 0x4F && photoByte[1] == 0x67 && photoByte[2] == 0x67 && photoByte[3] == 0x53)
        {
            strFileExtendName = "OGG";
        }
        // BAT（无标准魔数，这里检测@echo开头的文本）
        else if (photoByte[0] == '@' && photoByte[1] == 'e' && photoByte[2] == 'c' && photoByte[3] == 'h' && photoByte[4] == 'o')
        {
            strFileExtendName = "BAT";
        }
        // TXT（纯文本只能简单判断：首字节ASCII可见字符/换行等）
        else if ((photoByte[0] >= 0x20 && photoByte[0] <= 0x7E) || photoByte[0] == 0x0D || photoByte[0] == 0x0A)
        {
            strFileExtendName = "TXT";
        }
        // MP3
        else if ((photoByte[0] & 0xFF) == 0xFF && (photoByte[1] & 0xE0) == 0xE0)
        {
            strFileExtendName = "MP3";
        }
        // MP4
        else if (photoByte[4] == 0x66 && photoByte[5] == 0x74 && photoByte[6] == 0x79 && photoByte[7] == 0x70)
        {
            strFileExtendName = "MP4";
        }
        return strFileExtendName;
    }

    private static final Map<String, FileTypeInfo> fileTypeIndexMap = new HashMap<String, FileTypeInfo>() {{
        put("FILE", new FileTypeInfo(0, "文件"));
        put("DIR", new FileTypeInfo(1, "文件夹"));
        put("JPG", new FileTypeInfo(2, "JPG图片"));
        put("JPEG", new FileTypeInfo(3, "JPEG图片"));
        put("GIF", new FileTypeInfo(4, "GIF图片"));
        put("PNG", new FileTypeInfo(5, "PNG图片"));
        put("BMP", new FileTypeInfo(6, "BMP图片"));
        put("ZIP", new FileTypeInfo(7, "ZIP压缩包"));
        put("RAR", new FileTypeInfo(8, "RAR压缩包"));
        put("7Z", new FileTypeInfo(9, "7Z压缩包"));
        put("GZ", new FileTypeInfo(10, "GZ压缩包"));
        put("TXT", new FileTypeInfo(11, "文本文件"));
        put("YML", new FileTypeInfo(12, "YML配置"));
        put("YAML", new FileTypeInfo(13, "YAML配置"));
        put("DOC", new FileTypeInfo(14, "Word文档"));
        put("DOCX", new FileTypeInfo(15, "Word文档"));
        put("PDF", new FileTypeInfo(16, "PDF文档"));
        put("XML", new FileTypeInfo(17, "XML文件"));
        put("MP3", new FileTypeInfo(18, "MP3音频"));
        put("MP4", new FileTypeInfo(19, "MP4视频"));
        put("WAV", new FileTypeInfo(20, "WAV音频"));
        put("FLAC", new FileTypeInfo(21, "FLAC音频"));
        put("OGG", new FileTypeInfo(22, "OGG音频"));
        put("EXE", new FileTypeInfo(23, "可执行文件"));
        put("BAT", new FileTypeInfo(24, "批处理脚本"));
        put("IPA", new FileTypeInfo(25, "iOS安装包"));
        put("APK", new FileTypeInfo(26, "Android安装包"));
        put("CSV", new FileTypeInfo(27, "CSV文件"));
        put("JSON", new FileTypeInfo(28, "JSON文件"));
        put("HTML", new FileTypeInfo(29, "HTML页面"));
        put("PPT", new FileTypeInfo(30, "PPT幻灯片"));
        put("PPTX", new FileTypeInfo(31, "PPTX幻灯片"));
        put("XLS", new FileTypeInfo(32, "Excel表格"));
        put("XLSX", new FileTypeInfo(33, "Excel表格"));
        put("SVG", new FileTypeInfo(34, "SVG图像"));
        put("MDB", new FileTypeInfo(35, "Access数据库"));
        put("LOG", new FileTypeInfo(36, "日志文件"));
    }};

    private static final Map<Integer, String> indexToFileTypeLabelMap = new HashMap<>();
    static {
        for (Map.Entry<String, FileTypeInfo> entry : fileTypeIndexMap.entrySet()) {
            int index = entry.getValue().getIndex();
            indexToFileTypeLabelMap.putIfAbsent(index, entry.getValue().getLabel());
        }
    }


    public static int getFileTypeIndex(String _SuffixName)
    {
        if (com.ruoyi.common.utils.StringUtils.isNotEmpty(_SuffixName))
            return fileTypeIndexMap.getOrDefault(_SuffixName.toUpperCase(), new FileTypeInfo(-1, "未知")).getIndex();
        return -1;
    }

    public static String getFileTypeIndexKey(int _Index)
    {
        String typeName = indexToFileTypeLabelMap.get(_Index);
        return typeName == null ? "" : typeName;
    }

    public static String getFileTypeIndexLabel(int _Index)
    {
        String typeName = indexToFileTypeLabelMap.get(_Index);
        return typeName == null ? "" : typeName;
    }

}