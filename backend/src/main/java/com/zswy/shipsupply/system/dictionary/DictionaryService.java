package com.zswy.shipsupply.system.dictionary;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DictionaryService {

    private final DictionaryRepository dictionaryRepository;

    public DictionaryService(DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    public List<DictionaryTypeResponse> listTypes(String keyword, Boolean enabled) {
        return dictionaryRepository.listTypes(keyword, enabled);
    }

    public List<DictionaryItemResponse> listItems(String typeCode, String keyword, Boolean enabled) {
        return dictionaryRepository.listItems(typeCode, keyword, enabled);
    }

    @Transactional
    public DictionaryTypeResponse createType(DictionaryTypeSaveRequest request) {
        validateType(request, true);
        String typeCode = normalizeCode(request.typeCode());
        if (dictionaryRepository.typeExists(typeCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DICTIONARY_TYPE_EXISTS");
        }
        return dictionaryRepository.createType(request);
    }

    @Transactional
    public DictionaryTypeResponse updateType(String typeCode, DictionaryTypeSaveRequest request) {
        validateType(request, false);
        DictionaryTypeResponse updated = dictionaryRepository.updateType(typeCode, request);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DICTIONARY_TYPE_NOT_FOUND");
        }
        return updated;
    }

    @Transactional
    public void deleteType(String typeCode) {
        if (!dictionaryRepository.typeExists(typeCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DICTIONARY_TYPE_NOT_FOUND");
        }
        dictionaryRepository.disableType(typeCode);
    }

    @Transactional
    public DictionaryItemResponse createItem(DictionaryItemSaveRequest request) {
        validateItem(request);
        if (!dictionaryRepository.typeExists(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_NOT_FOUND");
        }
        if (dictionaryRepository.itemCodeExists(request.typeCode(), request.itemCode(), null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DICTIONARY_ITEM_EXISTS");
        }
        return dictionaryRepository.createItem(request);
    }

    @Transactional
    public DictionaryItemResponse updateItem(Long itemId, DictionaryItemSaveRequest request) {
        validateItem(request);
        if (!dictionaryRepository.typeExists(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_NOT_FOUND");
        }
        if (dictionaryRepository.itemCodeExists(request.typeCode(), request.itemCode(), itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DICTIONARY_ITEM_EXISTS");
        }
        DictionaryItemResponse updated = dictionaryRepository.updateItem(itemId, request);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DICTIONARY_ITEM_NOT_FOUND");
        }
        return updated;
    }

    @Transactional
    public void deleteItem(Long itemId) {
        dictionaryRepository.disableItem(itemId);
    }

    private void validateType(DictionaryTypeSaveRequest request, boolean requireCode) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_REQUIRED");
        }
        if (requireCode && isBlank(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_CODE_REQUIRED");
        }
        if (isBlank(request.typeName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_NAME_REQUIRED");
        }
    }

    private void validateItem(DictionaryItemSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_ITEM_REQUIRED");
        }
        if (isBlank(request.typeCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_TYPE_CODE_REQUIRED");
        }
        if (isBlank(request.itemCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_ITEM_CODE_REQUIRED");
        }
        if (isBlank(request.itemName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DICTIONARY_ITEM_NAME_REQUIRED");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
