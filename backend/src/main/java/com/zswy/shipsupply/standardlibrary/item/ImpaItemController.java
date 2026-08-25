package com.zswy.shipsupply.standardlibrary.item;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/standard-library/impa/items")
public class ImpaItemController {

    private final ImpaItemService impaItemService;

    public ImpaItemController(ImpaItemService impaItemService) {
        this.impaItemService = impaItemService;
    }

    @GetMapping(params = "!page")
    public List<ImpaItemResponse> listItems(
        @RequestParam(required = false) String categoryCode,
        @RequestParam(required = false) String segmentCode,
        @RequestParam(required = false) String keyword,
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return impaItemService.listItems(categoryCode, segmentCode, chooseKeyword(keyword, q), limit);
    }

    @GetMapping(params = "page")
    public ImpaItemPageResponse listItemPage(
        @RequestParam(required = false) String categoryCode,
        @RequestParam(required = false) String segmentCode,
        @RequestParam(required = false) String keyword,
        @RequestParam(name = "q", required = false) String q,
        @RequestParam int page,
        @RequestParam(defaultValue = "50") int pageSize
    ) {
        return impaItemService.listItemPage(categoryCode, segmentCode, chooseKeyword(keyword, q), page, pageSize);
    }

    private String chooseKeyword(String keyword, String q) {
        if (keyword != null && !keyword.isBlank()) {
            return keyword;
        }
        return q;
    }
}
