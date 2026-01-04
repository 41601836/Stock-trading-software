package com.stock.analysis.module.opinion;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/opinion")
@RequiredArgsConstructor
public class OpinionController {

    private final OpinionAnalysisService opinionAnalysisService;

    /**
     * 获取舆情聚合摘要
     * @param stockCode 股票代码
     * @param days 统计天数 (默认3天)
     */
    @GetMapping("/summary")
    public Result<OpinionSummary> getOpinionSummary(
            @RequestParam String stockCode,
            @RequestParam(defaultValue = "3") int days) {
        OpinionSummary summary = opinionAnalysisService.getOpinionSummary(stockCode, days);
        return Result.success(summary);
    }

    /**
     * 提交单条舆情 (用于测试双层过滤和热点检测)
     */
    @PostMapping("/submit")
    public Result<PublicOpinion> submitOpinion(@RequestBody PublicOpinion opinion) {
        PublicOpinion processed = opinionAnalysisService.processOpinion(opinion);
        if (processed == null) {
            return Result.error(400, "舆情内容被识别为广告或无效信息");
        }
        return Result.success(processed);
    }

    /**
     * 模拟批量导入 (用于测试热点激增)
     */
    @PostMapping("/mock-import")
    public Result<String> mockImport(
            @RequestParam String stockCode,
            @RequestParam int count) {
        opinionAnalysisService.mockImportOpinions(stockCode, count);
        return Result.success("成功导入 " + count + " 条模拟数据");
    }
}
