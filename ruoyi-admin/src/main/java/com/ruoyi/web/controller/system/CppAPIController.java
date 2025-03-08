package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.common.utils.CacheUtils;
import com.ruoyi.framework.web.service.CacheService;
import com.ruoyi.system.domain.WriteMockBO;
import com.ruoyi.system.service.CommonFileServer;
import com.ruoyi.web.controller.common.CommonController;
import com.ruoyi.web.controller.domain.dto.RCopyDTO;
import com.ruoyi.web.controller.domain.dto.WriteMockDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

/**
 * 为cpp程序提供的appi
 * 可能什么都有，因为这本身就是一个糅杂的接口
 */
@RequestMapping("/cpp-api")
@RestController
public class CppAPIController {

    private static final Logger log = LoggerFactory.getLogger(CppAPIController.class);
    @Value("${cpp-api.profile}")
    private String savePath;

    @Value("${cpp-api.download-block-size}")
    private int download_block_size;

    @Resource
    private CommonFileServer cs;

    /**
     *
     * @param _File
     * @param _Usr
     * @param _Local 在用户目录下将创建的文件夹
     * @return
     */
    @PostMapping("/f/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile _File, @RequestParam("user") String _Usr, @RequestParam(value = "local",required = false) String _Local)
    {
        try
        {
            if (StringUtils.isEmpty(_Usr))
                return AjaxResult.fail("用户名不能为空");
            if (null == _File || _File.isEmpty())
                return AjaxResult.fail("文件不能为空");
            StringBuilder bf = new StringBuilder(savePath).append("/").append(_Usr);
            {
                File f = new File(bf.toString());
                if (!f.exists())
                {
                    if (!f.mkdir())
                        throw new RuntimeException("用户目录创建失败");
                }
            }
            if (StringUtils.isNotEmpty(_Local))
            {
                bf.append(_Local);
                File f = new File(bf.toString());
                if (!f.exists())
                {
                    if (!f.mkdirs())
                        throw new RuntimeException("文件目录创建失败");
                }
            }
            bf.append("/").append(_File.getOriginalFilename());
            File f = new File(bf.toString());
            _File.transferTo(f);
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(), e);
            return AjaxResult.fail(e.getType(), e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getMessage());
        }
        catch (Exception e)
        {
            log.error("上传文件失败：",e);
            return AjaxResult.fail(e.getMessage());
        }
        return AjaxResult.success("上传成功");
    }

