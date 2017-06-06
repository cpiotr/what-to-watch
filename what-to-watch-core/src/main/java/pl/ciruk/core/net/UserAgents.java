package pl.ciruk.core.net;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UserAgents {
    private static final List<String> AGENTS = ImmutableList.of(
            "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.1.01 Safari/538.1",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.0.01 Safari/538.1",
            "Mozilla/5.0 (X11; Linux i686) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.2.01-dev Safari/538.1",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.3.01-dev Safari/538.1",
            "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.4.01 Safari/538.1",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.9.03 beta 3 Safari/538.1",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/538.1 (KHTML, like Gecko) Otter/0.9.05",
            "Opera/5.11 (Windows 98; U) [en]",
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98) Opera 5.12 [en]",
            "Opera/6.0 (Windows 2000; U) en]",
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 4.0) Opera 6.01 [en]",
            "Opera/7.03 (Windows NT 5.0; U) [en]",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1) Opera 7.10 [en]",
            "Opera/9.02 (Windows XP; U; en)",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 9.24",
            "Opera/9.51 (Macintosh; Intel Mac OS X; U; en)",
            "Opera/9.70 (Linux i686 ; U; en) Presto/2.2.1",
            "Opera/9.80 (Windows NT 5.1; U; en) Presto/2.2.15 Version/10.00",
            "Opera/9.80 (Windows NT 6.1; U; en) Presto/2.7.62 Version/11.01",
            "Opera/9.80 (Windows NT 6.1; U; en-GB) Presto/2.7.62 Version/11.00",
            "Opera/9.80 (Windows NT 6.1; U; en-US) Presto/2.7.62 Version/11.01",
            "Opera/9.80 (Windows NT 6.0; U; en) Presto/2.8.99 Version/11.10",
            "Opera/9.80 (X11; Linux i686; U; en) Presto/2.8.131 Version/11.11",
            "Opera/9.80 (Windows NT 5.1) Presto/2.12.388 Version/12.14",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.12 Safari/537.36 OPR/14.0.1116.4",
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36 OPR/15.0.1147.24 (Edition Next)",
            "Opera/9.80 (Linux armv6l ; U; CE-HTML/1.0 NETTV/3.0.1;; en) Presto/2.6.33 Version/10.60",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.154 Safari/537.36 OPR/20.0.1387.91",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Oupeng/10.2.1.86910 Safari/534.30",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36 OPR/26.0.1656.60",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36 OPR/32.0.1948.25"
    );

    private static final AtomicInteger index = new AtomicInteger(0);

    public static String next() {
        return AGENTS.get(index.incrementAndGet() % AGENTS.size());
    }
}
