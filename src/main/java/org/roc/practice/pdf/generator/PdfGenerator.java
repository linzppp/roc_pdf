package org.roc.practice.pdf.generator;

/**
 * PDF 生成器接口，每种 PDF 类型对应一个实现类并注册为 Spring Bean。
 *
 * <p>新增 PDF 类型时只需：
 * <ol>
 *   <li>创建对应 DTO（继承 PdfRequest）</li>
 *   <li>实现本接口并标注 @Component</li>
 *   <li>在外部挂载目录放入对应模板文件</li>
 * </ol>
 * 无需修改任何已有代码（开闭原则）。
 *
 * @param <T> 请求 DTO 类型
 */
public interface PdfGenerator<T> {

    /** 类型标识，与 POST /pdf/{type} 中的 type 对应，如 "invoice" */
    String type();

    /** DTO 的 Class，用于 JSON 反序列化 */
    Class<T> requestType();

    /**
     * 生成 PDF 字节数组
     *
     * @param request 已反序列化的请求对象
     * @return PDF 字节
     */
    byte[] generate(T request);
}
