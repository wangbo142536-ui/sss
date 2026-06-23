package com.zswy.shipsupply.system.dictionary;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/types")
    public List<DictionaryTypeResponse> types(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "enabled", required = false) Boolean enabled
    ) {
        return dictionaryService.listTypes(keyword, enabled);
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryTypeResponse createType(@RequestBody DictionaryTypeSaveRequest request) {
        return dictionaryService.createType(request);
    }

    @PutMapping("/types/{typeCode}")
    public DictionaryTypeResponse updateType(
        @PathVariable String typeCode,
        @RequestBody DictionaryTypeSaveRequest request
    ) {
        return dictionaryService.updateType(typeCode, request);
    }

    @DeleteMapping("/types/{typeCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteType(@PathVariable String typeCode) {
        dictionaryService.deleteType(typeCode);
    }

    @GetMapping("/items")
    public List<DictionaryItemResponse> items(
        @RequestParam(value = "typeCode", required = false) String typeCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "enabled", required = false) Boolean enabled
    ) {
        return dictionaryService.listItems(typeCode, keyword, enabled);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryItemResponse createItem(@RequestBody DictionaryItemSaveRequest request) {
        return dictionaryService.createItem(request);
    }

    @PutMapping("/items/{itemId}")
    public DictionaryItemResponse updateItem(
        @PathVariable Long itemId,
        @RequestBody DictionaryItemSaveRequest request
    ) {
        return dictionaryService.updateItem(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long itemId) {
        dictionaryService.deleteItem(itemId);
    }
}
