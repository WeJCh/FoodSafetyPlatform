package com.mortal.regulation.vo;

import lombok.Data;

/**
 * 文件预签名响应VO
 */
@Data
public class FilePresignVO {

    private String uploadUrl;
    private String fileUrl;
    private String objectKey;
}
