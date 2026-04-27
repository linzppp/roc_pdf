package org.roc.practice.pdf.storage;

/**
 * 存储服务抽象，便于替换底层实现（MinIO / OSS / 本地磁盘等）
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param objectKey 存储路径，如 2026/04/20/{taskId}.pdf
     * @param data      文件字节
     */
    void upload(String objectKey, byte[] data);

    /**
     * 构建文件公开访问 URL
     *
     * @param objectKey 存储路径
     * @return 完整 URL
     */
    String buildUrl(String objectKey);
}
