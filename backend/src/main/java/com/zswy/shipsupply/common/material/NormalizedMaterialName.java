package com.zswy.shipsupply.common.material;

import java.util.List;

public record NormalizedMaterialName(
    String cleanName,
    String coreName,
    List<MaterialNameAttribute> attributes,
    List<String> nameTokens,
    List<String> specTokens,
    List<String> riskFlags
) {
}
