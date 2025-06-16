package com.ruoyi.common.enums;

public enum ErrorCode
{
    /** 成功 */
    SUCCESS(0, "Success"),
    /** 警告 */
    WARN(301, "Warning"),
    /** 错误 */
    ERROR(500, "Server Error"),
    NOT_FOUND_FILE(501, "File Not Found"),
    LOST_MOCK_ID(502, "Mock ID Missing"),
    MOCK_WRITE_FAIL(503, "Failed to Write Mock File"),
    DEL_FILE_FAIL(504, "Failed to Delete File"),
    DOWNLOAD_FAIL(505, "File Download Failed"),
    FILE_NOT_EXISTS(506, "File Does Not Exist"),
    USER_NOT_EMPTY(507, "User Cannot Be Empty"),
    FILE_OFFSET_IS_NULL_ERR(508, "File Offset Cannot Be Null"),
    FILE_OFFSET_LESS_THAN_ZERO_ERR(509, "File Offset Cannot Be Less Than Zero"),
    FILE_OFFSET_MORE_THAN_FILE_SIZE_ERR(510, "File Offset Exceeds File Size"),
    NOT_A_DIR(511, "Is not a directory"),
    FILE_OR_DIR_HAS_EXISTS(512, "file or directory has exists"),
    NOT_COMPLETELY_DELETED(513, "Not completely deleted");
    private final int code;
    private final String msg;

    ErrorCode(int code, String _msg)
    {
        this.code = code;
        this.msg = _msg;
    }

    public int code()
    {
        return this.code;
    }

    public String what()
    {
        return this.msg;
    }
}
