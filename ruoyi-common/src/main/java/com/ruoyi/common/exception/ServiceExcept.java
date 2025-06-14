package com.ruoyi.common.exception;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.ErrorCode;

/**
 * 业务异常
 * 只是因为我喜欢这么写，内部逻辑与ServiceException完全一致
 *
 * @author gore
 */
public final class ServiceExcept extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 错误提示
     */
//    private String message;
    private ErrorCode type;
    private int code;

    /**
     * 错误明细，内部调试错误
     *
     * 和 {@link CommonResult#getDetailMessage()} 一致的设计
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceExcept()
    {
    }

//    public ServiceExcept(AjaxResult.Type type, String message)
//    {
//        super(message);
//        this.code = type.value();
////        this.message = message;
//    }

    public ServiceExcept(ErrorCode _Code, String message)
    {
        super(message);
        this.code = _Code.code();
//        this.message = message;
    }

    public ServiceExcept(String message)
    {
        super(message);
    }

    public String getDetailMessage()
    {
        return detailMessage;
    }

    public ServiceExcept setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage()
    {
        return super.getMessage();
    }

//    public ServiceExcept setMessage(String message)
//    {
//        ServiceExcept(message);
//        this.message = message;
//        return this;
//    }

//    public ErrorCode getType() {
//        return type;
//    }
//
//    public void setType(AjaxResult.Type type) {
//        this.type = type;
//    }


    public int getCode() {
        return code;
    }
}