    @GetMapping("/f/download")
    public AjaxResult download(@RequestParam("user")String _Usr, @RequestParam("local")String _Local, @RequestParam(value = "offs",defaultValue = "0") Long __offset, HttpServletResponse response)
    {
        try {
            if (null == _Local || _Local.isEmpty()) {
                throw new RuntimeException("文件的相对地址不能为空");
            }
            if (null == _Usr || _Usr.isEmpty())
            {
                throw new ServiceExcept(AjaxResult.Type.USER_NOT_EMPTY ,"必须指定用户名");
            }
            if (null == __offset)
            {
                throw new ServiceExcept(AjaxResult.Type.FILE_OFFSET_IS_NULL_ERR ,"文件偏移量不能为空");
            }
            if (0 > __offset)
            {
                throw new ServiceExcept(AjaxResult.Type.FILE_OFFSET_LESS_THAN_ZERO_ERR ,"文件偏移量不能小于0");
            }
            File tmp = new File(savePath + "/" + _Usr + _Local);
            if (!tmp.exists()) {
//                response.sendError(-1,"文件不存在");
                throw new ServiceExcept(AjaxResult.Type.FILE_NOT_EXISTS ,"文件不存在");
            }
            RandomAccessFile raf = new RandomAccessFile(tmp, "r");
            if (__offset > raf.length())
            {
                throw new ServiceExcept(AjaxResult.Type.FILE_OFFSET_MORE_THAN_FILE_SIZE_ERR ,"偏移量超过了文件大小");
            }
            response.addHeader("Total-Length",String.valueOf(raf.length()));
            raf.seek(__offset);
            byte buffer[] = new byte[download_block_size];
            int real_read_len = raf.read(buffer);
            response.addHeader("Content-Length",String.valueOf(real_read_len));
            if (__offset + real_read_len == raf.length())
            {
                response.addHeader("Next-Offset",String.valueOf(-1));
            }
            else
            {
                response.addHeader("Next-Offset",String.valueOf(__offset + real_read_len));
            }
            raf.close();
            BufferedOutputStream buffops = new BufferedOutputStream(response.getOutputStream());
//            ServletOutputStream buffops = response.getOutputStream();
            buffops.write(buffer, 0, real_read_len);
            buffops.flush();
            buffops.close();
            response.addHeader("code",Constants.SUCCESS);
            System.gc();
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(), e);
            response.addHeader("code", String.valueOf(e.getType()));
            return AjaxResult.fail(e.getType(), e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
            response.addHeader("code", String.valueOf(AjaxResult.Type.DOWNLOAD_FAIL));
            return AjaxResult.fail(AjaxResult.Type.DOWNLOAD_FAIL, e.getMessage());
        }
        catch (Exception e) {
            log.error("文件获取失败", e);
            response.addHeader("code", String.valueOf(AjaxResult.Type.DOWNLOAD_FAIL));
            return AjaxResult.fail(AjaxResult.Type.DOWNLOAD_FAIL, e.getMessage());
        }
        return AjaxResult.ok("下载成功");
    }

    @DeleteMapping("/f/del")
    public AjaxResult del(@RequestParam("user") String _Usr,@RequestParam(value = "local",required = false) String _Local)
    {
        StringBuffer msg = new StringBuffer(32);
        try {
            if (StringUtils.isEmpty(_Usr))
                throw new ServiceExcept(AjaxResult.Type.USER_NOT_EMPTY,"用户名不能为空");

            StringBuilder bf = new StringBuilder(savePath).append("/").append(_Usr);
            if (StringUtils.isNotEmpty(_Local))
            {
                bf.append(_Local);
            }
            File f = new File(bf.toString());
            if (!f.exists())
            {
                return AjaxResult.ok("文件或目录不存在，什么都没删除");
            }
            int argv[] = {0,0};
            cs.delete(f,argv);
            msg.append("文件或目录删除成功;");
            msg.append("一共").append(argv[1]).append("个文件,其中").append(argv[0]).append("个文件删除成功;").append(argv[1] - argv[0]).append("个文件删除失败.");
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getType(), e.getMessage());
        }
        catch (Exception e)
        {
            log.error("未知异常: ",e);
            return AjaxResult.fail(AjaxResult.Type.DEL_FILE_FAIL,e.getMessage());
        }
        return AjaxResult.ok(msg.toString());
    }

    @GetMapping("/f/ls")
    public AjaxResult list(@RequestParam("user") String _Usr,@RequestParam(value = "local",required = false) String _Local)
    {
        StringBuilder bf = new StringBuilder(savePath);
        bf.append("/").append(_Usr);
        if (StringUtils.isNotEmpty(_Local))
            bf.append(_Local);
        List<String> data = cs.list(bf.toString());
        return AjaxResult.ok(data.isEmpty() ? null : data);
    }

    /**
     *
     * @param dto addition 如果MockId相同，则 1时代表追加写，0代表覆盖写
     * @return
     */
    @PostMapping("/mock/w")
