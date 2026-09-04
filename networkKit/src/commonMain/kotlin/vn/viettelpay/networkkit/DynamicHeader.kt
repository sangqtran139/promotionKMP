package vn.viettelpay.networkkit

/**
 * Header phải tính lại giá trị mỗi request (vd `X-Request-ID`: UUID mới mỗi lần) — khác header tĩnh
 * trong [NetworkClientConfig.headers], vốn giữ nguyên một giá trị suốt vòng đời client.
 */
public class DynamicHeader(
    public val name: String,
    private val provideValue: () -> String,
) {
    public fun currentValue(): String = provideValue()
}
