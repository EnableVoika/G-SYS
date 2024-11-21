package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceExcept;
import com.ruoyi.system.domain.WriteMockBO;
import com.ruoyi.system.service.CommonFileServer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CommonFileServerImpl implements CommonFileServer {


    /**
     *
     * @param _F
     * @param argv 0是已经删除的文件个数 1是总文件个数
     */
    public void delete(File _F,int argv[])
    {
        if (_F.isFile())
        {
            argv[1] += 1;
            if(_F.delete())
            {
                argv[0] += 1;
            }
            return;
        }
        File fs[] = _F.listFiles();
        if (null != fs)
        {
            for (File file : fs)
            {
                delete(file,argv);
            }
        }
        // 把空文件夹删了
        _F.delete();
    }

    @Override
    public List<String> list(String _Local)
    {
        File list = new File(_Local);
        List<String> data = new ArrayList<>();
        // /结尾，说明是文件夹但不一定 100% 就是，只是如果结尾是/，可以直接认定是文件夹避免后续多余的操作
        if (_Local.endsWith("/"))
        {
            if (!list.isDirectory())
            {
                return data;
            }
            // 得确保不仅/结尾，还得是个文件夹，不然随便传一个dsw/，虽然/结尾，但不是个文件夹
            if (list.isDirectory())
            {
                for (File file : list.listFiles())
                {
                    StringBuilder bf = new StringBuilder(file.getName());
                    if (file.isDirectory())
                    {
                        bf.append("/");
                        data.add(bf.toString());
                        continue;
                    }
                    data.add(bf.toString());
                }
            }
            return data;
        }
        // 不是/结尾，但依然是文件夹
        if (list.isDirectory())
        {
            for (File file : list.listFiles())
            {
                StringBuilder bf = new StringBuilder(file.getName());
                if (file.isDirectory())
                {
                    bf.append("/");
                    data.add(bf.toString());
                    continue;
                }
                data.add(bf.toString());
            }
            return data;
        }
        if (list.isFile())
            data.add(list.getName());
        return data;
    }

    @Override
    public int write_mock(WriteMockBO bo) throws IOException
    {
        File mock = new File(bo.getMockId());
        // 判断文件夹是否存在
        File local = new File(bo.getMockId().substring(0,bo.getMockId().lastIndexOf("/")));
        if (!local.exists())
            local.mkdirs();
        // 如果不存在，则忽略 _Addition
        if (!mock.exists())
        {
            FileWriter fw = new FileWriter(mock);
            PrintWriter pw = new PrintWriter(fw);
            if (StringUtils.isNotEmpty(bo.getData()))
                pw.print(bo.getData());
            pw.close();
            fw.close();
            return 1;
        }
        // 如果存在，则看是否是追加写
        FileWriter fw = new FileWriter(mock,bo.getAddition() == 1);
        PrintWriter pw = new PrintWriter(fw);
        if (StringUtils.isNotEmpty(bo.getData()))
            pw.print(bo.getData());
        pw.close();
        fw.close();
        return 1;
    }

    @Override
    public int write_mockf(WriteMockBO bo) throws IOException
    {
        File mock = new File(bo.getMockId());
        // 判断文件夹是否存在
        File local = new File(bo.getMockId().substring(0,bo.getMockId().lastIndexOf("/")));
        if (!local.exists())
            local.mkdirs();
        // 如果不存在，则忽略 _Addition
        if (!mock.exists())
        {
            InputStream fis = bo.getFile().getInputStream();
            BufferedInputStream bis = new BufferedInputStream(fis,1024);
            byte binaryData[] = bis.readAllBytes();
            OutputStream mf = new FileOutputStream(bo.getMockId());
            BufferedOutputStream bos = new BufferedOutputStream(mf,1024);
            bos.write(binaryData);
            bos.close();
            mf.close();
            bis.close();
            fis.close();
            return 1;
        }
        InputStream fis = bo.getFile().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(fis,1024);
        byte binaryData[] = bis.readAllBytes();
        OutputStream mf = new FileOutputStream(bo.getMockId(),bo.getAddition() == 1);
        BufferedOutputStream bos = new BufferedOutputStream(mf,1024);
        bos.write(binaryData);
        bos.close();
        mf.close();
        bis.close();
        fis.close();
        return 1;
    }

    @Override
    public String read_mock(String _MockId) throws IOException {
        File mock = new File(_MockId);
        if (!mock.exists())
            throw new ServiceExcept("mockId不存在，无法找到指定的mock");
        FileReader fr = new FileReader(mock);
        BufferedReader br = new BufferedReader(fr);
        char buffer[] = new char[1024];
        StringBuilder data = new StringBuilder();
        int offset = 0;
        while (-1 != (offset = br.read(buffer,0,1024)))
        {
            char b[] = Arrays.copyOfRange(buffer,0,offset);
            data.append(b);
        }
        return data.toString();
    }

    public static Unsafe getUnsafe() throws IllegalAccessException, NoSuchFieldException
    {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        //Field unsafeField = Unsafe.class.getDeclaredFields()[0]; //也可以这样，作用相同
        unsafeField.setAccessible(true);
        Unsafe unsafe =(Unsafe) unsafeField.get(null);
        return unsafe;
    }

}
