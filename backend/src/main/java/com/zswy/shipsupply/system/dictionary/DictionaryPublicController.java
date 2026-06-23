package com.zswy.shipsupply.system.dictionary;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionaries")
public class DictionaryPublicController {

    private final DictionaryService dictionaryService;

    public DictionaryPublicController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/items")
    public List<DictionaryItemResponse> items(@RequestParam("typeCode") String typeCode) {
        return dictionaryService.listItems(typeCode, null, true);
    }
}
