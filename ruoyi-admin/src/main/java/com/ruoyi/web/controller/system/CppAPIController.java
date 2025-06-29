package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.ErrorCode;
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
            return AjaxResult.fail(e.getCode(), e.getMessage());
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
                throw new ServiceExcept("The relative path of the file cannot be empty.");
            }
            if (null == _Usr || _Usr.isEmpty())
            {
                throw new ServiceExcept(ErrorCode.USER_NOT_EMPTY ,ErrorCode.USER_NOT_EMPTY.what());
            }
            if (null == __offset)
            {
                throw new ServiceExcept(ErrorCode.FILE_OFFSET_IS_NULL_ERR ,ErrorCode.FILE_OFFSET_IS_NULL_ERR.what());
            }
            if (0 > __offset)
            {
                throw new ServiceExcept(ErrorCode.FILE_OFFSET_LESS_THAN_ZERO_ERR ,ErrorCode.FILE_OFFSET_LESS_THAN_ZERO_ERR.what());
            }
            // -1 表示文件不存在
            response.addIntHeader("File-Status",ErrorCode.FILE_NOT_EXISTS.code());
            File tmp = new File(savePath + "/" + _Usr + _Local);
            if (!tmp.exists()) {
                throw new ServiceExcept(ErrorCode.FILE_NOT_EXISTS ,ErrorCode.FILE_NOT_EXISTS.what());
            }
            // 文件存在
            response.setIntHeader("File-Status", 0);
            RandomAccessFile raf = new RandomAccessFile(tmp, "r");
            if (__offset > raf.length())
            {
                throw new ServiceExcept(ErrorCode.FILE_OFFSET_MORE_THAN_FILE_SIZE_ERR ,ErrorCode.FILE_OFFSET_MORE_THAN_FILE_SIZE_ERR.what());
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
            response.addIntHeader("Code",ErrorCode.SUCCESS.code());
            response.addHeader("Msg",ErrorCode.SUCCESS.what());
            BufferedOutputStream buffops = new BufferedOutputStream(response.getOutputStream());
//            ServletOutputStream buffops = response.getOutputStream();
            buffops.write(buffer, 0, real_read_len);
            buffops.flush();
            buffops.close();
            System.gc();
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(), e);
            response.addHeader("Code", String.valueOf(e.getCode()));
            response.addHeader("Msg", e.getMessage());
            return AjaxResult.fail(e.getCode(), e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
            response.addHeader("Code", String.valueOf(ErrorCode.DOWNLOAD_FAIL.code()));
            response.addHeader("Msg", e.getMessage());
            return AjaxResult.fail(ErrorCode.DOWNLOAD_FAIL, e.getMessage());
        }
        catch (Exception e) {
            log.error("文件获取失败", e);
            response.addHeader("Code", String.valueOf(ErrorCode.DOWNLOAD_FAIL.code()));
            response.addHeader("Msg", e.getMessage());
            return AjaxResult.fail(ErrorCode.DOWNLOAD_FAIL, e.getMessage());
        }
        return AjaxResult.ok("下载成功");
    }

    @DeleteMapping("/f/del")
    public AjaxResult del(@RequestParam("user") String _Usr,@RequestParam(value = "local",required = false) String _Local)
    {
        StringBuffer msg = new StringBuffer(32);
        try {
            if (StringUtils.isEmpty(_Usr))
                throw new ServiceExcept(ErrorCode.USER_NOT_EMPTY,"用户名不能为空");

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
            return AjaxResult.fail(e.getCode(), e.getMessage());
        }
        catch (Exception e)
        {
            log.error("未知异常: ",e);
            return AjaxResult.fail(ErrorCode.DEL_FILE_FAIL,e.getMessage());
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
                return AjaxResult.fail(ErrorCode.LOST_MOCK_ID,"必须指定Mock的id和user");
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
            return AjaxResult.fail(e.getCode(), e.getMessage());
        }
        catch (Exception e)
        {
            log.error("写入Mock失败,:",e);
            return AjaxResult.fail(ErrorCode.MOCK_WRITE_FAIL,"写入Mock失败");
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
                return AjaxResult.fail(ErrorCode.LOST_MOCK_ID,"必须指定Mock的id和user");
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
            return AjaxResult.fail(e.getCode(),e.getMessage());
        }
        catch (Exception e)
        {
            log.error("写入Mock失败,:",e);
            return AjaxResult.fail(ErrorCode.MOCK_WRITE_FAIL,"写入Mock失败");
        }

    }

    @GetMapping("/mock/r")
    public AjaxResult read_mock(@RequestParam("user") String _Usr,@RequestParam("mockId") String _MockId)
    {
        try
        {
            if (StringUtils.isEmpty(_MockId) || StringUtils.isEmpty(_Usr))
                return AjaxResult.fail(ErrorCode.LOST_MOCK_ID,"必须指定Mock的id和user");
            StringBuilder bf = new StringBuilder(savePath);
            bf.append("/").append(_Usr).append(_MockId);
            return AjaxResult.ok("请求成功",cs.read_mock(bf.toString()));
        }
        catch (ServiceExcept e)
        {
            log.error(e.getMessage(),e);
            return AjaxResult.fail(e.getCode(),e.getMessage());
        }
        catch (Exception e)
        {
            log.error("读取Mock失败,:",e);
            return AjaxResult.fail(ErrorCode.MOCK_WRITE_FAIL,"写入Mock失败");
        }
    }

    @PostMapping("/rcopy")
    public AjaxResult rcopy(RCopyDTO _Dto)
    {
        try
        {
            if (null == _Dto.getUserid() || _Dto.getUserid().isEmpty())
                return AjaxResult.fail("userid不能为空.");
            CacheUtils.put(_Dto.getUserid(), _Dto.getData());
        }
        catch (RuntimeException re)
        {
            log.error("/rcopy接口出现异常: ", re);
            return AjaxResult.fail("复制失败, 失败原因: " + re.getMessage());
        }
        return AjaxResult.ok("复制成功");
    }

    @GetMapping(value = "/rpaste")
    public AjaxResult rpaste(@RequestParam(value = "userid", required = false) String _User_id, HttpServletResponse response)
    {
        try
        {
            if (null == _User_id || _User_id.isEmpty())
                return AjaxResult.fail("userid不能为空");
            return AjaxResult.ok( "粘贴成功", null == CacheUtils.get(_User_id) ? "" : CacheUtils.get(_User_id).toString());
        }
        catch (RuntimeException re)
        {
            log.error("/rpaste接口出现异常,: ", re);
            String err_msg = "服务器异常, 异常信息: " + re.getMessage();
            return AjaxResult.fail("粘贴失败");
        }
    }

}