//    public AjaxResult write_mock(@RequestParam("user")String _Usr,@RequestParam("mockId")String _MockId,@RequestParam(value = "data",required = false)String _Data,@RequestParam(value = "addition",required = false) Integer _Addition)
    public AjaxResult write_mock(@RequestBody WriteMockDTO dto)
    {
        try
        {
            if (StringUtils.isEmpty(dto.getMockId()) || StringUtils.isEmpty(dto.getUser()))
                return AjaxResult.fail(AjaxResult.Type.LOST_MOCK_ID,"必须指定Mock的id和user");
            dto.setAddition(null == dto.getAddition() ? 0 : dto.getAddition());
            StringBuilder bf = new StringBuilder(savePath);
            bf.append("/").append(dto.getUser()).append(dto.getMockId());
            WriteMockBO bo = new WriteMockBO();
            BeanUtils.copyProperties(dto,bo);
            bo.setMockId(bf.toString());
            cs.write_mock(bo);
            return AjaxResult.ok("Mock数据写入成功");
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getType(), e.getMessage());
        }
        catch (Exception e)
        {
            log.error("写入Mock失败,:",e);
            return AjaxResult.fail(AjaxResult.Type.MOCK_WRITE_FAIL,"写入Mock失败");
        }

    }

    /**
     * 文件形式上传 Mock
     * @param _Usr
     * @param _MockId
     * @param _File
     * @param _Addition
     * @return
     */
    @PostMapping("/mock/wf")
    public AjaxResult write_mock(@RequestParam("user")String _Usr,@RequestParam("mockId")String _MockId,@RequestParam("file")MultipartFile _File,@RequestParam(value = "addition",required = false) Integer _Addition)
    {
        try
        {
            if (StringUtils.isEmpty(_Usr) || StringUtils.isEmpty(_MockId))
                return AjaxResult.fail(AjaxResult.Type.LOST_MOCK_ID,"必须指定Mock的id和user");
            if (null == _File || _File.isEmpty())
                return AjaxResult.fail("必须上传文件");
            StringBuilder bf = new StringBuilder(savePath);
            bf.append("/").append(_Usr).append(_MockId);
            WriteMockBO bo = new WriteMockBO();
            bo.setMockId(bf.toString());
            bo.setAddition(null == _Addition ? 0 : _Addition);
            bo.setFile(_File);
            cs.write_mockf(bo);
            return AjaxResult.ok("Mock数据写入成功");
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getType(),e.getMessage());
        }
        catch (Exception e)
        {
            log.error("写入Mock失败,:",e);
            return AjaxResult.fail(AjaxResult.Type.MOCK_WRITE_FAIL,"写入Mock失败");
        }

    }

    @GetMapping("/mock/r")
    public AjaxResult read_mock(@RequestParam("user") String _Usr,@RequestParam("mockId") String _MockId)
    {
        try
        {
            if (StringUtils.isEmpty(_MockId) || StringUtils.isEmpty(_Usr))
                return AjaxResult.fail(AjaxResult.Type.LOST_MOCK_ID,"必须指定Mock的id和user");
            StringBuilder bf = new StringBuilder(savePath);
            bf.append("/").append(_Usr).append(_MockId);
            return AjaxResult.ok("请求成功",cs.read_mock(bf.toString()));
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getType(),e.getMessage());
        }
        catch (Exception e)
        {
            log.error("写入Mock失败,:",e);
            return AjaxResult.fail(AjaxResult.Type.MOCK_WRITE_FAIL,"写入Mock失败");
        }
    }

    @PostMapping("/rcopy")
    public AjaxResult rcopyw(RCopyDTO _Dto)
    {
        try
        {
            CacheUtils.put(_Dto.getUsername(), _Dto.getData());
        }
        catch (RuntimeException re)
        {
            return AjaxResult.fail(re.getMessage());
        }
        return AjaxResult.ok();
    }

    @GetMapping(value = "/rpaste")
    public String rcopyr(@RequestParam("username") String _Usrname, HttpServletResponse response)
    {
        try
        {
            return null == CacheUtils.get(_Usrname) ? "" : CacheUtils.get(_Usrname).toString();
        }
        catch (RuntimeException re)
        {
            return "";
        }
    }

}
