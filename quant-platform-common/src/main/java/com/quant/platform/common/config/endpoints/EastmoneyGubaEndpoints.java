package com.quant.platform.common.config.endpoints;

/**
 * 东方财富股吧（guba.eastmoney.com）端点与路径模板。
 * <p>
 * 注意：股吧以 HTML 页面为主，结构可能变动；本模块提供「尽力解析」的帖子/评论获取能力，供舆情与研究辅助使用。
 */
public final class EastmoneyGubaEndpoints {

    public static final String BASE_GUBA = "https://guba.eastmoney.com";

    /**
     * 帖子列表页：{@code /list,{secCode}_{page}.html}
     */
    public static String listPagePath(String secCode, int page) {
        return "/list," + secCode + "_" + page + ".html";
    }

    /**
     * 帖子详情页：{@code /news,{secCode},{postId}.html}
     */
    public static String postDetailPath(String secCode, String postId) {
        return "/news," + secCode + "," + postId + ".html";
    }

    private EastmoneyGubaEndpoints() {
    }
}

