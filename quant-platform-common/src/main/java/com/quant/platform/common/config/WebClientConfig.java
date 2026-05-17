package com.quant.platform.common.config;

import com.google.common.collect.Lists;
import com.quant.platform.common.config.endpoints.*;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    private static final List<Map<String, String>> LIST = Lists.newArrayList();

    static {
        LIST.add(Map.of("user-agent",  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36 Edg/147.0.0.0", "cookie", "qgqp_b_id=7dae677ca9b8d024796b407c6aad04c0; st_nvi=uCUeNhs_2OqwcbWful1HS905f; nid18=0b0eb9f0d189fb3f741f40affe6d7195; nid18_create_time=1777076013420; gviem=sptyuWHOag9l3WawghznZ12a3; gviem_create_time=1777076013420; fullscreengg=1; fullscreengg2=1; st_si=77099612139390; st_asi=delete; st_pvi=29781748017840; st_sp=2026-04-25%2008%3A13%3A32; st_inirUrl=https%3A%2F%2Fwww.bing.com%2F; st_sn=2; st_psi=20260506201336338-117001350220-2151880913"));
        LIST.add(Map.of("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0", "cookie", "st_nvi=4dTg9bJG4ofDwVfJi8EGE7f24; qgqp_b_id=c4fcb558edf4549952fe18e91aa34086; nid18=0615ea8f58d0830fd45d0a6116deb67a; nid18_create_time=1776087678723; gviem=4YkxOOPeTAck5BiklQevw77b9; gviem_create_time=1776087678723; st_si=91938686989023; st_sn=12; st_psi=20260506202758941-117001356556-3945278801; st_asi=delete; fullscreengg=1; fullscreengg2=1; wsc_checkuser_ok=1; st_pvi=69968218841521; st_sp=2026-04-13%2021%3A41%3A18; st_inirUrl=https%3A%2F%2Fmguba.eastmoney.com%2F"));
        LIST.add(Map.of("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36 Edg/147.0.0.0", "cookie", "qgqp_b_id=7dae677ca9b8d024796b407c6aad04c0; st_nvi=qBhqEaiqZj-Uy3MTbXJ7a25de; nid18=0b0eb9f0d189fb3f741f40affe6d7195; nid18_create_time=1777553742022; gviem=IWQmaRozt-QkKQPlELFHEf6cb; gviem_create_time=1777553742022; fullscreengg=1; fullscreengg2=1; st_si=34400741560827; st_asi=delete; wsc_checkuser_ok=1; st_pvi=02436830875696; st_sp=2026-04-30%2020%3A55%3A41; st_inirUrl=https%3A%2F%2Fguba.eastmoney.com%2F; st_sn=3; st_psi=20260506195145838-117001354293-8458229896"));
    }



    /**
     * 记录即将发出的完整请求 URL（含 baseUrl、路径与查询串），便于排查对接问题。
     */
    private static ExchangeFilterFunction logFinalRequestUrl() {
        return (request, next) -> {
            log.info("WebClient {} {}", request.method(), request.url());
            return next.exchange(request);
        };
    }

    @Bean
    public WebClient.Builder commonWebClientBuilder() {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build();

        return WebClient.builder().filter(logFinalRequestUrl()).clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
//            .defaultHeader(HttpHeaders.USER_AGENT,
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36 Edg/147.0.0.0")
            ;
    }

    @Bean
    @Qualifier("eastmoneyWebClient")
    public WebClient eastmoneyWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney").maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient eastmoneyHttpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(eastmoneyHttpClient))
                .baseUrl(EastmoneyEndpoints.BASE_PUSH2)
                .defaultHeader(HttpHeaders.REFERER, "https://quote.eastmoney.com/")
                .defaultHeader(HttpHeaders.COOKIE,
                        "qgqp_b_id=7dae677ca9b8d024796b407c6aad04c0; st_nvi=uCUeNhs_2OqwcbWful1HS905f; nid18=0b0eb9f0d189fb3f741f40affe6d7195; nid18_create_time=1777076013420; gviem=sptyuWHOag9l3WawghznZ12a3; gviem_create_time=1777076013420; websitepoptg_api_time=1777383855902; st_si=26231410622211; fullscreengg=1; fullscreengg2=1; st_pvi=29781748017840; st_sp=2026-04-25%2008%3A13%3A32; st_inirUrl=https%3A%2F%2Fwww.bing.com%2F; st_sn=8; st_psi=20260429192311719-117001356556-4376538054; st_asi=delete")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 股吧 HTML：{@link EastmoneyGubaEndpoints#BASE_GUBA}
     */
    @Bean
    @Qualifier("eastmoneyGubaWebClient")
    public WebClient eastmoneyGubaWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-guba").maxConnections(10)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneyGubaEndpoints.BASE_GUBA)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE + "," + MediaType.ALL_VALUE)
            .defaultHeader(HttpHeaders.COOKIE, "qgqp_b_id=7dae677ca9b8d024796b407c6aad04c0; st_nvi=d3S5IGSMm_tapenrtmKUl66fc; nid18=0b0eb9f0d189fb3f741f40affe6d7195; nid18_create_time=1776175061015; gviem=LTZcV95CwK0BnD0VDKB08db84; gviem_create_time=1776175061015; fullscreengg=1; fullscreengg2=1; st_si=39746007156656; st_pvi=88758591172828; st_sp=2026-04-14%2021%3A57%3A40; st_inirUrl=https%3A%2F%2Fmguba.eastmoney.com%2F; st_sn=3; st_psi=20260415195545395-117001356556-8493777826; st_asi=delete")
                .defaultHeader(HttpHeaders.REFERER, EastmoneyGubaEndpoints.BASE_GUBA + "/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * K 线专用：{@link EastmoneyEndpoints#BASE_PUSH2HIS}，与列表/快照
     * {@link EastmoneyEndpoints#BASE_PUSH2} 分离。
     */
    @Bean
    @Qualifier("eastmoneyKlineWebClient")
    public WebClient eastmoneyKlineWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-kline").maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneyEndpoints.BASE_PUSH2HIS)
                .defaultHeader(HttpHeaders.REFERER, "https://quote.eastmoney.com/")
                .defaultHeader(HttpHeaders.USER_AGENT, LIST.get(1).get("user-agent"))
                .defaultHeader(HttpHeaders.COOKIE, LIST.get(1).get("cookie"))
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    @Bean
    @Qualifier("eastmoneyNoticeWebClient")
    public WebClient eastmoneyNoticeWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-notice").maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneyNoticeEndpoints.BASE_NOTICE)
                .defaultHeader(HttpHeaders.REFERER, "https://data.eastmoney.com/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 研报 API：{@link EastmoneyResearchReportEndpoints#BASE_REPORT_API}
     */
    @Bean
    @Qualifier("eastmoneyReportWebClient")
    public WebClient eastmoneyReportWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-report").maxConnections(10)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneyResearchReportEndpoints.BASE_REPORT_API)
                .defaultHeader(HttpHeaders.REFERER, "https://data.eastmoney.com/report/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 淘股吧 PC 页（/quotes/、/a/），HTML 与内嵌 JSON；需与浏览器近似的 UA/Referer。
     */
    @Bean
    @Qualifier("tgbWebClient")
    public WebClient tgbWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("tgb").maxConnections(10)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(25)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(25, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(25, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://www.tgb.cn").defaultHeader(HttpHeaders.REFERER, "https://www.tgb.cn/quotes/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 数据中心财务报表等：{@link EastmoneyFinancialStatementEndpoints#BASE_DATACENTER_WEB}
     */
    @Bean
    @Qualifier("eastmoneyDatacenterWebClient")
    public WebClient eastmoneyDatacenterWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-datacenter").maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneyFinancialStatementEndpoints.BASE_DATACENTER_WEB)
                .defaultHeader(HttpHeaders.REFERER, "https://data.eastmoney.com/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 东财 securities 数据中心（F10 利润表等）：{@link EastmoneySecuritiesEndpoints#BASE_DATACENTER_SECURITIES}
     */
    @Bean
    @Qualifier("eastmoneySecuritiesDatacenterWebClient")
    public WebClient eastmoneySecuritiesDatacenterWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("eastmoney-securities-datacenter").maxConnections(20)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(EastmoneySecuritiesEndpoints.BASE_DATACENTER_SECURITIES)
                .defaultHeader(HttpHeaders.REFERER, "https://emweb.securities.eastmoney.com/")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 同花顺 basic（个股财务页 JSON），需带个股 Referer。
     */
    @Bean
    @Qualifier("thsBasicWebClient")
    public WebClient thsBasicWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("ths-basic").maxConnections(10)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(ThsBasicEndpoints.BASE_BASIC_10JQKA)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }

    /**
     * 同花顺 dq「扶摇」市场分析图表（{@link ThsDqFuyaoEndpoints#BASE_DQ_10JQKA}），如全市场成交额分时。
     */
    @Bean
    @Qualifier("thsDqFuyaoWebClient")
    public WebClient thsDqFuyaoWebClient(WebClient.Builder commonWebClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("ths-dq-fuyao").maxConnections(10)
                .pendingAcquireTimeout(Duration.ofSeconds(10)).build();

        HttpClient httpClient = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15)).keepAlive(false)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));

        return commonWebClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(ThsDqFuyaoEndpoints.BASE_DQ_10JQKA)
                .defaultHeader(HttpHeaders.REFERER, "https://www.10jqka.com.cn/")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.CONNECTION, "close").build();
    }
}
