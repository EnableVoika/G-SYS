package com.ruoyi.common.utils.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.DelFailFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 文件处理工具类
 * 
 * @author ruoyi
 */
public class FileUtils
{
    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     * 
     * @param filePath 文件路径
     * @param os 输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException
    {
        FileInputStream fis = null;
        try
        {
            File file = new File(filePath);
            if (!file.exists())
            {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0)
            {
                os.write(b, 0, length);
            }
        }
        catch (IOException e)
        {
            throw e;
        }
        finally
        {
            IOUtils.close(os);
            IOUtils.close(fis);
        }
    }

    /**
     * 写数据到文件中
     *
     * @param data 数据
     * @return 目标文件
     * @throws IOException IO异常
     */
    public static String writeImportBytes(byte[] data) throws IOException
    {
        return writeBytes(data, RuoYiConfig.getImportPath());
    }

    /**
     * 写数据到文件中
     *
     * @param data 数据
     * @param uploadDir 目标文件
     * @return 目标文件
     * @throws IOException IO异常
     */
    public static String writeBytes(byte[] data, String uploadDir) throws IOException
    {
        FileOutputStream fos = null;
        String pathName = "";
        try
        {
            String extension = getFileExtendName(data);
            pathName = DateUtils.datePath() + "/" + IdUtils.fastUUID() + "." + extension;
            File file = FileUploadUtils.getAbsoluteFile(uploadDir, pathName);
            fos = new FileOutputStream(file);
            fos.write(data);
        }
        finally
        {
            IOUtils.close(fos);
        }
        return FileUploadUtils.getPathFileName(uploadDir, pathName);
    }

