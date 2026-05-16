package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyGubaClient;
import com.quant.platform.business.client.dto.GubaIndexSeriesDTO;
import com.quant.platform.business.client.dto.GubaPostDTO;
import com.quant.platform.business.client.dto.GubaPostDetailDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EastmoneyGubaController {

    @Autowired
    private EastmoneyGubaClient eastmoneyGubaClient;

    @GetMapping("/listPosts")
    public Result<List<GubaPostDTO>> listPosts() {
        return Result.ok(eastmoneyGubaClient.listPosts("000001", 1));
    }

    @GetMapping("/fetchPostDetail")
    public Result<GubaPostDetailDTO> fetchPostDetail() {
        return Result.ok(eastmoneyGubaClient.fetchPostDetail("000001", "1692726424"));
    }

    @GetMapping("/fetchIndexSeries")
    public Result<GubaIndexSeriesDTO> fetchIndexSeries() {
        GubaIndexSeriesDTO gubaIndexSeriesDTO = eastmoneyGubaClient.fetchIndexSeries(1);
        return Result.ok(gubaIndexSeriesDTO);
    }

}
