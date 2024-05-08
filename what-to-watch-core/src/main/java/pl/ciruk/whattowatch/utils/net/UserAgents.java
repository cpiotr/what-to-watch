package pl.ciruk.whattowatch.utils.net;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("PMD.ClassNamingConventions")
final class UserAgents {
    private static final List<String> AGENTS = List.of(
            "Mozilla/5.0 (U; Linux i541 x86_64) AppleWebKit/535.49 (KHTML, like Gecko) Chrome/54.0.3684.195 Safari/600",
            "Mozilla/5.0 (U; Linux i643 ) AppleWebKit/535.26 (KHTML, like Gecko) Chrome/55.0.3875.331 Safari/537",
            "Mozilla/5.0 (Windows; U; Windows NT 10.0; x64) AppleWebKit/535.18 (KHTML, like Gecko) Chrome/53.0.2675.323 Safari/602",
            "Mozilla/5.0 (Linux; Linux i586 ) AppleWebKit/602.7 (KHTML, like Gecko) Chrome/47.0.3251.176 Safari/536",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_5; en-US) AppleWebKit/534.28 (KHTML, like Gecko) Chrome/47.0.1848.173 Safari/600",
            "Mozilla/5.0 (Linux; U; Linux i572 ) AppleWebKit/601.42 (KHTML, like Gecko) Chrome/52.0.3638.315 Safari/603",
            "Mozilla/5.0 (Windows NT 10.1;; en-US) AppleWebKit/603.16 (KHTML, like Gecko) Chrome/49.0.2582.195 Safari/603",
            "Mozilla/5.0 (Windows NT 10.5; x64; en-US) AppleWebKit/601.25 (KHTML, like Gecko) Chrome/47.0.1833.397 Safari/601",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_6; en-US) AppleWebKit/601.39 (KHTML, like Gecko) Chrome/53.0.3840.237 Safari/601",
            "Mozilla/5.0 (Windows; Windows NT 10.2;) AppleWebKit/537.33 (KHTML, like Gecko) Chrome/55.0.1498.366 Safari/534",
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; x64) AppleWebKit/534.32 (KHTML, like Gecko) Chrome/50.0.1600.133 Safari/535",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_4_8) AppleWebKit/534.9 (KHTML, like Gecko) Chrome/49.0.1693.113 Safari/533",
            "Mozilla/5.0 (U; Linux i656 ; en-US) AppleWebKit/533.7 (KHTML, like Gecko) Chrome/48.0.1618.112 Safari/533",
            "Mozilla/5.0 (Windows NT 6.0; WOW64; en-US) AppleWebKit/602.31 (KHTML, like Gecko) Chrome/52.0.1777.309 Safari/535",
            "Mozilla/5.0 (Windows; Windows NT 10.2; WOW64; en-US) AppleWebKit/533.44 (KHTML, like Gecko) Chrome/55.0.1901.227 Safari/533",
            "Mozilla/5.0 (U; Linux x86_64) AppleWebKit/602.29 (KHTML, like Gecko) Chrome/50.0.3088.345 Safari/602",
            "Mozilla/5.0 (Windows NT 6.2;) AppleWebKit/602.36 (KHTML, like Gecko) Chrome/53.0.2060.199 Safari/600",
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/600.40 (KHTML, like Gecko) Chrome/54.0.2168.303 Safari/601",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 9_0_8) AppleWebKit/533.31 (KHTML, like Gecko) Chrome/49.0.2931.260 Safari/602",
            "Mozilla/5.0 (Windows; Windows NT 6.1; Win64; x64; en-US) AppleWebKit/534.42 (KHTML, like Gecko) Chrome/48.0.1010.388 Safari/600",
            "Mozilla/5.0 (Windows; Windows NT 6.1; Win64; x64; en-US) AppleWebKit/535.35 (KHTML, like Gecko) Chrome/49.0.1762.312 Safari/535",
            "Mozilla/5.0 (Windows; Windows NT 6.2;) AppleWebKit/600.40 (KHTML, like Gecko) Chrome/54.0.1191.103 Safari/534",
            "Mozilla/5.0 (Windows NT 10.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.3937.227 Safari/536",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_5_9) AppleWebKit/600.31 (KHTML, like Gecko) Chrome/50.0.2969.335 Safari/533",
            "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.14 (KHTML, like Gecko) Chrome/55.0.2035.273 Safari/533",
            "Mozilla/5.0 (Windows NT 6.0;) AppleWebKit/603.39 (KHTML, like Gecko) Chrome/51.0.2399.133 Safari/600",
            "Mozilla/5.0 (Windows; U; Windows NT 6.3; x64; en-US) AppleWebKit/533.24 (KHTML, like Gecko) Chrome/47.0.2150.194 Safari/533",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_2_6) AppleWebKit/601.15 (KHTML, like Gecko) Chrome/48.0.1567.254 Safari/602",
            "Mozilla/5.0 (Windows; Windows NT 10.3; WOW64) AppleWebKit/601.7 (KHTML, like Gecko) Chrome/53.0.2268.317 Safari/602",
            "Mozilla/5.0 (Linux x86_64) AppleWebKit/537.27 (KHTML, like Gecko) Chrome/55.0.2995.192 Safari/601",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 8_3_3; en-US) AppleWebKit/533.16 (KHTML, like Gecko) Chrome/55.0.1981.147 Safari/600",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_4_2) AppleWebKit/600.44 (KHTML, like Gecko) Chrome/54.0.2446.384 Safari/537",
            "Mozilla/5.0 (Linux x86_64; en-US) AppleWebKit/536.13 (KHTML, like Gecko) Chrome/54.0.2822.392 Safari/537",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_0_1; en-US) AppleWebKit/534.25 (KHTML, like Gecko) Chrome/55.0.3110.202 Safari/536",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_2) AppleWebKit/600.28 (KHTML, like Gecko) Chrome/52.0.1442.236 Safari/535",
            "Mozilla/5.0 (Linux; Linux i671 x86_64; en-US) AppleWebKit/600.35 (KHTML, like Gecko) Chrome/48.0.3246.350 Safari/534",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 7_6_6; en-US) AppleWebKit/603.34 (KHTML, like Gecko) Chrome/48.0.1214.285 Safari/603",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 7_1_7; en-US) AppleWebKit/602.29 (KHTML, like Gecko) Chrome/51.0.2024.147 Safari/602",
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; WOW64; en-US) AppleWebKit/536.2 (KHTML, like Gecko) Chrome/48.0.3814.163 Safari/533",
            "Mozilla/5.0 (Windows NT 6.2;) AppleWebKit/600.7 (KHTML, like Gecko) Chrome/47.0.1085.375 Safari/536",
            "Mozilla/5.0 (Windows; Windows NT 6.0;; en-US) AppleWebKit/602.13 (KHTML, like Gecko) Chrome/55.0.3213.109 Safari/534",
            "Mozilla/5.0 (Windows; U; Windows NT 10.0; x64; en-US) AppleWebKit/603.49 (KHTML, like Gecko) Chrome/50.0.2087.384 Safari/600",
            "Mozilla/5.0 (Linux; Linux i574 x86_64) AppleWebKit/533.46 (KHTML, like Gecko) Chrome/48.0.2598.297 Safari/601",
            "Mozilla/5.0 (Windows NT 6.0; WOW64; en-US) AppleWebKit/600.21 (KHTML, like Gecko) Chrome/49.0.1812.156 Safari/535",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/49.0.3523.343 Safari/600",
            "Mozilla/5.0 (Linux; U; Linux i640 ; en-US) AppleWebKit/534.27 (KHTML, like Gecko) Chrome/49.0.1526.299 Safari/535",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 9_5_9) AppleWebKit/534.26 (KHTML, like Gecko) Chrome/52.0.1953.319 Safari/600",
            "Mozilla/5.0 (Linux x86_64; en-US) AppleWebKit/602.36 (KHTML, like Gecko) Chrome/48.0.3090.338 Safari/603",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 7_1_6) AppleWebKit/534.31 (KHTML, like Gecko) Chrome/53.0.1832.328 Safari/600",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 7_2_7; en-US) AppleWebKit/537.47 (KHTML, like Gecko) Chrome/51.0.1023.361 Safari/600"
    );

    private static final AtomicInteger INDEX = new AtomicInteger(0);

    private UserAgents() {
        throw new AssertionError();
    }

    static String next() {
        return AGENTS.get(INDEX.incrementAndGet() % AGENTS.size());
    }
}