    /**
     * 删除文件
     * 
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath)
    {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists())
        {
            flag = file.delete();
        }
        return flag;
    }

    /**
     * 文件名称验证
     * 
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename)
    {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     * 
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource)
    {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, ".."))
        {
            return false;
        }

        // 检查允许下载的文件规则
        if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(resource)))
        {
            return true;
        }

        // 不在允许下载的文件规则
        return false;
    }

    /**
     * 下载文件名重新编码
     * 
     * @param request 请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException
    {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE"))
        {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        }
        else if (agent.contains("Firefox"))
        {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        }
        else if (agent.contains("Chrome"))
        {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        else
        {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 下载文件名重新编码
     *
     * @param response 响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException
    {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException
    {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }

    /**
     * 获取图像后缀
     * 
     * @param photoByte 图像数据
     * @return 后缀名
     */
    public static String getFileExtendName(byte[] photoByte)
    {
        String strFileExtendName = "jpg";
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70) && (photoByte[3] == 56)
                && ((photoByte[4] == 55) || (photoByte[4] == 57)) && (photoByte[5] == 97))
        {
            strFileExtendName = "gif";
        }
        else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73) && (photoByte[9] == 70))
        {
            strFileExtendName = "jpg";
        }
        else if ((photoByte[0] == 66) && (photoByte[1] == 77))
        {
            strFileExtendName = "bmp";
        }
        else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71))
        {
            strFileExtendName = "png";
        }
        return strFileExtendName;
    }

    /**
     * 获取文件名称 /profile/upload/2022/04/16/ruoyi.png -- ruoyi.png
     * 
     * @param fileName 路径名称
     * @return 没有文件路径的名称
     */
    public static String getName(String fileName)
    {
        if (fileName == null)
        {
            return null;
        }
        int lastUnixPos = fileName.lastIndexOf('/');
        int lastWindowsPos = fileName.lastIndexOf('\\');
        int index = Math.max(lastUnixPos, lastWindowsPos);
        return fileName.substring(index + 1);
    }

    /**
     * 获取不带后缀文件名称 /profile/upload/2022/04/16/ruoyi.png -- ruoyi
     * 
     * @param fileName 路径名称
     * @return 没有文件路径和后缀的名称
     */
    public static String getNameNotSuffix(String fileName)
    {
        if (fileName == null)
        {
            return null;
        }
        String baseName = FilenameUtils.getBaseName(fileName);
        return baseName;
    }

    /**
     * 递归清理 rootDir 及其所有子目录下的空目录。
     * 如遇有文件，记录日志并跳过。
     * 最后也会把自身删除, 比如清理/a, 那么/a下面所有空目录都会删除, 最后再把自身删除
     * 该函数遇到目录下有文件不会删除, 会跳过并写入错误原因到_DelFailList
     * @param rootDir 待清理的目录
     * @return 返回未能删除的目录（目录下有文件）
     */
    public static List<Path> cleanEmptyDirs(List<DelFailFile> _DelFailList, Path rootDir) throws IOException {
        List<Path> undeletedDirs = new ArrayList<>();
        cleanEmptyDirsInternal(_DelFailList, rootDir, undeletedDirs);
        return undeletedDirs;
    }

    public static List<Path> cleanEmptyDirs(List<DelFailFile> _DelFailList, String _Root, String relative) throws IOException {
        Path fullPath = Path.of(_Root, relative);
        List<Path> undeletedDirs = new ArrayList<>();
        cleanEmptyDirsInternal(_DelFailList, fullPath, undeletedDirs);
        return undeletedDirs;
    }

    // 内部递归函数
    public static boolean cleanEmptyDirsInternal(List<DelFailFile> _DelFailList, Path dir, List<Path> undeletedDirs) throws IOException
    {
        if (!Files.isDirectory(dir))
            return false;
        boolean canDelete = true;

        // 检查子目录和文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    boolean subDirCanDelete = cleanEmptyDirsInternal(_DelFailList, child, undeletedDirs);
                    if (!subDirCanDelete) canDelete = false;
                } else {
                    // 有文件就不能删
                    canDelete = false;
                }
            }
        }

        if (canDelete) {
            try {
                Files.delete(dir);
                // System.out.println("删除空目录：" + dir);
            } catch (IOException e) {
                undeletedDirs.add(dir);
                // System.err.println("无法删除空目录 " + dir + "，异常：" + e.getMessage());
                return false;
            }
            return true;
        } else {
            undeletedDirs.add(dir); // 目录下还有文件
            // System.err.println("目录下还有文件，不能删除: " + dir);
            _DelFailList.add(new DelFailFile(dir.toString(), "还有文件，可能之前移动失败，因此该路径无法删除"));
            return false;
        }
    }

    /**
     * 递归收集 root/relative 下所有叶子路径（相对root的路径）。
     * 所有相对路径保证无开头"/"或"\"，且分隔符全部为"/"。
     * @param resultList 输出结果（相对路径字符串，分隔符全为/）
     * @param root       根目录
     * @param relative   当前递归相对路径，可传"c/d"、"/c/d"、"\c\d"等任意风格
     */
    public static void collectLeafPaths(List<String> resultList, Path root, String relative) throws IOException
    {
        // 1. 规范化传入的相对路径
        String relNorm = relative.replaceAll("^[\\\\/]+", "")   // 去掉所有开头的斜杠
                .replace("\\", "/");           // 统一分隔符
        Path relPath = relNorm.isEmpty() ? Paths.get("") : Paths.get(relNorm);
        Path absPath = root.resolve(relPath);

        // 2. 判断叶子并收集
        if (Files.isRegularFile(absPath) || isEmptyDir(absPath)) {
            // 规范化list里所有路径
            String addPath = relPath.toString().replace("\\", "/");
            resultList.add(addPath);
            return;
        }

        // 3. 递归目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(absPath)) {
            for (Path child : stream) {
                // 递归时拼接新相对路径，并传递字符串格式
                String childRel = relNorm.isEmpty()
                        ? child.getFileName().toString()
                        : relNorm + "/" + child.getFileName().toString();
                collectLeafPaths(resultList, root, childRel);
            }
        }
    }

    public static void collectLeafPaths(List<String> _ResultList, String _Root, String _Relative) throws IOException {
        Path root = Path.of(_Root);
        collectLeafPaths(_ResultList, root, _Relative);
    }

    // 判断是不是空目录
    private static boolean isEmptyDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        }
    }
}

